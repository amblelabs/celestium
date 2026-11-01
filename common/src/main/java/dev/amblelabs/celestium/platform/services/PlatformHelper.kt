package dev.amblelabs.celestium.platform.services

import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import java.util.function.Function
import java.util.function.Supplier
import java.util.stream.Stream

// TODO: javadocs
interface PlatformHelper {
    /**
     * Gets the name of the current platform
     * 
     * @return The name of the current platform.
     */
    val platformName: String

    /**
     * Checks if a mod with the given id is loaded.
     * 
     * @param modId The mod to check if it is loaded.
     * @return `true` if the mod is loaded, `false` otherwise.
     */
    fun isModLoaded(modId: String): Boolean

    /**
     * Check if the game is currently in a development environment.
     * 
     * @return `true` if in a development environment, `false` otherwise.
     */
    val isDevelopmentEnvironment: Boolean

    fun sendToPlayer(target: ServerPlayer, packet: CustomPacketPayload)

    fun sendToAll(targets: Stream<ServerPlayer>, packet: CustomPacketPayload)

    fun sendToAll(targets: MutableCollection<ServerPlayer>, packet: CustomPacketPayload)

    fun sendToNearby(pos: Vec3, radius: Double, level: ServerLevel, packet: CustomPacketPayload)

    fun sendToTracking(entity: Entity, packet: CustomPacketPayload)

    fun <T : Any> createRegistrar(registry: Registry<T>): Registrar<T>
    fun <T : Any> createRegistrar(registry: ResourceKey<out Registry<T>>): Registrar<T>

    interface Registrar<B: Any> {
        fun <T : B> register(id: Identifier, t: Function<Identifier, T>): Supplier<T>

        fun <T : B> register(name: String, t: Function<Identifier, T>): Supplier<T> {
            return register(Identifier.fromNamespaceAndPath(namespace, name), t)
        }

        fun <T : B> register(id: Identifier, t: Supplier<T>): Supplier<T> {
            return register(id) { _ -> t.get() }
        }

        fun <T : B> register(name: String, t: Supplier<T>): Supplier<T> {
            return register(Identifier.fromNamespaceAndPath(namespace, name), t)
        }

        fun <T : B> registerHolder(id: Identifier, provider: Function<Identifier, T>): Holder<T>

        fun <T : B> registerHolder(name: String, provider: Function<Identifier, T>): Holder<T> {
            return registerHolder(Identifier.fromNamespaceAndPath(namespace, name), provider)
        }

        fun <T : B> registerHolder(id: Identifier, provider: Supplier<T>): Holder<T> {
            return registerHolder(id) { _ -> provider.get() }
        }

        fun <T : B> registerHolder(name: String, provider: Supplier<T>): Holder<T> {
            return registerHolder(Identifier.fromNamespaceAndPath(namespace, name), provider)
        }

        val namespace: String

        fun registerAll()
    }
}