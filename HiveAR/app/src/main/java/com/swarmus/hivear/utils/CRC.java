package com.swarmus.hivear.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static com.swarmus.hivear.utils.Serialization.int32ToBytes;

public final class CRC {
    private CRC() {}

    public static byte[] CalculateCRC32(byte[] data, int length)
    {
        int i, j;
        IntBuffer intBuf =
                ByteBuffer.wrap(data)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asIntBuffer();
        int[] intArray = new int[intBuf.remaining()];
        intBuf.get(intArray);
        int Crc = 0xffffffff;
        for (i = 0; i < intArray.length; i++) {
            Crc = Crc ^ intArray[i];
            for (j = 0; j < 32; j++)
                if ((Crc & 0x80000000) != 0)
                    Crc = (Crc << 1) ^ 0x04C11DB7; // Polynomial used in STM32
                else
                    Crc = (Crc << 1);
        }
        return int32ToBytes(Crc);
    }
}
