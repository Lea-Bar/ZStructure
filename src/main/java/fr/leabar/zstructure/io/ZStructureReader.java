package fr.leabar.zstructure.io;

import fr.leabar.zstructure.data.ZStructureBlock;
import fr.leabar.zstructure.data.ZStructureData;
import fr.leabar.zstructure.io.format.ZStructureFormat;
import fr.leabar.zstructure.serializer.ZStructureSerializer;
import fr.leabar.zstructure.utils.ZStructureUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ZStructureReader {

    private ByteBuffer headerBuffer = ByteBuffer.allocate(ZStructureFormat.HEADER_SIZE);

    public ZStructureData read(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis, 65536)) {
            return read(bis);
        }
    }

    public ZStructureData read(InputStream inputStream) throws IOException {
        HeaderInfo header = readHeader(inputStream);
        byte[] compressedIndex = inputStream.readNBytes(header.indexSize);
        byte[] indexData = ZStructureUtils.decompress(compressedIndex, estimateIndexSize(header));
        byte[] compressedBlocks = inputStream.readNBytes(header.dataSize);
        byte[] blockData = ZStructureUtils.decompress(compressedBlocks, estimateBlockSize(header));
        List<ZStructureBlock> blocks = ZStructureSerializer.deserializeBlocks(blockData);
        return new ZStructureData(header.name, header.width, header.height, header.length, blocks);
    }

    public CompletableFuture<ZStructureData> readAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return read(file);
            } catch (IOException e) {
                throw new RuntimeException("Error during async", e);
            }
        });
    }

    public ZStructureMetadata readMetadata(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file); BufferedInputStream bis = new BufferedInputStream(fis, ZStructureFormat.HEADER_SIZE + 1024)) {
            HeaderInfo header = readHeader(bis);
            return new ZStructureMetadata(header.name, header.width, header.height, header.length, header.blockCount, header.creationTime, header.flags, file.length());
        }
    }

    private HeaderInfo readHeader(InputStream inputStream) throws IOException {
        headerBuffer.clear();
        byte[] headerBytes = inputStream.readNBytes(ZStructureFormat.HEADER_SIZE);
        if (headerBytes.length != ZStructureFormat.HEADER_SIZE) {
            throw new IOException("ZSTRUCT header invalid");
        }
        headerBuffer.put(headerBytes);
        headerBuffer.flip();
        int magic = ZStructureUtils.readInt(headerBuffer);
        if (magic != ZStructureFormat.MAGIC_NUMBER) {
            throw new IOException("ZSTRUCT file invalid (magic number incorrect)");
        }
        byte version = headerBuffer.get();
        if (version != ZStructureFormat.FORMAT_VERSION) {
            throw new IOException("ZSTRUCT FILE VERSION INVALID : " + version);
        }

        HeaderInfo header = new HeaderInfo();
        header.flags = headerBuffer.get();
        headerBuffer.getShort();
        header.width = ZStructureUtils.readInt(headerBuffer);
        header.height = ZStructureUtils.readInt(headerBuffer);
        header.length = ZStructureUtils.readInt(headerBuffer);
        header.blockCount = ZStructureUtils.readInt(headerBuffer);
        header.creationTime = ZStructureUtils.readLong(headerBuffer);
        header.indexSize = ZStructureUtils.readInt(headerBuffer);
        header.dataSize = ZStructureUtils.readInt(headerBuffer);
        short nameLength = headerBuffer.getShort();
        if (nameLength > 22 || nameLength < 0) {
            throw new IOException("Length of the name invalid: " + nameLength);
        }

        byte[] nameBytes = new byte[nameLength];
        headerBuffer.get(nameBytes);
        header.name = new String(nameBytes, StandardCharsets.UTF_8);

        return header;
    }


    private int estimateIndexSize(HeaderInfo header) {
        int chunkSize = 16;
        int chunksX = (header.width+chunkSize-1)/chunkSize;
        int chunksY = (header.height+chunkSize-1)/chunkSize;
        int chunksZ = (header.length+chunkSize-1)/chunkSize;
        return 16 + (chunksX*chunksY*chunksZ*4);
    }

    private int estimateBlockSize(HeaderInfo header) {
        return header.blockCount*50;
    }


    private static class HeaderInfo {
        String name;
        int width, height, length, blockCount, indexSize, dataSize;
        long creationTime;
        byte flags;
    }


    public static class ZStructureMetadata {
        private final String name;
        private final int width, height, length, blockCount;
        private final long creationTime, fileSize;
        private final byte flags;

        public ZStructureMetadata(String name, int width, int height, int length, int blockCount, long creationTime, byte flags, long fileSize) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.length = length;
            this.blockCount = blockCount;
            this.creationTime = creationTime;
            this.flags = flags;
            this.fileSize = fileSize;
        }

        public String getName() {
            return name;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getLength() {
            return length;
        }

        public int getBlockCount() {
            return blockCount;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public byte getFlags() {
            return flags;
        }

        public long getFileSize() {
            return fileSize;
        }

        public boolean isSparse() {
            return (flags & ZStructureFormat.FLAG_SPARSE) != 0;
        }

        public boolean isUniform() {
            return (flags & ZStructureFormat.FLAG_UNIFORM) != 0;
        }

        public boolean isIndexed() {
            return (flags & ZStructureFormat.FLAG_INDEXED) != 0;
        }

        public double getCompressionRatio() {
            double estimatedUncompressed = blockCount * 50.0;
            return estimatedUncompressed / fileSize;
        }

        @Override
        public String toString() {
            return String.format("ZStructure '%s' [%dx%dx%d, %d blocs, %.1f%% compression]", name, width, height, length, blockCount, (getCompressionRatio() - 1) * 100);
        }
    }
}
