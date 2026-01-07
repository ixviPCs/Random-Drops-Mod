package com.randomdrops;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;

import java.util.*;

public class RandomBlockDrops {
    public static final Map<Block, Item> BLOCK_DROPS = new HashMap<>();

    public static void generateRandomDrops() {
        List<Block> blocks = new ArrayList<>(Registries.BLOCK.stream().toList());
        List<Item> items = new ArrayList<>(Registries.ITEM.stream().toList());

        Collections.shuffle(items); // randomize items

        // assign a unique item to each block
        for (int i = 0; i < blocks.size() && i < items.size(); i++) {
            BLOCK_DROPS.put(blocks.get(i), items.get(i));
        }
    }

    public static Item getDrop(Block block) {
        return BLOCK_DROPS.get(block);
    }
}
