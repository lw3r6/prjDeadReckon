package com.example.optiflow;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.text.format.Formatter;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.scichart.charting3d.visuals.SciChartSurface3D;

import net.majorkernelpanic.streaming.Session;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.video.VideoQuality;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
import java.util.Queue;

public class LiveGraphing extends Activity implements View.OnClickListener, Session.Callback, SurfaceHolder.Callback {

    final int FILTER_DEPTH = 1;

    int pos = 0;
    //Plotting3DHandler plotting3DHandler;
    boolean record = false;
    FileWriter csvWriter = null;
    Queue<String> sendBuffer = new LinkedList<String>();
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private MyRenderer mRenderer;
    private LineGraphXYX accWorldGraphData, accDeviceGraphData, velWorldGraphData, velDeviceGraphData, orientationGraphData, changeCamera, posCamera;
    private float[] posVectors = new float[3];
    private double[] rollingFilter = new double[FILTER_DEPTH];
    private String filename = "";
    private String SERVER_IP = "";
    private Session mSession;
    private SurfaceView mSurfaceView;
    RealSenseVideo realsense;


    @SuppressLint("SourceLockedOrientationActivity")
    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            SciChartSurface3D.setRuntimeLicenseKey("Q3tRdI1OPSLUA44Yt1SvSfqe/QkzMfkY8IV31PV7LsNYgCgRtw84OQEpqAULL0tZUH9kA7tdujJnm+tztEbVhdJt3JURQyBq+pJpPCwvRV3APKN26Ke56EQJWmHujpVlIkcEiodXZFpIN7hjDO7X26L0ucVZtJNMkI+xhuXwXG7irCuedkhhhPy0Vcp2p+oJbrMH/7x2hO4m4CruAaIaweLuQqLI4yn9zPZRQrb9hjLIK4XBZQQHbK0IySqPFjEMgea7KfIv/IcUOv/Ic6KA6bUNr2HVGYChTova3UTUQ63eo0Zsi9MhWXmgYHxKVpbM9w7CPsLFe1nqB4J74xM1B53rMa2tyaJVwKfYMIhFfOtrEdq12G6rhogXJ+4h6Sne1Qr7UItfp327LH1OU4hWkYe2bsNiBCRmavbafDmNBf+yZrR+XOcRWjNiLfnF6gF9I/GdpeDCT2l3NeWxCm44mxC6hHfPpbRoCO7429PvP7OZMV5bnt75XGwFH9VcN4u0//7Klyhb43BUJ3+dnJXZ8fz/b55wQ3e2lzX1aQvhNc5vQonsIhE=");// On iOS set this code once in AppDelegate or application startup
        } catch (Exception e) {
            Log.e("scichart", Objects.requireNonNull(e.getMessage()));
        }

        //Make sure the screen does not sleep or rotate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        //Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

        //Make sure the sensor reading happens in a thread an not in the UI Thread
        new Thread(() -> {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            Looper.prepare();
            mRenderer = new MyRenderer();
        }).start();

        setContentView(R.layout.activity_main);

        realsense = new RealSenseVideo(this);

        //Populate 3d Graph
        //plotting3DHandler = new Plotting3DHandler(findViewById(R.id.chartId), this);

        //Populate 2d Graphs
        accWorldGraphData = new LineGraphXYX(findViewById(R.id.graphAccWorld), 1000,this);
        accDeviceGraphData = new LineGraphXYX(findViewById(R.id.graphAccDevice), 1000,this);
        velWorldGraphData = new LineGraphXYX(findViewById(R.id.graphVelWorld), 1000,this);
        velDeviceGraphData = new LineGraphXYX(findViewById(R.id.graphVelDevice), 1000,this);
        orientationGraphData = new LineGraphXYX(findViewById(R.id.orientationGraph), 1000,this);
        changeCamera = new LineGraphXYX(findViewById(R.id.ChangeCamera), 100,this);
        posCamera = new LineGraphXYX(findViewById(R.id.PosCamera), 100,this);
        mSurfaceView = findViewById(R.id.mSurfaceView);

        mSession = SessionBuilder.getInstance()
                .setCallback(this)
                .setSurfaceView(mSurfaceView)
                .setPreviewOrientation(90)
                .setContext(getApplicationContext())
                .setVideoEncoder(SessionBuilder.VIDEO_H264)
                .setVideoQuality(new VideoQuality(320,240,30,500000))
                .build();

        mSurfaceView.getHolder().addCallback(this);

        FloatingActionButton fab = findViewById(R.id.floatingActionButton);
        fab.setOnClickListener(view -> {

            if (record) {
                mRenderer.stop();

            } else {
                accWorldGraphData.initData();
                accDeviceGraphData.initData();
                velWorldGraphData.initData();
                velDeviceGraphData.initData();
                orientationGraphData.initData();
                changeCamera.initData();
                posCamera.initData();
                realsense.captureCloud();
                //plotting3DHandler.initData();

                WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                SERVER_IP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Enter a file name and ip of the server to connect to");

                LinearLayout lilay1 = new LinearLayout(this);
                lilay1.setOrientation(LinearLayout.VERTICAL);

                final TextInputLayout testLayout1 = new TextInputLayout(this);
                final TextInputEditText inputFileName = new TextInputEditText(this);

                testLayout1.setHint("filename");
                inputFileName.setText("file");
                testLayout1.addView(inputFileName);

                final TextInputLayout testLayout2 = new TextInputLayout(this);
                final TextInputEditText inputIpAddress = new TextInputEditText(this);

                testLayout2.setHint("Ip Address");
                inputIpAddress.setText(SERVER_IP);
                testLayout2.addView(inputIpAddress);

                lilay1.addView(testLayout1);
                lilay1.addView(testLayout2);
                builder.setView(lilay1);


                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        filename = inputFileName.getText().toString();
                        SERVER_IP = inputIpAddress.getText().toString();
                        mRenderer.start();

                        mSession.setDestination(SERVER_IP);
                        if (!mSession.isStreaming()) {
                            mSession.configure();
                        } else {
                            mSession.stop();
                        }

                        record = !record;

                    }
                });

                builder.show();

            }
        });
    }


    @Override
    protected void onStop() {
        super.onStop();
        mRenderer.stop();
    }

    private float[] calculateAccVectors(float[] mRotMtrx, float[] accMatrix, int precision, double cutOff) {
        float[] accVectors = new float[3];
        precision = (int) Math.pow(10, precision);

        // Calculate the direction vectors by using the rotation of the device and the acceleration matrix
        accVectors[0] = mRotMtrx[0] * accMatrix[0] + mRotMtrx[1] * accMatrix[1] + mRotMtrx[2] * accMatrix[2];
        accVectors[1] = mRotMtrx[3] * accMatrix[0] + mRotMtrx[4] * accMatrix[1] + mRotMtrx[5] * accMatrix[2];
        accVectors[2] = mRotMtrx[6] * accMatrix[0] + mRotMtrx[7] * accMatrix[1] + mRotMtrx[8] * accMatrix[2];

        for (int i = 0; i < 3; i++) {
            if (Math.abs(accVectors[i]) < cutOff) {
                accVectors[i] = 0;
            }
        }

        // Round the to the defined precision
        accVectors[0] = (((int) (accVectors[0] * precision)) / ((float) precision));
        accVectors[1] = (((int) (accVectors[1] * precision)) / ((float) precision));
        accVectors[2] = (((int) (accVectors[2] * precision)) / ((float) precision));

        return accVectors;
    }

    private float[][] filterVector(float[][] accFilterVectors, float[] accVectors) {
        float[][] out = accFilterVectors;

        int depth = out[0].length;
        System.out.println(out.length);

        float x = 0;
        float y = 0;
        float z = 0;
        for (int i = 0; i < out.length; i++) {
            x = out[i][0] + x;
            y = out[i][0] + y;
            z = out[i][0] + z;
        }

        for (int i = out.length - 1; i > 0; i--) {
            out[i] = out[i - 1];
        }


        out[0] = accVectors;
        out[0][0] = ((x + out[0][0]) / (out.length + 1));
        out[0][1] = ((y + out[0][1]) / (out.length + 1));
        out[0][2] = ((z + out[0][2]) / (out.length + 1));

        return accFilterVectors;
    }

    float roundDecimal(float d) {
        return Float.parseFloat(new DecimalFormat("#.###").format(d));
    }

    @Override
    public void onResume() {
        super.onResume();
        realsense.init(getApplication());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realsense.close();
        mSession.release();
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public void onBitrateUpdate(long bitrate) {
        //Log.d(TAG,"Bitrate: "+bitrate);
    }

    @Override
    public void onSessionError(int message, int streamType, Exception e) {
        if (e != null) {
            logError(e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        realsense.stop();
    }

    @Override
    public void onPreviewStarted() {
        Log.d("TAG","Preview started.");
    }

    @Override
    public void onSessionConfigured() {
        Log.d("TAG","Preview configured.");
        // Once the stream is configured, you can get a SDP formated session description
        // that you can send to the receiver of the stream.
        // For example, to receive the stream in VLC, store the session description in a .sdp file
        // and open it with VLC while streming.
        Log.d("TAG", mSession.getSessionDescription());
        mSession.start();
    }

    @Override
    public void onSessionStarted() {
        Log.d("TAG","Session started.");
    }

    @Override
    public void onSessionStopped() {
        Log.d("TAG","Session stopped.");
    }

    /** Displays a popup to report the eror to the user */
    private void logError(final String msg) {
        final String error = (msg == null) ? "Error unknown" : msg;
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(LiveGraphing.this);
        builder.setMessage(error).setPositiveButton("OK", (dialog, id) -> {});
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mSession.startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSession.stop();

    }

    class MyRenderer implements SensorEventListener {

        private static final float NS2S = 1.0f / 1000000000.0f;
        float[] mMagFieldsArr = new float[3];
        float[] mGravArr = new float[3];
        float[] accMatrix;
        float[] accVectors;
        float[][] accFilterVectors;
        private float[] mRotMtrx = new float[9];
        private Sensor mRotationVectorSensor;
        private Sensor mAccelerationVectorSensor;
        private Sensor mGravityVectorSensor;
        private Sensor mMagVectorSensor;
        private float[] velVectWorld = new float[3];
        private float[] velVectDevice = new float[3];
        private SendAcc transmitter;
        private float timestamp;

        public MyRenderer() {
            // find the rotation-vector sensor
            mRotationVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_ROTATION_VECTOR);

            mAccelerationVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_LINEAR_ACCELERATION);

            mGravityVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_GRAVITY);

            mMagVectorSensor = mSensorManager.getDefaultSensor(
                    Sensor.TYPE_MAGNETIC_FIELD);
        }

        public void start() {
            // enable our sensor when the activity is resumed, ask for
            // 10 ms updates.
            mSensorManager.registerListener(this, mRotationVectorSensor, 10000);
            mSensorManager.registerListener(this, mAccelerationVectorSensor, 10000);
            mSensorManager.registerListener(this, mGravityVectorSensor, 10000);
            mSensorManager.registerListener(this, mMagVectorSensor, 10000);

            mRotMtrx = new float[9];
            mMagFieldsArr = new float[3];
            mGravArr = new float[3];
            velVectWorld = new float[3];
            velVectDevice = new float[3];

//            try {
//                File f = new File(getFilesDir(), filename + ".csv");
//                if (f.exists()) {
//                    f.createNewFile();
//                } else {
//                    f.delete();
//                    f.createNewFile();
//                }
//                csvWriter = new FileWriter(f);
//                csvWriter.append("Longitude Acc");
//                csvWriter.append(",");
//                csvWriter.append("Traverse Acc");
//                csvWriter.append(",");
//                csvWriter.append("Vertical Acc");
//                csvWriter.append(",");
//                csvWriter.append("Nor/Sou Acc");
//                csvWriter.append(",");
//                csvWriter.append("Eas/Wes Acc");
//                csvWriter.append(",");
//                csvWriter.append("Up/Down Acc");
//                csvWriter.append(",");
//                csvWriter.append("Azimuth");
//                csvWriter.append(",");
//                csvWriter.append("Pitch");
//                csvWriter.append(",");
//                csvWriter.append("Roll");
//                csvWriter.append(",");
//                csvWriter.append("EventTime");
//                csvWriter.append("\n");


                transmitter = new SendAcc(SERVER_IP);
                realsense.startRecording(transmitter, csvWriter, changeCamera, posCamera);
                transmitter.start();


//            } catch (IOException e) {
//                e.printStackTrace();
//            }

        }

        public void stop() {
            // make sure to turn our sensor off when the activity is paused
            if (transmitter != null) {
                transmitter.send("STOP###");
                transmitter.close();
            }

            realsense.stopRecording();
            mSensorManager.unregisterListener(this);
            try {
                if (csvWriter != null) {
                    csvWriter.flush();
                    csvWriter.close();
                }
            } catch (IOException e) {
                Log.d("can't close file writter", "File writter already closed");
            }

        }

        public void onSensorChanged(SensorEvent event) {

            // we received a sensor event. it is a good practice to check
            // that we received the proper event

            if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                mGravArr = event.values.clone();
                for (int i = 0; i < mGravArr.length; i++) {
                    mGravArr[i] = roundDecimal(mGravArr[i]);
                }
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mMagFieldsArr = event.values.clone();
                for (int i = 0; i < mMagFieldsArr.length; i++) {
                    mMagFieldsArr[i] = roundDecimal(mMagFieldsArr[i]);
                }
            } else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                // convert the rotation-vector to a 4x4 matrix. the matrix
                // is interpreted by Open GL as the inverse of the
                // rotation-vector, which is what we want.


                if (timestamp != 0) {
                    final float dT = (event.timestamp - timestamp) * NS2S;

                    accMatrix = event.values.clone();
                    for (int i = 0; i < accMatrix.length; i++) {
                        accMatrix[i] = roundDecimal(accMatrix[i]);
                    }

                    if (accFilterVectors == null) {
                        accFilterVectors = new float[FILTER_DEPTH][3];
                        for (float[] accFilterVector : accFilterVectors) {
                            Arrays.fill(accFilterVector, 0);
                        }
                    }

                    SensorManager.getRotationMatrix(mRotMtrx, null, mGravArr, mMagFieldsArr);

                    float[] orientation = new float[3];
                    SensorManager.getOrientation(mRotMtrx, orientation);

                    orientationGraphData.appendPoints((float) Math.toDegrees(orientation[0]), (float) Math.toDegrees(orientation[1]), (float) Math.toDegrees(orientation[2]));

                    for (int i = 0; i < mRotMtrx.length; i++) {
                        mRotMtrx[i] = roundDecimal(mRotMtrx[i]);
                    }

                    accVectors = calculateAccVectors(mRotMtrx, accMatrix, 2, 0.3);

                    //System.out.println("accVel: " + Arrays.toString(accMatrix));
                    for (int i = 0; i < accVectors.length; i++) {
                        accVectors[i] = roundDecimal(accVectors[i]);
                    }

                    //Add data points to world acceleration series
                    accWorldGraphData.appendPoints(accVectors[0], accVectors[1], accVectors[2]);
                    accDeviceGraphData.appendPoints(accMatrix[0], accMatrix[1], accMatrix[2]);

                    integrateVectors(velVectWorld, accVectors, dT);
                    integrateVectors(velVectDevice, accMatrix, dT);

                    //add vel vectors to velocity series
                    velWorldGraphData.appendPoints(velVectWorld[0], velVectWorld[1], velVectWorld[2]);
                    velDeviceGraphData.appendPoints(velVectDevice[0], velVectDevice[1], velVectDevice[2]);

                    integrateVectors(posVectors, velVectWorld, dT);

                    String write = String.format(Locale.ENGLISH, "%f,%f,%f,%f,%f,%f,%f,%f,%f,%f", accMatrix[0], accMatrix[1], accMatrix[2], accVectors[0], accVectors[1], accVectors[2], Math.toDegrees(orientation[0]), Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2]), (float) event.timestamp);
                    //transmitter.send(write);

