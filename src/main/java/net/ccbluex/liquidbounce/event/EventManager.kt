/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.event

import net.ccbluex.liquidbounce.utils.MinecraftInstance

class EventManager : MinecraftInstance() {

    private val registry = hashMapOf<Class<out Event>, MutableList<EventHook>>()

    /**
     * Register [listener]
     */
    fun registerListener(listener: Listenable) =
        listener.javaClass.declaredMethods.forEach { method ->
            if (method.isAnnotationPresent(EventTarget::class.java) && method.parameterTypes.size == 1) {
                if (!method.isAccessible)
                    method.isAccessible = true

                val eventClass = method.parameterTypes[0] as Class<out Event>
                val eventTarget = method.getAnnotation(EventTarget::class.java)

                with(registry.getOrPut(eventClass, ::ArrayList)) {
                    this += EventHook(listener, method, eventTarget)
                    this.sortByDescending { it.priority }
                }
            }
        }

    fun registerListenerAll(vararg listener: Listenable) {
        listener.forEach { registerListener(it) }
    }

    /**
     * Unregister listener
     *
     * @param listenable for unregister
     */
    fun unregisterListener(listenable: Listenable) =
        registry.forEach { (_, targets) ->
            targets.removeIf { it.eventClass == listenable }
        }

    /**
     * Call event to listeners
     *
     * @param event to call
     */
    fun callEvent(event: Event) {
        val targets = registry[event.javaClass] ?: return

        for (invokableEventTarget in targets) {
            try {
                if (!invokableEventTarget.eventClass.handleEvents() && !invokableEventTarget.isIgnoreCondition)
                    continue

                invokableEventTarget.method.invoke(invokableEventTarget.eventClass, event)
            } catch (throwable: Throwable) {
                throwable.printStackTrace()
            }
        }
    }
}
