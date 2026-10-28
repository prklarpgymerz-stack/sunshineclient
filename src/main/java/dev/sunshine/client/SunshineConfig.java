package dev.sunshine.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Persists Sunshine settings to config/sunshine.json; falls back to defaults on any problem. */
public final class SunshineConfig {
    private static final Logger LOG = LoggerFactory.getLogger("sunshine");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sunshine.json");

    public static boolean hud = true;
    public static boolean coords = true;
    public static boolean keystrokes = true;
    public static boolean hints = true;
    public static boolean sprintIndicator = true;
    public static boolean clockHud = false;
    public static boolean directionHud = false;
    public static boolean sessionTimerHud = false;
    public static boolean potionHud = false;
    public static boolean armorHud = false;
    public static boolean selfNametag = false;
    public static boolean attackCooldownHud = false;
    public static boolean targetInfoHud = false;
    public static boolean motionBlurEnabled = false;
    public static int motionBlurStrength = 35; // 1-100; higher values retain more of the previous frame
    public static int clockX = -1, clockY = 6;
    public static int directionX = 6, directionY = 48;
    public static int sessionX = 6, sessionY = 69;
    public static int potionX = -1, potionY = 8;
    public static int armorX = -1, armorY = -1;
    public static int attackCooldownX = -1, attackCooldownY = -1;
    public static int targetInfoX = -1, targetInfoY = -1;
    public static boolean freelookEnabled = false;
    public static boolean toggleSprint = false;
    public static boolean mouseVisualizer = true;
    public static boolean leftMouse = true;
    public static boolean rightMouse = true;
    // Custom sprint indicator controls (GUI-scaled pixels and ARGB color).
    public static String sprintText = "Sprinting";
    public static int sprintColor = 0xFFFFD447;
    public static int sprintTextSize = 1; // 1=normal, 2=large
    public static int sprintX = -1, sprintY = -1;
    public static boolean sprintBackground = true;
    // 0 = Fancy, 1 = Balanced, 2 = Performance. Affects Sunshine UI effects only.
    public static int performanceMode = 1;
    // Optional alternate dark/purple ClickGUI theme inspired by modern client layouts.
    public static boolean grimSkidTheme = false;
    public static String category = "HUD";
    // -1 means use the automatic default position. Editor positions are GUI-scaled pixels.
    public static int hudX = -1, hudY = 6;
    public static int coordsX = -1, coordsY = -1;
    public static int keysX = -1, keysY = -1;
    // Persisted HUD editor dimensions; zero means use the built-in default.
    public static int hudW = 140, hudH = 46;
    public static int coordsW = 150, coordsH = 18;
    public static int keysW = 64, keysH = 58;
    // GLFW key codes; -1 means unbound. Defaults preserve Phase 4 controls.
    public static int menuBind = 344, hudBind = 295, coordsBind = 296, keysBind = 297, sprintBind = 74, freelookBind = 76;

    private SunshineConfig() { }

