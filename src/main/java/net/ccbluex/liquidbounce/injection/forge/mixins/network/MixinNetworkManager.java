/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import net.ccbluex.liquidbounce.FDPClient;
import net.ccbluex.liquidbounce.event.EventState;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.modules.client.Animations;
import net.ccbluex.liquidbounce.features.module.modules.combat.BackTrack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.ThreadQuickExitException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/**
 * The type Mixin network manager.
 */
@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    @Shadow
    private Channel channel;

    @Shadow
    private INetHandler packetListener;



    /**
     * show player head in tab bar
     */
    @Inject(method = "getIsencrypted", at = @At("HEAD"), cancellable = true)
    private void getIsencrypted(CallbackInfoReturnable<Boolean> cir) {
        if(Animations.INSTANCE.getFlagRenderTabOverlay()) {
            cir.setReturnValue(true);
        }
    }

    /**
     * @author opZywl
     * @reason Packet Tracking
     */
    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void read(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        final PacketEvent event = new PacketEvent(packet, EventState.RECEIVE);
        BackTrack backTrack = FDPClient.moduleManager.getModule(BackTrack.class);
        assert backTrack != null;
        if (backTrack.getState()) {
            try {
                backTrack.onPacket(event);
            } catch (Exception e) {
                //Minecraft.logger.error("Exception caught in BackTrack", e);
            }
        }
        FDPClient.eventManager.callEvent(event);

        if (event.isCancelled())
            return;
        if (this.channel.isOpen()) {
            try {
                if (packet instanceof INetHandler) {

                }
            } catch (ThreadQuickExitException var4) {
            }
        }
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void send(Packet<?> packet, CallbackInfo callback) {
        final PacketEvent event = new PacketEvent(packet, EventState.SEND);
        BackTrack backTrack = FDPClient.moduleManager.getModule(BackTrack.class);
        assert backTrack != null;
        if (backTrack.getState()) {
            try {
                backTrack.onPacket(event);
            } catch (Exception e) {
                //Minecraft.logger.error("Exception caught in BackTrack", e);
            }
        }
        FDPClient.eventManager.callEvent(event);

        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}