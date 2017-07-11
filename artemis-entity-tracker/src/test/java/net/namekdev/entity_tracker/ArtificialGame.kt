package net.namekdev.entity_tracker

import com.artemis.Aspect
import com.artemis.Component
import com.artemis.Entity
import com.artemis.World
import com.artemis.WorldConfiguration
import com.artemis.systems.EntityProcessingSystem
import net.namekdev.entity_tracker.network.ArtemisWorldSerializer
import net.namekdev.entity_tracker.network.base.WebSocketServer

/**
 *
 */
fun main(args: Array<String>) {
    val entityTracker = EntityTracker(
        ArtemisWorldSerializer(WebSocketServer().start())
    )

    val world = World(WorldConfiguration()
        .setSystem(PositionSystem())
        .setSystem(RenderSystem())
        .setSystem(WeirdSystem())
        .setSystem(entityTracker)
    )

    for (i in 0..10) {
        val e = world.createEntity().edit()
        e.add(Pos(i * 6f, i * 2f))

        if (i % 3 == 0) {
            e.add(Speed(i * 10f))
        }
    }

    world.process()
}

class Pos(
    var x: Float = 0f,
    var y: Float = 0f
) : Component()

class Speed(var speed: Float = 1f) : Component()

class Renderable : Component()

class Weird : Component()


class PositionSystem : EntityProcessingSystem(
    Aspect.all(Pos::class.java, Speed::class.java)
) {
    override fun process(e: Entity) { }
}

class RenderSystem : EntityProcessingSystem(
    Aspect.all(Pos::class.java, Renderable::class.java)
) {
    override fun process(e: Entity) { }
}

class WeirdSystem : EntityProcessingSystem(
    Aspect.all(Pos::class.java, Speed::class.java, Weird::class.java)
) {
    override fun process(e: Entity) { }
}