package dev.amblelabs.celestium.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.init.CelestiumRegistries;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.CelestialMath;
import dev.amblelabs.celestium.space.StarSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;

import java.util.Set;

public final class OrbitCommand {

    private static final double CLEARANCE = 2.0;

    private static final DynamicCommandExceptionType UNKNOWN_BODY =
            new DynamicCommandExceptionType(id -> Component.literal("Unknown astral body: " + id));
    private static final SimpleCommandExceptionType NO_SPACE =
            new SimpleCommandExceptionType(Component.literal("Space dimension is not loaded"));

    private OrbitCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("celestium")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("orbit")
                        .then(Commands.argument("body", IdentifierArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        context.getSource().registryAccess()
                                                .lookupOrThrow(CelestiumRegistries.ASTRAL_BODY)
                                                .keySet(), builder))
                                .executes(OrbitCommand::teleport))));
    }

    private static int teleport(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Identifier id = IdentifierArgument.getId(context, "body");
        Entity entity = context.getSource().getEntityOrException();

        ServerLevel space = context.getSource().getServer().getLevel(CelestiumDimensions.SPACE_KEY);
        if (space == null) throw NO_SPACE.create();

        RegistryAccess access = context.getSource().registryAccess();
        Registry<AstralBody> bodies = access.lookupOrThrow(CelestiumRegistries.ASTRAL_BODY);
        Registry<StarSystem> systems = access.lookupOrThrow(CelestiumRegistries.STAR_SYSTEM);

        double ticks = space.getGameTime();
        for (StarSystem system : systems) {
            for (Holder<AstralBody> body : system.astralBodies()) {
                if (!body.is(id)) continue;

                Vector3d pos = CelestialMath.aboveBody(bodies, system, id, ticks, entity.getBbHeight() + CLEARANCE);
                if (pos == null) continue;

                entity.teleportTo(space, pos.x, pos.y, pos.z, Set.of(), entity.getYRot(), 90.0F, false);
                context.getSource().sendSuccess(() -> Component.literal("Teleported to orbit of " + id), true);
                return 1;
            }
        }
        throw UNKNOWN_BODY.create(id);
    }
}
