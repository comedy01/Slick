package dev.slick.showcase;

import com.mojang.datafixers.util.Pair;
import dev.slick.client.SlickClient;
import dev.slick.client.gui.SlickSettingsScreen;
import dev.slick.config.SlickConfig;
import dev.slick.core.Accent;
import dev.slick.core.Corners;
import dev.slick.core.Style;
import dev.slick.core.Theme;
import dev.slick.mixin.ContainerScreenAccessor;
import dev.slick.showcase.mixin.CreativeTabInvoker;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.BeaconMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class Showcase {
    private static final String WORLD = "slick-showcase";
    private static final long SETTLE_NANOS = 3_000_000_000L;
    private static final int GLIDE = 24;
    private static final int CAMP = 2;
    private static final int EDGE = 28;
    private static final String[] ARROW = {
            "B",
            "BB",
            "BWB",
            "BWWB",
            "BWWWB",
            "BWWWWB",
            "BWWWWWB",
            "BWWWWWWB",
            "BWWWWWWWB",
            "BWWWWWWWWB",
            "BWWWWWWWWWB",
            "BWWWWWWBBBBB",
            "BWWWBWWB",
            "BWWB BWWB",
            "BWB  BWWB",
            "BB    BWWB",
            "B     BWWB",
            "       BWWB",
            "        BB"};
    private static Showcase instance;

    private final Path out;
    private final Recorder recorder;
    private final long seed;
    private final List<Action> actions = new ArrayList<>();
    private final AtomicReference<BlockPos> site = new AtomicReference<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int frame;
    private long actionStart;
    private Path pendingStill;
    private boolean cursorShown;
    private double cursorX;
    private double cursorY;

    private interface Action {
        boolean run(int frame);
    }

    private record Key(int move, int hold, Vec3 target) {
    }

    private record Look(Style style, Theme theme, Accent accent, Corners corners) {
        void apply(SlickConfig config) {
            config.setStyle(style);
            config.setTheme(theme);
            config.setAccent(accent);
            config.setCorners(corners);
        }
    }

    private record Stop(int frame, Supplier<double[]> where) {
    }

    private Showcase(Path out, String ffmpeg, long seed) {
        this.out = out;
        this.recorder = new Recorder(ffmpeg);
        this.seed = seed;
    }

    public static void start() {
        String ffmpeg = System.getProperty("slick.ffmpeg", "ffmpeg");
        long seed = Long.parseLong(System.getProperty("slick.seed", "12345"));
        instance = new Showcase(Path.of(System.getProperty("slick.showcase")), ffmpeg, seed);
    }

    public static void frame() {
        if (instance != null) {
            instance.onFrame();
        }
    }

    public static void drawCursor(GuiGraphicsExtractor graphics) {
        if (instance == null || !instance.started || !instance.cursorShown) {
            return;
        }
        float cell = 2.0F / (float) Minecraft.getInstance().getWindow().getGuiScale();
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) instance.cursorX, (float) instance.cursorY);
        graphics.pose().scale(cell, cell);
        for (int row = 0; row < ARROW.length; row++) {
            String line = ARROW[row];
            int x = 0;
            while (x < line.length()) {
                char c = line.charAt(x);
                int end = x;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                if (c != ' ') {
                    graphics.fill(x, row, end, row + 1, c == 'B' ? 0xFF000000 : 0xFFFFFFFF);
                }
                x = end;
            }
        }
        graphics.pose().popMatrix();
    }

    private void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 120) {
                started = true;
                setUpOptions(mc);
                plan(mc);
                log("creating world with seed " + seed);
                deleteWorld(mc);
                Worlds.create(mc, WORLD, false, seed);
            }
            return;
        }
        Clock.step();
        mc.gui.toastManager().clear();
        try {
            while (index < actions.size()) {
                if (frame == 0) {
                    actionStart = System.nanoTime();
                }
                if (!actions.get(index).run(frame++)) {
                    if (cursorShown && Screens.current(mc) != null) {
                        double scale = mc.getWindow().getGuiScale();
                        Ui.moveMouse(mc, cursorX * scale, cursorY * scale);
                    } else {
                        Ui.moveMouse(mc, 0, 0);
                    }
                    return;
                }
                index++;
                frame = 0;
            }
        } catch (Throwable e) {
            log("FAILED: " + e);
            e.printStackTrace();
        }
        finish(mc);
    }

    private void finish(Minecraft mc) {
        finished = true;
        Clock.release();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(90_000L);
            } catch (InterruptedException e) {
                return;
            }
            log("the game did not close, forcing it");
            Runtime.getRuntime().halt(1);
        }, "slick-showcase-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        SlickClient.config().resetToDefaults();
        SlickClient.saveConfig();
        log("showcase done");
        mc.stop();
    }

    private static void setUpOptions(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.renderDistance().set(12);
        mc.options.bobView().set(false);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        guiScale(mc, 4);
    }

    private static void guiScale(Minecraft mc, int scale) {
        if (mc.options.guiScale().get() != scale) {
            mc.options.guiScale().set(scale);
            mc.resizeGui();
        }
    }

    private void once(Runnable action) {
        actions.add(f -> {
            action.run();
            return true;
        });
    }

    private void until(BooleanSupplier ready) {
        actions.add(f -> ready.getAsBoolean());
    }

    private void holdFor(int frames, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= frames;
        });
    }

    private void settle(Minecraft mc, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= 90 && System.nanoTime() - actionStart > SETTLE_NANOS
                    && (mc.levelRenderer.hasRenderedAllSections() || System.nanoTime() - actionStart > 20 * SETTLE_NANOS);
        });
    }

    private void record(String name, int frames, IntConsumer script) {
        actions.add(f -> {
            if (f == 0) {
                log("recording " + name);
                recorder.start(out.resolve("clips").resolve(name + ".mp4"));
            } else {
                Path still = pendingStill;
                pendingStill = null;
                recorder.capture(still);
            }
            if (f < frames) {
                script.accept(f);
                return false;
            }
            return true;
        });
        until(recorder::drained);
        once(recorder::stop);
    }

    private void shot(String name) {
        pendingStill = out.resolve("stills").resolve(name + ".png");
    }

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void cmd(Minecraft mc, String format, Object... args) {
        run(mc, String.format(Locale.ROOT, format, args));
    }

    private static void face(Minecraft mc, double yaw, double pitch) {
        LocalPlayer player = mc.player;
        player.setYRot((float) yaw);
        player.setXRot((float) pitch);
        player.yRotO = (float) yaw;
        player.xRotO = (float) pitch;
        player.setYHeadRot((float) yaw);
        player.yHeadRotO = (float) yaw;
    }

    private static void lookAt(Minecraft mc, Vec3 target) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        face(mc, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void follow(Minecraft mc, int f, Key... keys) {
        Vec3 target = keys[0].target();
        int t = f - keys[0].hold();
        for (int i = 1; i < keys.length && t >= 0; i++) {
            Key key = keys[i];
            if (t < key.move()) {
                target = lerp(keys[i - 1].target(), key.target(), smooth(t / (double) key.move()));
                break;
            }
            t -= key.move();
            target = key.target();
            t -= key.hold();
        }
        lookAt(mc, target);
    }

    private void cursor(int f, Stop... stops) {
        double[] at = stops[0].where().get();
        for (int i = 1; i < stops.length; i++) {
            Stop stop = stops[i];
            if (f >= stop.frame()) {
                at = stop.where().get();
            } else if (f >= stop.frame() - GLIDE) {
                double[] to = stop.where().get();
                double s = smooth((f - (stop.frame() - GLIDE)) / (double) GLIDE);
                at = new double[] {lerp(at[0], to[0], s), lerp(at[1], to[1], s)};
                break;
            } else {
                break;
            }
        }
        cursorShown = true;
        cursorX = at[0];
        cursorY = at[1];
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private static boolean wanted(String scene) {
        String only = System.getProperty("slick.scenes");
        return only == null || List.of(only.split(",")).contains(scene);
    }

    private Vec3 at(double dx, double dy, double dz) {
        BlockPos c = site.get();
        return new Vec3(c.getX() + dx + 0.5, c.getY() + dy, c.getZ() + dz + 0.5);
    }

    private BlockPos block(int dx, int dz) {
        BlockPos c = site.get();
        return new BlockPos(c.getX() + dx, c.getY(), c.getZ() + dz);
    }

    private static int height(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
    }

    private void locate(Minecraft mc) {
        once(() -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                RandomState noise = level.getChunkSource().randomState();
                Pair<BlockPos, Holder<Biome>> pair = level.findClosestBiome3d(h -> h.is(Biomes.PLAINS),
                        BlockPos.ZERO, 6400, 32, 64);
                BlockPos found = pair == null ? BlockPos.ZERO : pair.getFirst();
                BlockPos best = null;
                int bestScore = Integer.MAX_VALUE;
                for (int ox = -1536; ox <= 1536; ox += 96) {
                    for (int oz = -1536; oz <= 1536; oz += 96) {
                        int cx = found.getX() + ox;
                        int cz = found.getZ() + oz;
                        int centre = generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, level, noise);
                        if (!level.getBiome(new BlockPos(cx, centre, cz)).is(Biomes.PLAINS)) {
                            continue;
                        }
                        List<Integer> heights = new ArrayList<>();
                        int wet = 0;
                        for (int dx = -40; dx <= 40; dx += 8) {
                            for (int dz = -40; dz <= 40; dz += 8) {
                                int top = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.WORLD_SURFACE_WG,
                                        level, noise);
                                int floor = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                                        level, noise);
                                heights.add(top);
                                if (top != floor) {
                                    wet++;
                                }
                            }
                        }
                        int usual = mode(heights);
                        int score = wet * 3;
                        for (int h : heights) {
                            if (Math.abs(h - usual) > 1) {
                                score++;
                            }
                        }
                        if (score < bestScore) {
                            bestScore = score;
                            best = new BlockPos(cx, centre, cz);
                        }
                    }
                }
                site.set(best == null ? found : best);
                log("plains at " + site.get().toShortString() + ", " + bestScore + " uneven samples");
            });
        });
        until(() -> site.get() != null);
    }

    private static int mode(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        int best = values.get(0);
        for (int value : values) {
            int count = counts.merge(value, 1, Integer::sum);
            if (count > counts.get(best)) {
                best = value;
            }
        }
        return best;
    }

    private void level(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        int ground = server.submit(() -> {
            ServerLevel level = server.overworld();
            BlockPos c = site.get();
            List<Integer> heights = new ArrayList<>();
            for (int dx = -EDGE; dx <= EDGE; dx += 2) {
                for (int dz = -EDGE; dz <= EDGE; dz += 2) {
                    heights.add(height(level, c.getX() + dx, c.getZ() + dz));
                }
            }
            return mode(heights);
        }).join();
        BlockPos c = site.get();
        site.set(new BlockPos(c.getX(), ground + 1, c.getZ()));
        log("set stands at y " + (ground + 1));
    }

    private static void fill(Minecraft mc, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        int layers = Math.max(1, 32768 / ((Math.abs(x2 - x1) + 1) * (Math.abs(z2 - z1) + 1)));
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y += layers) {
            cmd(mc, "fill %d %d %d %d %d %d %s", x1, y, z1, x2, Math.min(y + layers - 1, Math.max(y1, y2)), z2, block);
        }
    }

    private void setblock(Minecraft mc, int dx, int dz, String block) {
        BlockPos pos = block(dx, dz);
        cmd(mc, "setblock %d %d %d minecraft:%s", pos.getX(), pos.getY(), pos.getZ(), block);
    }

    private void buildSet(Minecraft mc) {
        BlockPos c = site.get();
        int x = c.getX();
        int z = c.getZ();
        int ground = c.getY() - 1;
        fill(mc, x - EDGE, ground + 1, z - EDGE, x + EDGE, ground + 40, z + EDGE, "minecraft:air");
        fill(mc, x - EDGE, ground - 6, z - EDGE, x + EDGE, ground - 1, z + EDGE, "minecraft:dirt");
        fill(mc, x - EDGE, ground, z - EDGE, x + EDGE, ground, z + EDGE, "minecraft:grass_block");
        cmd(mc, "place feature minecraft:fancy_oak %d %d %d", x + 4, ground + 1, z + 15);
        String[] kinds = {"oak", "birch", "fancy_oak"};
        int[][] trees = {{-10, 13}, {9, 18}, {-17, 20}, {16, 11}, {-23, 9}, {23, 7}, {-5, 25}, {19, 24}, {25, 17},
                {-25, 19}, {-20, -24}, {8, -25}, {22, -23}, {-6, -26}};
        for (int i = 0; i < trees.length; i++) {
            cmd(mc, "place feature minecraft:%s %d %d %d", kinds[i % kinds.length], x + trees[i][0], ground + 1,
                    z + trees[i][1]);
        }
        Random random = new Random(seed);
        String[] flowers = {"dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet"};
        for (int dx = -EDGE + 1; dx <= EDGE - 1; dx++) {
            for (int dz = -EDGE + 1; dz <= EDGE - 1; dz++) {
                if (Math.abs(dx) <= 4 && dz >= -5 && dz <= CAMP + 1) {
                    continue;
                }
                double roll = random.nextDouble();
                String block;
                if (roll < 0.12) {
                    block = "short_grass";
                } else if (roll < 0.145) {
                    block = flowers[random.nextInt(flowers.length)];
                } else {
                    continue;
                }
                cmd(mc, "setblock %d %d %d minecraft:%s keep", x + dx, ground + 1, z + dz, block);
            }
        }
        setblock(mc, -2, CAMP, "crafting_table");
        setblock(mc, 0, CAMP, "chest[facing=north]");
        setblock(mc, 2, CAMP, "furnace[facing=north,lit=true]");
        setblock(mc, 3, CAMP + 1, "lantern");
        fillChest(mc);
        run(mc, "kill @e[type=!minecraft:player]");
    }

    private void fillChest(Minecraft mc) {
        BlockPos pos = block(0, CAMP);
        String[][] loot = {{"0", "diamond 7"}, {"2", "emerald 23"}, {"6", "ender_pearl 16"}, {"10", "golden_carrot 40"},
                {"13", "iron_ingot 64"}, {"16", "book"}, {"20", "redstone 51"}, {"24", "name_tag"}, {"26", "saddle"}};
        for (String[] item : loot) {
            cmd(mc, "item replace block %d %d %d container.%s with minecraft:%s", pos.getX(), pos.getY(), pos.getZ(),
                    item[0], item[1]);
        }
    }

    private static void kit(Minecraft mc) {
        run(mc, "clear @p");
        String[] items = {"diamond_sword", "iron_pickaxe", "bow", "cobblestone 64", "torch 32", "bread 12",
                "water_bucket", "oak_log 32"};
        for (int i = 0; i < items.length; i++) {
            cmd(mc, "item replace entity @p hotbar.%d with minecraft:%s", i, items[i]);
        }
        run(mc, "item replace entity @p inventory.0 with minecraft:oak_planks 48");
        run(mc, "item replace entity @p inventory.4 with minecraft:redstone 48");
        run(mc, "item replace entity @p inventory.10 with minecraft:golden_apple 3");
        run(mc, "item replace entity @p inventory.20 with minecraft:arrow 24");
        run(mc, "item replace entity @p armor.head with minecraft:iron_helmet");
        run(mc, "item replace entity @p armor.chest with minecraft:iron_chestplate");
        run(mc, "item replace entity @p armor.legs with minecraft:iron_leggings");
        run(mc, "item replace entity @p armor.feet with minecraft:iron_boots");
    }

    private static void close(Minecraft mc) {
        if (mc.player != null && mc.player.containerMenu != mc.player.inventoryMenu) {
            mc.player.closeContainer();
        }
        Screens.open(mc, null);
    }

    private void scene(Minecraft mc, SlickConfig config, int scale, boolean creative, Vec3 feet, Runnable setup,
                       Runnable hold) {
        once(() -> {
            close(mc);
            config.resetToDefaults();
            cursorShown = false;
            run(mc, creative ? "gamemode creative @p" : "gamemode survival @p");
            cmd(mc, "tp @p %.3f %.3f %.3f", feet.x, feet.y, feet.z);
            run(mc, "kill @e[type=minecraft:item]");
            kit(mc);
            select(mc, 0);
            guiScale(mc, scale);
            Ui.moveMouse(mc, 0, 0);
        });
        holdFor(40, hold);
        once(() -> run(mc, "kill @e[type=minecraft:item]"));
        holdFor(10, hold);
        once(setup);
        settle(mc, hold);
    }

    private void plan(Minecraft mc) {
        SlickConfig config = SlickClient.config();

        until(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null);
        once(() -> {
            log("world loaded");
            Clock.fix();
            config.resetToDefaults();
            run(mc, "time set 2500");
            run(mc, "weather clear 1000000");
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule spawn_mobs false");
            run(mc, "gamerule random_tick_speed 0");
            run(mc, "difficulty peaceful");
        });
        locate(mc);
        once(() -> {
            BlockPos c = site.get();
            cmd(mc, "forceload add %d %d %d %d", c.getX() - 40, c.getZ() - 40, c.getX() + 40, c.getZ() + 40);
            cmd(mc, "tp @p %d %d %d", c.getX(), c.getY() + 45, c.getZ());
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });
        settle(mc, () -> {
        });
        once(() -> level(mc));
        once(() -> buildSet(mc));
        holdFor(60, () -> {
        });
        once(() -> {
            mc.player.getAbilities().flying = false;
            mc.player.onUpdateAbilities();
            run(mc, "experience set @p 23 levels");
            run(mc, "experience set @p 60 points");
        });
        once(() -> film(mc, config));
    }

    private void film(Minecraft mc, SlickConfig config) {
        Vec3 spot = at(0, 0, CAMP - 4.5);
        Vec3 horizon = at(0, 1.2, 16);
        Runnable still = () -> lookAt(mc, horizon);

        if (wanted("intro")) {
            Key left = new Key(0, 20, at(4, 0.6, 12));
            Key right = new Key(290, 0, at(-4, 0.6, 12));
            scene(mc, config, 4, false, spot, () -> {
            }, () -> follow(mc, 0, left));
            record("01-intro", 330, f -> {
                follow(mc, f, left, right);
                if (f == 150) {
                    select(mc, 1);
                } else if (f == 250) {
                    select(mc, 3);
                }
                if (f == 200) {
                    shot("intro");
                }
            });
        }

        if (wanted("glide")) {
            scene(mc, config, 4, false, spot, () -> {
            }, still);
            record("02-glide", 330, f -> {
                still.run();
                int[][] moves = {{30, 1}, {40, 2}, {50, 3}, {60, 4}, {120, 7}, {175, 8}, {215, 0}, {265, 8}, {305, 0}};
                for (int[] move : moves) {
                    if (f == move[0]) {
                        select(mc, move[1]);
                    }
                }
                if (f == 54) {
                    shot("glide");
                } else if (f == 218) {
                    shot("wrap");
                } else if (f == 160) {
                    shot("hotbar");
                }
            });
        }

        if (wanted("chest")) {
            Vec3 lid = at(0, 0.8, CAMP);
            scene(mc, config, 3, false, at(0, 0, CAMP - 2.4), () -> {
            }, () -> lookAt(mc, lid));
            once(() -> fillChest(mc));
            holdFor(10, () -> lookAt(mc, lid));
            record("03-chest", 330, f -> {
                lookAt(mc, lid);
                if (f == 25) {
                    BlockPos pos = block(0, CAMP);
                    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
                            new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false));
                }
                if (Screens.current(mc) instanceof ContainerScreen && f < 262) {
                    cursor(f, new Stop(0, () -> corner(mc)),
                            new Stop(90, () -> slotAt(mc, 0)),
                            new Stop(155, () -> slotAt(mc, 62)),
                            new Stop(215, () -> slotAt(mc, 13)));
                    if (f == 95) {
                        clickSlot(mc, 0);
                    } else if (f == 160) {
                        clickSlot(mc, 62);
                    }
                }
                if (f == 262) {
                    cursorShown = false;
                    close(mc);
                } else if (f == 278) {
                    select(mc, 8);
                }
                if (f == 130) {
                    shot("chest-carry");
                } else if (f == 245) {
                    shot("chest");
                } else if (f == 320) {
                    shot("chest-hotbar");
                }
            });
        }

        if (wanted("stations")) {
            List<Supplier<Screen>> stations = List.of(() -> crafting(mc), () -> furnace(mc), () -> stonecutter(mc),
                    () -> enchanting(mc), () -> smithing(mc), () -> beacon(mc));
            String[] names = {"crafting", "furnace", "stonecutter", "enchanting", "smithing", "beacon"};
            scene(mc, config, 3, false, spot, () -> {
            }, still);
            record("04-stations", 330, f -> {
                still.run();
                if (f % 55 == 0) {
                    Screens.open(mc, stations.get(f / 55).get());
                } else if (f % 55 == 40) {
                    shot("station-" + names[f / 55]);
                }
            });
        }

        if (wanted("themes")) {
            Theme[] themes = {Theme.MIDNIGHT, Theme.GRAPHITE, Theme.FROST, Theme.DAYLIGHT, Theme.ONYX};
            scene(mc, config, 3, false, spot, () -> Screens.open(mc, chest(mc)), still);
            record("05-themes", 220, f -> {
                still.run();
                int step = Math.min(themes.length - 1, f / 44);
                config.setTheme(themes[step]);
                cursor(f, new Stop(0, () -> slotAt(mc, 12)));
                if (f % 44 == 30) {
                    shot("theme-" + themes[step].id());
                }
            });
        }

        if (wanted("styles")) {
            Style[] styles = {Style.MODERN, Style.CARDS, Style.MINIMAL, Style.OUTLINE, Style.BEVEL};
            scene(mc, config, 3, false, spot, () -> Screens.open(mc, new InventoryScreen(mc.player)), still);
            record("06-styles", 220, f -> {
                still.run();
                int step = Math.min(styles.length - 1, f / 44);
                config.setStyle(styles[step]);
                cursor(f, new Stop(0, () -> slotAt(mc, 22)));
                if (f % 44 == 30) {
                    shot("style-" + styles[step].id());
                }
            });
        }

        // Rapid looks: every few frames the whole look changes (style, theme, accent, corners), so the video shows
        // how the menus actually look in many combinations instead of the settings screen.
        if (wanted("mix")) {
            Look[] looks = {
                    new Look(Style.MODERN, Theme.MIDNIGHT, Accent.BLUE, Corners.ROUND),
                    new Look(Style.CARDS, Theme.ONYX, Accent.PINK, Corners.ROUND),
                    new Look(Style.OUTLINE, Theme.FROST, Accent.TEAL, Corners.SOFT),
                    new Look(Style.BEVEL, Theme.GRAPHITE, Accent.ORANGE, Corners.SQUARE),
                    new Look(Style.MINIMAL, Theme.DAYLIGHT, Accent.VIOLET, Corners.ROUND),
                    new Look(Style.MODERN, Theme.ONYX, Accent.RED, Corners.SOFT),
                    new Look(Style.CARDS, Theme.FROST, Accent.WHITE, Corners.ROUND),
                    new Look(Style.OUTLINE, Theme.MIDNIGHT, Accent.GREEN, Corners.SQUARE),
                    new Look(Style.BEVEL, Theme.DAYLIGHT, Accent.BLUE, Corners.SQUARE),
                    new Look(Style.MODERN, Theme.GRAPHITE, Accent.YELLOW, Corners.ROUND),
                    new Look(Style.MINIMAL, Theme.ONYX, Accent.TEAL, Corners.ROUND),
                    new Look(Style.CARDS, Theme.DAYLIGHT, Accent.PINK, Corners.SOFT),
                    new Look(Style.OUTLINE, Theme.ONYX, Accent.ORANGE, Corners.ROUND),
                    new Look(Style.MODERN, Theme.FROST, Accent.VIOLET, Corners.ROUND),
                    new Look(Style.BEVEL, Theme.MIDNIGHT, Accent.RED, Corners.SQUARE),
                    new Look(Style.CARDS, Theme.GRAPHITE, Accent.GREEN, Corners.ROUND),
                    new Look(Style.MINIMAL, Theme.FROST, Accent.YELLOW, Corners.SOFT),
                    new Look(Style.OUTLINE, Theme.DAYLIGHT, Accent.RED, Corners.SOFT),
                    new Look(Style.MODERN, Theme.MIDNIGHT, Accent.PINK, Corners.SQUARE),
                    new Look(Style.CARDS, Theme.MIDNIGHT, Accent.VIOLET, Corners.ROUND)};
            int[] hover = {1, 4, 8, 12, 22, 25, 30, 40, 44, 49, 53, 62, 18, 11, 7, 3, 36, 42, 50, 21};
            scene(mc, config, 3, false, spot, () -> Screens.open(mc, chest(mc)), still);
            record("10-mix", 20 * 18, f -> {
                still.run();
                int step = Math.min(looks.length - 1, f / 18);
                looks[step].apply(config);
                cursor(f, new Stop(0, () -> slotAt(mc, hover[step])));
                if (f % 18 == 12) {
                    shot("mix-" + step);
                }
            });
        }

        // Every change opens a different station in a different look.
        if (wanted("stationmix")) {
            List<Supplier<Screen>> stations = List.of(() -> crafting(mc), () -> furnace(mc), () -> enchanting(mc),
                    () -> smithing(mc), () -> stonecutter(mc), () -> beacon(mc), () -> chest(mc),
                    () -> new InventoryScreen(mc.player));
            Look[] looks = {
                    new Look(Style.CARDS, Theme.MIDNIGHT, Accent.TEAL, Corners.ROUND),
                    new Look(Style.BEVEL, Theme.ONYX, Accent.ORANGE, Corners.SQUARE),
                    new Look(Style.MODERN, Theme.FROST, Accent.VIOLET, Corners.ROUND),
                    new Look(Style.OUTLINE, Theme.GRAPHITE, Accent.PINK, Corners.SOFT),
                    new Look(Style.MINIMAL, Theme.DAYLIGHT, Accent.BLUE, Corners.ROUND),
                    new Look(Style.MODERN, Theme.ONYX, Accent.YELLOW, Corners.ROUND),
                    new Look(Style.CARDS, Theme.DAYLIGHT, Accent.RED, Corners.SOFT),
                    new Look(Style.BEVEL, Theme.MIDNIGHT, Accent.GREEN, Corners.SQUARE)};
            scene(mc, config, 3, false, spot, () -> {
            }, still);
            record("11-stationmix", 8 * 30, f -> {
                still.run();
                if (f % 30 == 0) {
                    looks[f / 30].apply(config);
                    Screens.open(mc, stations.get(f / 30).get());
                } else if (f % 30 == 22) {
                    shot("stationmix-" + f / 30);
                }
            });
        }

        // The hover glow follows the accent: the cursor hops to a new slot on every accent.
        if (wanted("accents")) {
            Accent[] accents = {Accent.BLUE, Accent.VIOLET, Accent.PINK, Accent.RED, Accent.ORANGE, Accent.YELLOW,
                    Accent.GREEN, Accent.TEAL, Accent.WHITE};
            int[] hover = {10, 11, 12, 14, 15, 16, 17, 18, 20};
            scene(mc, config, 3, false, spot, () -> {
                config.setTheme(Theme.ONYX);
                Screens.open(mc, new InventoryScreen(mc.player));
            }, still);
            // The cursor arrives at each slot (a 24-frame glide) as that accent comes in.
            Stop[] stops = new Stop[accents.length];
            for (int i = 0; i < accents.length; i++) {
                int slot = hover[i];
                stops[i] = new Stop(i * 24, () -> slotAt(mc, slot));
            }
            record("12-accents", 9 * 24, f -> {
                still.run();
                int step = Math.min(accents.length - 1, f / 24);
                config.setAccent(accents[step]);
                cursor(f, stops);
                if (f % 24 == 16) {
                    shot("accent-" + accents[step].id());
                }
            });
        }

        // Frost is see-through: sweep its opacity and step through the corner shapes.
        if (wanted("glass")) {
            scene(mc, config, 3, false, spot, () -> {
                config.setTheme(Theme.FROST);
                config.setAccent(Accent.TEAL);
                Screens.open(mc, chest(mc));
            }, still);
            record("13-glass", 210, f -> {
                still.run();
                double t = Math.min(1.0, f / 150.0);
                double eased = 0.5 - 0.5 * Math.cos(Math.PI * t);
                config.setOpacity((int) Math.round(SlickConfig.MIN_OPACITY
                        + (SlickConfig.MAX_OPACITY - SlickConfig.MIN_OPACITY) * (1.0 - eased)));
                config.setCorners(f < 70 ? Corners.ROUND : f < 140 ? Corners.SOFT : Corners.SQUARE);
                cursor(f, new Stop(0, () -> slotAt(mc, 12)));
                if (f == 60) {
                    shot("glass");
                }
            });
        }

        // The hotbar in the world, restyled on every hop of the selection.
        if (wanted("hotbars")) {
            Look[] looks = {
                    new Look(Style.MODERN, Theme.MIDNIGHT, Accent.BLUE, Corners.ROUND),
                    new Look(Style.CARDS, Theme.ONYX, Accent.PINK, Corners.ROUND),
                    new Look(Style.OUTLINE, Theme.FROST, Accent.WHITE, Corners.SOFT),
                    new Look(Style.BEVEL, Theme.GRAPHITE, Accent.ORANGE, Corners.SQUARE),
                    new Look(Style.MINIMAL, Theme.MIDNIGHT, Accent.TEAL, Corners.ROUND),
                    new Look(Style.MODERN, Theme.DAYLIGHT, Accent.VIOLET, Corners.ROUND),
                    new Look(Style.CARDS, Theme.FROST, Accent.YELLOW, Corners.SOFT),
                    new Look(Style.OUTLINE, Theme.ONYX, Accent.GREEN, Corners.ROUND),
                    new Look(Style.BEVEL, Theme.DAYLIGHT, Accent.RED, Corners.SQUARE),
                    new Look(Style.MODERN, Theme.GRAPHITE, Accent.PINK, Corners.SOFT),
                    new Look(Style.MINIMAL, Theme.ONYX, Accent.ORANGE, Corners.ROUND),
                    new Look(Style.CARDS, Theme.MIDNIGHT, Accent.VIOLET, Corners.ROUND)};
            int[] slots = {0, 2, 4, 6, 8, 5, 3, 1, 7, 0, 4, 8};
            scene(mc, config, 4, false, spot, () -> {
            }, still);
            record("14-hotbars", 12 * 22, f -> {
                still.run();
                if (f % 22 == 0) {
                    looks[f / 22].apply(config);
                } else if (f % 22 == 4) {
                    select(mc, slots[f / 22]);
                } else if (f % 22 == 18) {
                    shot("hotbar-" + f / 22);
                }
            });
        }

        if (wanted("creative")) {
            CreativeModeTab colored = tab(CreativeModeTab.Row.TOP, 1);
            CreativeModeTab combat = tab(CreativeModeTab.Row.BOTTOM, 1);
            CreativeModeTab search = typed(CreativeModeTab.Type.SEARCH);
            scene(mc, config, 3, true, spot, () -> Screens.open(mc,
                    new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), true)), still);
            record("07-creative", 330, f -> {
                still.run();
                cursor(f, new Stop(0, () -> corner(mc)),
                        new Stop(50, () -> tabAt(mc, colored)),
                        new Stop(130, () -> tabAt(mc, combat)),
                        new Stop(210, () -> tabAt(mc, search)),
                        new Stop(280, () -> slotAt(mc, 12)));
                if (f == 55) {
                    clickTab(mc, colored);
                } else if (f == 135) {
                    clickTab(mc, combat);
                } else if (f == 215) {
                    clickTab(mc, search);
                }
                if (f == 110) {
                    shot("creative-colored");
                } else if (f == 190) {
                    shot("creative-combat");
                } else if (f == 320) {
                    shot("creative-search");
                }
            });
        }

        if (wanted("settings")) {
            scene(mc, config, 3, false, spot, () -> {
            }, still);
            once(() -> Screens.open(mc, new SlickSettingsScreen(null, mc.options)));
            holdFor(20, () -> {
                still.run();
                cursor(0, new Stop(0, () -> corner(mc)));
            });
            record("08-settings", 600, f -> {
                still.run();
                if (f < 240) {
                    cursor(f, new Stop(0, () -> corner(mc)),
                            new Stop(40, () -> labelAt(mc, "Style", 0.5)),
                            new Stop(100, () -> labelAt(mc, "Theme", 0.5)),
                            new Stop(160, () -> labelAt(mc, "Accent", 0.5)),
                            new Stop(215, () -> corner(mc)));
                }
                if (f == 48) {
                    clickLabel(mc, "Style", 0.5);
                } else if (f == 108 || f == 125) {
                    clickLabel(mc, "Theme", 0.5);
                } else if (f == 168 || f == 180 || f == 192) {
                    clickLabel(mc, "Accent", 0.5);
                } else if (f == 220) {
                    shot("settings");
                } else if (f == 240) {
                    cursorShown = false;
                    Screens.open(mc, null);
                } else if (f == 262) {
                    select(mc, 4);
                } else if (f == 300) {
                    select(mc, 7);
                } else if (f == 365) {
                    Screens.open(mc, chest(mc));
                } else if (f == 540) {
                    cursorShown = false;
                    Screens.open(mc, null);
                } else if (f == 560) {
                    select(mc, 1);
                }
                if (Screens.current(mc) instanceof ContainerScreen && f > 365 && f < 540) {
                    cursor(f, new Stop(365, () -> slotAt(mc, 12)),
                            new Stop(430, () -> slotAt(mc, 4)),
                            new Stop(495, () -> slotAt(mc, 58)));
                }
                if (f == 340) {
                    shot("settings-result");
                } else if (f == 470) {
                    shot("settings-result-chest");
                }
            });
            once(() -> Screens.open(mc, null));
        }

        if (wanted("outro")) {
            once(() -> run(mc, "time set 12300"));
            Key from = new Key(0, 0, at(-4, 0.6, 12));
            Key to = new Key(330, 0, at(4, 0.6, 12));
            scene(mc, config, 4, false, spot, () -> {
            }, () -> follow(mc, 0, from, to));
            record("09-outro", 330, f -> {
                follow(mc, f, from, to);
                if (f == 120) {
                    select(mc, 2);
                }
                if (f == 150) {
                    shot("sunset");
                }
            });
            once(() -> run(mc, "time set 2500"));
        }
    }

    private static AbstractContainerScreen<?> chest(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        SimpleContainer container = new SimpleContainer(27);
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND, 7), new ItemStack(Items.EMERALD, 23), new ItemStack(Items.IRON_INGOT, 64),
                new ItemStack(Items.ENDER_PEARL, 16), new ItemStack(Items.GOLDEN_CARROT, 40), new ItemStack(Items.BOOK),
                new ItemStack(Items.REDSTONE, 51), new ItemStack(Items.NAME_TAG), new ItemStack(Items.SADDLE)};
        int[] spots = {0, 2, 6, 10, 13, 16, 20, 24, 26};
        for (int i = 0; i < loot.length; i++) {
            container.setItem(spots[i], loot[i]);
        }
        return new ContainerScreen(ChestMenu.threeRows(1, inventory, container), inventory,
                Component.translatable("container.chest"));
    }

    private static Screen crafting(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        CraftingMenu menu = new CraftingMenu(1, inventory);
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND));
        menu.getSlot(2).set(new ItemStack(Items.DIAMOND));
        menu.getSlot(3).set(new ItemStack(Items.DIAMOND));
        menu.getSlot(5).set(new ItemStack(Items.STICK));
        menu.getSlot(8).set(new ItemStack(Items.STICK));
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_PICKAXE));
        return new CraftingScreen(menu, inventory, Component.translatable("container.crafting"));
    }

    private static Screen furnace(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        FurnaceMenu menu = new FurnaceMenu(1, inventory);
        menu.getSlot(0).set(new ItemStack(Items.RAW_IRON, 8));
        menu.getSlot(1).set(new ItemStack(Items.COAL, 5));
        menu.getSlot(2).set(new ItemStack(Items.IRON_INGOT, 3));
        return new FurnaceScreen(menu, inventory, Component.translatable("container.furnace"));
    }

    private static Screen stonecutter(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        StonecutterMenu menu = new StonecutterMenu(1, inventory);
        menu.getSlot(0).set(new ItemStack(Items.STONE, 16));
        return new StonecutterScreen(menu, inventory, Component.translatable("container.stonecutter"));
    }

    private static Screen enchanting(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        EnchantmentMenu menu = new EnchantmentMenu(1, inventory);
        menu.getSlot(0).set(new ItemStack(Items.DIAMOND_SWORD));
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 12));
        menu.costs[0] = 3;
        menu.costs[1] = 12;
        menu.costs[2] = 30;
        return new EnchantmentScreen(menu, inventory, Component.translatable("container.enchant"));
    }

    private static Screen smithing(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        SmithingMenu menu = new SmithingMenu(1, inventory);
        menu.getSlot(0).set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        menu.getSlot(1).set(new ItemStack(Items.DIAMOND_CHESTPLATE));
        menu.getSlot(2).set(new ItemStack(Items.NETHERITE_INGOT));
        return new SmithingScreen(menu, inventory, Component.translatable("container.upgrade"));
    }

    private static Screen beacon(Minecraft mc) {
        Inventory inventory = mc.player.getInventory();
        return new BeaconScreen(new BeaconMenu(1, inventory), inventory, Component.translatable("container.beacon"));
    }

    private static CreativeModeTab tab(CreativeModeTab.Row row, int column) {
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.row() == row && tab.column() == column && tab.getType() == CreativeModeTab.Type.CATEGORY) {
                return tab;
            }
        }
        throw new IllegalStateException("no creative tab at " + row + " " + column);
    }

    private static CreativeModeTab typed(CreativeModeTab.Type type) {
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() == type) {
                return tab;
            }
        }
        throw new IllegalStateException("no creative tab of type " + type);
    }

    private static double[] tabAt(Minecraft mc, CreativeModeTab tab) {
        Screen screen = Screens.current(mc);
        CreativeTabInvoker tabs = (CreativeTabInvoker) screen;
        ContainerScreenAccessor frame = (ContainerScreenAccessor) screen;
        return new double[] {frame.slick$leftPos() + tabs.slickShowcase$tabX(tab) + 13,
                frame.slick$topPos() + tabs.slickShowcase$tabY(tab) + 16};
    }

    private static void clickTab(Minecraft mc, CreativeModeTab tab) {
        double[] at = tabAt(mc, tab);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static double[] slotAt(Minecraft mc, int index) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) Screens.current(mc);
        ContainerScreenAccessor frame = (ContainerScreenAccessor) screen;
        Slot slot = screen.getMenu().getSlot(index);
        return new double[] {frame.slick$leftPos() + slot.x + 8, frame.slick$topPos() + slot.y + 8};
    }

    private static void clickSlot(Minecraft mc, int index) {
        double[] at = slotAt(mc, index);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static double[] corner(Minecraft mc) {
        Screen screen = Screens.current(mc);
        return new double[] {screen.width - 36, screen.height - 30};
    }

    private static double[] labelAt(Minecraft mc, String label, double along) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no widget " + label);
        }
        return new double[] {widget.getX() + widget.getWidth() * along, widget.getY() + widget.getHeight() / 2.0};
    }

    private static void clickLabel(Minecraft mc, String label, double along) {
        double[] at = labelAt(mc, label, along);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static AbstractWidget find(GuiEventListener node, String label) {
        if (node instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(label + ":")) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void deleteWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static void log(String message) {
        System.out.println("[slick-showcase] " + message);
    }
}
