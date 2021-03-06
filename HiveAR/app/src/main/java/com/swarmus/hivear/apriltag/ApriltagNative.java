package com.swarmus.hivear.apriltag;

import android.graphics.Bitmap;

import java.util.ArrayList;

/**
 * Interface to native C AprilTag library.
 */

public class ApriltagNative {
    static {
        System.loadLibrary("apriltag");
        native_init();
    }

    public static native void native_init();

    public static native void apriltag_init(String tagFamily, int errorBits, double decimateFactor,
                                            double blurSigma, int nthreads);

    public static native ArrayList<ApriltagDetection> apriltag_detect_yuv(byte[] src, int width, int height, double tagWidth, double[] principalPoint, double[] focalLength);
}
