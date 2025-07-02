package fr.leabar.zstructure.data;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;


public class ZStructureData {
    private final int width;
    private final int height;
    private final int length;
    private final List<ZStructureBlock> blocks;
    private final String name;
    private final long creationTime;
    private Map<String, ZStructureBlock> positionCache;
    private Map<Integer, List<ZStructureBlock>> layerCache;
    private boolean cacheBuilt = false;
    private final Object cacheLock = new Object();

    public ZStructureData(String name, int width, int height, int length, List<ZStructureBlock> blocks) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.length = length;
        this.blocks = List.copyOf(blocks);
        this.creationTime = System.currentTimeMillis();
    }

    private void buildCache() {
        if (cacheBuilt) return;

        synchronized (cacheLock) {
            if (cacheBuilt) return;
            positionCache = new HashMap<>(blocks.size());
            layerCache = new HashMap<>();
            for (ZStructureBlock block : blocks) {
                String key = block.x() + "," + block.y() + "," + block.z();
                positionCache.put(key, block);
                layerCache.computeIfAbsent(block.y(), k -> new ArrayList<>()).add(block);
            }
            cacheBuilt = true;
        }
    }


    public void forEachBlock(Consumer<ZStructureBlock> consumer) {
        blocks.forEach(consumer);
    }

    public CompletableFuture<Void> forEachBlockAsync(Consumer<ZStructureBlock> consumer) {
        return CompletableFuture.runAsync(() -> blocks.parallelStream().forEach(consumer));
    }


    public void forEachBlockFiltered(Predicate<ZStructureBlock> filter, Consumer<ZStructureBlock> consumer) {
        blocks.stream().filter(filter).forEach(consumer);
    }

    public ZStructureBlock getBlockAt(int x, int y, int z) {
        buildCache();
        String key = x + "," + y + "," + z;
        return positionCache.get(key);
    }


    public List<ZStructureBlock> getBlocksAtLayer(int y) {
        buildCache();
        return layerCache.getOrDefault(y, List.of());
    }

    public void forEachBlockInLayer(int y, Consumer<ZStructureBlock> consumer) {
        getBlocksAtLayer(y).forEach(consumer);
    }


    public void forEachBlockInRegion(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Consumer<ZStructureBlock> consumer) {
        blocks.stream()
                .filter(block -> block.x() >= minX && block.x() <= maxX &&
                        block.y() >= minY && block.y() <= maxY &&
                        block.z() >= minZ && block.z() <= maxZ)
                .forEach(consumer);
    }


    public List<ZStructureBlock> findBlocksByMaterial(Material material) {
        return blocks.stream()
                .filter(block -> block.material() == material)
                .toList();
    }

    public boolean isValidPosition(int x, int y, int z) {
        return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < length;
    }

    public int getVolume() {
        return width * height * length;
    }


    public double getDensity() {
        return (double) getNonAirBlockCount()/getVolume();
    }

    public int[] getCenter() {
        return new int[]{width/2, height/2, length/2};
    }

    public ZStructureData clone(String newName) {
        return new ZStructureData(newName, width, height, length, blocks);
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

    public List<ZStructureBlock> getBlocks() {
        return blocks;
    }
    public long getCreationTime() {
        return creationTime;
    }

    public int getBlockCount() {
        return blocks.size();
    }


    public long getNonAirBlockCount() {
        return blocks.stream().filter(block -> !block.isAir()).count();
    }
}
