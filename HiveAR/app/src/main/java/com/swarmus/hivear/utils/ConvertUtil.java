package com.swarmus.hivear.utils;

import android.media.Image;

import java.nio.ByteBuffer;

public class ConvertUtil {
    public static double[] convertToDoubleArray(float[] inputArray)
    {
        if (inputArray== null)
            return null;

        double[] output = new double[inputArray.length];
        for (int i = 0; i < inputArray.length; i++)
            output[i] = inputArray[i];

        return output;
    }

    public static float[] convertToFloatArray(double[] inputArray)
    {
        if (inputArray== null)
            return null;

        float[] output = new float[inputArray.length];
        for (int i = 0; i < inputArray.length; i++)
            output[i] = (float) inputArray[i];

        return output;
    }

    // Convert ARCore capture image to nv1 format
    public static byte[] YUV_420_888_to_nv1(Image image) {
        if (image.getFormat() != com.google.ar.core.ImageFormat.YUV_420_888) {
            throw new IllegalArgumentException("Invalid image format");
        }

        byte[] nv21;
        ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
        ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
        ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        nv21 = new byte[ySize + uSize + vSize];

        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        return nv21;
    }
}
