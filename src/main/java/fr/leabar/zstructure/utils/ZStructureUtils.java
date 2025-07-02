package fr.leabar.zstructure.utils;

import com.github.luben.zstd.Zstd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class ZStructureUtils {

    public static byte[] compress(byte[] data, int level) {
        return Zstd.compress(data, level);
    }

    public static byte[] decompress(byte[] compressedData, int originalSize) {
        return Zstd.decompress(compressedData, originalSize);
    }

    public static void writeInt(ByteBuffer buffer, int value) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(value);
    }

    public static int readInt(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }


    public static void writeLong(ByteBuffer buffer, long value) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
    }

    public static long readLong(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getLong();
    }


    public static void writeString(ByteBuffer buffer, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buffer.putShort((short) bytes.length);
        buffer.put(bytes);
    }


    public static String readString(ByteBuffer buffer) {
        short length = buffer.getShort();
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }


    public static int estimateCompressedSize(int originalSize) {
        return (originalSize/2)+1024;
    }

    public static ByteBuffer createWriteBuffer(int estimatedSize) {
        return ByteBuffer.allocateDirect(estimatedSize).order(ByteOrder.LITTLE_ENDIAN);
    }


    public static ByteBuffer wrapBuffer(byte[] data) {
        return ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    }
}