    public static void load() {
        if (!Files.exists(FILE)) {
            save();
            return;
        }
        try {
            JsonElement root = JsonParser.parseString(Files.readString(FILE, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) throw new IllegalStateException("root is not an object");
            JsonObject o = root.getAsJsonObject();
            hud = bool(o, "hud", hud);
            coords = bool(o, "coords", coords);
            keystrokes = bool(o, "keystrokes", keystrokes);
            hints = bool(o, "hints", hints);
            sprintIndicator = bool(o, "sprintIndicator", sprintIndicator);
            clockHud = bool(o, "clockHud", clockHud);
            directionHud = bool(o, "directionHud", directionHud);
            sessionTimerHud = bool(o, "sessionTimerHud", sessionTimerHud);
            potionHud = bool(o, "potionHud", potionHud);
            armorHud = bool(o, "armorHud", armorHud);
            selfNametag = bool(o, "selfNametag", selfNametag);
            attackCooldownHud = bool(o, "attackCooldownHud", attackCooldownHud);
            targetInfoHud = bool(o, "targetInfoHud", targetInfoHud);
            motionBlurEnabled = bool(o, "motionBlurEnabled", motionBlurEnabled);
            motionBlurStrength = clamp(integer(o, "motionBlurStrength", motionBlurStrength), 1, 100);
            clockX = integer(o, "clockX", clockX); clockY = integer(o, "clockY", clockY);
            directionX = integer(o, "directionX", directionX); directionY = integer(o, "directionY", directionY);
            sessionX = integer(o, "sessionX", sessionX); sessionY = integer(o, "sessionY", sessionY);
            potionX = integer(o, "potionX", potionX); potionY = integer(o, "potionY", potionY);
            armorX = integer(o, "armorX", armorX); armorY = integer(o, "armorY", armorY);
            attackCooldownX = integer(o, "attackCooldownX", attackCooldownX); attackCooldownY = integer(o, "attackCooldownY", attackCooldownY);
            targetInfoX = integer(o, "targetInfoX", targetInfoX); targetInfoY = integer(o, "targetInfoY", targetInfoY);
            freelookEnabled = bool(o, "freelookEnabled", freelookEnabled);
            toggleSprint = bool(o, "toggleSprint", toggleSprint);
            mouseVisualizer = bool(o, "mouseVisualizer", mouseVisualizer);
            leftMouse = bool(o, "leftMouse", leftMouse);
            rightMouse = bool(o, "rightMouse", rightMouse);
            if (o.has("sprintText") && o.get("sprintText").isJsonPrimitive()) sprintText = o.get("sprintText").getAsString();
            sprintColor = integer(o, "sprintColor", sprintColor);
            sprintTextSize = clamp(integer(o, "sprintTextSize", sprintTextSize), 1, 2);
            sprintX = integer(o, "sprintX", sprintX); sprintY = integer(o, "sprintY", sprintY);
            sprintBackground = bool(o, "sprintBackground", sprintBackground);
            performanceMode = clamp(integer(o, "performanceMode", performanceMode), 0, 1);
            grimSkidTheme = bool(o, "grimSkidTheme", grimSkidTheme);
            if (o.has("category") && o.get("category").isJsonPrimitive()) category = o.get("category").getAsString();
            hudX = integer(o, "hudX", hudX); hudY = integer(o, "hudY", hudY);
            coordsX = integer(o, "coordsX", coordsX); coordsY = integer(o, "coordsY", coordsY);
            keysX = integer(o, "keysX", keysX); keysY = integer(o, "keysY", keysY);
            hudW = clamp(integer(o, "hudW", hudW), 100, 360); hudH = clamp(integer(o, "hudH", hudH), 36, 180);
            coordsW = clamp(integer(o, "coordsW", coordsW), 100, 360); coordsH = clamp(integer(o, "coordsH", coordsH), 18, 100);
            keysW = clamp(integer(o, "keysW", keysW), 60, 220); keysH = clamp(integer(o, "keysH", keysH), 50, 220);
            menuBind = bind(o, "menuBind", menuBind); hudBind = bind(o, "hudBind", hudBind);
            coordsBind = bind(o, "coordsBind", coordsBind); keysBind = bind(o, "keysBind", keysBind); freelookBind = bind(o, "freelookBind", freelookBind);
            sprintBind = bind(o, "sprintBind", sprintBind);
        } catch (Exception e) {
            LOG.warn("[Sunshine] Invalid config {}, backing it up and using defaults: {}", FILE, e.toString());
            try {
                Files.move(FILE, FILE.resolveSibling("sunshine.json.invalid"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) { }
        }
        save();
    }

    public static void save() {
        JsonObject o = new JsonObject();
        o.addProperty("hud", hud);
        o.addProperty("coords", coords);
        o.addProperty("keystrokes", keystrokes);
        o.addProperty("hints", hints);
        o.addProperty("sprintIndicator", sprintIndicator);
        o.addProperty("clockHud", clockHud);
        o.addProperty("directionHud", directionHud);
        o.addProperty("sessionTimerHud", sessionTimerHud);
        o.addProperty("potionHud", potionHud);
        o.addProperty("armorHud", armorHud);
        o.addProperty("selfNametag", selfNametag);
        o.addProperty("attackCooldownHud", attackCooldownHud); o.addProperty("targetInfoHud", targetInfoHud);
        o.addProperty("motionBlurEnabled", motionBlurEnabled); o.addProperty("motionBlurStrength", motionBlurStrength);
        o.addProperty("clockX", clockX); o.addProperty("clockY", clockY);
        o.addProperty("directionX", directionX); o.addProperty("directionY", directionY);
        o.addProperty("sessionX", sessionX); o.addProperty("sessionY", sessionY);
        o.addProperty("potionX", potionX); o.addProperty("potionY", potionY);
        o.addProperty("armorX", armorX); o.addProperty("armorY", armorY);
        o.addProperty("attackCooldownX", attackCooldownX); o.addProperty("attackCooldownY", attackCooldownY);
        o.addProperty("targetInfoX", targetInfoX); o.addProperty("targetInfoY", targetInfoY);
        o.addProperty("freelookEnabled", freelookEnabled);
        o.addProperty("toggleSprint", toggleSprint);
        o.addProperty("mouseVisualizer", mouseVisualizer);
        o.addProperty("leftMouse", leftMouse);
        o.addProperty("rightMouse", rightMouse);
        o.addProperty("sprintText", sprintText); o.addProperty("sprintColor", sprintColor);
        o.addProperty("sprintTextSize", sprintTextSize); o.addProperty("sprintX", sprintX); o.addProperty("sprintY", sprintY);
        o.addProperty("sprintBackground", sprintBackground);
        o.addProperty("performanceMode", performanceMode);
        o.addProperty("grimSkidTheme", grimSkidTheme);
        o.addProperty("category", category);
        o.addProperty("hudX", hudX); o.addProperty("hudY", hudY);
        o.addProperty("coordsX", coordsX); o.addProperty("coordsY", coordsY);
        o.addProperty("keysX", keysX); o.addProperty("keysY", keysY);
        o.addProperty("hudW", hudW); o.addProperty("hudH", hudH);
        o.addProperty("coordsW", coordsW); o.addProperty("coordsH", coordsH);
        o.addProperty("keysW", keysW); o.addProperty("keysH", keysH);
        o.addProperty("menuBind", menuBind); o.addProperty("hudBind", hudBind);
        o.addProperty("coordsBind", coordsBind); o.addProperty("keysBind", keysBind); o.addProperty("sprintBind", sprintBind); o.addProperty("freelookBind", freelookBind);
        try {
            Files.createDirectories(FILE.getParent());
            Path tmp = FILE.resolveSibling("sunshine.json.tmp");
            Files.writeString(tmp, GSON.toJson(o), StandardCharsets.UTF_8);
            Files.move(tmp, FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            LOG.warn("[Sunshine] Could not save config: {}", e.toString());
        }
    }

    private static int integer(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        try { return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : fallback; }
        catch (Exception ignored) { return fallback; }
    }

    private static int bind(JsonObject o, String key, int fallback) { return clamp(integer(o, key, fallback), -1, 400); }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        JsonElement e = o.get(key);
        if (e == null || !e.isJsonPrimitive() || !e.getAsJsonPrimitive().isBoolean()) return fallback;
        return e.getAsBoolean();
    }
}
