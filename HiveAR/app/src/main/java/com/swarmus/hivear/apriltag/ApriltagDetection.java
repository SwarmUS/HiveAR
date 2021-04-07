package com.swarmus.hivear.apriltag;

public class ApriltagDetection {
    // The decoded ID of the tag
    public int id;

    // How many error bits were corrected? Note: accepting large numbers of
    // corrected errors leads to greatly increased false positive rates.
    // NOTE: As of this implementation, the detector cannot detect tags with
    // a hamming distance greater than 2.
    public int hamming;

    // The center of the detection in image pixel coordinates.
    public double[] c = new double[2];

    // The corners of the tag in image pixel coordinates. These always
    // wrap counter-clock wise around the tag.
    // Flattened to [x0 y0 x1 y1 ...] for JNI convenience
    public double[] p = new double[8];

    // Flattened to [x1, x2, x3, y1, y2, y3, z1, z2, z3]
    public double[] pose_r = new double[9];

    public double[] pose_t = new double[3];
}