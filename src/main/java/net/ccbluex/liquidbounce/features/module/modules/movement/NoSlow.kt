
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.minecraft.item.*
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C16PacketClientStatus.EnumState
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S09PacketHeldItemChange
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.play.server.S30PacketWindowItems
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.util.*
import kotlin.math.sqrt

@ModuleInfo(name = "NoSlow", category = ModuleCategory.MOVEMENT)
object NoSlow : Module() {

    val mode = arrayOf(
        "Vanilla",
        "LiquidBounce",
        "Custom",
        "WatchDog",
        "WatchDog2",
        "UNCP",
        "NCP",
        "NewMatrix",
        "AAC",
        "AAC4",
        "AAC5",
        "SwitchItem",
        "Matrix",
        "Medusa",
        "OldIntave",
        "Intave",
        "InvalidC08",
        "GrimAC",
        "GrimC09",
        "Hypixel",
        "HypixelNew",
        "SpamItemChange",
        "SpamPlace",
        "SpamEmptyPlace"
    )
    //Basic settings
    private val modeValue = ListValue(
        "PacketMode",
        mode.sortedArray(),
        "Vanilla"
    )
    private val antiSwitchItem = BoolValue("AntiSwitchItem", false)
    private val onlyGround = BoolValue("OnlyGround", false)
    private val onlyMove = BoolValue("OnlyMove", false)

    //Modify Slowdown / Packets
    private val blockModifyValue = BoolValue("Blocking", true)
    private val blockForwardMultiplier =
        FloatValue("BlockForwardMultiplier", 1.0F, 0.2F, 1.0F) { blockModifyValue.get() }
    private val blockStrafeMultiplier =
        FloatValue("BlockStrafeMultiplier", 1.0F, 0.2F, 1.0F) { blockModifyValue.get() }
    private val consumeModifyValue = BoolValue("Consume", true)
    private val consumePacketValue = ListValue(
        "ConsumePacket",
        arrayOf("None", "AAC5", "SpamItemChange", "SpamPlace", "SpamEmptyPlace","UNCP", "Glitch", "Grim","Bug","Intave","InvalidC08", "Packet").sortedArray(),
        "None"
    ) { consumeModifyValue.get() }
    private val consumeTimingValue =
        ListValue("ConsumeTiming", arrayOf("Pre", "Post"), "Pre") { consumeModifyValue.get() }
    private val consumeForwardMultiplier =
        FloatValue("ConsumeForwardMultiplier", 1.0F, 0.2F, 1.0F) { consumeModifyValue.get() }
    private val consumeStrafeMultiplier =
        FloatValue("ConsumeStrafeMultiplier", 1.0F, 0.2F, 1.0F) { consumeModifyValue.get() }
    private val bowModifyValue = BoolValue("Bow", true)
    private val bowPacketValue = ListValue(
        "BowPacket",
        arrayOf("None", "AAC5", "SpamItemChange", "SpamPlace", "SpamEmptyPlace","UNCP", "Glitch", "Grim","InvalidC08", "Packet"),
        "None"
    ) { bowModifyValue.get() }
    private val bowTimingValue =
        ListValue("BowTiming", arrayOf("Pre", "Post"), "Pre") { bowModifyValue.get() }
    private val bowForwardMultiplier =
        FloatValue("BowForwardMultiplier", 1.0F, 0.2F, 1.0F) { bowModifyValue.get() }
    private val bowStrafeMultiplier =
        FloatValue("BowStrafeMultiplier", 1.0F, 0.2F, 1.0F) { bowModifyValue.get() }
    private val customOnGround = BoolValue("CustomOnGround", false) { modeValue.equals("Custom") }
    private val customDelayValue = IntegerValue("CustomDelay", 60, 10, 200) { modeValue.equals("Custom") }
    val soulSandValue = BoolValue("SoulSand", true)

    //AACv4
    private val c07Value = BoolValue("AAC4-C07", true) { modeValue.equals("AAC4") }
    private val c08Value = BoolValue("AAC4-C08", true) { modeValue.equals("AAC4") }
    private val groundValue = BoolValue("AAC4-OnGround", true) { modeValue.equals("AAC4") }

