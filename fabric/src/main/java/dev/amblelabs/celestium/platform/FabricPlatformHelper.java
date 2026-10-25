package dev.amblelabs.celestium.platform;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.platform.services.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Function;
import java.util.function.Supplier;

public class FabricPlatformHelper implements PlatformHelper {

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public <T> Registrar<T> createRegistrar(Registry<T> registry) {
        return new FabricRegistrar<>(Celestium.MOD_ID, registry);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registrar<T> createRegistrar(ResourceKey<? extends Registry<T>> registry) {
        return this.createRegistrar((Registry<T>) BuiltInRegistries.REGISTRY.get(registry.identifier()).orElseThrow().value());
    }

    public record FabricRegistrar<B>(String namespace, Registry<B> registry) implements Registrar<B> {

        @Override
        public <T extends B> Supplier<T> register(Identifier id, Function<Identifier, T> t) {
            T value = t.apply(id);
            Registry.register(registry, id, value);
            return () -> value;
        }

        @Override
        public <T extends B> Holder<T> registerHolder(Identifier id, Function<Identifier, T> provider) {
            T value = provider.apply(id);
            Registry.register(registry, id, value);
            return (Holder<T>) registry.wrapAsHolder(value);
        }

        @Override
        public void registerAll() { }
    }
}