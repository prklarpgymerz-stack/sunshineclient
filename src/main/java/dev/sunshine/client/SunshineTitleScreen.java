package dev.sunshine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;

/** Sunshine's custom title screen: asymmetric hero layout with a compact navigation dock. */
public final class SunshineTitleScreen extends Screen {
    private static final int GOLD = 0xFFFFD447;
    private static final int PALE_GOLD = 0xFFFFEAA0;
    private static final int WHITE = 0xFFF4F5F7;
    private static final int MUTED = 0xFF9DA5B2;
    private static final int SURFACE = 0xF20C0E12;
    private int x, y, w, h, buttonX, buttonW, buttonY, buttonH, gap;
    private long startedAt = System.currentTimeMillis();

    public SunshineTitleScreen() { super(Component.literal("Sunshine Client")); }

    @Override protected void init() {
        // Wide, asymmetrical composition deliberately avoids the familiar centered vertical button stack.
        w = Math.min(820, width - 20);
        h = Math.min(430, height - 20);
        x = (width - w) / 2;
        y = (height - h) / 2;
        int inner = w - 40;
        buttonW = Math.max(88, Math.min(174, (inner - 30) / 4));
        buttonH = h < 300 ? 20 : 27;
        gap = 10;
        buttonX = x + 20;
        buttonY = y + h - buttonH - 22;
        addRenderableWidget(Button.builder(Component.literal("SINGLEPLAYER"), b -> Minecraft.getInstance().gui.setScreen(new SelectWorldScreen(this)))
                .bounds(buttonX, buttonY, buttonW, buttonH).build());
        addRenderableWidget(Button.builder(Component.literal("MULTIPLAYER"), b -> Minecraft.getInstance().gui.setScreen(new JoinMultiplayerScreen(this)))
                .bounds(buttonX + buttonW + gap, buttonY, buttonW, buttonH).build());
        addRenderableWidget(Button.builder(Component.literal("SETTINGS"), b -> Minecraft.getInstance().gui.setScreen(new OptionsScreen(this, Minecraft.getInstance().options, false)))
                .bounds(buttonX + (buttonW + gap) * 2, buttonY, buttonW, buttonH).build());
        addRenderableWidget(Button.builder(Component.literal("QUIT GAME"), b -> Minecraft.getInstance().stop())
                .bounds(buttonX + (buttonW + gap) * 3, buttonY, buttonW, buttonH).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        long now = System.currentTimeMillis();
        int sweep = (int)((now - startedAt) / 32L % Math.max(1, width + 160)) - 80;
        // Full-bleed gold-to-black atmosphere, with broad lighting shapes rather than a thin animated line.
        g.fillGradient(0, 0, width, height, 0xFF4A3508, 0xFF050609);
        g.fillGradient(0, 0, width, height / 2, 0x553E2C08, 0x00050609);
        g.fillGradient(0, height / 2, width, height, 0x00050609, 0xFF030406);
        g.fill(sweep, 0, Math.min(width, sweep + 210), height, 0x05FFD447);
        g.fill(0, 0, width, 2, GOLD);

        // Floating ambient bars add depth without turning the whole background into stripes.
        g.fill(24, height - 42, Math.max(25, width / 3), height - 41, 0x18FFD447);
        g.fill(Math.max(0, width - 250), 34, width - 24, 35, 0x12FFD447);

        // Main frame and inset surfaces.
        g.fill(x - 5, y - 5, x + w + 5, y + h + 5, 0x14FFD447);
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x36FFD447);
        g.fillGradient(x, y, x + w, y + h, 0xF21A1C20, 0xF207080B);
        g.outline(x, y, w, h, 0xFF51472D);
        g.fill(x, y, x + w, y + 3, GOLD);

        int leftW = Math.max(270, w * 48 / 100);
        g.fillGradient(x + 1, y + 4, x + leftW, y + h - 1, 0xA9252116, 0x30202020);
        g.fill(x + leftW, y + 18, x + leftW + 1, y + h - 74, 0xFF393326);

        // Brand and hero copy.
        int logoX = x + 24, logoY = y + 24;
        g.fillGradient(logoX, logoY, logoX + 42, logoY + 42, 0xFF4B3A0E, 0xFF17150F);
        g.outline(logoX, logoY, 42, 42, 0xFFB89A40);
        g.centeredText(font, Component.literal("☀").withStyle(s -> s.withColor(PALE_GOLD).withBold(true)), logoX + 21, logoY + 14, WHITE);
        g.text(font, Component.literal("SUNSHINE").withStyle(s -> s.withColor(GOLD).withBold(true)), logoX + 54, logoY + 9, WHITE);
        g.text(font, Component.literal("CLIENT").withStyle(s -> s.withColor(WHITE).withBold(true)), logoX + 54, logoY + 22, WHITE);
        g.text(font, Component.literal("FABRIC EDITION  /  26.2").withStyle(s -> s.withColor(MUTED)), logoX + 54, logoY + 36, WHITE);

        int heroY = y + 105;
        g.text(font, Component.literal("YOUR GAME. YOUR LIGHT.").withStyle(s -> s.withColor(PALE_GOLD).withBold(true)), x + 26, heroY, WHITE);
        g.text(font, Component.literal("A brighter way to play.").withStyle(s -> s.withColor(WHITE).withBold(true)), x + 26, heroY + 21, WHITE);
        g.text(font, Component.literal("HUD tools, visual controls and a cleaner"), x + 26, heroY + 43, MUTED);
        g.text(font, Component.literal("Minecraft experience — shaped your way."), x + 26, heroY + 55, MUTED);

        // Small feature tiles create a purposeful hero area instead of a generic button column.
        int tileY = y + h - 105;
        int tileW = Math.max(70, (leftW - 50) / 3);
        String[] tileTitles = {"HUD", "TOOLS", "STYLE"};
        String[] tileSubs = {"CUSTOM", "BUILT IN", "SUNSHINE"};
        for (int i = 0; i < 3; i++) {
            int tx = x + 24 + i * (tileW + 5);
            g.fill(tx, tileY, tx + tileW, tileY + 38, 0x9D101216);
            g.outline(tx, tileY, tileW, 38, 0xFF39352A);
            g.fill(tx, tileY, tx + 2, tileY + 38, i == 0 ? GOLD : 0xFF776332);
            g.text(font, Component.literal(tileTitles[i]).withStyle(s -> s.withColor(WHITE).withBold(true)), tx + 9, tileY + 7, WHITE);
            g.text(font, Component.literal(tileSubs[i]).withStyle(s -> s.withColor(MUTED)), tx + 9, tileY + 21, WHITE);
        }

        // Right-hand information panel.
        int rx = x + leftW + 18;
        int rw = x + w - 18 - rx;
        int ry = y + 26;
        int rh = h - 104;
        g.fillGradient(rx, ry, rx + rw, ry + rh, 0xE6101216, 0xB6090A0D);
        g.outline(rx, ry, rw, rh, 0xFF343941);
        g.fill(rx, ry, rx + rw, ry + 3, GOLD);
        g.text(font, Component.literal("SUNSHINE OVERVIEW").withStyle(s -> s.withColor(GOLD).withBold(true)), rx + 13, ry + 15, WHITE);
        g.text(font, Component.literal("CLIENT PROFILE").withStyle(s -> s.withColor(MUTED)), rx + 13, ry + 36, WHITE);
        g.text(font, Component.literal(SunshineConfig.performanceMode == 1 ? "BALANCED" : "QUALITY")
                .withStyle(s -> s.withColor(WHITE).withBold(true)), rx + 13, ry + 49, WHITE);
        g.fill(rx + 13, ry + 69, rx + rw - 13, ry + 70, 0xFF30343B);
        g.text(font, Component.literal("VISUAL MODULES").withStyle(s -> s.withColor(MUTED)), rx + 13, ry + 82, WHITE);
        g.text(font, Component.literal("HUD  ·  MOVEMENT  ·  UTILITY").withStyle(s -> s.withColor(WHITE)), rx + 13, ry + 96, WHITE);
        int statusY = ry + rh - 28;
        g.fill(rx + 13, statusY + 2, rx + 19, statusY + 8, 0xFF7DE0A0);
        g.text(font, Component.literal("CLIENT MENU READY").withStyle(s -> s.withColor(0xFF9CE9B2).withBold(true)), rx + 25, statusY, WHITE);

        g.text(font, Component.literal("SUNSHINE CLIENT").withStyle(s -> s.withColor(MUTED)), x + 22, y + h - 11, WHITE);
        g.text(font, Component.literal("RIGHT SHIFT  ·  IN-GAME MENU").withStyle(s -> s.withColor(MUTED)), x + w - 190, y + h - 11, WHITE);

        // Paint the clickable native widgets as custom Sunshine controls after their hitboxes are rendered.
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        String[] labels = {"SINGLEPLAYER", "MULTIPLAYER", "SETTINGS", "QUIT GAME"};
        for (int i = 0; i < labels.length; i++) {
            int bx = buttonX + i * (buttonW + gap);
            boolean hover = mouseX >= bx && mouseX < bx + buttonW && mouseY >= buttonY && mouseY < buttonY + buttonH;
            int bg = hover ? 0xFF3D3215 : 0xFF202329;
            g.fillGradient(bx, buttonY, bx + buttonW, buttonY + buttonH, hover ? 0xFF4B3B13 : 0xFF2B2E34, bg);
            g.outline(bx, buttonY, buttonW, buttonH, hover ? GOLD : 0xFF454A53);
            g.fill(bx, buttonY, bx + 3, buttonY + buttonH, hover ? PALE_GOLD : 0xFF8D7432);
            g.centeredText(font, Component.literal(labels[i]).withStyle(s -> s.withColor(hover ? PALE_GOLD : WHITE).withBold(true)), bx + buttonW / 2, buttonY + (buttonH - 8) / 2, WHITE);
        }
    }

    @Override public boolean isPauseScreen() { return false; }
}