    // Slowdown on teleport
    private val teleportValue = BoolValue("Teleport", false)
    private val teleportModeValue = ListValue(
        "TeleportMode",
        arrayOf("Vanilla", "VanillaNoSetback", "Custom", "Decrease"),
        "Vanilla"
    ) { teleportValue.get() }
    private val teleportNoApplyValue = BoolValue("TeleportNoApply", false) { teleportValue.get() }
    private val teleportCustomSpeedValue = FloatValue("Teleport-CustomSpeed", 0.13f, 0f, 1f) {
        teleportValue.get() && teleportModeValue.equals("Custom")
    }
    private val teleportCustomYValue =
        BoolValue("Teleport-CustomY", false) { teleportValue.get() && teleportModeValue.equals("Custom") }
    private val teleportDecreasePercentValue = FloatValue(
        "Teleport-DecreasePercent",
        0.13f,
        0f,
        1f
    ) { teleportValue.get() && teleportModeValue.equals("Decrease") }

    private var pendingFlagApplyPacket = false
    private var lastMotionX = 0.0
    private var lastMotionY = 0.0
    private var lastMotionZ = 0.0
    private val msTimer = MSTimer()
    private val matrixcheck = MSTimer()
    private var sendBuf = false
    private var packetBuf = LinkedList<Packet<INetHandlerPlayServer>>()
    private var nextTemp = false
    private var waitC03 = false
    private var sendPacket = false
    private var lastBlockingStat = false
    private var eatslow = false
    // bug
    private var bugt = 0
    private var start = false
    private var stop = false
    private var mstimer2 = MSTimer()

    private var shouldNoSlow = false

    private var hasDropped = false
    //hypixel
    private var postPlace = false
    //UNCP
    private var shouldSwap = false

    override fun onEnable() {
        start = false
        stop = false
        mstimer2.reset()
    }

    override fun onDisable() {
        shouldSwap = false
        bugt = 0
        matrixcheck.reset()
        msTimer.reset()
        eatslow = false
        pendingFlagApplyPacket = false
        sendBuf = false
        packetBuf.clear()
        nextTemp = false
        waitC03 = false
    }

