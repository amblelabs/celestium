package dev.amblelabs.celestium.platform;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.CelestiumNeoForge;
import dev.amblelabs.celestium.platform.services.PlatformHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class NeoForgePlatformHelper implements PlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public <T> Registrar<T> createRegistrar(Registry<T> registry) {
        return this.createRegistrar(registry.key());
    }

    @Override
    public <T> Registrar<T> createRegistrar(ResourceKey<? extends Registry<T>> registry) {
        return new NeoForgeRegistrar<>(Celestium.MOD_ID, registry);
    }

    public record NeoForgeRegistrar<B>(String namespace, ResourceKey<? extends Registry<B>> registryKey, Map<String, DeferredRegister<B>> registers) implements Registrar<B> {

        public NeoForgeRegistrar(String namespace, ResourceKey<? extends Registry<B>> registryKey) {
            this(namespace, registryKey, new HashMap<>());
        }

        public DeferredRegister<B> get(String namespace) {
            return this.registers.computeIfAbsent(namespace, id -> DeferredRegister.create(registryKey, id));
        }

        public DeferredRegister<B> get(Identifier loc) {
            return this.get(loc.getNamespace());
        }

        @Override
        public <T extends B> Supplier<T> register(String id, Supplier<T> provider) {
            return get(this.namespace).register(id, provider);
        }

        @Override
        public <T extends B> Holder<T> registerHolder(Identifier id, Function<Identifier, T> provider) {
            return get(this.namespace).register(id, provider::apply);
        }

        @Override
        public <T extends B> Supplier<T> register(Identifier id, Function<Identifier, T> t) {
            return get(this.namespace).register(id, t.apply(id));
        }

        @Override
        public <T extends B> Supplier<T> register(Identifier loc, Supplier<T> supplier) {
            return get(loc).register(loc.getPath(), supplier);
        }

        @Override
        public void registerAll() {
            registers.forEach((modId, register) ->
                    register.register(CelestiumNeoForge.EVENT_BUS)); // TODO: dont depend on celestium
        }
    }
}