package com.randomdrops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.*;

public class RandomDrops implements ModInitializer {
    // Stores which item each block drops
    private static final Map<Block, Item> blockDrops = new HashMap<>();
    private static final Random random = new Random();

    @Override
    public void onInitialize() {
        // Generate initial random drops
        generateRandomDrops();

        // Override block drops
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient()) {
                Block block = state.getBlock();
                Item dropItem = blockDrops.get(block);

                if (dropItem != null) {
                    // Drop only the random item
                    Block.dropStack(world, pos, new ItemStack(dropItem));
                    // Remove the vanilla drop by setting the block to air manually
                    world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
                }
            }
            return false; // cancel normal drop
        });

        // Register /redrop command to reshuffle all drops
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

    // Generates a new random item for each block
    private static void generateRandomDrops() {
        blockDrops.clear();

        List<Item> allItems = new ArrayList<>(Registries.ITEM.stream().toList());
        List<Block> allBlocks = new ArrayList<>(Registries.BLOCK.stream().toList());

        Collections.shuffle(allItems, random);

        for (int i = 0; i < allBlocks.size(); i++) {
            blockDrops.put(allBlocks.get(i), allItems.get(i % allItems.size()));
        }
    }

    // Optional helper to get a block's current random drop
    public static Item getDrop(Block block) {
        return blockDrops.get(block);
    }
}
