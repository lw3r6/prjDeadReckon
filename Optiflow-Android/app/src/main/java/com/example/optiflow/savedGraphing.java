package com.example.optiflow;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.scichart.charting3d.visuals.SciChartSurface3D;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class savedGraphing extends Activity {

    private static final float NS2S = 1.0f / 1000000000.0f;
    String filename;
    Plotting3DHandler plotting3DHandler;
    TextView depthLab, cutOffLab;
    double cutoff = 0, smoothingValue = 1;
    private LineGraphXYX accWorldGraphData, accDeviceGraphData, velWorldGraphData, velDeviceGraphData, orientationGraphData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            SciChartSurface3D.setRuntimeLicenseKey("Q3tRdI1OPSLUA44Yt1SvSfqe/QkzMfkY8IV31PV7LsNYgCgRtw84OQEpqAULL0tZUH9kA7tdujJnm+tztEbVhdJt3JURQyBq+pJpPCwvRV3APKN26Ke56EQJWmHujpVlIkcEiodXZFpIN7hjDO7X26L0ucVZtJNMkI+xhuXwXG7irCuedkhhhPy0Vcp2p+oJbrMH/7x2hO4m4CruAaIaweLuQqLI4yn9zPZRQrb9hjLIK4XBZQQHbK0IySqPFjEMgea7KfIv/IcUOv/Ic6KA6bUNr2HVGYChTova3UTUQ63eo0Zsi9MhWXmgYHxKVpbM9w7CPsLFe1nqB4J74xM1B53rMa2tyaJVwKfYMIhFfOtrEdq12G6rhogXJ+4h6Sne1Qr7UItfp327LH1OU4hWkYe2bsNiBCRmavbafDmNBf+yZrR+XOcRWjNiLfnF6gF9I/GdpeDCT2l3NeWxCm44mxC6hHfPpbRoCO7429PvP7OZMV5bnt75XGwFH9VcN4u0//7Klyhb43BUJ3+dnJXZ8fz/b55wQ3e2lzX1aQvhNc5vQonsIhE=");// On iOS set this code once in AppDelegate or application startup
        } catch (Exception e) {
            Log.e("scichart", Objects.requireNonNull(e.getMessage()));
        }

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        //Make sure the screen does not sleep or rotate
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
//Remove notification bar
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        FloatingActionButton floatingActionButton = findViewById(R.id.floatingActionButton);
        floatingActionButton.setVisibility(View.GONE);

        accWorldGraphData = new LineGraphXYX(findViewById(R.id.graphAccWorld), 1000,this);
        velWorldGraphData = new LineGraphXYX(findViewById(R.id.graphVelWorld), 1000,this);
        accDeviceGraphData = new LineGraphXYX(findViewById(R.id.graphAccDevice), 1000,this);
        velDeviceGraphData = new LineGraphXYX(findViewById(R.id.graphVelDevice), 1000,this);
        orientationGraphData = new LineGraphXYX(findViewById(R.id.orientationGraph), 1000,this);
        plotting3DHandler = new Plotting3DHandler(findViewById(R.id.chartId), this);

        SeekBar depthBar = findViewById(R.id.depthSeek);
        depthBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                depthLab = findViewById(R.id.filterDepthLabel);
                depthLab.setText("Filter: " + Math.pow(2,i));
                smoothingValue = Math.pow(2,i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                popGraph(cutoff, smoothingValue);
            }

        });

        SeekBar cutOffBar = findViewById(R.id.cutOffSeek);
        cutOffBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                cutOffLab = findViewById(R.id.cutOffLabel);
                cutOffLab.setText("Cut Off: " + i / 100.0);

                cutoff = i / 100.0;

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                popGraph(cutoff, smoothingValue);
            }
        });

        popGraph(0, smoothingValue);


    }

    private void popGraph(double cutOff, double smoothingValue) {
        //populate 2d Graphs
        accWorldGraphData.initData();

        //populate 2d Graphs
        velWorldGraphData.initData();

        //populate 2d Graphs
        accDeviceGraphData.initData();

        //populate 2d Graphs
        velDeviceGraphData.initData();

        //populate 2d Graphs
        orientationGraphData.initData();

        plotting3DHandler.initData();

        filename = getIntent().getStringExtra("fileName");

        Log.d("file: ", filename);
        File f = new File(getFilesDir(), filename);
        try {
            Scanner scan = new Scanner(f);
            scan.useDelimiter("([,:\n])");

            float prevTimeStamp = 0;
            float timeStamp = 0;
            float[] accDeviceVectors = new float[3];
            float[] velDeviceVectors = new float[3];
            float[] accWorldVectors = new float[3];
            float[] velWorldVectors = new float[3];
            float[] posWorldVectors = new float[3];

            //Get rid of header
            Log.d("headers", scan.nextLine());
            Log.d("headers", scan.nextLine());


            float[] smoothedDeviceArr = {0, 0, 0};
            float[] smoothedWorldArr = {0, 0, 0};
            while (scan.hasNextLine()) {

                if (scan.next().startsWith("Sensors")) {

                    accDeviceVectors[0] = scan.nextFloat();
                    if (Math.abs(accDeviceVectors[0]) < cutOff) {
                        accDeviceVectors[0] = 0;
                    }

                    accDeviceVectors[1] = scan.nextFloat();
                    if (Math.abs(accDeviceVectors[1]) < cutOff) {
                        accDeviceVectors[1] = 0;
                    }

                    accDeviceVectors[2] = scan.nextFloat();
                    if (Math.abs(accDeviceVectors[2]) < cutOff) {
                        accDeviceVectors[2] = 0;
                    }

                    accWorldVectors[0] = scan.nextFloat();
                    if (Math.abs(accWorldVectors[0]) < cutOff) {
                        accWorldVectors[0] = 0;
                    }
                    accWorldVectors[1] = scan.nextFloat();
                    if (Math.abs(accWorldVectors[1]) < cutOff) {
                        accWorldVectors[1] = 0;
                    }
                    accWorldVectors[2] = scan.nextFloat();
                    if (Math.abs(accWorldVectors[2]) < cutOff) {
                        accWorldVectors[2] = 0;
                    }

                    //START FIltering
                    if (prevTimeStamp != 0) {

                        smoothedDeviceArr[0] += (accDeviceVectors[0] - smoothedDeviceArr[0]) / smoothingValue;
                        smoothedDeviceArr[1] += (accDeviceVectors[1] - smoothedDeviceArr[1]) / smoothingValue;
                        smoothedDeviceArr[2] += (accDeviceVectors[2] - smoothedDeviceArr[2]) / smoothingValue;

                        smoothedWorldArr[0] += (accWorldVectors[0] - smoothedWorldArr[0]) / smoothingValue;
                        smoothedWorldArr[1] += (accWorldVectors[1] - smoothedWorldArr[1]) / smoothingValue;
                        smoothedWorldArr[2] += (accWorldVectors[2] - smoothedWorldArr[2]) / smoothingValue;
                    }

                    accDeviceVectors[0] = smoothedDeviceArr[0];
                    accDeviceVectors[1] = smoothedDeviceArr[1];
                    accDeviceVectors[2] = smoothedDeviceArr[2];

                    accWorldVectors[0] = smoothedWorldArr[0];
                    accWorldVectors[1] = smoothedWorldArr[1];
                    accWorldVectors[2] = smoothedWorldArr[2];


                    //Graph the points
                    accDeviceGraphData.appendPoints(accDeviceVectors[0], accDeviceVectors[1], accDeviceVectors[2]);
                    accWorldGraphData.appendPoints(accWorldVectors[0], accWorldVectors[1], accWorldVectors[2]);
                    orientationGraphData.appendPoints(scan.nextFloat(), scan.nextFloat(), scan.nextFloat());

                    prevTimeStamp = timeStamp;
                    timeStamp = scan.nextFloat();

                    if (prevTimeStamp != 0) {


                        final float dT = (timeStamp - prevTimeStamp) * NS2S;
                        integrateVectors(velDeviceVectors, accDeviceVectors, dT);

                        Log.d("Vel", Arrays.toString(velDeviceVectors) + " " + Arrays.toString(accDeviceVectors) + " " + dT);
                        velDeviceGraphData.appendPoints(velDeviceVectors[0], velDeviceVectors[1], velDeviceVectors[2]);

                        integrateVectors(velWorldVectors, accWorldVectors, dT);
                        velWorldGraphData.appendPoints(velWorldVectors[0], velWorldVectors[1], velWorldVectors[2]);

                        integrateVectors(posWorldVectors, velWorldVectors, dT);
                        plotting3DHandler.appendPoints(posWorldVectors[0], posWorldVectors[1], posWorldVectors[2]);
                    }

                    scan.nextLine();

                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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

    float roundDecimal(float d) {
        return Float.parseFloat(new DecimalFormat("#.###").format(d));
    }


}
