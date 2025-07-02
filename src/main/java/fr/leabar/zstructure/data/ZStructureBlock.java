package fr.leabar.zstructure.data;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public record ZStructureBlock(
        int x,
        int y,
        int z,
        Material material,
        String blockDataString
) {
    public static ZStructureBlock from(int x, int y, int z, BlockData blockData) {
        return new ZStructureBlock(x, y, z, blockData.getMaterial(), blockData.getAsString());
    }

    public BlockData getBlockData() {
        return Bukkit.createBlockData(blockDataString);
    }

    public boolean isAir(){
        return material == Material.AIR || material == Material.VOID_AIR || material == Material.CAVE_AIR;
    }

}
