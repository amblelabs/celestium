package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AstralBodyEntries {

    private static final float AU = 50000.0F;
    private static final float YEAR = 360.0F / 1200.0F;
    private static final float DAY = 360.0F / 60.0F;

    // STARS

    // Solar System
    public static final AstralBody SOL = add(new AstralBody(Celestium.id("sol"), true,
            ARGB.color(255, 255, 100, 10), 7.25F, DAY / 25.38F, new Orbit(0, 0, 0),
            null, false, List.of(), 13927, 13927, 13927));

    // Proxima Centauri System
    public static final AstralBody PROXIMA_CENTAURI = add(new AstralBody(Celestium.id("proxima_centauri"), true,
            ARGB.color(255, 255, 0, 0), 0, DAY / 83.0F, new Orbit(0, 0, 45),
            null, false, List.of(), 2148 / 2, 2148 / 2, 2148 / 2));

    // MOONS

    // Solar System
    public static final AstralBody LUNA = add(new AstralBody(Celestium.id("luna"), false,
            ARGB.color(255, 225, 225, 225), 6.68F, 0, new Orbit(YEAR / 0.0748F - YEAR, 3844, 112),
            null, true, List.of(), 35, 35, 35));

    // PLANETS

    // Solar System
    public static final AstralBody MERCURY = add(new AstralBody(Celestium.id("mercury"), false,
            ARGB.color(255, 150, 10, 10), 0.03F, DAY / 58.65F, new Orbit(YEAR / 0.2408F, 0.387F * AU, 20),
            null, false, List.of(), 49, 49, 49));

    public static final AstralBody VENUS = add(new AstralBody(Celestium.id("venus"), false,
            ARGB.color(255, 255, 110, 110), 177.4F, DAY / 243.02F, new Orbit(YEAR / 0.6152F, 0.723F * AU, 90),
            new Atmosphere(ARGB.color(255, 110, 110), 3), false, List.of(), 121, 121, 121));

    public static final AstralBody TERRA = add(new AstralBody(Celestium.id("terra"), false,
            ARGB.color(255, 0, 170, 255), 23.44F, DAY / 0.9973F, new Orbit(YEAR, AU, 120),
            new Atmosphere(ARGB.color(255, 0, 170, 255), 3), false, List.of(AstralBodyEntries.LUNA), 127, 127, 127));

    public static final AstralBody MARS = add(new AstralBody(Celestium.id("mars"), false,
            ARGB.color(255, 255, 110, 110), 25.19F, DAY / 1.026F, new Orbit(YEAR / 1.8809F, 1.524F * AU, 320),
            new Atmosphere(ARGB.color(255, 255, 100, 10), 1), false, List.of(), 68, 68, 68));

    public static final AstralBody JUPITER = add(new AstralBody(Celestium.id("jupiter"), false,
            ARGB.color(255, 200, 110, 110), 3.13F, DAY / 0.4135F, new Orbit(YEAR / 11.862F, 5.203F * AU, 20),
            new Atmosphere(ARGB.color(255, 255, 100, 10), 4), false, List.of(), 1398, 1398, 1398));

    public static final AstralBody SATURN = add(new AstralBody(Celestium.id("saturn"), false,
            ARGB.color(255, 200, 150, 90), 26.73F, DAY / 0.444F, new Orbit(YEAR / 29.457F, 9.537F * AU, 170),
            new Atmosphere(ARGB.color(255, 200, 150, 90), 2), false, List.of(), 1165, 1165, 1165));

    public static final AstralBody URANUS = add(new AstralBody(Celestium.id("uranus"), false,
            ARGB.color(255, 170, 170, 190), 97.77F, DAY / 0.718F, new Orbit(YEAR / 84.011F, 19.19F * AU, 145),
            new Atmosphere(ARGB.color(255, 170, 170, 190), 2), false, List.of(), 507, 507, 507));

    public static final AstralBody NEPTUNE = add(new AstralBody(Celestium.id("neptune"), false,
            ARGB.color(255, 10, 20, 240), 28.32F, DAY / 0.671F, new Orbit(YEAR / 164.79F, 30.07F * AU, 75),
            new Atmosphere(ARGB.color(255, 120, 170, 190), 3), false, List.of(), 492, 492, 492));

    public static final AstralBody PLUTO = add(new AstralBody(Celestium.id("pluto"), false,
            ARGB.color(255, 190, 20, 225), 122.53F, DAY / 6.387F, new Orbit(YEAR / 247.94F, 39.48F * AU, 12),
            new Atmosphere(ARGB.color(255, 190, 20, 225), 1), false, List.of(), 24, 24, 24));

    // Proxima Centauri System
    public static final AstralBody PROXIMA_CENTAURI_A = add(new AstralBody(Celestium.id("proxima_centauri_a"), false,
            ARGB.color(255, 255, 100, 255), 0, 0, new Orbit(YEAR / 0.03064F, 0.0485F * AU, 12),
            new Atmosphere(ARGB.color(255, 255, 100, 255), 1), false, List.of(), 136, 136, 136));

    public static final AstralBody PROXIMA_CENTAURI_B = add(new AstralBody(Celestium.id("proxima_centauri_b"), false,
            ARGB.color(255, 100, 100, 255), 0, DAY, new Orbit(YEAR / 5.279F, 1.49F * AU, 60),
            new Atmosphere(ARGB.color(255, 100, 100, 255), 1), false, List.of(), 191, 191, 191));
}