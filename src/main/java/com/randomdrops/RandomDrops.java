package com.randomdrops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class RandomDrops implements ModInitializer {
    private static final Map<Block, Item> blockDrops = new HashMap<>();
    private static final Random random = new Random();

    @Override
    public void onInitialize() {
        generateRandomDrops();

        // ===== LOOT TABLE (natural breaks like sand, bamboo, sugar cane, explosions, TNT) =====
        LootTableEvents.MODIFY_DROPS.register((resource, context, drops) -> {
            if (context.hasParameter(LootContextParameters.BLOCK_STATE)) {
                Block block = context.get(LootContextParameters.BLOCK_STATE).getBlock();
                Item dropItem = getDrop(block);

                if (dropItem != null) {
                    drops.clear();
                    drops.add(new ItemStack(dropItem));
                }
            }
        });

        // ===== PLAYER BREAK =====
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!world.isClient()) {
                Item drop = getDrop(state.getBlock());
                if (drop != null) {
                    Block.dropStack(world, pos, new ItemStack(drop));
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                    return false;
                }
            }
            return true;
        });

        // ===== COMMAND =====
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

    private static void generateRandomDrops() {
        blockDrops.clear();

        List<Item> allItems = new ArrayList<>(Registries.ITEM.stream().toList());
        List<Block> allBlocks = new ArrayList<>(Registries.BLOCK.stream().toList());

        Collections.shuffle(allItems, random);

        for (int i = 0; i < allBlocks.size(); i++) {
            blockDrops.put(allBlocks.get(i), allItems.get(i % allItems.size()));
        }
    }

    public static Item getDrop(Block block) {
        return blockDrops.getOrDefault(block, null);
    }
}
