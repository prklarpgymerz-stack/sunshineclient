package dev.sunshine.client;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;

/**
 * Camera motion blur.
 *
 * Instead of blending in the previous frame (which leaves ghost trails), this measures how fast the
 * camera is turning, converts that to a screen-space direction, and smears the current frame along it
 * with a weighted multi-sample blur. Result: a clean directional streak while you turn, and a perfectly
 * sharp image when you stand still.
 *
 *  - Blur length depends on turn SPEED (degrees per second), not frame rate, so it looks the same at
 *    60 or 600 fps.
 *  - Velocity is smoothed slightly so mouse-polling jitter doesn't flicker the blur.
 *  - The strength setting (1-100%) scales the virtual shutter time.
 *  - When the camera is still, no GL work is done at all.
 *
 * Runs as raw OpenGL after the world renders and restores every piece of GL state it touches.
 * Any GL error switches the effect off and prints "[Sunshine] Motion blur disabled: ..." to the log.
 */
public final class MotionBlurRenderer {
    private static final int GL_SAMPLER_BINDING = 0x8919;
    private static final int GL_FRAMEBUFFER_SRGB = 0x8DB9;

    // Tuning
    private static final double MAX_SHUTTER_SECONDS = 1.0 / 25.0; // shutter at 100% strength
    private static final double VELOCITY_SMOOTHING_SECONDS = 0.022;
    private static final double ASSUMED_VERTICAL_FOV_DEGREES = 70.0;
    private static final double MAX_BLUR_UV = 0.15;   // longest streak, as a fraction of the screen
    private static final double MIN_BLUR_UV = 0.0006; // below this the frame is left untouched
    private static final int SAMPLES = 24;

    private static int width, height;
    private static int currentTexture, program, vao, targetFbo;
    private static int currentUniform, blurUniform;
    private static boolean failed;

    // camera tracking
    private static boolean haveLast;
    private static float lastYaw, lastPitch;
    private static long lastNanos;
    private static double velYaw, velPitch; // smoothed, degrees per second

    private MotionBlurRenderer() { }

    public static void render() {
        if (failed || !SunshineConfig.motionBlurEnabled || SunshineConfig.motionBlurStrength <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || (mc.gui.screen() != null && !(mc.gui.screen() instanceof SunshineModuleSettingsScreen))) {
            resetTracking();
            return;
        }

        RenderTarget target = mc.gameRenderer.mainRenderTarget();
        if (target == null) return;
        GpuTexture colorTexture = target.getColorTexture();
        if (!(colorTexture instanceof GlTexture glColor)) return;
        int w = target.width;
        int h = target.height;
        if (w <= 0 || h <= 0) return;

        double[] blur = computeBlurVector(mc.player.getYRot(), mc.player.getXRot(), (double) w / h);
        if (blur == null) return; // camera is still: nothing to do, image stays perfectly sharp

        // Clear any error left over from vanilla so we only report our own.
        while (GL11.glGetError() != GL11.GL_NO_ERROR) { }

        int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        int oldProgram = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int oldVao = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        int oldActiveTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int[] oldViewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, oldViewport);
        int[] oldColorMask = new int[4];
        GL11.glGetIntegerv(GL11.GL_COLOR_WRITEMASK, oldColorMask);
        boolean depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
        boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        boolean srgb = GL11.glIsEnabled(GL_FRAMEBUFFER_SRGB);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        int oldTex0 = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int oldSampler0 = GL11.glGetInteger(GL_SAMPLER_BINDING);

