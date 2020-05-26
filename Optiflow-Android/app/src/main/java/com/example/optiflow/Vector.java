package com.example.optiflow;

import androidx.annotation.NonNull;

public class Vector {
    private int prevX;
    private int prevY;
    private int prevZ;
    private int X;
    private int Y;
    private int Z;
    private int changeX;
    private int changeY;
    private int changeZ;

    public Vector(int prevX, int prevY, int prevZ, int x, int y, int z) {
        this.prevX = prevX;
        this.prevY = prevY;
        this.prevZ = prevZ;
        X = x;
        Y = y;
        Z = z;
        changeX = X - this.prevX;
        changeY = Y - this.prevY;
        changeZ = Z - this.prevZ;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("%d,%d,%d,%d,%d,%d", prevX, prevY, prevZ, X, Y, Z);
    }

    public int[] changeVec() {
        return new int[]{changeX, changeY, changeZ};
    }
}