//                    try {
//                        csvWriter.append("Sensors:").append(write).append('\n');
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }

                    //deviceacc[1,2,3], worldacc[1,2,3], deviceVel[1,2,3], worldVel[1,2,3]
                    String write1 = String.format(Locale.ENGLISH, "%f,%f,%f," + //DeviceAcc [0,1,2]
                                    "%f,%f,%f," + //WorldAcc [3,4,5]
                                    "%f,%f,%f," + //DeviceVel [6,7,8]
                                    "%f,%f,%f," + //WorldVel [9,10,11]
                                    "%f,%f,%f," + //orientation [12,13,14]
                                    "%f,%f,%f," + //position [15,16,17]
                                    "%f", //Timestamp [18]
                            accMatrix[0], accMatrix[1], accMatrix[2],
                            accVectors[0], accVectors[1], accVectors[2],
                            velVectDevice[0], velVectDevice[1], velVectDevice[2],
                            velVectWorld[0], velVectWorld[1], velVectWorld[2],
                            posVectors[0], posVectors[1], posVectors[2],
                            Math.toDegrees(orientation[0]), Math.toDegrees(orientation[1]), Math.toDegrees(orientation[2]),
                            (float) event.timestamp);

                    transmitter.send("DataPush: " + write1 + "###\n");

//                    Log.d("Writing to csv", write);

                }


                timestamp = event.timestamp;


            }
        }


        private void integrateVectors(float[] saveLoc, float[] vecIn, float timeElapsed) {
            saveLoc[0] = saveLoc[0] + vecIn[0] * (timeElapsed);
            saveLoc[1] = saveLoc[1] + vecIn[1] * (timeElapsed);
            saveLoc[2] = saveLoc[2] + vecIn[2] * (timeElapsed);

            for (int i = 0; i < saveLoc.length; i++) {
                saveLoc[i] = roundDecimal(saveLoc[i]);
            }

        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }


    }

}



