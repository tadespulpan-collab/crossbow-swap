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

    // Looking up is negative pitch. The bow triggers while your pitch is inside this range.
    private static final float PITCH_MIN = -75.0f;
    private static final float PITCH_MAX = -70.0f;

    private boolean wasUseDown = false;
    private boolean wasInRange = false;
    private boolean autoDraw = false;
    private int autoTicks = 0;

    @Override
    public void onInitializeClient() {
        // START_CLIENT_TICK runs before the game processes input, so changes apply to this same tick.
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null) {
                if (autoDraw) {
                    client.options.useKey.setPressed(false);
                    autoDraw = false;
                }
                wasUseDown = false;
                wasInRange = false;
                return;
            }

            // ---------- Crossbow ----------
            // Right click (new press only) with an empty crossbow -> switch to a loaded one.
            // Holding sneak turns the swap off so you can charge the empty crossbow normally.
            boolean useDown = client.options.useKey.isPressed();
            boolean sneakDown = client.options.sneakKey.isPressed();
            if (!autoDraw && useDown && !wasUseDown && !sneakDown) {
                trySwap(client);
            }
            wasUseDown = useDown;

            // ---------- Bow ----------
            // Your aim is never touched, you still point the bow yourself.
            float pitch = client.player.getPitch();
            boolean inRange = pitch >= PITCH_MIN && pitch <= PITCH_MAX;
            boolean holdingBow = client.player.getMainHandStack().isOf(Items.BOW);
            boolean drawingBow = client.player.isUsingItem()
                    && client.player.getActiveItem().isOf(Items.BOW);

            // Entering the angle range with a bow in hand -> start drawing (once per entry).
            if (holdingBow && inRange && !wasInRange && !client.player.isUsingItem() && !autoDraw) {
                client.options.useKey.setPressed(true);
                autoDraw = true;
                autoTicks = 0;
            }
            wasInRange = inRange;

            if (autoDraw) {
                autoTicks++;
                if (drawingBow && client.player.getItemUseTime() >= MIN_BOW_TICKS
                        && client.interactionManager != null) {
                    client.interactionManager.stopUsingItem(client.player);
                    client.options.useKey.setPressed(false);
                    autoDraw = false;
                } else if (!holdingBow || autoTicks > 40) {
                    // Safety: no arrows, bow swapped away, or the draw never started.
                    client.options.useKey.setPressed(false);
                    autoDraw = false;
                }
            } else if (inRange && drawingBow
                    && client.player.getItemUseTime() >= MIN_BOW_TICKS
                    && client.interactionManager != null) {
                // Manual draw (you hold right click yourself) while inside the range -> release at minimum charge.
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
