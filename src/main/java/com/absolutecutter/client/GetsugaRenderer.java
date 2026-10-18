package com.absolutecutter.client;

import com.absolutecutter.AbsoluteCutterMod;
import com.absolutecutter.entity.GetsugaEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Procedural crescent MESH (not a flat sprite):
 *  - pass 1: near-black translucent body + a larger, wobbling "turbulent" black shell (RenderType.entityTranslucent)
 *  - pass 2: additive golden rim, silver inner edge and silver streaks trailing behind (RenderType.lightning)
 * The two passes are written one after the other because a buffer source can only build one RenderType at a time.
 */
public class GetsugaRenderer extends EntityRenderer<GetsugaEntity> {
    private static final ResourceLocation TEX = new ResourceLocation(AbsoluteCutterMod.MODID, "textures/entity/getsuga.png");

    private static final int N = 28;                       // segments along the arc
    private static final float A = 1.25F;                  // half-angle of the arc (radians)
    private static final float R = 2.6F;                   // arc radius (at projectileWidth = 5)
    private static final float TMAX = 1.15F;               // maximum thickness of the crescent
    private static final float SWEEP = 1.7F;               // how far the horns are swept backwards
    private static final float COSA = (float) Math.cos(A);
    private static final float[][] LAYERS = {{75.0F, 1.0F}, {165.0F, 0.66F}};   // {roll degrees, scale}

