package fr.leabar.zstructure.io.format;

public class ZStructureFormat {
    public static final int MAGIC_NUMBER = 0x5A535452;

    public static final byte FORMAT_VERSION = 1;

    public static final int HEADER_SIZE = 64;

    public static final int OFFSET_MAGIC = 0;
    public static final int OFFSET_VERSION = 4;
    public static final int OFFSET_FLAGS = 5;
    public static final int OFFSET_RESERVED = 6;
    public static final int OFFSET_WIDTH = 8;
    public static final int OFFSET_HEIGHT = 12;
    public static final int OFFSET_LENGTH = 16;
    public static final int OFFSET_BLOCK_COUNT = 20;
    public static final int OFFSET_CREATION_TIME = 24;
    public static final int OFFSET_INDEX_SIZE = 32;
    public static final int OFFSET_DATA_SIZE = 36;
    public static final int OFFSET_NAME_LENGTH = 40;
    public static final int OFFSET_NAME = 42;

    public static final byte FLAG_SPARSE = 0x01;
    public static final byte FLAG_UNIFORM = 0x02;
    public static final byte FLAG_INDEXED = 0x04;

    public static final int COMPRESSION_LEVEL_FAST = 1;
    public static final int COMPRESSION_LEVEL_DEFAULT = 3;
    public static final int COMPRESSION_LEVEL_MAX = 22;
}