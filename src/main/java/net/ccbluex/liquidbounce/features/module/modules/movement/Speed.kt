/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.EnumAutoDisableType
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.ccbluex.liquidbounce.utils.ClassUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import kotlin.math.roundToInt

@ModuleInfo(name = "Speed", category = ModuleCategory.MOVEMENT, autoDisable = EnumAutoDisableType.FLAG)
object Speed : Module() {

    private val modes = ClassUtils.resolvePackage("${this.javaClass.`package`.name}.speeds", SpeedMode::class.java)
            .map { it.newInstance() as SpeedMode }
            .sortedBy { it.modeName }

    private val mode: SpeedMode
        get() = modes.find { modeValue.equals(it.modeName) } ?: modes.firstOrNull() ?: throw IllegalStateException("No mode available")

    private val modeValue: ListValue = object : ListValue("Mode", modes.map { it.modeName }.toTypedArray(), "VulcanHop") {
        override fun onChange(oldValue: String, newValue: String) {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }

    private val string = ListValue("String", arrayOf("Mode","Speed"),"Mode")
    private val noWater = BoolValue("NoWater", true)
    private val forceSprint = BoolValue("alwaysSprint", true)

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.thePlayer.isSneaking || (mc.thePlayer.isInWater && noWater.get())) return
        if (MovementUtils.isMoving && forceSprint.get()) mc.thePlayer.isSprinting = true
        mode.onUpdate()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {

        if (MovementUtils.isMoving && forceSprint.get()) {
            mc.thePlayer.isSprinting = true
        }

        mode.onMotion(event)

        if (mc.thePlayer.isSneaking || event.eventState !== EventState.PRE || (mc.thePlayer.isInWater && noWater.get())) {
            return
        }

        mode.onPreMotion()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer.isSneaking || (mc.thePlayer.isInWater && noWater.get())) {
            return
        }

        mode.onMove(event)
        FDPClient.moduleManager[TargetStrafe::class.java]!!.doMove(event)
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        if (mc.thePlayer.isSneaking || (mc.thePlayer.isInWater && noWater.get())) {
            return
        }

        mode.onTick()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mode.onPacket(event)
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        mode.onJump(event)
    }

    override fun onEnable() {
        mc.timer.timerSpeed = 1f
        mode.onEnable()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        mode.onDisable()
    }

    override val tag: String
        get() =
            (if (string.equals("Mode")) {
                modeValue.get()
            } else {
                (MovementUtils.bps).roundToInt()
            }).toString()
    /**
     * 读取mode中的value并和本体中的value合并
     * 所有的value必须在这个之前初始化
     */
    override val values = super.values.toMutableList().also {
        modes.map { mode ->
            mode.values.forEach { value ->
                //it.add(value.displayable { modeValue.equals(mode.modeName) })
                val displayableFunction = value.displayableFunction
                it.add(value.displayable { displayableFunction.invoke() && modeValue.equals(mode.modeName) })
            }
        }
    }
}
