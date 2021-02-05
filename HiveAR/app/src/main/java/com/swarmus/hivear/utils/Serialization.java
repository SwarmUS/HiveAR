package com.swarmus.hivear.utils;

public final class Serialization {
    private Serialization() {}

    public static byte[] int32ToBytes(final int data) {
        return new byte[] {
                (byte)((data) & 0xff),
                (byte)((data >> 8) & 0xff),
                (byte)((data >> 16) & 0xff),
                (byte)((data >> 24) & 0xff),
        };
    }

    public static byte[] int16ToBytes(final short data) {
        return new byte[] {
                (byte)((data) & 0xff),
                (byte)((data >> 8) & 0xff),
        };
    }

    public static int bytesToInt16(final byte[] data) {
        return java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN).getShort();
    }

    public static int bytesToInt32(final byte[] data) {
        return java.nio.ByteBuffer.wrap(data).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
    }
}

