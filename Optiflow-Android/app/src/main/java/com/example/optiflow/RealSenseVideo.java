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
import com.intel.realsense.librealsense.Frame;
import com.intel.realsense.librealsense.FrameCallback;
import com.intel.realsense.librealsense.FrameReleaser;
import com.intel.realsense.librealsense.FrameSet;
import com.intel.realsense.librealsense.GLRsSurfaceView;
import com.intel.realsense.librealsense.HoleFillingFilter;
import com.intel.realsense.librealsense.Option;
import com.intel.realsense.librealsense.Pipeline;
import com.intel.realsense.librealsense.PipelineProfile;
import com.intel.realsense.librealsense.Pointcloud;
import com.intel.realsense.librealsense.Points;
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
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class RealSenseVideo {

    private static final String TAG = "RealSenseVideo.java";
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
    private Pointcloud mPointCloud;
    private boolean mIsStreaming;
    private GLRsSurfaceView mGLSurfaceView;
    private Mat mat;
    private DepthFrame prevDepthMat;
    private DepthFrame latestDepthMat;
    private Point[] prevPoints;
    private Point[] latestPoints;
    private DepthFlowModel depthFlow;
    private Optiflowmodel colorFlow;
    private Frame pointFrame;
    byte[] pointCloudArray;
    private SendAcc networking;
    FileWriter writer;
    private LineGraphXYX changeCamera;
    private float[] posCameraVect = new float[3];
    private static final float NS2S = 1.0f / 1000000000.0f;
    private boolean first = true;
    private File filesDir;
    int count = 0;
    ArrayList<Vector> vectors;
    boolean caputreCloud = false;

    Runnable mStreaming = new Runnable() {
        AtomicInteger frameCount = new AtomicInteger();

        @Override
        public void run() {


            //Recommended filter order from intel
            //Depth Frame >> Decimation Filter >> Depth2Disparity Transform** ->
            // Spatial Filter >> Temporal Filter >> Disparity2Depth Transform** >>
            // Hole Filling Filter >> Filtered Depth. <br/>

            try (FrameReleaser fr = new FrameReleaser()) {
                FrameSet frames = mPipeline.waitForFrames(1000).releaseWith(fr);
                FrameSet orgSet = frames.releaseWith(fr).applyFilter(mPointCloud).releaseWith(fr);
                FrameSet processedSet = frames.releaseWith(fr)
                        .applyFilter(mDecimationFilter).releaseWith(fr)
                        .applyFilter(mDisparity).releaseWith(fr)
                        .applyFilter(mSpatialFilter).releaseWith(fr)
                        .applyFilter(mTemporalFilter).releaseWith(fr)
                        .applyFilter(mDisparity).releaseWith(fr)
                        .applyFilter(mHoleFillingFilter).releaseWith(fr)
                        .applyFilter(mColorizer).releaseWith(fr)
                        .applyFilter(mAlign).releaseWith(fr);

                //Frame processed = processedSet.first(StreamType.DEPTH,StreamFormat.XYZ32F).releaseWith(fr);

                Frame colorDepthFrame = processedSet.first(StreamType.DEPTH).releaseWith(fr);
                Frame texture = processedSet.first(StreamType.COLOR, StreamFormat.RGB8).releaseWith(fr);
                DepthFrame depth = orgSet.first(StreamType.DEPTH, StreamFormat.Z16).releaseWith(fr).as(Extension.DEPTH_FRAME);
//                Frame pointCloudFrame = orgSet.first(StreamType.DEPTH, StreamFormat.XYZ32F).releaseWith(fr);

                // POINT CLOUD WRITE TO FILE
//                if (caputreCloud) {
//                    Log.d(TAG, "Capturing Cloud");
//                    Points points1 = pointCloudFrame.as(Extension.POINTS);
//                    float[] ver = points1.getVertices();
//                    System.out.println("points: " + Arrays.toString(ver));
//
//                    File f = new File(filesDir, "pCloudTest.txt");
//                    if (!f.exists()) {
//                        f.createNewFile();
//                    }
//
//                    //PrintWriter writer = new PrintWriter(f);
//                    //writer.print("");
//
////                    for (int i = 0; i < ver.length; i = i + 3) {
////                        writer.append(ver[i] + " ").append(ver[i + 1] + " ").append(-ver[i + 2] + "\n");
////                    }
//
//                    //writer.flush();
//                    //writer.close();
//                    Log.d(TAG, "Finish Capturing Cloud");
//                    caputreCloud = false;
//
//                }
                //Log.d(TAG, "Is Depth Frame: " + depth.getDistance(50,50));

                //depth.getDistance(50,50);

                if (depth.is(Extension.DEPTH_FRAME)) {
                    Log.d("TAG", "Is Depth Frame");
                    if (latestDepthMat != null) {
                        prevDepthMat = latestDepthMat;
                    }

                    latestDepthMat = depth.as(Extension.DEPTH_FRAME);


                    if (prevDepthMat != null) {

                        StringBuilder stringBuilder = new StringBuilder();
                        vectors = new ArrayList<>();

                        for (int i = 0; i < prevPoints.length; i++) {

                            if (prevPoints[i].x >= 0 && prevPoints[i].x <= latestDepthMat.getWidth() - 1 &&
                                    latestPoints[i].x >= 0 && latestPoints[i].x <= latestDepthMat.getWidth() - 1 &&
                                    prevPoints[i].y >= 0 && prevPoints[i].y <= latestDepthMat.getHeight() - 1 &&
                                    latestPoints[i].y >= 0 && latestPoints[i].y <= latestDepthMat.getHeight() - 1) {

                                try {
                                    int prevDepth = (int) (prevDepthMat.getDistance((int) prevPoints[i].x, (int) prevPoints[i].y) * 1000);
                                    int latestDepth = (int) (latestDepthMat.getDistance((int) latestPoints[i].x, (int) latestPoints[i].y) * 1000);

                                    //If the depths of both frames are 0 reject it
                                    if (prevDepth + latestDepth != 0) {
                                        vectors.add(new Vector((int) prevPoints[i].x, (int) prevPoints[i].y, prevDepth,
                                                (int) latestPoints[i].x, (int) latestPoints[i].y, latestDepth));
                                    }
                                } catch (RuntimeException e) {
                                    System.out.println("Caught Exception for pos: " + (int) prevPoints[i].x + " " + (int) prevPoints[i].y +
                                            " " + (int) latestPoints[i].x + " " + (int) latestPoints[i].y +
                                            " " + (prevDepthMat.getWidth() + " " + prevDepthMat.getHeight()) +
                                            " " + (latestDepthMat.getWidth() + " " + latestDepthMat.getHeight()));
                                }
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

                        System.out.println(stringBuilder.toString());

                        double finalXTot = xTot / vectors.size();
                        double finalYTot = yTot / vectors.size();
                        double finalZTot = zTot / vectors.size();
                        activity.runOnUiThread(() -> {

                        });

                        if (running) {
                            networking.send("OptiFlow: " + stringBuilder.toString() + "###\n");
                            changeCamera.appendPoints((float) finalXTot, (float) finalYTot, (float) (finalZTot / 1000.0));
                            integrateVectors(posCameraVect, new float[]{(float) xTot, (float) yTot, (float) (zTot / 1000.0)}, 0.01666666666f);
                            posCamera.appendPoints(posCameraVect[0], posCameraVect[1], posCameraVect[2]);

 //                           writer.append("OptiFlow:").append(stringBuilder.toString()).append(String.valueOf('\n'));

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

                } else {
                    Log.d(TAG, "Not depth frame");
                }

//                 ---GET THE VIDEO FROM THE DEPTH FRAME AND DISPLAY IT AS WELL AS GET THE DEPTH FLOW--- //
//                    Gets the video frame (depth or video)

                VideoFrame video = colorDepthFrame.as(Extension.VIDEO_FRAME);

                //Coverts the video into a matrix
                mat = new Mat(video.getHeight(), video.getWidth(), CvType.CV_8UC3);
                int size = (int) (mat.total() * mat.elemSize());
                byte[] return_buff = new byte[size];

                //put the video into a byte array and add it to the matrix
                video.getData(return_buff);
                mat.put(0, 0, return_buff);

                //Test for stream type
                if (video.getProfile().getType() == StreamType.DEPTH) {

                    Bitmap bm = depthFlow.processFrame(mat, 25);
                    Point[][] points = depthFlow.getOpticalChange();

                    prevPoints = points[0];
                    latestPoints = points[1];

                    activity.runOnUiThread(() -> {

                        // find the image view and draw it!
                        ImageView iv = activity.findViewById(R.id.openCVDepthView);
                        iv.setImageBitmap(bm);

                    });
                }

                // ---GET THE VIDEO FROM THE VIDEO FRAME AND DISPLAY IT AS WELL AS GET THE OPTICAL FLOW--- //
                //Gets the video frame (depth or video)
                video = texture.as(Extension.VIDEO_FRAME);

                //Coverts the video into a matrix
                mat = new Mat(video.getHeight(), video.getWidth(), CvType.CV_8UC3);
                size = (int) (mat.total() * mat.elemSize());
                return_buff = new byte[size];

                //put the video into a byte array and add it to the matrix
                video.getData(return_buff);
                mat.put(0, 0, return_buff);
                if (video.getProfile().getType() == StreamType.COLOR) {

                    Bitmap bm = colorFlow.processFrame(mat, 1);

                    activity.runOnUiThread(() -> {

                        // find the image view and draw it!
                        ImageView iv = activity.findViewById(R.id.openCVColorView);
                        iv.setImageBitmap(bm);
                        //transmitter.send(bm);

                    });

                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }


            mHandler.post(mStreaming);
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

        Log.d("realsense", "started");

        filesDir = activity.getFilesDir();

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
        mPointCloud = new Pointcloud();

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
            config.enableStream(StreamType.DEPTH, 480, 270);
            config.enableStream(StreamType.COLOR, 424, 240);
            //config.enableStream(StreamType.DEPTH,0,480,270,StreamFormat.XYZ32F,60);
            // try statement needed here to release resources allocated by the Pipeline:start() method

            try (PipelineProfile pp = mPipeline.start(config)) {
                Device d = pp.getDevice();
                Sensor laser_sensor = d.querySensors().get(0);
                laser_sensor.setValue(Option.LASER_POWER, 360);

            }
            if (!config.canResolve(mPipeline)) {
                config.disableAllStreams();
                config.enableStream(StreamType.DEPTH, 480, 270);
                config.enableStream(StreamType.COLOR, 424, 240);
            }
            ;

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

    public void captureCloud() {
        this.caputreCloud = true;
    }
}
