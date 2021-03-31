package com.swarmus.hivear.utils;

public class MathUtil {
    public static double getNorm(double[] vector)
    {
        if (vector.length == 3) {
            return Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2));
        } else if (vector.length == 2) {
            return Math.sqrt(Math.pow(vector[0], 2) + Math.pow(vector[1], 2));
        } else if (vector.length == 1) {
            return vector[0];
        }
        return -1;
    }

    public static float[] getHomogeneous(float[] rot, float[] pos) {
        if (rot.length != 9 || pos.length != 3) {return null;}
        float[] h = new float[16];
        h[0] = rot[0]; h[4] = rot[3] ; h[8] = rot[6];  h[12] = pos[0];
        h[1] = rot[1]; h[5] = rot[4] ; h[9] = rot[7];  h[13] = pos[1];
        h[2] = rot[2]; h[6] = rot[5] ; h[10] = rot[8]; h[14] = pos[2];
        h[3] = 0;      h[7] = 0;       h[11] = 0;      h[15] = 1;
        return h;
    }
}
