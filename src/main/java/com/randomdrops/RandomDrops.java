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
import net.minecraft.text.Text;

import java.util.*;

public class RandomDrops implements ModInitializer {
    private static final Map<Block, Item> blockDrops = new HashMap<>();
    private static final Random random = new Random();

    // Toggle for random drops
    private static boolean enabled = true;

    @Override
    public void onInitialize() {
        generateRandomDrops();

        // ===== LOOT TABLE (natural drops like plants, sand, bamboo, sugar cane, TNT, explosions) =====
        LootTableEvents.MODIFY_DROPS.register((resource, context, drops) -> {
            if (!enabled) return;
            if (context.hasParameter(net.minecraft.loot.context.LootContextParameters.BLOCK_STATE)) {
                Block block = context.get(net.minecraft.loot.context.LootContextParameters.BLOCK_STATE).getBlock();
                Item dropItem = getDrop(block);

                if (dropItem != null && !drops.isEmpty()) { // only replace if vanilla would drop
                    drops.clear();
                    drops.add(new ItemStack(dropItem));
                }
            }
        });

        // ===== PLAYER BREAK =====
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (!enabled || world.isClient()) return true;

            ItemStack hand = player.getMainHandStack();
            if (!canHarvest(state, hand)) return true; // respect vanilla mining rules

            Item drop = getDrop(state.getBlock());
            if (drop != null) {
                Block.dropStack(world, pos, new ItemStack(drop));
                world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
                return false; // cancel vanilla drop
            }

            return true; // allow normal drop if no random drop
        });

        // ===== COMMANDS =====
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    CommandManager.literal("rd")
                            .then(CommandManager.literal("reload")
                                    .executes(context -> {
                                        generateRandomDrops();
                                        context.getSource().sendFeedback(() -> Text.literal("🔄 All block drops have been randomized!"), false);
                                        return 1;
                                    })
                            )
                            .then(CommandManager.literal("true")
                                    .executes(context -> {
                                        enabled = true;
                                        context.getSource().sendFeedback(() -> Text.literal("✅ Random drops enabled"), false);
                                        return 1;
                                    })
                            )
                            .then(CommandManager.literal("false")
                                    .executes(context -> {
                                        enabled = false;
                                        context.getSource().sendFeedback(() -> Text.literal("❌ Random drops disabled"), false);
                                        return 1;
                                    })
                            )
            );
        });
    }

    // ===== RANDOM DROP GENERATOR =====
    private static void generateRandomDrops() {
        blockDrops.clear();

        List<Item> allItems = new ArrayList<>(Registries.ITEM.stream().toList());
        List<Block> allBlocks = new ArrayList<>(Registries.BLOCK.stream().toList());

        Collections.shuffle(allItems, random);

        for (int i = 0; i < allBlocks.size(); i++) {
            blockDrops.put(allBlocks.get(i), allItems.get(i % allItems.size()));
        }
    }

    // ===== RANDOM DROP GETTER =====
    public static Item getDrop(Block block) {
        return blockDrops.getOrDefault(block, null);
    }

    // ===== VANILLA MINING RULES CHECK =====
    private static boolean canHarvest(BlockState state, ItemStack stack) {
        if (!state.isToolRequired()) return true; // blocks that don’t require a proper tool
        return stack.isSuitableFor(state);        // modern method for Fabric 1.21+
    }
}
