package dev.amblelabs.celestium.platform

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.platform.services.PlatformHelper
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener
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

class FabricPlatformHelper : PlatformHelper {
    override val platformName = "Fabric"

    override fun isModLoaded(modId: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    override val isDevelopmentEnvironment: Boolean = FabricLoader.getInstance().isDevelopmentEnvironment

    override fun sendToPlayer(target: ServerPlayer, packet: CustomPacketPayload) {
        ServerPlayNetworking.send(target, packet)
    }

    override fun sendToAll(targets: Stream<ServerPlayer>, packet: CustomPacketPayload) {
        val wrapped: Packet<ClientCommonPacketListener> =
            ServerPlayNetworking.createClientboundPacket(packet)
        targets.forEach { target: ServerPlayer? -> target!!.connection.send(wrapped) }
    }

    override fun sendToAll(targets: MutableCollection<ServerPlayer>, packet: CustomPacketPayload) {
        sendToAll(targets.stream(), packet)
    }

    override fun sendToNearby(pos: Vec3, radius: Double, level: ServerLevel, packet: CustomPacketPayload) {
        sendToAll(PlayerLookup.around(level, pos, radius), packet)
    }

    override fun sendToTracking(entity: Entity, packet: CustomPacketPayload) {
        sendToAll(PlayerLookup.tracking(entity), packet)
    }

    override fun <T : Any> createRegistrar(registry: Registry<T>): PlatformHelper.Registrar<T> {
        return FabricRegistrar(Celestium.MOD_ID, registry)
    }

    override fun <T : Any> createRegistrar(registry: ResourceKey<out Registry<T>>): PlatformHelper.Registrar<T> {
        return this.createRegistrar(
            BuiltInRegistries.REGISTRY.get(registry.identifier()).orElseThrow().value() as Registry<T>
        )
    }

    @JvmRecord
    data class FabricRegistrar<B : Any>(override val namespace: String, val registry: Registry<B>) : PlatformHelper.Registrar<B> {
        override fun <T : B> register(id: Identifier, t: Function<Identifier, T>): Supplier<T> {
            val value = t.apply(id)
            Registry.register(registry, id, value)
            return Supplier { value }
        }

        override fun <T : B> registerHolder(id: Identifier, provider: Function<Identifier, T>): Holder<T> {
            val value = provider.apply(id)
            Registry.register(registry, id, value)
            return registry.wrapAsHolder(value) as Holder<T>
        }

        override fun registerAll() {}
    }
}