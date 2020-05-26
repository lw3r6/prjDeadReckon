package com.example.optiflow;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Arrays;

public class DepthFlowModel extends Optiflowmodel {

    DepthFlowModel(){
        super();
    }

    DepthFlowModel(double d){
        super(d);
    }

    @Override
    public Point[][] getOpticalChange() {
        return super.getOpticalChange();
    }
}
