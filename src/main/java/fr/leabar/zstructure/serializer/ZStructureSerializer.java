package fr.leabar.zstructure.serializer;

import fr.leabar.zstructure.data.ZStructureBlock;
import fr.leabar.zstructure.io.format.ZStructureFormat;
import org.bukkit.Material;

import java.io.*;
import java.util.*;

public class ZStructureSerializer {
    private static final Map<String, Material> MATERIAL_CACHE = new HashMap<>();
    private static final Map<Material, Integer> MATERIAL_ID_CACHE = new HashMap<>();
    private static final List<Material> ID_TO_MATERIAL = new ArrayList<>();

    static {
        initializeMaterialCache();
    }

    public static byte[] serializeBlocks(List<ZStructureBlock> blocks) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            BlockAnalysis analysis = analyzeBlocks(blocks);
            dos.writeByte(analysis.flags);
            dos.writeInt(analysis.uniqueMaterials.size());

            writeMaterialPalette(dos, analysis.uniqueMaterials);

            if ((analysis.flags & ZStructureFormat.FLAG_SPARSE) != 0) {
                writeSparseBocks(dos, blocks, analysis);
            } else {
                writeRegularBlocks(dos, blocks, analysis);
            }

            dos.flush();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error during the serialization of blocks.", e);
        }
    }

    public static List<ZStructureBlock> deserializeBlocks(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        try {
            byte flags = dis.readByte();
            int paletteSize = dis.readInt();
            List<Material> palette = readMaterialPalette(dis, paletteSize);
            if ((flags & ZStructureFormat.FLAG_SPARSE) != 0) {
                return readSparseBlocks(dis, palette);
            } else {
                return readRegularBlocks(dis, palette);
            }

        } catch (IOException e) {
            throw new RuntimeException("Error during the serialization of blocks.", e);
        }
    }

    private static BlockAnalysis analyzeBlocks(List<ZStructureBlock> blocks) {
        BlockAnalysis analysis = new BlockAnalysis();
        Set<Material> materials = new HashSet<>();
        int airBlocks = 0;

        for (ZStructureBlock block : blocks) {
            materials.add(block.material());
            if (block.isAir()) {
                airBlocks++;
            }
        }

        analysis.uniqueMaterials = new ArrayList<>(materials);
        double airRatio = (double) airBlocks/blocks.size();
        if (airRatio > 0.3) {
            analysis.flags |= ZStructureFormat.FLAG_SPARSE;
        }

        if (materials.size() < 10) {
            analysis.flags |= ZStructureFormat.FLAG_UNIFORM;
        }

        return analysis;
    }

    private static void writeMaterialPalette(DataOutputStream dos, List<Material> materials) throws IOException {
        for (Material material : materials) {
            dos.writeUTF(material.name());
        }
    }

    private static List<Material> readMaterialPalette(DataInputStream dis, int size) throws IOException {
        List<Material> palette = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String name = dis.readUTF();
            Material material = MATERIAL_CACHE.computeIfAbsent(name, Material::valueOf);
            palette.add(material);
        }
        return palette;
    }

    private static void writeSparseBocks(DataOutputStream dos, List<ZStructureBlock> blocks, BlockAnalysis analysis) throws IOException {
        List<ZStructureBlock> nonAirBlocks = blocks.stream()
                .filter(block -> !block.isAir())
                .toList();

        dos.writeInt(nonAirBlocks.size());
        for (ZStructureBlock block : nonAirBlocks) {
            dos.writeShort(block.x());
            dos.writeShort(block.y());
            dos.writeShort(block.z());
            dos.writeByte(analysis.uniqueMaterials.indexOf(block.material()));
            dos.writeUTF(block.blockDataString());
        }
    }

    private static void writeRegularBlocks(DataOutputStream dos, List<ZStructureBlock> blocks, BlockAnalysis analysis) throws IOException {
        dos.writeInt(blocks.size());
        for (ZStructureBlock block : blocks) {
            dos.writeShort(block.x());
            dos.writeShort(block.y());
            dos.writeShort(block.z());
            dos.writeByte(analysis.uniqueMaterials.indexOf(block.material()));
            dos.writeUTF(block.blockDataString());
        }
    }


    private static List<ZStructureBlock> readSparseBlocks(DataInputStream dis, List<Material> palette) throws IOException {
        int blockCount = dis.readInt();
        List<ZStructureBlock> blocks = new ArrayList<>(blockCount);

        for (int i = 0; i < blockCount; i++) {
            int x = dis.readShort();
            int y = dis.readShort();
            int z = dis.readShort();
            Material material = palette.get(dis.readByte() & 0xFF);
            String blockData = dis.readUTF();

            blocks.add(new ZStructureBlock(x, y, z, material, blockData));
        }

        return blocks;
    }


    private static List<ZStructureBlock> readRegularBlocks(DataInputStream dis, List<Material> palette) throws IOException {
        int blockCount = dis.readInt();
        List<ZStructureBlock> blocks = new ArrayList<>(blockCount);
        for (int i = 0; i < blockCount; i++) {
            int x = dis.readShort();
            int y = dis.readShort();
            int z = dis.readShort();
            Material material = palette.get(dis.readByte() & 0xFF);
            String blockData = dis.readUTF();
            blocks.add(new ZStructureBlock(x, y, z, material, blockData));
        }
        return blocks;
    }

    private static void initializeMaterialCache() {
        String[] commonMaterials = {
                "AIR", "STONE", "DIRT", "GRASS_BLOCK", "COBBLESTONE", "OAK_PLANKS",
                "OAK_LOG", "GLASS", "WATER", "LAVA", "SAND", "GRAVEL", "BEDROCK"
        };

        for (int i = 0; i < commonMaterials.length; i++) {
            try {
                Material material = Material.valueOf(commonMaterials[i]);
                MATERIAL_CACHE.put(commonMaterials[i], material);
                MATERIAL_ID_CACHE.put(material, i);
                ID_TO_MATERIAL.add(material);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static class BlockAnalysis {
        byte flags = 0;
        List<Material> uniqueMaterials = new ArrayList<>();
    }
}