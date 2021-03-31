package com.swarmus.hivear.utils;

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
}
