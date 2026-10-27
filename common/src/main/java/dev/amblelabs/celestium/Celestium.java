package dev.amblelabs.celestium;

import dev.amblelabs.celestium.init.*;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class is part of the common project meaning it is shared between all supported loaders. Code written here can only
// import and access the vanilla codebase, libraries used by vanilla, and optionally third party libraries that provide
// common compatible binaries. This means common code can not directly use loader specific concepts such as Forge events
// however it will be compatible with all supported mod loaders.
public class Celestium {
    
    public static final String MOD_ID = "celestium";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String s) {
        return Identifier.fromNamespaceAndPath(MOD_ID, s);
    }

    public static final SimpleParticleType THRUSTER = new SimpleParticleType(false) {};

    public static void init() {
        CelestiumSounds.register();
        CelestiumBlocks.register();
        CelestiumItems.register();

        CelestiumEntityTypes.initialize();

        Registry.register(
                BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(MOD_ID, "thruster"),
                THRUSTER
        );
    }
}
