package com.swarmus.hivear.utils;

import com.google.ar.sceneform.math.Quaternion;

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

    public static String homogeneousToString(float[] m) {
        String s = "\nHomogeneous\n";
        if (m.length == 16) {
            s += "[" + m[0] + "\t" + m[4] + "\t" + m[8]  + "\t" + m[12] + " ]\n";
            s += "[" + m[1] + "\t" + m[5] + "\t" + m[9]  + "\t" + m[13] + " ]\n";
            s += "[" + m[2] + "\t" + m[6] + "\t" + m[10] + "\t" + m[14] + " ]\n";
            s += "[" + m[3] + "\t" + m[7] + "\t" + m[11] + "\t" + m[15] + " ]";
        }
        return s;
    }

    public static final Quaternion getQuaternion(float[] m) {
        float t = m[0] + m[5] + m[10];
        Quaternion q = new Quaternion();

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            float s = (float)Math.sqrt((double)(t + 1)); // |s|>=1 ...
            q.w = 0.5f * s;
            s = 0.5f / s;                 // so this division isn't bad
            q.x = (m[6] - m[9]) * s;
            q.y = (m[8] - m[2]) * s;
            q.z = (m[1] - m[4]) * s;
        } else if ((m[0] > m[5]) && (m[0] > m[10])) {
            float s = (float)Math.sqrt((double)(1.0f + m[0] - m[5] - m[10])); // |s|>=1
            q.x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            q.y = (m[1] + m[4]) * s;
            q.z = (m[8] + m[2]) * s;
            q.w = (m[6] - m[9]) * s;
        } else if (m[5] > m[10]) {
            float s = (float)Math.sqrt((double)(1.0f + m[5] - m[0] - m[10])); // |s|>=1
            q.y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            q.x = (m[1] +m[4]) * s;
            q.z = (m[6] + m[9]) * s;
            q.w = (m[8] - m[2]) * s;
        } else {
            float s = (float)Math.sqrt((double)(1.0f + m[10] - m[0] - m[5])); // |s|>=1
            q.z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            q.x = (m[8] + m[2]) * s;
            q.y = (m[6] + m[9]) * s;
            q.w = (m[1] - m[4]) * s;
        }
        return q;
    }

    public static final Quaternion convertToRightHanded(Quaternion input) {
        return new Quaternion(
                -input.x,   // -(  right = -left  )
                input.y,   // -(     up =  up     )
                input.z,   // -(forward =  forward)
                -input.w
        );
    }
}
