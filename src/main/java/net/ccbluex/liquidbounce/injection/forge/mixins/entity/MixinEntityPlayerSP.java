/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.entity;

import net.ccbluex.liquidbounce.FDPClient;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.extensions.MathExtensionsKt;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.modules.combat.Criticals;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.AntiKB;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AntiDesync;
import net.ccbluex.liquidbounce.features.module.modules.exploit.PortalMenu;
import net.ccbluex.liquidbounce.features.module.modules.movement.Flight;
import net.ccbluex.liquidbounce.features.module.modules.movement.InvMove;
import net.ccbluex.liquidbounce.features.module.modules.movement.NoSlow;
import net.ccbluex.liquidbounce.features.module.modules.movement.Sprint;
import net.ccbluex.liquidbounce.features.module.modules.movement.StrafeFix;
import net.ccbluex.liquidbounce.features.module.modules.player.AutoGap;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.Scaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.Scaffold2;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.Rotation;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.potion.Potion;
import net.minecraft.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Objects;

import static java.lang.Math.abs;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP extends MixinAbstractClientPlayer {

    @Shadow
    private boolean serverSneakState;

    @Shadow
    public boolean serverSprintState;

    @Shadow
    public abstract void playSound(String name, float volume, float pitch);

    @Shadow
    public int sprintingTicksLeft;

    @Shadow
    protected int sprintToggleTimer;

    @Shadow
    public float timeInPortal;

    @Shadow
    public float prevTimeInPortal;

    @Shadow
    protected Minecraft mc;

    @Shadow
    public MovementInput movementInput;

    @Shadow
    public abstract void setSprinting(boolean sprinting);

    @Shadow
    protected abstract boolean pushOutOfBlocks(double x, double y, double z);

    @Shadow
    public abstract void sendPlayerAbilities();

    @Shadow
    public float horseJumpPower;

    @Shadow
    public int horseJumpPowerCounter;

    @Shadow
    protected abstract void sendHorseJump();

    @Shadow
    public abstract boolean isRidingHorse();

    @Shadow
    @Final
    public NetHandlerPlayClient sendQueue;

    @Shadow
    public abstract boolean isSneaking();

    @Shadow
    private double lastReportedPosX;

    @Shadow
    public int positionUpdateTicks;

    @Shadow
    private double lastReportedPosY;

    @Shadow
    private double lastReportedPosZ;

    @Shadow
    private float lastReportedYaw;

    @Shadow
    private float lastReportedPitch;

    @Shadow
    protected abstract boolean isCurrentViewEntity();

    @Shadow public abstract void addChatMessage(IChatComponent p_addChatMessage_1_);

    /**
     * @author CCBlueX, liulihaocai
     *
     * use inject to make sure this works with ViaForge mod
     */
    @Inject(method = "onUpdateWalkingPlayer", at = @At("HEAD"), cancellable = true)
    public void onUpdateWalkingPlayer(CallbackInfo ci) {
        final StrafeFix strafeFix = FDPClient.moduleManager.getModule(StrafeFix.class);
        Objects.requireNonNull(strafeFix).updateOverwrite();

        MotionEvent motionEvent = new MotionEvent(
                posX,
                getEntityBoundingBox().minY,
                posZ,
                onGround,
                EventState.PRE
        );

        FDPClient.eventManager.callEvent(motionEvent);

        boolean flag = this.isSprinting();
        //alert("Attempt: " + debug_AttemptSprint + " Actual: " + this.isSprinting() + " Server: " + this.serverSprintState);
        if (flag != this.serverSprintState) {
            if (flag) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.START_SPRINTING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.STOP_SPRINTING));
            }

            this.serverSprintState = flag;
        }

        boolean flag1 = this.isSneaking();
        if (flag1 != this.serverSneakState) {
            if (flag1) {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.START_SNEAKING));
            } else {
                this.sendQueue.addToSendQueue(new C0BPacketEntityAction((EntityPlayerSP) (Object) this, C0BPacketEntityAction.Action.STOP_SNEAKING));
            }

            this.serverSneakState = flag1;
        }

        if (this.isCurrentViewEntity()) {
            float yaw = this.rotationYaw;
            float pitch = this.rotationPitch;
            float lastReportedYaw = Objects.requireNonNull(RotationUtils.serverRotation).getYaw();
            float lastReportedPitch = RotationUtils.serverRotation.getPitch();

            if (RotationUtils.targetRotation != null) {
                yaw = RotationUtils.targetRotation.getYaw();
                pitch = RotationUtils.targetRotation.getPitch();
            }

            double xDiff = motionEvent.getX() - this.lastReportedPosX;
            double yDiff = motionEvent.getY() - this.lastReportedPosY;
            double zDiff = motionEvent.getZ() - this.lastReportedPosZ;
            double yawDiff = yaw - lastReportedYaw;
            double pitchDiff = pitch - lastReportedPitch;

            final Flight fly = FDPClient.moduleManager.getModule(Flight.class);
            final Criticals criticals = FDPClient.moduleManager.getModule(Criticals.class);
            final AntiDesync antiDesync = FDPClient.moduleManager.getModule(AntiDesync.class);
            double v = xDiff * xDiff + yDiff * yDiff + zDiff * zDiff;
            boolean moved = v > 9.0E-4D || this.positionUpdateTicks >= 20 || (Objects.requireNonNull(fly).getState() && fly.getAntiDesync()) || (Objects.requireNonNull(criticals).getState() && criticals.getAntiDesync()) || (Objects.requireNonNull(antiDesync).getState() && v > 0.0D);
            boolean rotated = yawDiff != 0.0D || pitchDiff != 0.0D;

            if (this.ridingEntity == null) {
                if (moved && rotated) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(motionEvent.getX(), motionEvent.getY(), motionEvent.getZ(), yaw, pitch, motionEvent.getOnGround()));
                } else if (moved) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C04PacketPlayerPosition(motionEvent.getX(), motionEvent.getY(), motionEvent.getZ(), motionEvent.getOnGround()));
                } else if (rotated) {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, motionEvent.getOnGround()));
                } else {
                    this.sendQueue.addToSendQueue(new C03PacketPlayer(motionEvent.getOnGround()));
                }
            } else {
                this.sendQueue.addToSendQueue(new C03PacketPlayer.C06PacketPlayerPosLook(this.motionX, -999.0D, this.motionZ, yaw, pitch, motionEvent.getOnGround()));
                moved = false;
            }

            ++this.positionUpdateTicks;

            if (moved) {
                this.lastReportedPosX = motionEvent.getX();
                this.lastReportedPosY = motionEvent.getY();
                this.lastReportedPosZ = motionEvent.getZ();
                this.positionUpdateTicks = 0;
            }

            if (rotated) {
                this.lastReportedYaw = yaw;
                this.lastReportedPitch = pitch;
            }
        }

        FDPClient.eventManager.callEvent(new MotionEvent(posX, getEntityBoundingBox().minY, posZ, onGround, EventState.POST));

        FDPClient.eventManager.callEvent(new RotationUpdateEvent());

        ci.cancel();
    }

    @Inject(method = "pushOutOfBlocks", at = @At("HEAD"), cancellable = true)
    private void onPushOutOfBlocks(CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        PushOutEvent event = new PushOutEvent();
        if (this.noClip) event.cancelEvent();
        FDPClient.eventManager.callEvent(event);

        if (event.isCancelled())
            callbackInfoReturnable.setReturnValue(false);
    }

    /**
     * @author CCBlueX && Xebook1
     * @reason Fix Sprint
     */
    @Overwrite
    public void onLivingUpdate() {
        FDPClient.eventManager.callEvent(new UpdateEvent());

        if (sprintingTicksLeft > 0) {
            --sprintingTicksLeft;

            if (sprintingTicksLeft == 0) {
                setSprinting(false);
            }
        }

        if (sprintToggleTimer > 0) {
            --sprintToggleTimer;
        }

        prevTimeInPortal = timeInPortal;

        if (inPortal) {
            if (mc.currentScreen != null && !mc.currentScreen.doesGuiPauseGame() && !PortalMenu.INSTANCE.handleEvents()) {
                mc.displayGuiScreen(null);
            }

            if (timeInPortal == 0f) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("portal.trigger"), rand.nextFloat() * 0.4F + 0.8F));
            }

            timeInPortal += 0.0125F;

            if (timeInPortal >= 1f) {
                timeInPortal = 1f;
            }

            inPortal = false;
        } else if (isPotionActive(Potion.confusion) && getActivePotionEffect(Potion.confusion).getDuration() > 60) {
            timeInPortal += 0.006666667F;

            if (timeInPortal > 1f) {
                timeInPortal = 1f;
            }
        } else {
            if (timeInPortal > 0f) {
                timeInPortal -= 0.05F;
            }

            if (timeInPortal < 0f) {
                timeInPortal = 0f;
            }
        }

        if (timeUntilPortal > 0) {
            --timeUntilPortal;
        }

        boolean flag = movementInput.jump;
        boolean flag1 = movementInput.sneak;
        float f = 0.8F;
        boolean flag2 = movementInput.moveForward >= f;
        movementInput.updatePlayerMoveState();

        final Rotation currentRotation = RotationUtils.targetRotation;

        // A separate movement input for currentRotation
        MovementInput modifiedInput = new MovementInput();

        // Recreate inputs
        modifiedInput.moveForward = movementInput.moveForward;
        modifiedInput.moveStrafe = movementInput.moveStrafe;

        // Reverse the effects of sneak and apply them after the input variable calculates the input
        if (movementInput.sneak) {
            modifiedInput.moveStrafe /= 0.3f;
            modifiedInput.moveForward /= 0.3f;
        }

        // Calculate and apply the movement input based on rotation
        float moveForward = currentRotation != null ? Math.round(modifiedInput.moveForward * MathHelper.cos(MathExtensionsKt.toRadians(rotationYaw - currentRotation.getYaw())) + modifiedInput.moveStrafe * MathHelper.sin(MathExtensionsKt.toRadians(rotationYaw - currentRotation.getYaw()))) : modifiedInput.moveForward;
        float moveStrafe = currentRotation != null ? Math.round(modifiedInput.moveStrafe * MathHelper.cos(MathExtensionsKt.toRadians(rotationYaw - currentRotation.getYaw())) - modifiedInput.moveForward * MathHelper.sin(MathExtensionsKt.toRadians(rotationYaw - currentRotation.getYaw()))) : modifiedInput.moveStrafe;

        modifiedInput.moveForward = moveForward;
        modifiedInput.moveStrafe = moveStrafe;

        if (movementInput.sneak) {
            final SneakSlowDownEvent sneakSlowDownEvent = new SneakSlowDownEvent(movementInput.moveStrafe, movementInput.moveForward);
            FDPClient.eventManager.callEvent(sneakSlowDownEvent);
            movementInput.moveStrafe = sneakSlowDownEvent.getStrafe();
            movementInput.moveForward = sneakSlowDownEvent.getForward();
            // Add the sneak effect back
            modifiedInput.moveForward *= 0.3f;
            modifiedInput.moveStrafe *= 0.3f;
            // Call again the event but this time have the modifiedInput
            final SneakSlowDownEvent secondSneakSlowDownEvent = new SneakSlowDownEvent(modifiedInput.moveStrafe, modifiedInput.moveForward);
            FDPClient.eventManager.callEvent(secondSneakSlowDownEvent);
            modifiedInput.moveStrafe = secondSneakSlowDownEvent.getStrafe();
            modifiedInput.moveForward = secondSneakSlowDownEvent.getForward();
        }

        final NoSlow noSlow = NoSlow.INSTANCE;
        final KillAura killAura = KillAura.INSTANCE;

        boolean isUsingItem = getHeldItem() != null && (isUsingItem() || (getHeldItem().getItem() instanceof ItemSword && killAura.getBlockingStatus()) || NoSlow.INSTANCE.isUNCPBlocking());

        if (isUsingItem && !isRiding()) {
            final SlowDownEvent slowDownEvent = new SlowDownEvent(0.2F, 0.2F);
            FDPClient.eventManager.callEvent(slowDownEvent);
            movementInput.moveStrafe *= slowDownEvent.getStrafe();
            movementInput.moveForward *= slowDownEvent.getForward();
            sprintToggleTimer = 0;
            modifiedInput.moveStrafe *= slowDownEvent.getStrafe();
            modifiedInput.moveForward *= slowDownEvent.getForward();
        }

        pushOutOfBlocks(posX - width * 0.35, getEntityBoundingBox().minY + 0.5, posZ + width * 0.35);
        pushOutOfBlocks(posX - width * 0.35, getEntityBoundingBox().minY + 0.5, posZ - width * 0.35);
        pushOutOfBlocks(posX + width * 0.35, getEntityBoundingBox().minY + 0.5, posZ - width * 0.35);
        pushOutOfBlocks(posX + width * 0.35, getEntityBoundingBox().minY + 0.5, posZ + width * 0.35);

        final Sprint sprint = Sprint.INSTANCE;

        boolean flag3 = (float) getFoodStats().getFoodLevel() > 6F || capabilities.allowFlying;
        if (onGround && !flag1 && !flag2 && movementInput.moveForward >= f && !isSprinting() && flag3 && !isUsingItem() && !isPotionActive(Potion.blindness)) {
            if (sprintToggleTimer <= 0 && !mc.gameSettings.keyBindSprint.isKeyDown()) {
                sprintToggleTimer = 7;
            } else {
                setSprinting(true);
            }
        }

        if (!isSprinting() && movementInput.moveForward >= f && flag3 && (noSlow.handleEvents() || !isUsingItem()) && !isPotionActive(Potion.blindness) && mc.gameSettings.keyBindSprint.isKeyDown()) {
            setSprinting(true);
        }

        if (isSprinting() && (movementInput.moveForward < f || isCollidedHorizontally || !flag3)) {
            setSprinting(false);
        }

        FDPClient.eventManager.callEvent(new SprintUpdateEvent(EventState.POST));

        sprint.correctSprintState(modifiedInput, isUsingItem);

        if (capabilities.allowFlying) {
            if (mc.playerController.isSpectatorMode()) {
                if (!capabilities.isFlying) {
                    capabilities.isFlying = true;
                    sendPlayerAbilities();
                }
            } else if (!flag && movementInput.jump) {
                if (flyToggleTimer == 0) {
                    flyToggleTimer = 7;
                } else {
                    capabilities.isFlying = !capabilities.isFlying;
                    sendPlayerAbilities();
                    flyToggleTimer = 0;
                }
            }
        }

        if (capabilities.isFlying && isCurrentViewEntity()) {
            if (movementInput.sneak) {
                motionY -= capabilities.getFlySpeed() * 3f;
            }

            if (movementInput.jump) {
                motionY += capabilities.getFlySpeed() * 3f;
            }
        }

        if (isRidingHorse()) {
            if (horseJumpPowerCounter < 0) {
                ++horseJumpPowerCounter;

                if (horseJumpPowerCounter == 0) {
                    horseJumpPower = 0f;
                }
            }

            if (flag && !movementInput.jump) {
                horseJumpPowerCounter = -10;
                sendHorseJump();
            } else if (!flag && movementInput.jump) {
                horseJumpPowerCounter = 0;
                horseJumpPower = 0f;
            } else if (flag) {
                ++horseJumpPowerCounter;

                if (horseJumpPowerCounter < 10) {
                    horseJumpPower = (float) horseJumpPowerCounter * 0.1F;
                } else {
                    horseJumpPower = 0.8F + 2f / (float) (horseJumpPowerCounter - 9) * 0.1F;
                }
            }
        } else {
            horseJumpPower = 0f;
        }

        super.onLivingUpdate();

        if (onGround && capabilities.isFlying && !mc.playerController.isSpectatorMode()) {
            capabilities.isFlying = false;
            sendPlayerAbilities();
        }
    }

    @Override
    public void moveEntity(double x, double y, double z) {
        MoveEvent moveEvent = new MoveEvent(x, y, z);
        FDPClient.eventManager.callEvent(moveEvent);

        if (moveEvent.isCancelled())
            return;

        x = moveEvent.getX();
        y = moveEvent.getY();
        z = moveEvent.getZ();

        if (noClip) {
            setEntityBoundingBox(getEntityBoundingBox().offset(x, y, z));
            posX = (getEntityBoundingBox().minX + getEntityBoundingBox().maxX) / 2;
            posY = getEntityBoundingBox().minY;
            posZ = (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) / 2;
        } else {
            worldObj.theProfiler.startSection("move");
            double d0 = posX;
            double d1 = posY;
            double d2 = posZ;

            if (this.isInWeb) {
                this.isInWeb = false;
                x *= 0.25;
                y *= 0.05000000074505806;
                z *= 0.25;
                motionX = 0;
                motionY = 0;
                motionZ = 0;
            }

            double d3 = x;
            double d4 = y;
            double d5 = z;
            boolean flag = onGround && isSneaking();

            if (flag || moveEvent.isSafeWalk()) {
                double d6;

                //noinspection ConstantConditions
                for (d6 = 0.05; x != 0 && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(x, -1, 0)).isEmpty(); d3 = x) {
                    if (x < d6 && x >= -d6) {
                        x = 0;
                    } else if (x > 0) {
                        x -= d6;
                    } else {
                        x += d6;
                    }
                }

                //noinspection ConstantConditions
                for (; z != 0 && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(0, -1, z)).isEmpty(); d5 = z) {
                    if (z < d6 && z >= -d6) {
                        z = 0;
                    } else if (z > 0) {
                        z -= d6;
                    } else {
                        z += d6;
                    }
                }

                //noinspection ConstantConditions
                for (; x != 0 && z != 0 && worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().offset(x, -1, z)).isEmpty(); d5 = z) {
                    if (x < d6 && x >= -d6) {
                        x = 0;
                    } else if (x > 0) {
                        x -= d6;
                    } else {
                        x += d6;
                    }

                    d3 = x;

                    if (z < d6 && z >= -d6) {
                        z = 0;
                    } else if (z > 0) {
                        z -= d6;
                    } else {
                        z += d6;
                    }
                }
            }

            // noinspection ConstantConditions
            List<AxisAlignedBB> list1 = worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().addCoord(x, y, z));
            AxisAlignedBB axisalignedbb = getEntityBoundingBox();

            for (AxisAlignedBB axisalignedbb1 : list1) {
                y = axisalignedbb1.calculateYOffset(getEntityBoundingBox(), y);
            }

            setEntityBoundingBox(getEntityBoundingBox().offset(0, y, 0));
            boolean flag1 = onGround || d4 != y && d4 < 0;

            for (AxisAlignedBB axisalignedbb2 : list1) {
                x = axisalignedbb2.calculateXOffset(getEntityBoundingBox(), x);
            }

            setEntityBoundingBox(getEntityBoundingBox().offset(x, 0, 0));

            for (AxisAlignedBB axisalignedbb13 : list1) {
                z = axisalignedbb13.calculateZOffset(getEntityBoundingBox(), z);
            }

            setEntityBoundingBox(getEntityBoundingBox().offset(0, 0, z));

            if (this.stepHeight > 0.0F && flag1 && (d3 != x || d5 != z)) {
                StepEvent stepEvent = new StepEvent(this.stepHeight, EventState.PRE);
                FDPClient.eventManager.callEvent(stepEvent);
                double d11 = x;
                double d7 = y;
                double d8 = z;
                AxisAlignedBB axisalignedbb3 = getEntityBoundingBox();
                setEntityBoundingBox(axisalignedbb);
                y = stepEvent.getStepHeight();
                //noinspection ConstantConditions
                List<AxisAlignedBB> list = worldObj.getCollidingBoundingBoxes((Entity) (Object) this, getEntityBoundingBox().addCoord(d3, y, d5));
                AxisAlignedBB axisalignedbb4 = getEntityBoundingBox();
                AxisAlignedBB axisalignedbb5 = axisalignedbb4.addCoord(d3, 0, d5);
                double d9 = y;

                for (AxisAlignedBB axisalignedbb6 : list) {
                    d9 = axisalignedbb6.calculateYOffset(axisalignedbb5, d9);
                }

                axisalignedbb4 = axisalignedbb4.offset(0, d9, 0);
                double d15 = d3;

                for (AxisAlignedBB axisalignedbb7 : list) {
                    d15 = axisalignedbb7.calculateXOffset(axisalignedbb4, d15);
                }

                axisalignedbb4 = axisalignedbb4.offset(d15, 0, 0);
                double d16 = d5;

                for (AxisAlignedBB axisalignedbb8 : list) {
                    d16 = axisalignedbb8.calculateZOffset(axisalignedbb4, d16);
                }

                axisalignedbb4 = axisalignedbb4.offset(0, 0, d16);
                AxisAlignedBB axisalignedbb14 = getEntityBoundingBox();
                double d17 = y;

                for (AxisAlignedBB axisalignedbb9 : list) {
                    d17 = axisalignedbb9.calculateYOffset(axisalignedbb14, d17);
                }

                axisalignedbb14 = axisalignedbb14.offset(0, d17, 0);
                double d18 = d3;

                for (AxisAlignedBB axisalignedbb10 : list) {
                    d18 = axisalignedbb10.calculateXOffset(axisalignedbb14, d18);
                }

                axisalignedbb14 = axisalignedbb14.offset(d18, 0, 0);
                double d19 = d5;

                for (AxisAlignedBB axisalignedbb11 : list) {
                    d19 = axisalignedbb11.calculateZOffset(axisalignedbb14, d19);
                }

                axisalignedbb14 = axisalignedbb14.offset(0, 0, d19);
                double d20 = d15 * d15 + d16 * d16;
                double d10 = d18 * d18 + d19 * d19;

                if (d20 > d10) {
                    x = d15;
                    z = d16;
                    y = -d9;
                    setEntityBoundingBox(axisalignedbb4);
                } else {
                    x = d18;
                    z = d19;
                    y = -d17;
                    setEntityBoundingBox(axisalignedbb14);
                }

                for (AxisAlignedBB axisalignedbb12 : list) {
                    y = axisalignedbb12.calculateYOffset(getEntityBoundingBox(), y);
                }

                setEntityBoundingBox(getEntityBoundingBox().offset(0, y, 0));

                if (d11 * d11 + d8 * d8 >= x * x + z * z) {
                    x = d11;
                    y = d7;
                    z = d8;
                    setEntityBoundingBox(axisalignedbb3);
                } else {
                    FDPClient.eventManager.callEvent(new StepEvent(-1f, EventState.POST));
                }
            }

            worldObj.theProfiler.endSection();
            worldObj.theProfiler.startSection("rest");
            posX = (getEntityBoundingBox().minX + getEntityBoundingBox().maxX) / 2;
            posY = getEntityBoundingBox().minY;
            posZ = (getEntityBoundingBox().minZ + getEntityBoundingBox().maxZ) / 2;
            isCollidedHorizontally = d3 != x || d5 != z;
            isCollidedVertically = d4 != y;
            onGround = isCollidedVertically && d4 < 0;
            isCollided = isCollidedHorizontally || isCollidedVertically;
            int i = MathHelper.floor_double(posX);
            int j = MathHelper.floor_double(posY - 0.20000000298023224);
            int k = MathHelper.floor_double(posZ);
            BlockPos blockpos = new BlockPos(i, j, k);
            Block block1 = worldObj.getBlockState(blockpos).getBlock();

            if (block1.getMaterial() == Material.air) {
                Block block = worldObj.getBlockState(blockpos.down()).getBlock();

                if (block instanceof BlockFence || block instanceof BlockWall || block instanceof BlockFenceGate) {
                    block1 = block;
                    blockpos = blockpos.down();
                }
            }

            updateFallState(y, onGround, block1, blockpos);

            if (d3 != x) {
                motionX = 0;
            }

            if (d5 != z) {
                motionZ = 0;
            }

            if (d4 != y) {
                //noinspection ConstantConditions
                block1.onLanded(worldObj, (Entity) (Object) this);
            }

            if (canTriggerWalking() && !flag && ridingEntity == null) {
                double d12 = posX - d0;
                double d13 = posY - d1;
                double d14 = posZ - d2;

                if (block1 != Blocks.ladder) {
                    d13 = 0;
                }

                if (onGround) {
                    //noinspection ConstantConditions
                    block1.onEntityCollidedWithBlock(worldObj, blockpos, (Entity) (Object) this);
                }

                distanceWalkedModified = (float) (distanceWalkedModified + MathHelper.sqrt_double(d12 * d12 + d14 * d14) * 0.6);
                distanceWalkedOnStepModified = (float) (distanceWalkedOnStepModified + MathHelper.sqrt_double(d12 * d12 + d13 * d13 + d14 * d14) * 0.6);

                if (distanceWalkedOnStepModified > (float) getNextStepDistance() && block1.getMaterial() != Material.air) {
                    setNextStepDistance((int) distanceWalkedOnStepModified + 1);

                    if (isInWater()) {
                        float f = MathHelper.sqrt_double(motionX * motionX * 0.20000000298023224 + motionY * motionY + motionZ * motionZ * 0.20000000298023224) * 0.35F;

                        if (f > 1f) {
                            f = 1f;
                        }

                        playSound(getSwimSound(), f, 1f + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
                    }

                    playStepSound(blockpos, block1);
                }
            }

            try {
                doBlockCollisions();
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Checking entity block collision");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Entity being checked for collision");
                addEntityCrashInfo(crashreportcategory);
                throw new ReportedException(crashreport);
            }

            boolean flag2 = isWet();

            if (worldObj.isFlammableWithin(getEntityBoundingBox().contract(0.001, 0.001, 0.001))) {
                dealFireDamage(1);

                if (!flag2) {
                    setFire(getFire() + 1);

                    if (getFire() == 0) {
                        setFire(8);
                    }
                }
            } else if (getFire() <= 0) {
                setFire(-fireResistance);
            }

            if (flag2 && getFire() > 0) {
                playSound("random.fizz", 0.7F, 1.6F + (rand.nextFloat() - rand.nextFloat()) * 0.4F);
                setFire(-fireResistance);
            }

            worldObj.theProfiler.endSection();
        }
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onUpdate()V", shift = At.Shift.BEFORE, ordinal = 0), cancellable = true)
    private void preTickEvent(CallbackInfo ci) {
        final PlayerTickEvent tickEvent = new PlayerTickEvent(EventState.PRE);
        FDPClient.eventManager.callEvent(tickEvent);

        if (tickEvent.isCancelled()) {
            FDPClient.eventManager.callEvent(new RotationUpdateEvent());
            ci.cancel();
        }
    }

    @Inject(method = "onUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/AbstractClientPlayer;onUpdate()V", shift = At.Shift.AFTER, ordinal = 0))
    private void postTickEvent(CallbackInfo ci) {
        final PlayerTickEvent tickEvent = new PlayerTickEvent(EventState.POST);
        FDPClient.eventManager.callEvent(tickEvent);
    }
}

