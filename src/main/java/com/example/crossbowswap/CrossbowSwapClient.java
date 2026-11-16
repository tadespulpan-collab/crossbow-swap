package com.example.crossbowswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class CrossbowSwapClient implements ClientModInitializer {
    private boolean wasUseDown = false;

    @Override
    public void onInitializeClient() {
        // START_CLIENT_TICK runs before the game processes input, so the new slot is already active for this click.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) {
                wasUseDown = false;
                return;
            }

            boolean useDown = client.options.useKey.isPressed();
            boolean sneakDown = client.options.sneakKey.isPressed();

            // Right click (new press only) with an empty crossbow -> switch to a loaded one.
            // Holding sneak turns the swap off so you can charge the empty crossbow normally.
            if (useDown && !wasUseDown && !sneakDown) {
                trySwap(client);
            }

            wasUseDown = useDown;
        });
    }

    private void trySwap(MinecraftClient client) {
        PlayerInventory inv = client.player.getInventory();
        ItemStack held = client.player.getMainHandStack();

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
