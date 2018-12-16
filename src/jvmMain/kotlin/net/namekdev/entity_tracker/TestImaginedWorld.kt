package net.namekdev.entity_tracker

import com.artemis.*
import com.artemis.systems.EntityProcessingSystem
import net.namekdev.entity_tracker.network.ArtemisWorldSerializer
import net.namekdev.entity_tracker.network.base.WebSocketServer

object TestImaginedWorld {
    @JvmStatic fun main(args: Array<String>) {
        initTestWorld()
    }

    lateinit var world: World

    fun initTestWorld() {
        val entityTracker = EntityTracker(
            ArtemisWorldSerializer(WebSocketServer().start())
        )

        world = World(WorldConfiguration()
            .setSystem(entityTracker)
            .setSystem(PlayerSystem::class.java)
            .setSystem(MovementSystem::class.java)
            .setSystem(ShootingSystem::class.java)
        )

        world.process()

        val player = world.createEntity().edit()
            .add(Player())
            .add(Movement())
            .add(Position())
            .entity

        val bullet = createBullet()
        world.process()

        world.delete(bullet.id)
        world.process()

        val bullets = arrayOf(
            createBullet(), createBullet(), createBullet()
        )
        world.process()
    }

    fun createBullet() = world.createEntity().edit()
        .add(Bullet())
        .add(Movement())
        .add(Position())
        .entity

    class PlayerSystem : EntityProcessingSystem(Aspect.all(Player::class.java)) {
        override fun process(e: Entity?) {
        }
    }

    class MovementSystem : BaseEntitySystem(Aspect.all(Position::class.java, Movement::class.java)) {
        override fun processSystem() {
        }
    }

    class ShootingSystem : BaseEntitySystem(Aspect.all(Bullet::class.java, Position::class.java, Movement::class.java)) {
        override fun processSystem() {
        }
    }

    class Player : Component() {
        var name: String? = null
    }

    class Bullet : Component() {
        var type: Int = 0
    }

    class Position : Component() {
        var x: Float = 0f
        var y: Float = 0f
    }

    class Movement : Component() {
        var forward: Boolean = true
    }
}