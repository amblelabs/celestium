package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class StarSystemEntries {

    public static final StarSystem SOLAR_SYSTEM = add(new StarSystem(Celestium.id("solar_system"),
            List.of(AstralBodyEntries.SOL,
                    AstralBodyEntries.MERCURY,
                    AstralBodyEntries.VENUS,
                    AstralBodyEntries.TERRA,
                    AstralBodyEntries.LUNA,
                    AstralBodyEntries.MARS,
                    AstralBodyEntries.JUPITER,
                    AstralBodyEntries.SATURN,
                    AstralBodyEntries.URANUS,
                    AstralBodyEntries.NEPTUNE,
                    AstralBodyEntries.PLUTO),
            0, AstralBodyEntries.SOL, new Vector3f(0, 0, 0), 0, 300, 0));

    public static final StarSystem PROXIMA_CENTAURI_SYSTEM = add(new StarSystem(Celestium.id("proxima_centauri_system"),
            List.of(AstralBodyEntries.PROXIMA_CENTAURI, AstralBodyEntries.PROXIMA_CENTAURI_A, AstralBodyEntries.PROXIMA_CENTAURI_B),
            0, AstralBodyEntries.PROXIMA_CENTAURI, new Vector3f(45f, 0, 22.5f), 175000, 300, 125000));

}
