/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import cc.paimonmc.viamcp.gui.AsyncVersionSlider;
import cn.hanabi.gui.cloudmusic.ui.MusicPlayerUI;
import net.ccbluex.liquidbounce.ui.client.GuiProxySelect;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.elements.ToolDropdown;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatComponentText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiMultiplayer.class)
public abstract class MixinGuiMultiplayer extends MixinGuiScreen {

    @Unique
    private GuiButton toolButton;
    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        buttonList.add(toolButton = new GuiButton(997, 5, 8, 98, 20, "Tools"));
   //     buttonList.add(new GuiButton(998, width - 104, 8, 98, 20, "%ui.serverSpoof%"));
        buttonList.add(new GuiButton(999, width - 213, 8, 98, 20, "Proxy"));
        buttonList.add(new GuiButton(10, 109 , 8, 98, 20, "AltManager"));
    }

    @Inject(method = "createButtons",at = @At("HEAD"))
    public void createButtons(CallbackInfo ci){
        buttonList.add(new AsyncVersionSlider(-1, this.width - 109, 8, 98, 20));
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) {
        switch (button.id) {
            case 999:
                mc.displayGuiScreen(new GuiProxySelect((GuiScreen) (Object) this));
                break;
            case 10:
                mc.displayGuiScreen(new GuiAltManager((GuiScreen) (Object) this));
                break;
            case 997:
                ToolDropdown.toggleState();
                break;
        }
    }

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void injectToolDraw(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        ToolDropdown.handleDraw(toolButton);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void injectToolClick(int mouseX, int mouseY, int mouseButton, CallbackInfo callbackInfo) {
        if (mouseButton == 0)
            if (ToolDropdown.handleClick(mouseX, mouseY, toolButton))
                callbackInfo.cancel();
    }

    @Inject(method="connectToServer", at=@At(value="HEAD"))
    public void connectToServer(CallbackInfo callbackInfo) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.getNetHandler() != null) {
            minecraft.getNetHandler().getNetworkManager().closeChannel(new ChatComponentText(""));
        }
    }

}