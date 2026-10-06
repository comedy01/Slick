package dev.slick.gui;

import dev.slick.client.SlickClient;
import dev.slick.config.SlickConfig;
import dev.slick.core.Glide;
import dev.slick.core.Motion;
import dev.slick.core.Speed;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HotbarRenderer {
    public static final int SLOTS = 9;

    private static final Glide GLIDE = new Glide(SLOTS);
    private static final Motion CLOCK = new Motion();
    private static final float[] LIFT = new float[SLOTS];

    private static long frames;

    public interface ItemPainter {
        void draw(int x, int y, ItemStack stack, int seed);
    }

    private HotbarRenderer() {
    }

    public static boolean active() {
        return SlickClient.config().hotbar();
    }

    public static float glidePosition() {
        return GLIDE.position();
    }

    public static long frames() {
        return frames;
    }

    public static void draw(Canvas canvas, Player player, ItemPainter items, boolean attackIndicator) {
        SlickConfig config = SlickClient.config();
        Speed speed = config.speed();
        Skin skin = Skin.of(canvas);
        float dt = CLOCK.tick(System.nanoTime());
        int selected = HeldSlot.get(player.getInventory());
        boolean boxes = config.hotbarSlots();

        int center = canvas.width() / 2;
        int bottom = canvas.height();
        int left = center - 91;
        int top = bottom - 22;

        float position = GLIDE.update(selected, speed.rate(), dt, config.wrap());
        canvas.batch(() -> {
            skin.hotbarBase(left, top, 182, 22);
            for (int i = 0; i < SLOTS; i++) {
                skin.hotbarSlot(left + 2 + i * 20, top + 2, 18, 18, boxes);
            }
            canvas.scissor(left - 1, top - 1, left + 183, bottom);
            skin.selection(left + 1 + position * 20.0F, top + 1, 20, 20);
            if (position > SLOTS - 1) {
                skin.selection(left + 1 + (position - SLOTS) * 20.0F, top + 1, 20, 20);
            }
            canvas.endScissor();
        });

        int seed = 1;
        for (int i = 0; i < SLOTS; i++) {
            float target = config.lift() && i == selected ? 1.0F : 0.0F;
            LIFT[i] = Motion.approach(LIFT[i], target, speed.rate(), dt);
            int x = left + 3 + i * 20;
            int y = top + 3;
            ItemStack stack = player.getInventory().getItem(i);
            if (LIFT[i] > 0.0F) {
                canvas.push();
                canvas.translate(0.0F, -1.5F * LIFT[i]);
                items.draw(x, y, stack, seed++);
                canvas.pop();
            } else {
                items.draw(x, y, stack, seed++);
            }
        }

        ItemStack offhand = player.getOffhandItem();
        HumanoidArm offhandArm = player.getMainArm().getOpposite();
        if (!offhand.isEmpty()) {
            int cellX = offhandArm == HumanoidArm.LEFT ? left - 29 : left + 182 + 7;
            canvas.batch(() -> {
                skin.hotbarBase(cellX, top, 22, 22);
                skin.hotbarSlot(cellX + 2, top + 2, 18, 18, boxes);
            });
            items.draw(cellX + 3, top + 3, offhand, seed);
        }

        if (attackIndicator) {
            float strength = player.getAttackStrengthScale(0.0F);
            if (strength < 1.0F) {
                int x = offhandArm == HumanoidArm.RIGHT ? left - 22 : left + 182 + 6;
                int y = bottom - 20;
                float fill = 18.0F * strength;
                canvas.batch(() -> {
                    skin.track(x + 7, y, 4, 18);
                    skin.fill(x + 7, y + 18 - fill, 4, fill, skin.palette().accent());
                });
            }
        }
        frames++;
    }
}
