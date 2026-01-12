package com.randomdrops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.loot.context.LootContextParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class RandomDrops implements ModInitializer {
    private static final Map<Block, Item> blockDrops = new HashMap<>();
    private static final Random random = new Random();

    @Override
    public void onInitialize() {
        // Generate initial random drops
        generateRandomDrops();

        // Intercept all loot table drops
        LootTableEvents.MODIFY_DROPS.register((resource, context, drops) -> {
            // Only modify block drops
            if (context.hasParameter(LootContextParameters.BLOCK_STATE)) {
                Block block = context.get(LootContextParameters.BLOCK_STATE).getBlock();
                Item dropItem = getDrop(block);

                if (dropItem != null) {
                    drops.clear(); // remove normal drops
                    drops.add(new ItemStack(dropItem));
                }
            }
        });

        // /redrop command to reshuffle all drops
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("redrop")
                            .executes(context -> {
                                generateRandomDrops();
                                ServerCommandSource source = context.getSource();
                                source.sendFeedback(() -> Text.literal("🔄 All block drops have been randomized!"), false);
                                return 1;
                            })
            );
        });
    }

    // Assign a random item to each block
    private static void generateRandomDrops() {
        blockDrops.clear();

        List<Item> allItems = new ArrayList<>(Registries.ITEM.stream().toList());
        List<Block> allBlocks = new ArrayList<>(Registries.BLOCK.stream().toList());

        Collections.shuffle(allItems, random);

        for (int i = 0; i < allBlocks.size(); i++) {
            blockDrops.put(allBlocks.get(i), allItems.get(i % allItems.size()));
        }
    }

    // Get the random drop for a block
    public static Item getDrop(Block block) {
        return blockDrops.getOrDefault(block, null);
    }
}
