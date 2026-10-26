package dev.amblelabs.celestium.platform.services;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

// TODO: javadocs
public interface PlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return The name of the current platform.
     */
    String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId The mod to check if it is loaded.
     * @return {@code true} if the mod is loaded, {@code false} otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return {@code true} if in a development environment, {@code false} otherwise.
     */
    boolean isDevelopmentEnvironment();

    void sendToPlayer(ServerPlayer target, CustomPacketPayload packet);

    void sendToAll(Stream<ServerPlayer> targets, CustomPacketPayload packet);

    void sendToAll(Collection<ServerPlayer> targets, CustomPacketPayload packet);

    void sendToNearby(Vec3 pos, double radius, ServerLevel level, CustomPacketPayload packet);

    void sendToTracking(Entity entity, CustomPacketPayload packet);

    <T> Registrar<T> createRegistrar(Registry<T> registry);
    <T> Registrar<T> createRegistrar(ResourceKey<? extends Registry<T>> registry);

    interface Registrar<B> {

        <T extends B> Supplier<T> register(Identifier id, Function<Identifier, T> t);

        default <T extends B> Supplier<T> register(String name, Function<Identifier, T> t) {
            return register(Identifier.fromNamespaceAndPath(this.namespace(), name), t);
        }

        default <T extends B> Supplier<T> register(Identifier id, Supplier<T> t) {
            return register(id, _ -> t.get());
        }

        default <T extends B> Supplier<T> register(String name, Supplier<T> t) {
            return register(Identifier.fromNamespaceAndPath(this.namespace(), name), t);
        }

        <T extends B> Holder<T> registerHolder(Identifier id, Function<Identifier, T> provider);

        default <T extends B> Holder<T> registerHolder(String name, Function<Identifier, T> provider) {
            return registerHolder(Identifier.fromNamespaceAndPath(this.namespace(), name), provider);
        }

        default <T extends B> Holder<T> registerHolder(Identifier id, Supplier<T> provider) {
            return registerHolder(id, _ -> provider.get());
        }

        default <T extends B> Holder<T> registerHolder(String name, Supplier<T> provider) {
            return registerHolder(Identifier.fromNamespaceAndPath(this.namespace(), name), provider);
        }

        String namespace();

        void registerAll();
    }
}