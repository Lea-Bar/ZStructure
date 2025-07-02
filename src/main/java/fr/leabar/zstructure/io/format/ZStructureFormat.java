package fr.leabar.zstructure.io.format;

public class ZStructureFormat {
    public static final int MAGIC_NUMBER = 0x5A535452;

    public static final byte FORMAT_VERSION = 1;

    public static final int HEADER_SIZE = 64;


    public static final byte FLAG_SPARSE = 0x01;
    public static final byte FLAG_UNIFORM = 0x02;
    public static final byte FLAG_INDEXED = 0x04;

    public static final int COMPRESSION_LEVEL_DEFAULT = 3;
}