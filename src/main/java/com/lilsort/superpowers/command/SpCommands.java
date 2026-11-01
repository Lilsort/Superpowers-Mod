package com.lilsort.superpowers.command;

import com.lilsort.superpowers.laser.LaserPlayer;
import com.lilsort.superpowers.flight.FlightPlayer;
import com.lilsort.superpowers.grab.GrabPlayer;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registers the /sp command tree.
 * Only operators (permission level 2+) can use these commands.
 */
public class SpCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(SpCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                          CommandBuildContext registryAccess,
                                          Commands.CommandSelection environment) {
        dispatcher.register(Commands.literal("sp")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("laser")
                        // /sp laser on
                        .then(Commands.literal("on")
                                .executes(ctx -> setLaser(ctx, true))
                        )
                        // /sp laser off
                        .then(Commands.literal("off")
                                .executes(ctx -> setLaser(ctx, false))
                        )
                        // /sp laser offset <val>
                        .then(Commands.literal("offset")
                                .then(Commands.argument("val", FloatArgumentType.floatArg())
                                        .suggests((ctx, builder) -> builder.suggest("-3.5", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setLaserOffset(ctx, FloatArgumentType.getFloat(ctx, "val")))
                                )
                        )
                        // /sp laser gap <val>
                        .then(Commands.literal("gap")
                                .then(Commands.argument("val", FloatArgumentType.floatArg())
                                        .suggests((ctx, builder) -> builder.suggest("1.5", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setLaserGap(ctx, FloatArgumentType.getFloat(ctx, "val")))
                                )
                        )
                        // /sp laser delay <val>
                        .then(Commands.literal("delay")
                                .then(Commands.argument("val", FloatArgumentType.floatArg())
                                        .suggests((ctx, builder) -> builder.suggest("0.0", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setLaserDelay(ctx, FloatArgumentType.getFloat(ctx, "val")))
                                )
                        )
                        // /sp laser distance <val>
                        .then(Commands.literal("distance")
                                .then(Commands.argument("val", FloatArgumentType.floatArg(0.0f, 128.0f))
                                        .suggests((ctx, builder) -> builder.suggest("64.0", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setLaserDistance(ctx, FloatArgumentType.getFloat(ctx, "val")))
                                )
                        )
                        // /sp laser damage <val>
                        .then(Commands.literal("damage")
                                .then(Commands.argument("val", FloatArgumentType.floatArg(0.0f, 1024.0f))
                                        .suggests((ctx, builder) -> builder.suggest("10.0", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setLaserDamage(ctx, FloatArgumentType.getFloat(ctx, "val")))
                                )
                        )
                        // /sp laser color <hex>
                        .then(Commands.literal("color")
                                .then(Commands.argument("hex", StringArgumentType.word())
                                        .suggests((ctx, builder) -> builder.suggest("FF0000", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setLaserColor(ctx, StringArgumentType.getString(ctx, "hex")))
                                )
                        )
                )
                .then(Commands.literal("fly")
                        // /sp fly on
                        .then(Commands.literal("on")
                                .executes(ctx -> setFlight(ctx, true))
                        )
                        // /sp fly off
                        .then(Commands.literal("off")
                                .executes(ctx -> setFlight(ctx, false))
                        )
                        // /sp fly speed <val>
                        .then(Commands.literal("speed")
                                .then(Commands.argument("val", FloatArgumentType.floatArg(0.1f, 4.0f))
                                        .suggests((ctx, builder) -> builder.suggest("1.0", Component.literal("По умолчанию")).buildFuture())
                                        .executes(ctx -> setFlightSpeed(ctx, FloatArgumentType.getFloat(ctx, "val")))
                                )
                        )
                )
                .then(Commands.literal("grab")
                        // /sp grab on
                        .then(Commands.literal("on")
                                .executes(ctx -> setGrab(ctx, true))
                        )
                        // /sp grab off
                        .then(Commands.literal("off")
                                .executes(ctx -> setGrab(ctx, false))
                        )
                )
        );
    }

    private static int setLaser(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();

        if (target instanceof LaserPlayer laserPlayer) {
            laserPlayer.setLaserEnabled(enabled);

            // If disabling, also stop firing
            if (!enabled) {
                laserPlayer.setFiringLaser(false);
            }

            if (enabled) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§aЛазерные глаза включены!"), true);
            } else {
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§cЛазерные глаза выключены!"), true);
            }
            return 1;
        }

        return 0;
    }

    private static int setFlight(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();

        if (target instanceof FlightPlayer flightPlayer) {
            flightPlayer.setSuperFlightEnabled(enabled);

            if (enabled) {
                target.getAbilities().mayfly = true;
                target.getAbilities().flying = true;
                target.onUpdateAbilities();
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§aПолет включен!"), true);
            } else {
                if (!target.isCreative() && !target.isSpectator()) {
                    target.getAbilities().mayfly = false;
                    target.getAbilities().flying = false;
                }
                target.onUpdateAbilities();
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§cПолет выключен!"), true);
            }
            return 1;
        }
        return 0;
    }

    private static int setGrab(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();

        if (target instanceof GrabPlayer grabPlayer) {
            grabPlayer.setCanGrab(enabled);
            if (!enabled) {
                grabPlayer.releaseEntity();
            }

            if (enabled) {
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§aЗахват существ включен!"), true);
            } else {
                ctx.getSource().sendSuccess(() ->
                        Component.literal("§cЗахват существ выключен!"), true);
            }
            return 1;
        }

        return 0;
    }

    private static int setFlightSpeed(CommandContext<CommandSourceStack> ctx, float speed) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof FlightPlayer flightPlayer) {
            flightPlayer.setFlightSpeedMultiplier(speed);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§aМножитель скорости полёта установлен на %.2f", speed)), true);
            return 1;
        }
        return 0;
    }

    private static int setLaserOffset(CommandContext<CommandSourceStack> ctx, float offset) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof LaserPlayer laserPlayer) {
            laserPlayer.setLaserOffset(offset);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§aСмещение лазера установлено на %.2f", offset)), true);
            return 1;
        }
        return 0;
    }

    private static int setLaserGap(CommandContext<CommandSourceStack> ctx, float gap) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof LaserPlayer laserPlayer) {
            laserPlayer.setLaserGap(gap);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§aРасстояние между лазерами установлено на %.2f", gap)), true);
            return 1;
        }
        return 0;
    }

    private static int setLaserDelay(CommandContext<CommandSourceStack> ctx, float delay) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof LaserPlayer laserPlayer) {
            laserPlayer.setLaserDelay(delay);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§aЗадержка лазера установлена на %.2f секунд", delay)), true);
            return 1;
        }
        return 0;
    }

    private static int setLaserDistance(CommandContext<CommandSourceStack> ctx, float distance) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof LaserPlayer laserPlayer) {
            laserPlayer.setLaserDistance(distance);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§aДистанция лазера установлена на %.2f блоков", distance)), true);
            return 1;
        }
        return 0;
    }

    private static int setLaserDamage(CommandContext<CommandSourceStack> ctx, float damage) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof LaserPlayer laserPlayer) {
            laserPlayer.setLaserDamage(damage);
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§aУрон лазера установлен на %.2f", damage)), true);
            return 1;
        }
        return 0;
    }

    private static int setLaserColor(CommandContext<CommandSourceStack> ctx, String hex) throws CommandSyntaxException {
        ServerPlayer target = ctx.getSource().getPlayerOrException();
        if (target instanceof LaserPlayer laserPlayer) {
            // Strip any leading # just in case user typed it
            if (hex.startsWith("#")) {
                hex = hex.substring(1);
            }
            laserPlayer.setLaserColor(hex);
            String finalHex = hex;
            ctx.getSource().sendSuccess(() ->
                    Component.literal(String.format("§fЦвет лазера установлен на: §e%s", finalHex)), true);
            return 1;
        }
        return 0;
    }
}
