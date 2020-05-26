package com.example.optiflow;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.intel.realsense.librealsense.Align;
import com.intel.realsense.librealsense.Colorizer;
import com.intel.realsense.librealsense.Config;
import com.intel.realsense.librealsense.DecimationFilter;
import com.intel.realsense.librealsense.DepthFrame;
import com.intel.realsense.librealsense.Device;
import com.intel.realsense.librealsense.DeviceList;
import com.intel.realsense.librealsense.DeviceListener;
import com.intel.realsense.librealsense.DisparityTransformFilter;
import com.intel.realsense.librealsense.Extension;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.HoleFillingFilter;
import com.intel.realsense.librealsense.Option;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.RsContext;
import com.intel.realsense.librealsense.Sensor;
import com.intel.realsense.librealsense.SpatialFilter;
import com.intel.realsense.librealsense.StreamFormat;
import com.intel.realsense.librealsense.StreamType;
import com.intel.realsense.librealsense.TemporalFilter;
import com.intel.realsense.librealsense.ThresholdFilter;
import com.intel.realsense.librealsense.VideoFrame;
import com.scichart.charting3d.common.math.Point3D;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class RealSenseVideo {

    private static final String TAG = "librs capture example";
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;
    private final Handler mHandler = new Handler();
    Activity activity;
    private RsContext mRsContext;
    private Align mAlign;
    private Pipeline mPipeline;
    private Colorizer mColorizer;
    private DecimationFilter mDecimationFilter;
    private HoleFillingFilter mHoleFillingFilter;
    private DisparityTransformFilter mDisparity;
    private ThresholdFilter mThresholdFilter;
    private TemporalFilter mTemporalFilter;
    private SpatialFilter mSpatialFilter;
    private boolean mIsStreaming;
    private GLRsSurfaceView mGLSurfaceView;
    private Mat mat;
    private DepthFrame prevDepthMat;
    private DepthFrame latestDepthMat;
    private Point[] prevPoints;
    private Point[] latestPoints;
    private DepthFlowModel depthFlow;
    private Optiflowmodel colorFlow;
    private SendAcc networking;
    FileWriter writer;
    private LineGraphXYX changeCamera;
    private float[] posCameraVect = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;


    Runnable mStreaming = new Runnable() {
        AtomicInteger frameCount = new AtomicInteger();

        @Override
        public void run() {

            //Recommended filter order from intel
            //Depth Frame >> Decimation Filter >> Depth2Disparity Transform** ->
            // Spatial Filter >> Temporal Filter >> Disparity2Depth Transform** >>
            // Hole Filling Filter >> Filtered Depth. <br/>

            try {
                try (FrameSet frames = mPipeline.waitForFrames()) {
                    try (FrameSet processed = frames.applyFilter(mDecimationFilter)) {
                        try (FrameSet processed1 = processed.applyFilter(mDisparity)) {
                            try (FrameSet processed2 = processed1.applyFilter(mSpatialFilter)) {
                                try (FrameSet processedDisparity = processed2.applyFilter(mTemporalFilter)) {
                                    try (FrameSet processed3 = processedDisparity.applyFilter(mDisparity)) {
                                        try (FrameSet processed4 = processed3.applyFilter(mHoleFillingFilter)) {
                                            try (FrameSet processed5 = processed4.applyFilter(mColorizer)) {
                                                try (FrameSet processed6 = processed5.applyFilter(mAlign)) {

                                                    processed6.foreach(inputFrame -> {

                                                        if (inputFrame.is(Extension.DEPTH_FRAME)) {

                                                            if (latestDepthMat != null) {
                                                                prevDepthMat = latestDepthMat;
                                                            }

                                                            latestDepthMat = inputFrame.as(Extension.DEPTH_FRAME);

                                                            if (prevDepthMat != null) {

                                                                StringBuilder stringBuilder = new StringBuilder();
                                                                ArrayList<Vector> vectors = new ArrayList<>();

                                                                for (int i = 0; i < prevPoints.length; i++) {

                                                                    if (prevPoints[i].x >= 0 && prevPoints[i].x <= latestDepthMat.getWidth() &&
                                                                            latestPoints[i].x >= 0 && latestPoints[i].x <= latestDepthMat.getWidth() &&
                                                                            prevPoints[i].y >= 0 && prevPoints[i].y <= latestDepthMat.getHeight() &&
                                                                            latestPoints[i].y >= 0 && latestPoints[i].y <= latestDepthMat.getHeight()) {

                                                                        vectors.add(new Vector((int) prevPoints[i].x, (int) prevPoints[i].y, (int) prevDepthMat.getDistance((int) prevPoints[i].x, (int) prevPoints[i].y),
                                                                                (int) latestPoints[i].x, (int) latestPoints[i].y, (int) latestDepthMat.getDistance((int) latestPoints[i].x, (int) latestPoints[i].y)));
                                                                    }
                                                                }

                                                                double xTot = 0, yTot = 0, zTot = 0;
                                                                for (int i = 0; i < vectors.size(); i++) {
                                                                    Vector v = vectors.get(i);
                                                                    xTot += v.changeVec()[0];
                                                                    yTot += v.changeVec()[1];
                                                                    zTot += v.changeVec()[2];
                                                                    stringBuilder.append(v).append(';');
                                                                }

                                                                double finalXTot = xTot / vectors.size();
                                                                double finalYTot = yTot / vectors.size();
                                                                double finalZTot = zTot / vectors.size();
                                                                activity.runOnUiThread(() -> {
//                                                                    TextView t = activity.findViewById(R.id.opticalFlowModel);
//                                                                    t.setText(String.format(Locale.ENGLISH, "change X: %f\nchange Y: %f\nchange Z: %f", finalXTot, finalYTot, finalZTot));
                                                                });

                                                                if (running) {
                                                                    //System.out.println(stringBuilder.toString());
                                                                    networking.send("OptiFlow: " + stringBuilder.toString() + "###\n");
                                                                    changeCamera.appendPoints((float) finalXTot, (float) finalYTot, (float) (finalZTot / 1000.0));
                                                                    //System.out.println(NS2S);
                                                                    //System.out.println((latestDepthMat.getTimestamp() - prevDepthMat.getTimestamp()) * NS2S);
                                                                    integrateVectors(posCameraVect, new float[] {(float) xTot, (float) yTot, (float) (zTot/1000.0)}, 0.01666666666f);
                                                                    posCamera.appendPoints(posCameraVect[0],posCameraVect[1],posCameraVect[2]);
                                                                    System.out.println(xTot + " " + yTot + " " + zTot);
                                                                    System.out.println(Arrays.toString(posCameraVect));

                                                                    try {
                                                                        writer.append("OptiFlow:").append(stringBuilder.toString()).append(String.valueOf('\n'));
                                                                    } catch (IOException e) {
                                                                        e.printStackTrace();
                                                                    }

                                                                }
                                                            }
                                                            //final float deptValue = depth.getDistance(depth.getWidth() / 2, depth.getHeight() / 2);

                                                            mat = new Mat(latestDepthMat.getHeight(), latestDepthMat.getWidth(), CvType.CV_16UC1);
                                                            int size = (int) (mat.total() * mat.elemSize());
                                                            byte[] return_buff = new byte[size];
                                                            latestDepthMat.getData(return_buff);
                                                            short[] shorts = new short[size / 2];
                                                            ByteBuffer.wrap(return_buff).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
                                                            mat.put(0, 0, shorts);

                                                            activity.runOnUiThread(() -> {

//                                                                    mat.convertTo(mat, CvType.CV_8UC3);
//                                                                    // convert to bitmap:
//                                                                    Bitmap bm = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
//                                                                    Utils.matToBitmap(mat, bm);

                                                                //mBackGroundText.setText("Distance: " + (0));
                                                            });

                                                        } else if (inputFrame.is(Extension.VIDEO_FRAME)) {

                                                            //Gets the video frame (depth or video)
                                                            VideoFrame video = inputFrame.as(Extension.VIDEO_FRAME);

                                                            //Coverts the video into a matrix
                                                            mat = new Mat(video.getHeight(), video.getWidth(), CvType.CV_8UC3);
                                                            int size = (int) (mat.total() * mat.elemSize());
                                                            byte[] return_buff = new byte[size];

                                                            //put the video into a byte array and add it to the matrix
                                                            video.getData(return_buff);
                                                            mat.put(0, 0, return_buff);

                                                            //Test for stream type
                                                            if (video.getProfile().getType() == StreamType.DEPTH) {

                                                                Bitmap bm = depthFlow.processFrame(mat, 100);
                                                                Point[][] points = depthFlow.getOpticalChange();

                                                                prevPoints = points[0];
                                                                latestPoints = points[1];

                                                                activity.runOnUiThread(() -> {

                                                                    // find the image view and draw it!
                                                                    ImageView iv = activity.findViewById(R.id.openCVDepthView);
                                                                    iv.setImageBitmap(bm);

                                                                });
                                                            } else if (video.getProfile().getType() == StreamType.COLOR) {

                                                                Bitmap bm = colorFlow.processFrame(mat, 1);

                                                                activity.runOnUiThread(() -> {

                                                                    // find the image view and draw it!
                                                                    ImageView iv = activity.findViewById(R.id.openCVColorView);
                                                                    iv.setImageBitmap(bm);
                                                                    //transmitter.send(bm);

                                                                });

                                                            }
                                                        }
                                                        //    }
                                                    });
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                mHandler.post(mStreaming);
            } catch (Exception e) {
                Log.e(TAG, "streaming, error: " + e.getMessage());
            }
        }
    };

    private DeviceListener mListener = new DeviceListener() {
        @Override
        public void onDeviceAttach() {
        }

        @Override
        public void onDeviceDetach() {
            stop();
        }
    };
    private boolean mPermissionsGranted = false;
    private boolean running = false;
    private LineGraphXYX posCamera;

    RealSenseVideo(Activity activity) {

        // Android 9 also requires camera permissions
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.O &&
                ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, PERMISSIONS_REQUEST_CAMERA);
            return;
        }

        OpenCVLoader.initDebug();

        depthFlow = new DepthFlowModel(0.01);
        colorFlow = new Optiflowmodel();
        //mGLSurfaceView = activity.findViewById(R.id.glSurfaceView);
        this.activity = activity;

        mPermissionsGranted = true;

    }

    public void init(Context cont) {
        //RsContext.init must be called once in the application lifetime before any interaction with physical RealSense devices.
        //For multi activities applications use the application context instead of the activity context

        if (!mPermissionsGranted) {
            Log.e(TAG, "missing permissions");
            return;
        }

        RsContext.init(cont);

        mRsContext = new RsContext();
        mRsContext.setDevicesChangedCallback(mListener);

        mPipeline = new Pipeline();

        //filters
        mAlign = new Align(StreamType.COLOR);
        mColorizer = new Colorizer();
        mDecimationFilter = new DecimationFilter();
        mHoleFillingFilter = new HoleFillingFilter();
        mDisparity = new DisparityTransformFilter(true);
        mThresholdFilter = new ThresholdFilter();
        mTemporalFilter = new TemporalFilter();
        mSpatialFilter = new SpatialFilter();

        //config filters
        mThresholdFilter.setValue(Option.MIN_DISTANCE, 0.1f);
        mThresholdFilter.setValue(Option.MAX_DISTANCE, 8.0f);

        mDecimationFilter.setValue(Option.FILTER_MAGNITUDE, 1);

        try (DeviceList dl = mRsContext.queryDevices()) {
            if (dl.getDeviceCount() > 0) {
                start();
            }
        }
        Log.d("init", "init started");

    }

    private synchronized void start() {
        Log.d(TAG, "try start streaming");

        if (mIsStreaming) {
            Log.d(TAG, "already streaming");
            return;
        }
        try {
            Log.d(TAG, "starting to stream");

            //mGLSurfaceView.clear();
            configAndStart();
            mIsStreaming = true;
            mHandler.post(mStreaming);
            Log.d(TAG, "streaming started successfully");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "1: failed to start streaming");
        }
    }

    private void configAndStart() throws Exception {
        try (Config config = new Config()) {
            config.enableStream(StreamType.DEPTH, 0, 480, 270, StreamFormat.Z16, 60);
            config.enableStream(StreamType.COLOR, 0, 424, 240, StreamFormat.RGB8, 60);
            // try statement needed here to release resources allocated by the Pipeline:start() method
            try (PipelineProfile pp = mPipeline.start(config)) {
                Device d = pp.getDevice();
                System.out.println(d.isInAdvancedMode());
                Sensor laser_sensor = d.querySensors().get(0);
                laser_sensor.setValue(Option.LASER_POWER, 360);

            }
        }
    }

    public synchronized void stop() {
        if (mRsContext != null)
            mRsContext.close();

        if (!mIsStreaming)
            return;
        try {
            Log.d(TAG, "try stop streaming");
            mIsStreaming = false;
            mHandler.removeCallbacks(mStreaming);
            mPipeline.stop();
            //mGLSurfaceView.clear();
            Log.d(TAG, "streaming stopped successfully");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "failed to stop streaming");
        }
        mPipeline.close();
    }

    public void close() {
        if (mGLSurfaceView != null)
            mGLSurfaceView.close();
    }


    void startRecording(SendAcc transmitter, FileWriter f, LineGraphXYX changeCamera, LineGraphXYX posCamera) {
        this.running = true;
        this.posCamera = posCamera;
        this.networking = transmitter;
        this.writer = f;
        this.changeCamera = changeCamera;
    }

    void stopRecording() {
        this.running = false;
        this.networking = null;
        this.writer = null;
        this.changeCamera = null;
        this.posCamera = null;
    }

    private void integrateVectors(float[] saveLoc, float[] vecIn, float timeElapsed) {
        saveLoc[0] = saveLoc[0] + vecIn[0] * (timeElapsed);
        saveLoc[1] = saveLoc[1] + vecIn[1] * (timeElapsed);
        saveLoc[2] = saveLoc[2] + vecIn[2] * (timeElapsed);

        for (int i = 0; i < saveLoc.length; i++) {
            saveLoc[i] = roundDecimal(saveLoc[i]);
        }

    }

    float roundDecimal(float d) {
        return Float.parseFloat(new DecimalFormat("#.###").format(d));
    }
}
