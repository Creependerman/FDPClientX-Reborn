package dev.tr7zw.skinlayers.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.tr7zw.skinlayers.SkinLayersModBase;
import dev.tr7zw.skinlayers.SkinUtil;
import dev.tr7zw.skinlayers.accessor.PlayerEntityModelAccessor;
import dev.tr7zw.skinlayers.accessor.PlayerSettings;
import dev.tr7zw.skinlayers.renderlayers.BodyLayerFeatureRenderer;
import dev.tr7zw.skinlayers.renderlayers.HeadLayerFeatureRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;

@Mixin(RenderPlayer.class)
public abstract class PlayerRendererMixin extends RendererLivingEntity<AbstractClientPlayer> implements PlayerEntityModelAccessor {

    @Shadow
    private boolean smallArms;
    private HeadLayerFeatureRenderer headLayer;
    private BodyLayerFeatureRenderer bodyLayer;
    
    public PlayerRendererMixin(RenderManager p_i46156_1_, ModelBase p_i46156_2_, float p_i46156_3_) {
        super(p_i46156_1_, p_i46156_2_, p_i46156_3_);
    }

    @Inject(method = "<init>*", at = @At("RETURN"))
    public void onCreate(CallbackInfo info) {
        headLayer = new HeadLayerFeatureRenderer((RenderPlayer)(Object)this);
        bodyLayer = new BodyLayerFeatureRenderer((RenderPlayer)(Object)this);
    }
    
    @Inject(method = "setModelVisibilities", at = @At("HEAD"))
    private void setModelProperties(AbstractClientPlayer abstractClientPlayer, CallbackInfo info) {
        ModelPlayer playerModel = getMainModel();
        if(Minecraft.getMinecraft().thePlayer.getPositionVector().squareDistanceTo(abstractClientPlayer.getPositionVector()) < SkinLayersModBase.config.renderDistanceLOD*SkinLayersModBase.config.renderDistanceLOD) {
            playerModel.bipedHeadwear.isHidden = playerModel.bipedHeadwear.isHidden || SkinLayersModBase.config.enableHat;
            playerModel.bipedBodyWear.isHidden = playerModel.bipedBodyWear.isHidden || SkinLayersModBase.config.enableJacket;
            playerModel.bipedLeftArmwear.isHidden = playerModel.bipedLeftArmwear.isHidden || SkinLayersModBase.config.enableLeftSleeve;
            playerModel.bipedRightArmwear.isHidden = playerModel.bipedRightArmwear.isHidden || SkinLayersModBase.config.enableRightSleeve;
            playerModel.bipedLeftLegwear.isHidden = playerModel.bipedLeftLegwear.isHidden || SkinLayersModBase.config.enableLeftPants;
            playerModel.bipedRightLegwear.isHidden = playerModel.bipedRightLegwear.isHidden || SkinLayersModBase.config.enableRightPants;
        } else {
            // not correct, but the correct way doesn't work cause 1.8 or whatever
            if(!abstractClientPlayer.isSpectator()) {
                playerModel.bipedHeadwear.isHidden = false;
                playerModel.bipedBodyWear.isHidden = false;
                playerModel.bipedLeftArmwear.isHidden = false;
                playerModel.bipedRightArmwear.isHidden = false;
                playerModel.bipedLeftLegwear.isHidden = false;
                playerModel.bipedRightLegwear.isHidden = false;
            }
        }
    }
    
    
    
    @Override
    public HeadLayerFeatureRenderer getHeadLayer() {
        return headLayer;
    }

    @Override
    public BodyLayerFeatureRenderer getBodyLayer() {
        return bodyLayer;
    }

    @Override
    public boolean hasThinArms() {
        return smallArms;
    }

    @Shadow
    public abstract ModelPlayer getMainModel();
    
    @Inject(method = "renderRightArm", at = @At("RETURN"))
    public void renderRightArm(AbstractClientPlayer player, CallbackInfo info) {
        renderFirstPersonArm(player, 3);
    }

    @Inject(method = "renderLeftArm", at = @At("RETURN"))
    public void renderLeftArm(AbstractClientPlayer player, CallbackInfo info) {
        renderFirstPersonArm(player, 2);
    }
    
    private void renderFirstPersonArm(AbstractClientPlayer player, int layerId) {
        ModelPlayer modelplayer = getMainModel();
        float pixelScaling = SkinLayersModBase.config.baseVoxelSize;
        PlayerSettings settings = (PlayerSettings) player;
        if(settings.getSkinLayers() == null && !setupModel(player, settings)) {
            return;
        }
        GlStateManager.pushMatrix();
        modelplayer.bipedRightArm.postRender(0.0625F);
        GlStateManager.scale(0.0625, 0.0625, 0.0625);
        GlStateManager.scale(pixelScaling, pixelScaling, pixelScaling);
        if(!smallArms) {
            settings.getSkinLayers()[layerId].x = -0.998f*16f;
        } else {
            settings.getSkinLayers()[layerId].x = -0.499f*16;
        }
        settings.getSkinLayers()[layerId].render(false);
        GlStateManager.popMatrix();
    }
    
    private boolean setupModel(AbstractClientPlayer abstractClientPlayerEntity, PlayerSettings settings) {
        
        if(!SkinUtil.hasCustomSkin(abstractClientPlayerEntity)) {
            return false; // default skin
        }
        SkinUtil.setup3dLayers(abstractClientPlayerEntity, settings, smallArms, null);
        return true;
    }

}
