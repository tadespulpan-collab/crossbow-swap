package com.example.crossbowswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class CrossbowSwapClient implements ClientModInitializer {
    private boolean wasDown = false;

    @Override
    public void onInitializeClient() {
        // START_CLIENT_TICK runs before the game processes input, so the new slot is already active for this click.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) {
                wasDown = false;
                return;
            }
            boolean down = client.options.attackKey.isPressed();
            if (down && !wasDown) {
                trySwap(client);
            }
            wasDown = down;
        });
    }

    private void trySwap(MinecraftClient client) {
        PlayerInventory inv = client.player.getInventory();
        ItemStack held = inv.getMainHandStack();

        // Only act if the held item is an empty crossbow.
        if (!held.isOf(Items.CROSSBOW) || CrossbowItem.isCharged(held)) return;

        for (int i = 0; i < 9; i++) {
            if (i == inv.getSelectedSlot()) continue;
            ItemStack s = inv.getStack(i);
            if (s.isOf(Items.CROSSBOW) && CrossbowItem.isCharged(s)) {
                inv.setSelectedSlot(i);
                return;
            }
        }
    }
}
