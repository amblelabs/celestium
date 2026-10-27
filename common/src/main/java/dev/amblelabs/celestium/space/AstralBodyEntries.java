package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.Celestium;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AstralBodyEntries {

    public static ConcurrentHashMap<Identifier, AstralBody> ENTRIES = new ConcurrentHashMap<>();

    private static final int MEDIAN_DISTANCE = 750;

    // STARS
    public static final AstralBody SOL = add(new AstralBody(Celestium.id("sol"), true,
            ARGB.color(255, 255, 100, 10), 35, new Orbit(0.2f, 0),
            null, false, List.of(), 500, 500, 500));

    // MOONS
    public static final AstralBody LUNA = add(new AstralBody(Celestium.id("luna"), false,
            ARGB.color(255, 225, 225, 225), 24.5424f, new Orbit(0.4f, 125),
            null, true, List.of(), 10, 10, 10));

    // PLANETS
    public static final AstralBody MERCURY = add(new AstralBody(Celestium.id("mercury"), false,
            ARGB.color(255, 150, 10, 10), 0, new Orbit(1.0f, MEDIAN_DISTANCE),
            null, false, List.of(), 40, 40, 40));

    public static final AstralBody VENUS = add(new AstralBody(Celestium.id("venus"), false,
            ARGB.color(255, 255, 110, 110), 177, new Orbit(0.8f, MEDIAN_DISTANCE + 500),
            new Atmosphere(ARGB.color(255, 110, 110), 3), false, List.of(), 51, 51, 51));

    public static final AstralBody TERRA = add(new AstralBody(Celestium.id("terra"), false,
            ARGB.color(255, 0, 170, 255), 23, new Orbit(0.4f, MEDIAN_DISTANCE + 1500),
            new Atmosphere(ARGB.color(255, 0, 170, 255), 3), false, List.of(AstralBodyEntries.LUNA), 50, 50, 50));

    public static final AstralBody MARS = add(new AstralBody(Celestium.id("mars"), false,
            ARGB.color(255, 255, 110, 110), 25, new Orbit(0.3f, MEDIAN_DISTANCE + 2500),
            new Atmosphere(ARGB.color(255, 255, 100, 10), 1), false, List.of(), 56, 56, 56));

    public static final AstralBody JUPITER = add(new AstralBody(Celestium.id("jupiter"), false,
            ARGB.color(255, 200, 110, 110), 3, new Orbit(1.5f, MEDIAN_DISTANCE + 6500),
            new Atmosphere(ARGB.color(255, 255, 100, 10), 4), false, List.of(), 300, 300, 300));

    public static final AstralBody SATURN = add(new AstralBody(Celestium.id("saturn"), false,
            ARGB.color(255, 200, 150, 90), 27, new Orbit(2f, MEDIAN_DISTANCE + 11500),
            new Atmosphere(ARGB.color(255, 200, 150, 90), 2), false, List.of(), 250, 250, 250));

    public static final AstralBody URANUS = add(new AstralBody(Celestium.id("uranus"), false,
            ARGB.color(255, 170, 170, 190), 98, new Orbit(4f, MEDIAN_DISTANCE + 17500),
            new Atmosphere(ARGB.color(255, 170, 170, 190), 2),false, List.of(), 220, 220, 220));

    public static final AstralBody NEPTUNE = add(new AstralBody(Celestium.id("neptune"), false,
            ARGB.color(255, 10, 20, 240), 28, new Orbit(5f, MEDIAN_DISTANCE + 24500),
            new Atmosphere(ARGB.color(255, 120, 170, 190), 3),false, List.of(), 220, 220, 220));

    public static final AstralBody PLUTO = add(new AstralBody(Celestium.id("pluto"), false,
            ARGB.color(255, 190, 20, 225), 27, new Orbit(2f, MEDIAN_DISTANCE + 35500),
            new Atmosphere(ARGB.color(255, 190, 20, 225), 1),false, List.of(), 20, 20, 20));

    private static AstralBody add(AstralBody body) {
        ENTRIES.put(body.id(), body);
        return body;
    }
}
