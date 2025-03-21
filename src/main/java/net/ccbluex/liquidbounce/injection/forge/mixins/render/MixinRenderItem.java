/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.render;

import net.ccbluex.liquidbounce.FDPClient;
import net.ccbluex.liquidbounce.features.module.modules.visual.Glint;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(RenderItem.class)

public abstract class MixinRenderItem {
    @Final
    @Shadow
    private TextureManager textureManager;

    @Final
    @Shadow
    private static ResourceLocation RES_ITEM_GLINT;

    @Shadow
    protected abstract void renderModel(IBakedModel model, int color);

    @Inject(method = "renderEffect", at = @At("HEAD"), cancellable = true)
    private void renderEffect(IBakedModel model, CallbackInfo callbackInfo) {
        final Glint enchantEffect = FDPClient.moduleManager.getModule(Glint.class);
        if (enchantEffect.getState()) {
            int rainbowColour = RenderUtils.getRainbowOpaque(enchantEffect.getRainbowSpeedValue().get(), enchantEffect.getRainbowSatValue().get(), enchantEffect.getRainbowBrgValue().get(), ((int) Minecraft.getSystemTime() % 2) * (enchantEffect.getRainbowDelayValue().get() * 10));
            int skyColor = RenderUtils.SkyRainbow(0, enchantEffect.getRainbowSatValue().get(), enchantEffect.getRainbowBrgValue().get());
            int currentColor = new Color(enchantEffect.getRedValue().get(), enchantEffect.getGreenValue().get(), enchantEffect.getBlueValue().get()).getRGB();
            GlStateManager.depthMask(false);
            GlStateManager.depthFunc(514);
            GlStateManager.disableLighting();
            GlStateManager.blendFunc(768, 1);
            this.textureManager.bindTexture(RES_ITEM_GLINT);
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0f, 8.0f, 8.0f);
            float f = (float)(Minecraft.getSystemTime() % 3000L) / 3000.0f / 8.0f;
            GlStateManager.translate(f, 0.0f, 0.0f);
            GlStateManager.rotate(-50.0f, 0.0f, 0.0f, 1.0f);
            switch (enchantEffect.getModeValue().get().toLowerCase()) {
                case "custom":
                    this.renderModel(model, currentColor);
                    break;
                case "rainbow":
                    this.renderModel(model, rainbowColour);
                    break;
                case "sky":
                    this.renderModel(model, skyColor);
            }
            GlStateManager.popMatrix();
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0f, 8.0f, 8.0f);
            float f1 = (float)(Minecraft.getSystemTime() % 4873L) / 4873.0f / 8.0f;
            GlStateManager.translate(-f1, 0.0f, 0.0f);
            GlStateManager.rotate(10.0f, 0.0f, 0.0f, 1.0f);
            switch (enchantEffect.getModeValue().get().toLowerCase()) {
                case "custom":
                    this.renderModel(model, currentColor);
                    break;
                case "rainbow":
                    this.renderModel(model, rainbowColour);
                    break;
                case "sky":
                    this.renderModel(model, skyColor);
            }
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
            GlStateManager.blendFunc(770, 771);
            GlStateManager.enableLighting();
            GlStateManager.depthFunc(515);
            GlStateManager.depthMask(true);
            this.textureManager.bindTexture(TextureMap.locationBlocksTexture);
            callbackInfo.cancel();
        }
    }
}
