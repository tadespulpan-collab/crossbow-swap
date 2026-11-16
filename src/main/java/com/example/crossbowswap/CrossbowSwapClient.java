package com.example.crossbowswap;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

public class CrossbowSwapClient implements ClientModInitializer {
    // Ticks of draw before the bow is released. 3 is the minimum that fires an arrow.
    // If arrows don't fire, change this to 4.
    private static final int MIN_BOW_TICKS = 3;

    // Looking up is negative pitch. The bow auto-releases while your pitch is inside this range.
    private static final float PITCH_MIN = -75.0f;
    private static final float PITCH_MAX = -70.0f;

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

            // Crossbow: right click (new press only) with an empty crossbow -> switch to a loaded one.
            // Holding sneak turns the swap off so you can charge the empty crossbow normally.
            if (useDown && !wasUseDown && !sneakDown) {
                trySwap(client);
            }
            wasUseDown = useDown;

            // Bow: while drawing a bow with your pitch between -75 and -70, release at minimum charge.
            // Your aim is never touched, you still point the bow yourself.
            float pitch = client.player.getPitch();
            if (pitch >= PITCH_MIN && pitch <= PITCH_MAX
                    && client.interactionManager != null
                    && client.player.isUsingItem()
                    && client.player.getActiveItem().isOf(Items.BOW)
                    && client.player.getItemUseTime() >= MIN_BOW_TICKS) {
                client.interactionManager.stopUsingItem(client.player);
            }
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
