package com.example.optiflow;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.Arrays;

class Optiflowmodel {

    protected MatOfPoint p0MatofPoint = new MatOfPoint();
    protected MatOfPoint2f p0;
    protected MatOfPoint2f p1;

    protected Point[] pointsBefore;
    protected Point[] pointsAfter;

    protected Mat old_frame;
    protected Mat old_gray;
    protected Mat mask;
    protected Mat frame_gray = new Mat();
    protected boolean isFirstFrame = true;
    protected double qualityLevel = 0.2;

    Optiflowmodel() {
        old_frame = new Mat();
        old_gray = new Mat();
    }

    Optiflowmodel(double qualityLevel) {
        this.qualityLevel = qualityLevel;
        old_frame = new Mat();
        old_gray = new Mat();
    }

    Bitmap processFrame(Mat matrixIn, int corners) {

        if (isFirstFrame) {
            isFirstFrame = false;
            old_frame = matrixIn.clone();
            Imgproc.cvtColor(old_frame, old_gray, Imgproc.COLOR_BGR2GRAY);
        }

        old_frame = matrixIn.clone();
        Imgproc.cvtColor(old_frame, frame_gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.goodFeaturesToTrack(old_gray, p0MatofPoint, corners, qualityLevel, 10, new Mat(), 3, false, 0.04);
        p0 = new MatOfPoint2f(p0MatofPoint.toArray());
        p1 = new MatOfPoint2f();
        mask = Mat.zeros(old_frame.size(), old_frame.type());

        // calculate optical flow
        MatOfByte status = new MatOfByte();
        MatOfFloat err = new MatOfFloat();
        TermCriteria criteria = new TermCriteria(TermCriteria.COUNT + TermCriteria.EPS, 10, 0.03);

        Video.calcOpticalFlowPyrLK(old_gray, frame_gray, p0, p1, status, err, new Size(15, 15), 2, criteria);

        byte[] StatusArr = status.toArray();
        Point[] p0Arr = p0.toArray();
        Point[] p1Arr = p1.toArray();
        pointsBefore = p0.toArray().clone();
        pointsAfter = p1.toArray().clone();
        ArrayList<Point> good_new = new ArrayList<>();
        for (int i = 0; i < StatusArr.length; i++) {
            if (StatusArr[i] == 1) {
                good_new.add(p1Arr[i]);
                Imgproc.line(mask, p1Arr[i], p0Arr[i], new Scalar(256, 256, 256), 2);
                Imgproc.circle(matrixIn.clone(), p1Arr[i], 5, new Scalar(256, 256, 256), -1);
            }
        }
        Mat img = new Mat();
        Core.add(matrixIn.clone(), mask, img);

        // Now update the previous frame and previous points
        old_gray = frame_gray.clone();
        Point[] good_new_arr = new Point[good_new.size()];
        good_new_arr = good_new.toArray(good_new_arr);
        p0 = new MatOfPoint2f(good_new_arr);

        img.convertTo(img, CvType.CV_8UC3);

        // convert to bitmap:
        Bitmap bm = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img, bm);

        return bm;

    }

    public Point[][] getOpticalChange(){
        Point[][] points = new Point[2][];
        points[0] = pointsBefore;
        points[1] = pointsAfter;

        return points;
    }
}
