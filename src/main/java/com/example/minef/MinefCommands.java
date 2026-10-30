package com.example.minef;

import com.example.minef.bot.MinefBot;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MinefMod.MODID)
public class MinefCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("minef")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("start")
                        .executes(ctx -> reply(ctx.getSource(), MinefBot.start(ctx.getSource().getServer()))))
                .then(Commands.literal("stop")
                        .executes(ctx -> reply(ctx.getSource(), MinefBot.stop(ctx.getSource().getServer()))))
                .then(Commands.literal("status")
                        .executes(ctx -> reply(ctx.getSource(), MinefBot.status(ctx.getSource().getServer()))))
                .then(Commands.literal("say")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(ctx -> reply(ctx.getSource(), MinefBot.say(
                                        ctx.getSource().getServer(),
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "text")))))));
    }

    private static int reply(CommandSourceStack source, MinefBot.Result result) {
        if (result.ok()) {
            source.sendSuccess(result::message, true);
            return 1;
        }
        source.sendFailure(result.message());
        return 0;
    }
}
