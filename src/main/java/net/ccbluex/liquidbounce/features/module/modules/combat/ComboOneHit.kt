/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.IntegerValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C0APacketAnimation

@ModuleInfo(name = "ComboOneHit", category = ModuleCategory.COMBAT)
@SuppressWarnings("ALL")
object ComboOneHit : Module() {

    private val amountValue by IntegerValue("Packets", 200, 0, 500)
    private val swingValue by ListValue("SwingMode", arrayOf("Normal","Packet"), "Normal")
    private val onlyAuraValue by BoolValue("OnlyAura", false)
    private val gameBreaking by BoolValue("GameBreaking", false)

    @EventTarget
    fun onAttack(event: AttackEvent) {
        fun sendPacket() {
            mc.netHandler.addToSendQueue(C02PacketUseEntity(event.targetEntity, C02PacketUseEntity.Action.ATTACK))
        }
        fun swingPacket() {
            when(swingValue.lowercase()) {
                "normal" -> mc.thePlayer.swingItem()

                "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
        }
        if (onlyAuraValue && !FDPClient.moduleManager[KillAura::class.java]!!.state && !FDPClient.moduleManager[InfiniteAura::class.java]!!.state) return

        repeat (amountValue) {
            swingPacket()
            sendPacket()
        }
        if (gameBreaking) {
            repeat (amountValue) {
                swingPacket()
                repeat(3) {
                    sendPacket()
                }
            }
        }
    }
}