        try {
            ensureResources(w, h);
            if (program == 0 || currentTexture == 0 || targetFbo == 0) return;

            // Wrap the main render target's color texture in our own FBO.
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, targetFbo);
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, glColor.glId(), 0);
            int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
            if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
                fail("render target framebuffer incomplete (0x" + Integer.toHexString(status) + ")");
                return;
            }

            // Neutralise state that could clip, mask or tint our fullscreen pass.
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL_FRAMEBUFFER_SRGB);
            GL11.glColorMask(true, true, true, true);
            GL33.glBindSampler(0, 0);

            // current frame -> currentTexture
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, targetFbo);
            GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, currentTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, w, h);
            if (glError("copy current frame")) return;

            // directional blur of currentTexture -> main render target
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, targetFbo);
            GL11.glViewport(0, 0, w, h);
            GL20.glUseProgram(program);
            GL20.glUniform1i(currentUniform, 0);
            GL20.glUniform2f(blurUniform, (float) blur[0], (float) blur[1]);
            GL30.glBindVertexArray(vao);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            glError("draw blur pass");
        } catch (Throwable t) {
            fail("render error: " + t);
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
            GL11.glViewport(oldViewport[0], oldViewport[1], oldViewport[2], oldViewport[3]);
            GL20.glUseProgram(oldProgram);
            GL30.glBindVertexArray(oldVao);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, oldTex0);
            GL33.glBindSampler(0, oldSampler0);
            GL13.glActiveTexture(oldActiveTexture);
            GL11.glColorMask(oldColorMask[0] != 0, oldColorMask[1] != 0, oldColorMask[2] != 0, oldColorMask[3] != 0);
            setEnabled(GL11.GL_DEPTH_TEST, depth);
            setEnabled(GL11.GL_BLEND, blend);
            setEnabled(GL11.GL_CULL_FACE, cull);
            setEnabled(GL11.GL_SCISSOR_TEST, scissor);
            setEnabled(GL11.GL_STENCIL_TEST, stencil);
            setEnabled(GL_FRAMEBUFFER_SRGB, srgb);
        }
    }

    /**
     * Tracks camera rotation and returns the blur streak as a screen-space vector in UV units
     * (the full blur length; the shader samples from -0.5x to +0.5x of it), or null if there is
     * nothing to blur.
     */
    private static double[] computeBlurVector(float yaw, float pitch, double aspect) {
        long now = System.nanoTime();
        if (!haveLast) {
            lastYaw = yaw; lastPitch = pitch; lastNanos = now; haveLast = true;
            velYaw = velPitch = 0;
            return null;
        }
        double dt = (now - lastNanos) / 1.0e9;
        double dYaw = wrapDegrees(yaw - lastYaw);
        double dPitch = pitch - lastPitch;
        lastYaw = yaw; lastPitch = pitch; lastNanos = now;

        // Hitches, pauses and teleports/respawns: drop the history instead of smearing the screen.
        if (dt <= 0.0 || dt > 0.25 || Math.abs(dYaw) > 90.0 || Math.abs(dPitch) > 90.0) {
            velYaw = velPitch = 0;
            return null;
        }

        double alpha = 1.0 - Math.exp(-dt / VELOCITY_SMOOTHING_SECONDS);
        velYaw += (dYaw / dt - velYaw) * alpha;
        velPitch += (dPitch / dt - velPitch) * alpha;

        double percent = Math.max(1, Math.min(100, SunshineConfig.motionBlurStrength));
        double shutter = (percent / 100.0) * MAX_SHUTTER_SECONDS;

        double tanHalfY = Math.tan(Math.toRadians(ASSUMED_VERTICAL_FOV_DEGREES) / 2.0);
        double tanHalfX = tanHalfY * aspect;
        // Looking right moves the scene left; pitching down moves it up (UV y points up).
        double ux = -Math.tan(Math.toRadians(velYaw * shutter)) / (2.0 * tanHalfX);
        double uy = Math.tan(Math.toRadians(velPitch * shutter)) / (2.0 * tanHalfY);

        double len = Math.sqrt(ux * ux + uy * uy);
        if (len < MIN_BLUR_UV) return null;
        if (len > MAX_BLUR_UV) {
            double k = MAX_BLUR_UV / len;
            ux *= k; uy *= k;
        }
        return new double[] { ux, uy };
    }

    private static double wrapDegrees(double d) {
        d %= 360.0;
        if (d >= 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    private static void resetTracking() {
        haveLast = false;
        velYaw = velPitch = 0;
    }

    private static void ensureResources(int w, int h) {
        if (width == w && height == h && program != 0 && currentTexture != 0) return;
        dispose();
        width = w;
        height = h;
        currentTexture = createTexture(w, h);
        targetFbo = GL30.glGenFramebuffers();
        program = createProgram();
        if (program != 0) {
            currentUniform = GL20.glGetUniformLocation(program, "CurrentFrame");
            blurUniform = GL20.glGetUniformLocation(program, "BlurVector");
        }
        vao = GL30.glGenVertexArrays();
    }

    private static int createTexture(int w, int h) {
        int texture = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        // Only mip level 0 exists; keep the texture complete whatever filter a sampler asks for.
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_BASE_LEVEL, 0);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL12.GL_TEXTURE_MAX_LEVEL, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, w, h, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, 0L);
        return texture;
    }

    private static int createProgram() {
        String vertex = "#version 150 core\n"
                + "out vec2 uv;\n"
                + "void main(){ vec2 p=vec2((gl_VertexID<<1)&2, gl_VertexID&2); uv=p; gl_Position=vec4(p*2.0-1.0,0.0,1.0); }";
        String fragment = "#version 150 core\n"
                + "uniform sampler2D CurrentFrame;\n"
                + "uniform vec2 BlurVector;\n"
                + "in vec2 uv;\n"
                + "out vec4 fragColor;\n"
                + "const int SAMPLES = " + SAMPLES + ";\n"
                + "void main(){\n"
                + "  vec3 sum = vec3(0.0);\n"
                + "  float total = 0.0;\n"
                + "  for (int i = 0; i < SAMPLES; i++) {\n"
                + "    float t = (float(i) + 0.5) / float(SAMPLES) - 0.5;\n"
                + "    float weight = 1.0 - abs(t) * 1.2;\n"          // tent: keeps the centre crisp
                + "    sum += texture(CurrentFrame, uv + BlurVector * t).rgb * weight;\n"
                + "    total += weight;\n"
                + "  }\n"
                + "  fragColor = vec4(sum / total, 1.0);\n"
                + "}";
        int vs = compile(GL20.GL_VERTEX_SHADER, vertex);
        int fs = compile(GL20.GL_FRAGMENT_SHADER, fragment);
        if (vs == 0 || fs == 0) return 0;
        int p = GL20.glCreateProgram();
        GL20.glAttachShader(p, vs); GL20.glAttachShader(p, fs); GL20.glLinkProgram(p);
        GL20.glDeleteShader(vs); GL20.glDeleteShader(fs);
        if (GL20.glGetProgrami(p, GL20.GL_LINK_STATUS) == 0) {
            System.err.println("[Sunshine] Motion blur shader link failed: " + GL20.glGetProgramInfoLog(p));
            GL20.glDeleteProgram(p);
            return 0;
        }
        return p;
    }

    private static int compile(int type, String source) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source); GL20.glCompileShader(shader);
        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == 0) {
            System.err.println("[Sunshine] Motion blur shader compile failed: " + GL20.glGetShaderInfoLog(shader));
            GL20.glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    /** Returns true (and switches the effect off) if OpenGL reported an error. */
    private static boolean glError(String stage) {
        int err = GL11.glGetError();
        if (err == GL11.GL_NO_ERROR) return false;
        fail("GL error 0x" + Integer.toHexString(err) + " during: " + stage);
        return true;
    }

    private static void fail(String reason) {
        failed = true;
        System.err.println("[Sunshine] Motion blur disabled: " + reason);
        dispose();
    }

    private static void setEnabled(int cap, boolean enabled) {
        if (enabled) GL11.glEnable(cap); else GL11.glDisable(cap);
    }

    private static void dispose() {
        if (currentTexture != 0) GL11.glDeleteTextures(currentTexture);
        if (targetFbo != 0) GL30.glDeleteFramebuffers(targetFbo);
        if (program != 0) GL20.glDeleteProgram(program);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        currentTexture = targetFbo = program = vao = 0;
        width = height = 0;
    }
}
