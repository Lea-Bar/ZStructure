package fr.leabar.zstructure.io;


import fr.leabar.zstructure.data.ZStructureData;
import fr.leabar.zstructure.io.format.ZStructureFormat;
import fr.leabar.zstructure.serializer.ZStructureSerializer;
import fr.leabar.zstructure.utils.ZStructureUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class ZStructureWriter {

    private int compressionLevel = ZStructureFormat.COMPRESSION_LEVEL_DEFAULT;

    public ZStructureWriter() {}

    public ZStructureWriter(int compressionLevel) {
        this.compressionLevel = Math.max(1, Math.min(22, compressionLevel));
    }

    public void write(ZStructureData data, File file) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedOutputStream bos = new BufferedOutputStream(fos, 65536)) {
            write(data, bos);
        }
    }

    public void write(ZStructureData data, OutputStream outputStream) throws IOException {
        byte[] blockData = ZStructureSerializer.serializeBlocks(data.getBlocks());
        byte[] compressedBlocks = ZStructureUtils.compress(blockData, compressionLevel);

        byte[] indexData = createSpatialIndex(data);
        byte[] compressedIndex = ZStructureUtils.compress(indexData, compressionLevel);

        writeHeader(outputStream, data, compressedIndex.length, compressedBlocks.length);

        outputStream.write(compressedIndex);
        outputStream.write(compressedBlocks);
        outputStream.flush();
    }

    public CompletableFuture<Void> writeAsync(ZStructureData data, File file) {
        return CompletableFuture.runAsync(() -> {
            try {
                write(data, file);
            } catch (IOException e) {
                throw new RuntimeException("Error during asynchronous write", e);
            }
        });
    }

    private void writeHeader(OutputStream out, ZStructureData data, int indexSize, int dataSize) throws IOException {
        ByteBuffer header = ByteBuffer.allocate(ZStructureFormat.HEADER_SIZE);

        ZStructureUtils.writeInt(header, ZStructureFormat.MAGIC_NUMBER);
        header.put(ZStructureFormat.FORMAT_VERSION);

        byte flags = calculateFlags(data);
        header.put(flags);

        header.putShort((short) 0);

        ZStructureUtils.writeInt(header, data.getWidth());
        ZStructureUtils.writeInt(header, data.getHeight());
        ZStructureUtils.writeInt(header, data.getLength());

        ZStructureUtils.writeInt(header, data.getBlockCount());
        ZStructureUtils.writeLong(header, data.getCreationTime());

        ZStructureUtils.writeInt(header, indexSize);
        ZStructureUtils.writeInt(header, dataSize);
        String name = data.getName();
        if (name.length() > 22) {
            name = name.substring(0, 22);
        }
        header.putShort((short) name.length());
        header.put(name.getBytes(StandardCharsets.UTF_8));

        while (header.position() < ZStructureFormat.HEADER_SIZE) {
            header.put((byte) 0);
        }

        out.write(header.array());
    }

    private byte calculateFlags(ZStructureData data) {
        byte flags = 0;
        long nonAirCount = data.getNonAirBlockCount();
        double density = (double) nonAirCount / data.getBlockCount();

        if (density < 0.7) {
            flags |= ZStructureFormat.FLAG_SPARSE;
        }

        if (data.getBlockCount() > 1000) {
            flags |= ZStructureFormat.FLAG_INDEXED;
        }

        return flags;
    }

    private byte[] createSpatialIndex(ZStructureData data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {
            int chunkSize = 16;
            int chunksX = (data.getWidth() + chunkSize - 1) / chunkSize;
            int chunksY = (data.getHeight() + chunkSize - 1) / chunkSize;
            int chunksZ = (data.getLength() + chunkSize - 1) / chunkSize;
            dos.writeInt(chunkSize);
            dos.writeInt(chunksX);
            dos.writeInt(chunksY);
            dos.writeInt(chunksZ);
            for (int cx = 0; cx < chunksX; cx++) {
                for (int cy = 0; cy < chunksY; cy++) {
                    for (int cz = 0; cz < chunksZ; cz++) {
                        int blockCount = countBlocksInChunk(data, cx, cy, cz, chunkSize);
                        dos.writeInt(blockCount);
                    }
                }
            }
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error creating spatial index", e);
        }
    }

    private int countBlocksInChunk(ZStructureData data, int chunkX, int chunkY, int chunkZ, int chunkSize) {
        int minX = chunkX * chunkSize;
        int maxX = Math.min(minX + chunkSize, data.getWidth());
        int minY = chunkY * chunkSize;
        int maxY = Math.min(minY + chunkSize, data.getHeight());
        int minZ = chunkZ * chunkSize;
        int maxZ = Math.min(minZ + chunkSize, data.getLength());

        return (int) data.getBlocks().stream()
                .filter(block -> block.x() >= minX && block.x() < maxX &&
                        block.y() >= minY && block.y() < maxY &&
                        block.z() >= minZ && block.z() < maxZ)
                .count();
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = Math.max(1, Math.min(22, compressionLevel));
    }
}
