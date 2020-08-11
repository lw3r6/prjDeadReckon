package com.example.optiflow;

import androidx.annotation.NonNull;

import com.intel.realsense.librealsense.Point_3D;

public class Vector implements Comparable<Vector> {

    private Point_3D prevPoint, latePoint;

    public Vector(Point_3D prevPoint, Point_3D latePoint) {

        this.prevPoint = prevPoint;
        this.latePoint = latePoint;

    }

    public Point_3D getLatePoint() {
        return latePoint;
    }

    public double[] getlatestasArray() {
        return (new double[]{(double) prevPoint.mX, (double) prevPoint.mY, (double) prevPoint.mZ});
    }

    public double[] getprevasArray() {
        return (new double[]{(double) latePoint.mX, (double) latePoint.mY, (double) latePoint.mZ});
    }

    public Point_3D getPrevPoint() {
        return prevPoint;
    }


    @NonNull
    @Override
    public String toString() {
        return String.format("'%f,%f,%f -> %f,%f,%f'", prevPoint.mX, prevPoint.mY, prevPoint.mZ, latePoint.mX, latePoint.mY, latePoint.mZ);
    }

    public double[] changeVec() {
        return new double[]{latePoint.mX - prevPoint.mX, latePoint.mY - prevPoint.mY, latePoint.mZ - prevPoint.mZ};
    }

    @Override
    public int compareTo(Vector vector) {
        return Double.compare(prevPoint.mZ, latePoint.mZ);
    }
}
