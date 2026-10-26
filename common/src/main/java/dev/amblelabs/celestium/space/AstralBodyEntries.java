package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.util.ARGB;

import java.util.List;

public class AstralBodyEntries {

    // STARS
    public static final AstralBody SOL = new AstralBody(Celestium.id("sol"), true,
            ARGB.color(255, 255, 100, 10), 35, new Orbit(0.2f, 0),
            false, List.of(), 200, 200, 200);

    // MOONS
    public static final AstralBody LUNA = new AstralBody(Celestium.id("luna"), false,
            ARGB.color(255, 225, 225, 225), 24.5424f, new Orbit(0.4f, 125), true, List.of(), 10, 10, 10);

    // PLANETS
    public static final AstralBody MERCURY = new AstralBody(Celestium.id("mercury"), false,
            ARGB.color(255, 150, 10, 10), 0, new Orbit(1.0f, 300),
            false, List.of(), 40, 40, 40);

    public static final AstralBody VENUS = new AstralBody(Celestium.id("venus"), false,
            ARGB.color(255, 255, 110, 110), 177, new Orbit(0.8f, 1000),
            false, List.of(), 51, 51, 51);

    public static final AstralBody TERRA = new AstralBody(Celestium.id("terra"), false,
            ARGB.color(255, 0, 100, 255), 23, new Orbit(0.4f, 2000),
            false, List.of(AstralBodyEntries.LUNA), 50, 50, 50);

    public static final AstralBody MARS = new AstralBody(Celestium.id("mars"), false,
            ARGB.color(255, 255, 110, 110), 25, new Orbit(0.3f, 3000),
            false, List.of(), 56, 56, 56);

    public static final AstralBody JUPITER = new AstralBody(Celestium.id("jupiter"), false,
            ARGB.color(255, 200, 110, 110), 3, new Orbit(1.5f, 7000),
            false, List.of(), 300, 300, 300);
}
