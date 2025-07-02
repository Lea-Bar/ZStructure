package fr.leabar.zstructure;

import fr.leabar.zstructure.data.ZStructureBlock;
import fr.leabar.zstructure.data.ZStructureData;
import fr.leabar.zstructure.io.ZStructureReader;
import fr.leabar.zstructure.io.ZStructureWriter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ZStructure {
    private final ZStructureWriter writer;
    private final ZStructureReader reader;

    public ZStructure() {
        writer = new ZStructureWriter();
        reader = new ZStructureReader();
    }

    public ZStructure(int compressionLevel) {
        writer = new ZStructureWriter(compressionLevel);
        reader = new ZStructureReader();
    }

    public ZStructureData captureRegion(World world, Location corner1, Location corner2, String name) {
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int width  = maxX - minX + 1;
        int height = maxY - minY + 1;
        int length = maxZ - minZ + 1;

        List<ZStructureBlock> blocks = new ArrayList<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Block worldBlock = world.getBlockAt(x, y, z);
                    int relX = x - minX;
                    int relY = y - minY;
                    int relZ = z - minZ;
                    blocks.add(ZStructureBlock.from(relX, relY, relZ, worldBlock.getBlockData()));
                }
            }
        }

        return new ZStructureData(name, width, height, length, blocks);
    }

    public CompletableFuture<ZStructureData> captureRegionAsync(World world, Location corner1, Location corner2, String name) {
        return CompletableFuture.supplyAsync(
                () -> captureRegion(world, corner1, corner2, name)
        );
    }

    public void saveStructure(ZStructureData structure, File file) throws IOException {
        writer.write(structure, file);
    }

    public CompletableFuture<Void> saveStructureAsync(ZStructureData structure, File file) {
        return writer.writeAsync(structure, file);
    }

    public ZStructureData loadStructure(File file) throws IOException {
        return reader.read(file);
    }

    public CompletableFuture<ZStructureData> loadStructureAsync(File file) {
        return reader.readAsync(file);
    }

    public ZStructureReader.ZStructureMetadata getStructureInfo(File file) throws IOException {
        return reader.readMetadata(file);
    }

    public void placeStructure(
            ZStructureData structure,
            World world,
            Location location
    ) {
        placeStructure(structure, world, location, block -> true);
    }

    public void placeStructure(
            ZStructureData structure,
            World world,
            Location location,
            Predicate<ZStructureBlock> filter
    ) {
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();

        structure.forEachBlockFiltered(filter, block -> {
            int worldX = baseX + block.x();
            int worldY = baseY + block.y();
            int worldZ = baseZ + block.z();
            world.getBlockAt(worldX, worldY, worldZ).setBlockData(block.getBlockData());
        });
    }

    public CompletableFuture<Void> placeStructureAsync(ZStructureData structure, World world, Location location, Consumer<Double> progressCallback) {
        return CompletableFuture.runAsync(() -> {
            int baseX = location.getBlockX();
            int baseY = location.getBlockY();
            int baseZ = location.getBlockZ();
            List<ZStructureBlock> blocks = structure.getBlocks();
            int total  = blocks.size();
            int placed = 0;

            for (ZStructureBlock block : blocks) {
                int worldX = baseX + block.x();
                int worldY = baseY + block.y();
                int worldZ = baseZ + block.z();
                world.getBlockAt(worldX, worldY, worldZ)
                        .setBlockData(block.getBlockData());
                placed++;
                if (progressCallback != null && placed%100 == 0) {
                    progressCallback.accept((double) placed/total);
                }
            }
            if (progressCallback != null) {
                progressCallback.accept(1.0);
            }
        });
    }

    public static final Predicate<ZStructureBlock> IGNORE_AIR = block -> !block.isAir();
    public static final Predicate<ZStructureBlock> ONLY_SOLID = block -> !block.isAir() && block.material().isSolid();

    public static Predicate<ZStructureBlock> filterByMaterial(Material... materials) {
        return block -> {
            for (Material material : materials) {
                if (block.material() == material) {
                    return true;
                }
            }
            return false;
        };
    }

    public static Predicate<ZStructureBlock> filterByRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return block ->
                block.x() >= minX && block.x() <= maxX &&
                        block.y() >= minY && block.y() <= maxY &&
                        block.z() >= minZ && block.z() <= maxZ;
    }

    public StructureStats analyzeStructure(ZStructureData structure) {
        return new StructureStats(structure);
    }

    public static class StructureStats {
        private final ZStructureData structure;
        private final long nonAirBlocks;
        private final Map<Material, Integer> materialCount;

        public StructureStats(ZStructureData structure) {
            this.structure = structure;
            this.nonAirBlocks  = structure.getNonAirBlockCount();
            this.materialCount = new HashMap<>();
            structure.forEachBlock(
                    block -> materialCount.merge(block.material(), 1, Integer::sum)
            );
        }

        public int getTotalBlocks() {
            return structure.getBlockCount();
        }

        public long getNonAirBlocks() {
            return nonAirBlocks;
        }

        public double getDensity() {
            return (double) nonAirBlocks/structure.getBlockCount();
        }

        public int getUniqueBlockTypes() {
            return materialCount.size();
        }

        public Map<Material, Integer> getMaterialDistribution() {
            return materialCount;
        }

        public Material getMostCommonBlock() {
            return materialCount.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(Material.AIR);
        }

        @Override
        public String toString() {
            return String.format("Structure '%s': %d blocks (%d non-air, %.1f%% density, %d types)",
                    structure.getName(),
                    getTotalBlocks(),
                    getNonAirBlocks(),
                    getDensity()*100,
                    getUniqueBlockTypes()
            );
        }
    }

    public void setCompressionLevel(int level) {
        writer.setCompressionLevel(level);
    }

    public int getCompressionLevel() {
        return writer.getCompressionLevel();
    }
}