    public GetsugaRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GetsugaEntity entity) {
        return TEX;
    }

    @Override
    public void render(GetsugaEntity e, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float age = e.tickCount + pt;
        float s = e.getCrescentWidth() / 5.0F * Math.min(1.0F, (age + 0.5F) / 3.0F);    // pops in over 3 ticks

        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pt, e.yRotO, e.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        ps.scale(s, s, s);

        VertexConsumer body = buf.getBuffer(RenderType.entityTranslucent(TEX));
        for (float[] l : LAYERS) {
            ps.pushPose();
            layerTransform(ps, l);
            drawBody(ps, body, age);
            ps.popPose();
        }

        VertexConsumer glow = buf.getBuffer(RenderType.lightning());
        for (float[] l : LAYERS) {
            ps.pushPose();
            layerTransform(ps, l);
            drawGlow(ps, glow, age);
            ps.popPose();
        }
        ps.popPose();
    }

    private static void layerTransform(PoseStack ps, float[] layer) {
        ps.mulPose(Axis.ZP.rotationDegrees(layer[0]));
        ps.scale(layer[1], layer[1], layer[1]);
        ps.translate(0.0F, 0.89F, 0.0F);                  // centre the crescent on the entity position
    }

    /** Arc geometry. Index [0..N]. out/in = outer/inner edge points, nx/ny = outward normal, tapr = taper (0 at horns). */
    private static final class Arc {
        final float[] ox = new float[N + 1], oy = new float[N + 1], ix = new float[N + 1], iy = new float[N + 1];
        final float[] z = new float[N + 1], nx = new float[N + 1], ny = new float[N + 1], tapr = new float[N + 1];

        Arc(float age, float thickMul, float wobble) {
            for (int i = 0; i <= N; i++) {
                float t = -1.0F + 2.0F * i / N;
                float a = t * A;
                float rr = R + wobble * Mth.sin(age * 0.8F + a * 8.0F);
                float sa = Mth.sin(a), ca = Mth.cos(a);
                float taper = Mth.cos(Mth.HALF_PI * t);
                float th = TMAX * thickMul * taper;
                ox[i] = rr * sa;
                oy[i] = R * COSA - rr * ca;
                ix[i] = ox[i] - sa * th;
                iy[i] = oy[i] + ca * th;
                z[i] = -SWEEP * t * t;
                nx[i] = sa;
                ny[i] = -ca;
                tapr[i] = taper;
            }
        }
    }

    private static void drawBody(PoseStack ps, VertexConsumer vc, float age) {
        Matrix4f m = ps.last().pose();
        Matrix3f n = ps.last().normal();
        Arc core = new Arc(age, 1.0F, 0.07F);
        Arc shell = new Arc(age + 3.0F, 1.4F, 0.16F);
        for (int i = 0; i < N; i++) {
            bq(vc, m, n, core, i, 0.0F, 0.015F, 0.015F, 0.02F, 0.97F);          // almost pure black, swallows light
            bq(vc, m, n, shell, i, -0.25F, 0.0F, 0.0F, 0.0F, 0.38F);             // turbulent dark aura
        }
    }

    private static void bq(VertexConsumer vc, Matrix4f m, Matrix3f n, Arc a, int i, float dz, float r, float g, float b, float al) {
        bv(vc, m, n, a.ox[i], a.oy[i], a.z[i] + dz, r, g, b, al);
        bv(vc, m, n, a.ox[i + 1], a.oy[i + 1], a.z[i + 1] + dz, r, g, b, al);
        bv(vc, m, n, a.ix[i + 1], a.iy[i + 1], a.z[i + 1] + dz, r, g, b, al);
        bv(vc, m, n, a.ix[i], a.iy[i], a.z[i] + dz, r, g, b, al);
    }

    private static void bv(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(LightTexture.FULL_BRIGHT).normal(n, 0.0F, 0.0F, 1.0F).endVertex();
    }

    private static void drawGlow(PoseStack ps, VertexConsumer vc, float age) {
        Matrix4f m = ps.last().pose();
        Arc a = new Arc(age, 1.0F, 0.07F);

        // brilliant golden edge along the outside of the crescent (bright at the edge, soft falloff outward)
        for (int i = 0; i < N; i++) {
            float w0 = 0.22F * (0.35F + 0.65F * a.tapr[i]);
            float w1 = 0.22F * (0.35F + 0.65F * a.tapr[i + 1]);
            gq(vc, m,
                    a.ox[i], a.oy[i], a.z[i], 1.0F, 0.80F, 0.22F, 1.0F,
                    a.ox[i + 1], a.oy[i + 1], a.z[i + 1], 1.0F, 0.80F, 0.22F, 1.0F,
                    a.ox[i + 1] + a.nx[i + 1] * w1, a.oy[i + 1] + a.ny[i + 1] * w1, a.z[i + 1], 1.0F, 0.55F, 0.08F, 0.0F,
                    a.ox[i] + a.nx[i] * w0, a.oy[i] + a.ny[i] * w0, a.z[i], 1.0F, 0.55F, 0.08F, 0.0F);
            // thin silver-white line on the inner edge
            float k = 0.07F * a.tapr[i], k1 = 0.07F * a.tapr[i + 1];
            gq(vc, m,
                    a.ix[i], a.iy[i], a.z[i], 0.9F, 0.94F, 1.0F, 0.6F,
                    a.ix[i + 1], a.iy[i + 1], a.z[i + 1], 0.9F, 0.94F, 1.0F, 0.6F,
                    a.ix[i + 1] + a.nx[i + 1] * k1, a.iy[i + 1] + a.ny[i + 1] * k1, a.z[i + 1], 0.9F, 0.94F, 1.0F, 0.0F,
                    a.ix[i] + a.nx[i] * k, a.iy[i] + a.ny[i] * k, a.z[i], 0.9F, 0.94F, 1.0F, 0.0F);
        }

        // silver streaks trailing behind the body (alpha fades towards the tail)
        for (int j = 0; j < 5; j++) {
            int i = Math.round((-0.7F + 0.35F * j + 1.0F) * 0.5F * N);
            float mx = (a.ox[i] + a.ix[i]) * 0.5F, my = (a.oy[i] + a.iy[i]) * 0.5F, mz = a.z[i];
            float len = 2.6F * (0.7F + 0.3F * Mth.sin(age * 0.6F + j * 1.7F));
            float hw = 0.045F;
            gq(vc, m,
                    mx - hw, my, mz, 0.9F, 0.95F, 1.0F, 0.75F,
                    mx + hw, my, mz, 0.9F, 0.95F, 1.0F, 0.75F,
                    mx + hw, my, mz - len, 0.8F, 0.88F, 1.0F, 0.0F,
                    mx - hw, my, mz - len, 0.8F, 0.88F, 1.0F, 0.0F);
        }
    }

    /** Additive quad, written in both windings so it is visible from either side (lightning type culls back faces). */
    private static void gq(VertexConsumer vc, Matrix4f m,
                           float x1, float y1, float z1, float r1, float g1, float b1, float a1,
                           float x2, float y2, float z2, float r2, float g2, float b2, float a2,
                           float x3, float y3, float z3, float r3, float g3, float b3, float a3,
                           float x4, float y4, float z4, float r4, float g4, float b4, float a4) {
        gv(vc, m, x1, y1, z1, r1, g1, b1, a1);
        gv(vc, m, x2, y2, z2, r2, g2, b2, a2);
        gv(vc, m, x3, y3, z3, r3, g3, b3, a3);
        gv(vc, m, x4, y4, z4, r4, g4, b4, a4);
        gv(vc, m, x4, y4, z4, r4, g4, b4, a4);
        gv(vc, m, x3, y3, z3, r3, g3, b3, a3);
        gv(vc, m, x2, y2, z2, r2, g2, b2, a2);
        gv(vc, m, x1, y1, z1, r1, g1, b1, a1);
    }

    private static void gv(VertexConsumer vc, Matrix4f m, float x, float y, float z, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).endVertex();
    }
}
