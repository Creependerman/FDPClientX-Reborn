/*
 * Kevin Client - Reborn
 * GPL3
 */
package net.ccbluex.liquidbounce.features.module.modules.other

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer.Companion.getColorIndex
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.ping
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemArmor
import net.minecraft.network.play.server.*
import net.minecraft.world.WorldSettings
import java.awt.Color
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.regex.Pattern

@ModuleInfo("AntiBot","Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.OTHER)
object  AntiBot : Module() {
    private val modeValue = ListValue("Mode", arrayOf("Custom", "NoColorArmor", "UnusualArmor"), "Custom")
    private val removeFromWorld = BoolValue("RemoveFromWord", false)
    private val debugValue = BoolValue("Debug", true)

    private val tabValue = BoolValue("Tab", true).displayable { modeValue.get() == "Custom"}
    private val tabModeValue = ListValue("TabMode", arrayOf("Equals", "Contains"), "Contains").displayable { modeValue.get() == "Custom"}
    private val entityIDValue = BoolValue("EntityID", true).displayable { modeValue.get() == "Custom"}
    private val colorValue = BoolValue("Color", false).displayable { modeValue.get() == "Custom"}
    private val livingTimeValue = BoolValue("LivingTime", false).displayable { modeValue.get() == "Custom"}
    private val livingTimeTicksValue = IntegerValue("LivingTimeTicks", 40, 1, 200).displayable { modeValue.get() == "Custom"}
    private val groundValue = BoolValue("Ground", true).displayable { modeValue.get() == "Custom"}
    private val airValue = BoolValue("Air", false).displayable { modeValue.get() == "Custom"}
    private val invalidGroundValue = BoolValue("InvalidGround", true).displayable { modeValue.get() == "Custom"}
    private val swingValue = BoolValue("Swing", false).displayable { modeValue.get() == "Custom"}
    private val healthValue = BoolValue("Health", false).displayable { modeValue.get() == "Custom"}
    private val derpValue = BoolValue("Derp", true).displayable { modeValue.get() == "Custom"}
    private val wasInvisibleValue = BoolValue("WasInvisible", false).displayable { modeValue.get() == "Custom"}
    private val validNameValue = BoolValue("ValidName", true).displayable { modeValue.get() == "Custom"}
    private val armorValue = BoolValue("Armor", false).displayable { modeValue.get() == "Custom"}
    private val pingValue = BoolValue("Ping", false).displayable { modeValue.get() == "Custom"}
    private val needHitValue = BoolValue("NeedHit", false).displayable { modeValue.get() == "Custom"}
    private val noClipValue = BoolValue("NoClip", false).displayable { modeValue.get() == "Custom"}
    private val matrix7 = BoolValue("Matrix7", false).displayable { modeValue.get() == "Custom"}
    private val czechHekValue = BoolValue("CzechMatrix", false).displayable { modeValue.get() == "Custom"}
    private val czechHekPingCheckValue = BoolValue("PingCheck", true).displayable { modeValue.get() == "Custom" && czechHekValue.get()}
    private val czechHekGMCheckValue = BoolValue("GamemodeCheck", true).displayable { modeValue.get() == "Custom" && czechHekValue.get()}
    private val reusedEntityIdValue = BoolValue("ReusedEntityId", false).displayable { modeValue.get() == "Custom"}
    private val spawnInCombatValue = BoolValue("SpawnInCombat", false).displayable { modeValue.get() == "Custom"}
    private val skinValue = BoolValue("SkinCheck", false).displayable { modeValue.get() == "Custom"}
    private val duplicateInWorldValue = BoolValue("DuplicateInWorld", false).displayable { modeValue.get() == "Custom"}
    private val duplicateInTabValue = BoolValue("DuplicateInTab", false).displayable { modeValue.get() == "Custom"}
    private val duplicateCompareModeValue = ListValue("DuplicateCompareMode", arrayOf("OnTime", "WhenSpawn"), "OnTime").displayable { modeValue.get() == "Custom"}
    private val fastDamageValue = BoolValue("FastDamage", false).displayable { modeValue.get() == "Custom"}
    private val fastDamageTicksValue = IntegerValue("FastDamageTicks", 5, 1, 20).displayable { modeValue.get() == "Custom" && fastDamageValue.get()}
    private val alwaysInRadiusValue = BoolValue("AlwaysInRadius", false).displayable { modeValue.get() == "Custom"}
    private val alwaysRadiusValue = FloatValue("AlwaysInRadiusBlocks", 20f, 5f, 30f).displayable { modeValue.get() == "Custom"}
    private val alwaysInRadiusRemoveValue = BoolValue("AlwaysInRadiusRemove", false).displayable { modeValue.get() == "Custom"}
    private val alwaysInRadiusWithTicksCheckValue = BoolValue("AlwaysInRadiusWithTicksCheck", false).displayable { modeValue.get() == "Custom"}

    //Helmet
    private val allowDiamondHelmet = BoolValue("AllowDiamondHelmet", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowGoldenHelmet = BoolValue("AllowGoldenHelmet", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowIronHelmet = BoolValue("AllowIronHelmet", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowChainHelmet = BoolValue("AllowChainHelmet", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowLeatherHelmet = BoolValue("AllowLeatherHelmet", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowNoHelmet = BoolValue("AllowNoHelmet", true).displayable { modeValue.get() == "UnusualArmor"}

    //Chestplate
    private val allowDiamondChestplate = BoolValue("AllowDiamondChestplate", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowGoldenChestplate = BoolValue("AllowGoldenChestplate", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowIronChestplate = BoolValue("AllowIronChestplate", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowChainChestplate = BoolValue("AllowChainChestplate", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowLeatherChestplate = BoolValue("AllowLeatherChestplate", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowNoChestplate = BoolValue("AllowNoChestplate", true).displayable { modeValue.get() == "UnusualArmor"}

    //Leggings
    private val allowDiamondLeggings = BoolValue("AllowDiamondLeggings", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowGoldenLeggings = BoolValue("AllowGoldenLeggings", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowIronLeggings = BoolValue("AllowIronLeggings", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowChainLeggings = BoolValue("AllowChainLeggings", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowLeatherLeggings = BoolValue("AllowLeatherLeggings", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowNoLeggings = BoolValue("AllowNoLeggings", true).displayable { modeValue.get() == "UnusualArmor"}

    //Boots
    private val allowDiamondBoots = BoolValue("AllowDiamondBoots", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowGoldenBoots = BoolValue("AllowGoldenBoots", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowIronBoots = BoolValue("AllowIronBoots", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowChainBoots = BoolValue("AllowChainBoots", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowLeatherBoots = BoolValue("AllowLeatherBoots", true).displayable { modeValue.get() == "UnusualArmor"}
    private val allowNoBoots = BoolValue("AllowNoBoots", true).displayable { modeValue.get() == "UnusualArmor"}

    private val removeNoColorLeatherArmor = BoolValue("NoColorLeatherArmor", true).displayable { modeValue.get() == "UnusualArmor"}

    private val botList = CopyOnWriteArrayList<EntityLivingBase>()

    private val ground = mutableListOf<Int>()
    private val air = mutableListOf<Int>()
    private val invalidGround = mutableMapOf<Int, Int>()
    private val swing = mutableListOf<Int>()
    private val invisible = mutableListOf<Int>()
    private val hitted = mutableListOf<Int>()
    private val spawnInCombat = mutableListOf<Int>()
    private val notAlwaysInRadius = mutableListOf<Int>()
    private val lastDamage = mutableMapOf<Int, Int>()
    private val lastDamageVl = mutableMapOf<Int, Float>()
    private val duplicate = mutableListOf<UUID>()
    private val noClip = mutableListOf<Int>()
    private val matrix = mutableListOf<Int>()
    private val hasRemovedEntities = mutableListOf<Int>()
    private val regex = Regex("\\w{3,16}")
    private var wasAdded = mc.thePlayer != null

    override val tag: String
        get() = modeValue.get()

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val playerEntities = mc.theWorld.playerEntities.toList()
        botList.clear()
        when {
            modeValue.get() == "NoColorArmor" -> {
                for (player in playerEntities) {
                    if (player == mc.thePlayer) continue
                    var isBot = false
                    val armorInventory = player.inventory.armorInventory
                    for (armor in armorInventory) {
                        if (armor == null || armor.item == null) continue
                        val itemArmor: ItemArmor
                        try {
                            itemArmor = armor.item as ItemArmor
                        } catch (e: Exception) {
                            continue
                        }
                        if (itemArmor.armorMaterial == ItemArmor.ArmorMaterial.LEATHER) {
                            if (!armor.hasTagCompound()) isBot = true
                        }
                    }
                    if (isBot) {
                        botList.add(player)
                    }
                }
            }

            modeValue.get() == "UnusualArmor" -> {
                for (player in playerEntities) {
                    if (player == mc.thePlayer) continue
                    var isBot = false
                    val armorInventory = player.inventory.armorInventory
                    val boots = armorInventory[0]
                    val leggings = armorInventory[1]
                    val chestPlate = armorInventory[2]
                    val helmet = armorInventory[3]
                    if (
                    //NoArmor
                        ((boots == null || boots.item == null) && !allowNoBoots.get())
                        || ((leggings == null || leggings.item == null) && !allowNoLeggings.get())
                        || ((chestPlate == null || chestPlate.item == null) && !allowNoChestplate.get())
                        || ((helmet == null || helmet.item == null) && !allowNoHelmet.get())
                        //Diamond
                        || ((helmet != null && helmet.item != null && helmet.item is ItemArmor) && (helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND && !allowDiamondHelmet.get())
                        || ((chestPlate != null && chestPlate.item != null && chestPlate.item is ItemArmor) && (chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND && !allowDiamondChestplate.get())
                        || ((leggings != null && leggings.item != null && leggings.item is ItemArmor) && (leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND && !allowDiamondLeggings.get())
                        || ((boots != null && boots.item != null && boots.item is ItemArmor) && (boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND && !allowDiamondBoots.get())
                        //Golden
                        || ((helmet != null && helmet.item != null && helmet.item is ItemArmor) && (helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD && !allowGoldenHelmet.get())
                        || ((chestPlate != null && chestPlate.item != null && chestPlate.item is ItemArmor) && (chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD && !allowGoldenChestplate.get())
                        || ((leggings != null && leggings.item != null && leggings.item is ItemArmor) && (leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD && !allowGoldenLeggings.get())
                        || ((boots != null && boots.item != null && boots.item is ItemArmor) && (boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD && !allowGoldenBoots.get())
                        //Iron
                        || ((helmet != null && helmet.item != null && helmet.item is ItemArmor) && (helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON && !allowIronHelmet.get())
                        || ((chestPlate != null && chestPlate.item != null && chestPlate.item is ItemArmor) && (chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON && !allowIronChestplate.get())
                        || ((leggings != null && leggings.item != null && leggings.item is ItemArmor) && (leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON && !allowIronLeggings.get())
                        || ((boots != null && boots.item != null && boots.item is ItemArmor) && (boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON && !allowIronBoots.get())
                        //Chain
                        || ((helmet != null && helmet.item != null && helmet.item is ItemArmor) && (helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN && !allowChainHelmet.get())
                        || ((chestPlate != null && chestPlate.item != null && chestPlate.item is ItemArmor) && (chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN && !allowChainChestplate.get())
                        || ((leggings != null && leggings.item != null && leggings.item is ItemArmor) && (leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN && !allowChainLeggings.get())
                        || ((boots != null && boots.item != null && boots.item is ItemArmor) && (boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN && !allowChainBoots.get())
                        //Leather
                        || ((helmet != null && helmet.item != null && helmet.item is ItemArmor) && (helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !allowLeatherHelmet.get())
                        || ((chestPlate != null && chestPlate.item != null && chestPlate.item is ItemArmor) && (chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !allowLeatherChestplate.get())
                        || ((leggings != null && leggings.item != null && leggings.item is ItemArmor) && (leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !allowLeatherLeggings.get())
                        || ((boots != null && boots.item != null && boots.item is ItemArmor) && (boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !allowLeatherBoots.get())
                        //LeatherNoColor
                        || ((
                                ((helmet != null && helmet.item != null && helmet.item is ItemArmor) && (helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !helmet.hasTagCompound())
                                        || ((chestPlate != null && chestPlate.item != null && chestPlate.item is ItemArmor) && (chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !chestPlate.hasTagCompound())
                                        || ((leggings != null && leggings.item != null && leggings.item is ItemArmor) && (leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !leggings.hasTagCompound())
                                        || ((boots != null && boots.item != null && boots.item is ItemArmor) && (boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER && !boots.hasTagCompound())
                                ) && removeNoColorLeatherArmor.get())
                    ) isBot = true
                    if (isBot) {
                        botList.add(player)
                    }
                }
            }
        }
        if (removeFromWorld.get()) {
            if (mc.thePlayer == null || mc.theWorld == null) return
            val bots: MutableList<EntityPlayer> = ArrayList()
            for (player in playerEntities)
                if (player !== mc.thePlayer && isBot(player)) bots.add(player)
            for (bot in bots)
                removeBot(bot)
        }
    }

    private fun removeBot(bot: Entity) {
        mc.theWorld.removeEntityFromWorld(bot.entityId)
        if (debugValue.get())
            FDPClient.hud.addNotification(Notification("Removed Bot", "AntiBot", NotifyType.INFO))
    }

    @JvmStatic
    fun isBot(entity: EntityLivingBase): Boolean {
        // Check if entity is a player
        if (entity !is EntityPlayer || entity === mc.thePlayer) {
            return false
        }

        // Check if anti bot is enabled
        if (!state) {
            return false
        }

        if (entity in botList)
            return true

        if (modeValue.get() != "Custom")
            return false

        if (validNameValue.get() && !entity.name.matches(regex)) {
            return true
        }

        // Anti Bot checks
        if (colorValue.get() && !entity.displayName.formattedText.replace("§r", "").contains("§")) {
            return true
        }

        if (livingTimeValue.get() && entity.ticksExisted < livingTimeTicksValue.get()) {
            return true
        }

        if (groundValue.get() && !ground.contains(entity.entityId)) {
            return true
        }

        if (airValue.get() && !air.contains(entity.entityId)) {
            return true
        }

        if (swingValue.get() && !swing.contains(entity.entityId)) {
            return true
        }

        if (noClipValue.get() && noClip.contains(entity.entityId)) {
            return true
        }

        if (reusedEntityIdValue.get() && hasRemovedEntities.contains(entity.entityId)) {
            return false
        }

        if (healthValue.get() && (entity.health > 20F || entity.health <= 0F)) {
            return true
        }

        if (spawnInCombatValue.get() && spawnInCombat.contains(entity.entityId)) {
            return true
        }

        if (entityIDValue.get() && (entity.entityId >= 1000000000 || entity.entityId <= -1)) {
            return true
        }

        if (derpValue.get() && (entity.rotationPitch > 90F || entity.rotationPitch < -90F)) {
            return true
        }

        if (wasInvisibleValue.get() && invisible.contains(entity.entityId)) {
            return true
        }

        if (armorValue.get()) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null
            ) {
                return true
            }
        }

        if (pingValue.get()) {
            if (mc.netHandler.getPlayerInfo(entity.uniqueID)?.responseTime == 0) {
                return true
            }
        }

        if (needHitValue.get() && !hitted.contains(entity.entityId)) {
            return true
        }

        if (invalidGroundValue.get() && invalidGround.getOrDefault(entity.entityId, 0) >= 10) {
            return true
        }

        if (tabValue.get()) {
            val equals = tabModeValue.equals("Equals")
            val targetName = stripColor(entity.displayName.formattedText)

            for (networkPlayerInfo in mc.netHandler.playerInfoMap) {
                val networkName = stripColor(networkPlayerInfo.getFullName())

                if (if (equals) targetName == networkName else targetName.contains(networkName)) {
                    return false
                }
            }

            return true
        }

        if (matrix7.get() && matrix.contains(entity.entityId)) {
            return true
        }

        if (duplicateCompareModeValue.equals("WhenSpawn") && duplicate.contains(entity.gameProfile.id)) {
            return true
        }

        if (duplicateInWorldValue.get() && duplicateCompareModeValue.equals("OnTime") && mc.theWorld.loadedEntityList.count { it is EntityPlayer && it.name == it.name } > 1) {
            return true
        }

        if (duplicateInTabValue.get() && duplicateCompareModeValue.equals("OnTime") && mc.netHandler.playerInfoMap.count { entity.name == it.gameProfile.name } > 1) {
            return true
        }

        if (fastDamageValue.get() && lastDamageVl.getOrDefault(entity.entityId, 0f) > 0) {
            return true
        }

        if (alwaysInRadiusValue.get() && !notAlwaysInRadius.contains(entity.entityId)) {
            return true
        }

        if (skinValue.get()) {
            val info = mc.netHandler.getPlayerInfo(entity.uniqueID) ?: return true
            if (!info.hasLocationSkin()) return true
        }

        return entity.name.isEmpty() || entity.name == mc.thePlayer.name
    }

    private fun NetworkPlayerInfo.getFullName(): String {
        if (displayName != null) {
            return displayName!!.formattedText
        }

        val team = playerTeam
        val name = gameProfile.name
        return team?.formatString(name) ?: name
    }

    override fun onDisable() {
        clearAll()
        super.onDisable()
    }

    private fun processEntityMove(entity: Entity, onGround: Boolean) {
        if (entity is EntityPlayer) {
            if (onGround && !ground.contains(entity.entityId)) {
                ground.add(entity.entityId)
            }

            if (!onGround && !air.contains(entity.entityId)) {
                air.add(entity.entityId)
            }

            if (onGround) {
                if (entity.prevPosY != entity.posY) {
                    invalidGround[entity.entityId] = invalidGround.getOrDefault(entity.entityId, 0) + 1
                }
            } else {
                val currentVL = invalidGround.getOrDefault(entity.entityId, 0) / 2
                if (currentVL <= 0) {
                    invalidGround.remove(entity.entityId)
                } else {
                    invalidGround[entity.entityId] = currentVL
                }
            }

            if (entity.isInvisible && !invisible.contains(entity.entityId)) {
                invisible.add(entity.entityId)
            }

            if (!noClip.contains(entity.entityId)) {
                val cb = mc.theWorld.getCollidingBoundingBoxes(
                    entity,
                    entity.entityBoundingBox.contract(0.0625, 0.0625, 0.0625)
                )
//                alert("NOCLIP[${cb.size}] ${entity.displayName.unformattedText} ${entity.posX} ${entity.posY} ${entity.posZ}")
                if (cb.isNotEmpty()) {
                    noClip.add(entity.entityId)
                }
            }

            if ((!livingTimeValue.get() || entity.ticksExisted > livingTimeTicksValue.get() || !alwaysInRadiusWithTicksCheckValue.get()) && !notAlwaysInRadius.contains(
                    entity.entityId
                ) && mc.thePlayer.getDistanceToEntity(entity) > alwaysRadiusValue.get()
            ) {
                notAlwaysInRadius.add(entity.entityId)
                if (alwaysInRadiusRemoveValue.get()) {
                    mc.theWorld.removeEntity(entity)
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) return
        val packet = event.packet

        when (packet) {
            is S18PacketEntityTeleport -> processEntityMove(mc.theWorld.getEntityByID(packet.entityId) ?: return, packet.onGround)
            is S14PacketEntity -> {
                if (czechHekValue.get()) wasAdded = false
                processEntityMove(packet.getEntity(mc.theWorld) ?: return, packet.onGround)
            }
            is S0BPacketAnimation -> {
                val entity = mc.theWorld.getEntityByID(packet.entityID)

                if (entity != null && entity is EntityLivingBase && packet.animationType == 0 &&
                    !swing.contains(entity.entityId)
                ) {
                    swing.add(entity.entityId)
                }
            }
            is S38PacketPlayerListItem -> {
                val data = packet.entries[0]
                if (duplicateCompareModeValue.equals("WhenSpawn") && packet.action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                    packet.entries.forEach { entry ->
                        val name = entry.profile.name
                        if (duplicateInWorldValue.get() && mc.theWorld.playerEntities.any { it.name == name } ||
                            duplicateInTabValue.get() && mc.netHandler.playerInfoMap.any { it.gameProfile.name == name }) {
                            duplicate.add(entry.profile.id)
                        }
                    }
                }
                //
                if (czechHekValue.get()) {
                    if (data.profile != null && data.profile.name != null) {
                        if (!wasAdded) wasAdded =
                            data.profile.name == mc.thePlayer.name else if (!mc.thePlayer.isSpectator && !mc.thePlayer.capabilities.allowFlying && (!czechHekPingCheckValue.get() || data.ping != 0) && (!czechHekGMCheckValue.get() || data.gameMode != WorldSettings.GameType.NOT_SET)) {
                            event.cancelEvent()
                            if (debugValue.get()) Chat.print("§7[§a§lAnti Bot/§6Matrix§7] §fPrevented §r" + data.profile.name + " §ffrom spawning.")
                        }
                    }
                }
                //
                if (matrix7.get()) {
                    mc.theWorld.playerEntities.forEach { entity ->
                        if (entity.inventory.armorInventory.all { it == null } || entity.heldItem == null)
                            return

                        val player = mc.thePlayer ?: return
                        val playerPosY = player.posY - 2..player.posY + 2

                        if (entity.posY in playerPosY) {
                            if (packet.action == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                                if (data.gameMode == WorldSettings.GameType.SURVIVAL) {
                                    if (!matrix.contains(entity.entityId)) {
                                        matrix.add(entity.entityId)
                                        if (debugValue.get()) Chat.alert("AntiBot + ${entity.gameProfile?.name}")
                                    }
                                }
                            }

                            if (packet.action == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                                if (matrix.contains(entity.entityId)) {
                                    matrix.remove(entity.entityId)
                                    if (debugValue.get()) Chat.alert("AntiBot - ${entity.gameProfile?.name}")
                                }
                            }
                        }
                    }
                }
            }
            is S0CPacketSpawnPlayer -> {
                if (FDPClient.combatManager.inCombat && !hasRemovedEntities.contains(packet.entityID)) {
                    spawnInCombat.add(packet.entityID)
                }
            }
            is S13PacketDestroyEntities -> hasRemovedEntities.addAll(packet.entityIDs.toTypedArray())
        }

        if (packet is S19PacketEntityStatus && packet.opCode.toInt() == 2 || packet is S0BPacketAnimation && packet.animationType == 1) {
            val entity = if (packet is S19PacketEntityStatus) {
                packet.getEntity(mc.theWorld)
            } else if (packet is S0BPacketAnimation) {
                mc.theWorld.getEntityByID(packet.entityID)
            } else {
                null
            } ?: return

            if (entity is EntityPlayer) {
                lastDamageVl[entity.entityId] =
                    lastDamageVl.getOrDefault(entity.entityId, 0f) + if (entity.ticksExisted - lastDamage.getOrDefault(
                            entity.entityId,
                            0
                        ) <= fastDamageTicksValue.get()
                    ) {
                        1f
                    } else {
                        -0.5f
                    }
                lastDamage[entity.entityId] = entity.ticksExisted
            }
        }
    }

    @EventTarget
    fun onAttack(e: AttackEvent) {
        val entity = e.targetEntity

        if (entity is EntityLivingBase && !hitted.contains(entity.entityId)) {
            hitted.add(entity.entityId)
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        clearAll()
    }

    private fun clearAll() {
        hitted.clear()
        swing.clear()
        ground.clear()
        invalidGround.clear()
        invisible.clear()
        lastDamage.clear()
        lastDamageVl.clear()
        notAlwaysInRadius.clear()
        duplicate.clear()
        spawnInCombat.clear()
        noClip.clear()
        hasRemovedEntities.clear()
        matrix.clear()
    }

}