    private fun sendPacket(
        event: MotionEvent,
        sendC07: Boolean,
        sendC08: Boolean,
        delay: Boolean,
        delayValue: Long,
        onGround: Boolean,
        watchDog: Boolean = false
    ) {
        val digging = C07PacketPlayerDigging(
            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
            BlockPos(-1, -1, -1),
            EnumFacing.DOWN
        )
        val blockPlace = C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem())
        val blockMent = C08PacketPlayerBlockPlacement(
            BlockPos(-1, -1, -1),
            255,
            mc.thePlayer.inventory.getCurrentItem(),
            0f,
            0f,
            0f
        )
        if (onGround && !mc.thePlayer.onGround) {
            return
        }
        if (sendC07 && event.eventState == EventState.PRE) {
            if (delay && msTimer.hasTimePassed(delayValue)) {
                mc.netHandler.addToSendQueue(digging)
            } else if (!delay) {
                mc.netHandler.addToSendQueue(digging)
            }
        }
        if (sendC08 && event.eventState == EventState.POST) {
            if (delay && msTimer.hasTimePassed(delayValue) && !watchDog) {
                mc.netHandler.addToSendQueue(blockPlace)
                msTimer.reset()
            } else if (!delay && !watchDog) {
                mc.netHandler.addToSendQueue(blockPlace)
            } else if (watchDog) {
                mc.netHandler.addToSendQueue(blockMent)
            }
        }
    }

    private fun sendPacket2(packetType: String,event: MotionEvent) {
        when (packetType.lowercase()) {
            "aac5" -> {
                mc.netHandler.addToSendQueue(
                    C08PacketPlayerBlockPlacement(
                        BlockPos(-1, -1, -1),
                        255,
                        mc.thePlayer.inventory.getCurrentItem(),
                        0f,
                        0f,
                        0f
                    )
                )
            }
            "invalidc08" -> {
                val heldItem = mc.thePlayer.heldItem
                if (start) {
                    // Food Only
                    if (heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk) {
                        return
                    }

                    if (getEmptySlot() != -1) {
                        if (mc.thePlayer.ticksExisted % 3 == 0)
                            PacketUtils.sendPacket(
                                C08PacketPlayerBlockPlacement(
                                    BlockPos(-1, -1, -1),
                                    1,
                                    null,
                                    0f,
                                    0f,
                                    0f
                                ),
                                false
                            )
                    }
                }
            }

            "spamitemchange" -> {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            }

            "spamplace" -> {
                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
            }

            "spamemptyplace" -> {
                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement())
            }

            "glitch" -> {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            }
            "intave" -> {
                if (event.eventState == EventState.PRE) mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,BlockPos.ORIGIN,EnumFacing.UP))
            }
            "uncp" -> {
                if (start && (shouldSwap)) {
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                    PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.heldItem, 0f, 0f, 0f),false)
                    shouldSwap = false
                }
            }

            "grim" -> {
                val handle = mc.thePlayer.inventory.currentItem
                PacketUtils.sendPackets(
                    C09PacketHeldItemChange(handle % 8 + 1),
                    C09PacketHeldItemChange(handle % 7 + 2),
                    C09PacketHeldItemChange(handle)
                )
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        if ((!MovementUtils.isMoving && onlyMove.get()) || (onlyGround.get() && !mc.thePlayer.onGround)) {
            return
        }

        val killAura = FDPClient.moduleManager[KillAura::class.java]!!
        val heldItem = mc.thePlayer.heldItem?.item
        if (event.eventState == EventState.POST && modeValue.equals("Hypixel") && postPlace) {
            if (mc.thePlayer.ticksExisted % 3 == 0) {
                mc.thePlayer.sendQueue.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
            }
            postPlace = false
        }
        start = event.eventState == EventState.PRE
        if (consumeModifyValue.get() && mc.thePlayer.isUsingItem && (heldItem is ItemFood || heldItem is ItemPotion || heldItem is ItemBucketMilk)) {
            if ((consumeTimingValue.equals("Pre") && event.eventState == EventState.PRE) || (consumeTimingValue.equals("Post") && event.eventState == EventState.POST)) {
                sendPacket2(consumePacketValue.get(), event)
            }
        }

        if (bowModifyValue.get() && mc.thePlayer.isUsingItem && heldItem is ItemBow) {
            if ((bowTimingValue.equals("Pre") && event.eventState == EventState.PRE) || (bowTimingValue.equals("Post") && event.eventState == EventState.POST)) {
                sendPacket2(bowPacketValue.get(),event)
            }
        }

        if ((blockModifyValue.get() && (mc.thePlayer.isBlocking || killAura.blockingStatus) && heldItem is ItemSword)
            || (bowModifyValue.get() && mc.thePlayer.isUsingItem && heldItem is ItemBow && bowPacketValue.equals("Packet"))
            || (consumeModifyValue.get() && mc.thePlayer.isUsingItem && (heldItem is ItemFood || heldItem is ItemPotion || heldItem is ItemBucketMilk) && consumePacketValue.equals(
                "Packet"
            ))
        ) {
            when (modeValue.get().lowercase()) {
                "liquidbounce" -> {
                    sendPacket(event, sendC07 = true, sendC08 = true, delay = false, delayValue = 0, onGround = false)
                }

                "aac" -> {
                    if (mc.thePlayer.ticksExisted % 3 == 0) {
                        sendPacket(
                            event,
                            sendC07 = true,
                            sendC08 = false,
                            delay = false,
                            delayValue = 0,
                            onGround = false
                        )
                    } else if (mc.thePlayer.ticksExisted % 3 == 1) {
                        sendPacket(
                            event,
                            sendC07 = false,
                            sendC08 = true,
                            delay = false,
                            delayValue = 0,
                            onGround = false
                        )
                    }
                }

                "aac4" -> {
                    sendPacket(event, c07Value.get(), c08Value.get(), true, 80, groundValue.get())
                }

                "aac5" -> {
                    if (event.eventState == EventState.POST) {
                        mc.netHandler.addToSendQueue(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1),
                                255,
                                mc.thePlayer.inventory.getCurrentItem(),
                                0f,
                                0f,
                                0f
                            )
                        )
                    }
                }
                "UNCP" -> {
                    if (event.eventState == EventState.POST && usingItemFunc()) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, mc.thePlayer.heldItem, 0f, 0f, 0f),false)
                        PacketUtils.sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, mc.thePlayer.heldItem, 0f, 0f, 0f
                            ),
                            false
                        )
                    }
                }

                "custom" -> {
                    sendPacket(
                        event,
                        sendC07 = true,
                        sendC08 = true,
                        delay = true,
                        delayValue = customDelayValue.get().toLong(),
                        onGround = customOnGround.get()
                    )
                }

                "ncp" -> {
                    sendPacket(event, sendC07 = true, sendC08 = true, delay = false, delayValue = 0, onGround = false)
                }

                "watchdog2" -> {
                    if (event.eventState == EventState.PRE) {
                        mc.netHandler.addToSendQueue(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                BlockPos.ORIGIN,
                                EnumFacing.DOWN
                            )
                        )
                    } else {
                        mc.netHandler.addToSendQueue(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1),
                                255,
                                null,
                                0.0f,
                                0.0f,
                                0.0f
                            )
                        )
                    }
                }
                "intave" -> {
                    if (event.eventState == EventState.PRE) mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
                }
                "invalidc08" -> {
                    val heldItem = mc.thePlayer.heldItem ?: return
                    if (event.eventState == EventState.PRE) {
                        // Food Only
                        if (heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk) {
                            return
                        }

                        if (getEmptySlot() != -1) {
                            if (mc.thePlayer.ticksExisted % 3 == 0)
                                PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f),false)
                        }
                    }
                }

                "hypixel" -> {
                    postPlace = false
                    if (mc.thePlayer.ticksExisted % 3 == 0) {
                        PacketUtils.sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1),
                                EnumFacing.UP.index,
                                null,
                                0.0f,
                                0.0f,
                                0.0f
                            ),
                            false
                        )
                    }
                }

                "watchdog" -> {
                    if (mc.thePlayer.ticksExisted % 2 == 0) {
                        sendPacket(event, true, sendC08 = false, delay = true, delayValue = 50, onGround = true)
                    } else {
                        sendPacket(
                            event,
                            sendC07 = false,
                            sendC08 = true,
                            delay = false,
                            delayValue = 0,
                            onGround = true,
                            watchDog = true
                        )
                    }
                }

                "oldintave" -> {
                    if (mc.thePlayer.isUsingItem) {
                        if (event.eventState == EventState.PRE) {
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                        }
                        if (event.eventState == EventState.POST) {
                            mc.netHandler.addToSendQueue(
                                C08PacketPlayerBlockPlacement(
                                    mc.thePlayer.inventoryContainer.getSlot(
                                        mc.thePlayer.inventory.currentItem + 36
                                    ).stack
                                )
                            )
                        }
                    }
                }
                "grimc09" -> {
                    val handle = mc.thePlayer.inventory.currentItem
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(handle % 8 + 1))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(handle % 7 + 2))
                    mc.netHandler.addToSendQueue(C09PacketHeldItemChange(handle))
                }

                "switchitem" -> {
                    PacketUtils.sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1),false)
                    PacketUtils.sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem),false)
                }

                "hypixelnew" -> {
                    if (getEmptySlot() != -1 && event.eventState == EventState.PRE && mc.thePlayer.ticksExisted % 3 != 0) {
                        mc.netHandler.addToSendQueue(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1),
                                255,
                                null,
                                0f,
                                0f,
                                0f
                            )
                        )
                    }
                }

                "spamitemchange" -> {
                    if (event.eventState == EventState.PRE)
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                }

                "spamplace" -> {
                    if (event.eventState == EventState.PRE)
                        mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                }
            }
        }
    }

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        if (mc.thePlayer == null || mc.theWorld == null || (onlyGround.get() && !mc.thePlayer.onGround))
            return
        val heldItem = mc.thePlayer.heldItem?.item

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is ItemFood, is ItemPotion, is ItemBucketMilk -> {
            if (consumeModifyValue.get())
                if (isForward) this.consumeForwardMultiplier.get() else this.consumeStrafeMultiplier.get() else 0.2F
        }

        is ItemSword -> {
            if (blockModifyValue.get())
                if (isForward) this.blockForwardMultiplier.get() else this.blockStrafeMultiplier.get() else 0.2F
        }

        is ItemBow -> {
            if (bowModifyValue.get())
                if (isForward) this.bowForwardMultiplier.get() else this.bowStrafeMultiplier.get() else 0.2F
        }

        else -> 0.2F
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val isBlocking =
            mc.thePlayer.isUsingItem && mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword
        if (mc.thePlayer == null || mc.theWorld == null || (onlyGround.get() && !mc.thePlayer.onGround))
            return

        if (blockModifyValue.get()) {
            if ((modeValue.equals("Matrix") || modeValue.equals("GrimAC")) && (lastBlockingStat || isBlocking)) {
                if (msTimer.hasTimePassed(230) && nextTemp) {
                    nextTemp = false
                    if (modeValue.equals("GrimAC")) {
                        PacketUtils.sendPacket(
                            C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9),
                            false
                        )
                        PacketUtils.sendPacket(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem), false)
                    } else {
                        PacketUtils.sendPacket(
                            C07PacketPlayerDigging(
                                C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                                BlockPos(-1, -1, -1),
                                EnumFacing.DOWN
                            ),
                            false
                        )
                    }
                    if (packetBuf.isNotEmpty()) {
                        var canAttack = false
                        for (packet in packetBuf) {
                            if (packet is C03PacketPlayer) {
                                canAttack = true
                            }
                            if (!((packet is C02PacketUseEntity || packet is C0APacketAnimation) && !canAttack)) {
                                PacketUtils.sendPacket(packet, false)
                            }
                        }
                        packetBuf.clear()
                    }
                }
                if (!nextTemp) {
                    lastBlockingStat = isBlocking
                    if (!isBlocking) {
                        return
                    }
                    PacketUtils.sendPacket(
                        C08PacketPlayerBlockPlacement(
                            BlockPos(-1, -1, -1),
                            255,
                            mc.thePlayer.inventory.getCurrentItem(),
                            0f,
                            0f,
                            0f
                        ),
                        false
                    )
                    nextTemp = true
                    waitC03 = false
                    msTimer.reset()
                }
            }
        }
    }

    private val isBlocking: Boolean
        get() = (mc.thePlayer.isUsingItem || FDPClient.moduleManager[KillAura::class.java]!!.blockingStatus) && mc.thePlayer.heldItem != null && mc.thePlayer.heldItem.item is ItemSword

    private fun getEmptySlot(): Int {
        for (i in 1..44) {
            mc.thePlayer.inventoryContainer.getSlot(i).stack ?: return i
        }
        return -1
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null || (onlyGround.get() && !mc.thePlayer.onGround))
            return

        val packet = event.packet
        val heldItem = mc.thePlayer.heldItem.item ?: return

        if (consumePacketValue.equals("Bug")) {
            if (mc.thePlayer.heldItem?.item !is ItemFood) return

            val isUsingItem = packet is C08PacketPlayerBlockPlacement && packet.placedBlockDirection == 255

            if (!mc.thePlayer.isUsingItem) {
                shouldNoSlow = false
                hasDropped = false
            }

            if (isUsingItem && !hasDropped) {
                PacketUtils.sendPacket(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                shouldNoSlow = false
                hasDropped = true
            } else if (packet is S2FPacketSetSlot && mc.thePlayer.isUsingItem) {
                if (packet.func_149175_c() != 0) return

                event.cancelEvent()
                shouldNoSlow = true

                mc.thePlayer.itemInUse = packet.func_149174_e()
                if (!mc.thePlayer.isUsingItem) mc.thePlayer.itemInUseCount = 0
            }
            mc.thePlayer.stopUsingItem()
            mc.gameSettings.keyBindUseItem.pressed = false

        }

        stop = packet is C07PacketPlayerDigging && packet.status == (C07PacketPlayerDigging.Action.RELEASE_USE_ITEM)
        if (consumeModifyValue.get() && mc.thePlayer.isUsingItem && (heldItem is ItemFood || heldItem is ItemPotion || heldItem is ItemBucketMilk)) {
            if (consumePacketValue.equals("Grim")) {
                when (packet) {
                    is S30PacketWindowItems -> {
                        event.cancelEvent()
                        eatslow = false
                    }
                    is S2FPacketSetSlot -> {
                        event.cancelEvent()
                    }
                    is C08PacketPlayerBlockPlacement -> eatslow = true
                    is C07PacketPlayerDigging -> {
                        if (packet.status == C07PacketPlayerDigging.Action.RELEASE_USE_ITEM) {
                            eatslow = true
                        }
                    }
                }
            }
        }
        when (packet) {
            is C08PacketPlayerBlockPlacement -> {
                if (packet.stack?.item != null && mc.thePlayer.heldItem?.item != null && packet.stack.item == mc.thePlayer.heldItem?.item) {
                    if ((consumePacketValue.get() == "UNCP" && (packet.stack.item is ItemFood || packet.stack.item is ItemPotion || packet.stack.item is ItemBucketMilk)) || (bowPacketValue.get() == "UNCP" && packet.stack.item is ItemBow)) {
                        shouldSwap = true
                    }
                }
            }
        }

        if (antiSwitchItem.get() && packet is S09PacketHeldItemChange && (mc.thePlayer.isUsingItem || mc.thePlayer.isBlocking)) {
            event.cancelEvent()
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(packet.heldItemHotbarIndex))
            mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
        }

        if (modeValue.equals("Medusa")) {
            if ((mc.thePlayer.isUsingItem || mc.thePlayer.isBlocking) && sendPacket) {
                PacketUtils.sendPacket(
                    C0BPacketEntityAction(
                        mc.thePlayer,
                        C0BPacketEntityAction.Action.STOP_SPRINTING
                    ),
                    false
                )
                sendPacket = false
            }
            if (!mc.thePlayer.isUsingItem || !mc.thePlayer.isBlocking) {
                sendPacket = true
            }
        }


        if ((modeValue.equals("Matrix") || modeValue.equals("GrimAC")) && nextTemp) {
            if ((packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement) && isBlocking) {
                event.cancelEvent()
            } else if (packet is C03PacketPlayer || packet is C0APacketAnimation || packet is C0BPacketEntityAction || packet is C02PacketUseEntity || packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement) {
                packetBuf.add(packet as Packet<INetHandlerPlayServer>)
                event.cancelEvent()
            }
        } else if (teleportValue.get() && packet is S08PacketPlayerPosLook) {
            pendingFlagApplyPacket = true
            lastMotionX = mc.thePlayer.motionX
            lastMotionY = mc.thePlayer.motionY
            lastMotionZ = mc.thePlayer.motionZ
            when (teleportModeValue.get().lowercase()) {
                "vanillanosetback" -> {
                    val x = packet.x - mc.thePlayer.posX
                    val y = packet.y - mc.thePlayer.posY
                    val z = packet.z - mc.thePlayer.posZ
                    val diff = sqrt(x * x + y * y + z * z)
                    if (diff <= 8) {
                        event.cancelEvent()
                        pendingFlagApplyPacket = false
                        PacketUtils.sendPacket(
                            C06PacketPlayerPosLook(
                                packet.x,
                                packet.y,
                                packet.z,
                                packet.getYaw(),
                                packet.getPitch(),
                                mc.thePlayer.onGround
                            ),
                            false
                        )
                    }
                }
            }
        } else if (pendingFlagApplyPacket && packet is C06PacketPlayerPosLook) {
            pendingFlagApplyPacket = false
            if (teleportNoApplyValue.get()) {
                event.cancelEvent()
            }
            when (teleportModeValue.get().lowercase()) {
                "vanilla", "vanillanosetback" -> {
                    mc.thePlayer.motionX = lastMotionX
                    mc.thePlayer.motionY = lastMotionY
                    mc.thePlayer.motionZ = lastMotionZ
                }

                "custom" -> {
                    if (MovementUtils.isMoving) {
                        MovementUtils.strafe(teleportCustomSpeedValue.get())
                    }

                    if (teleportCustomYValue.get()) {
                        if (lastMotionY > 0) {
                            mc.thePlayer.motionY = teleportCustomSpeedValue.get().toDouble()
                        } else {
                            mc.thePlayer.motionY = -teleportCustomSpeedValue.get().toDouble()
                        }
                    }
                }

                "decrease" -> {
                    mc.thePlayer.motionX = lastMotionX * teleportDecreasePercentValue.get()
                    mc.thePlayer.motionY = lastMotionY * teleportDecreasePercentValue.get()
                    mc.thePlayer.motionZ = lastMotionZ * teleportDecreasePercentValue.get()
                }
            }
        }
    }
    fun isUNCPBlocking() = modeValue.get() == "UNCP" && mc.gameSettings.keyBindUseItem.isKeyDown && (mc.thePlayer.heldItem?.item is ItemSword)
    private fun usingItemFunc() = mc.thePlayer?.heldItem != null && (mc.thePlayer.isUsingItem || (mc.thePlayer.heldItem?.item is ItemSword && KillAura.blockingStatus) || isUNCPBlocking())

    override val tag: String
        get() = if (blockModifyValue.get()) {
            modeValue.get()
        } else if (consumeModifyValue.get()) {
            consumePacketValue.get()
        } else if (bowModifyValue.get()){
            bowPacketValue.get()
        } else {
            "NoOpen"
        }
}