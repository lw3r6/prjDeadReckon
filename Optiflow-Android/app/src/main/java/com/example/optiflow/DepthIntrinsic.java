package com.example.optiflow;

import com.intel.realsense.librealsense.DistortionType;
import com.intel.realsense.librealsense.Intrinsic;

public class DepthIntrinsic extends Intrinsic {

    private int mWidth;
    private int mHeight;
    private float mPpx;
    private float mPpy;
    private float mFx;
    private float mFy;
    private DistortionType mModel;
    private int mModelValue;
    private float[] mCoeffs;

    DepthIntrinsic(Intrinsic in){

    }
}
