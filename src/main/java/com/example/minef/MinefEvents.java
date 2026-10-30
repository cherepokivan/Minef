package com.example.minef;

import com.example.minef.bot.MinefBot;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = MinefMod.MODID)
public class MinefEvents {
    private static long startAtTick = -1;
    private static long commandsAtTick = -1;

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        startAtTick = -1;
        commandsAtTick = -1;
        if (Config.AUTO_START.get()) {
            startAtTick = event.getServer().getTickCount() + Config.STARTUP_DELAY_SECONDS.get() * 20L;
            MinefMod.LOGGER.info("[Minef] Автозапуск бота через {} сек.", Config.STARTUP_DELAY_SECONDS.get());
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        long now = server.getTickCount();

        if (startAtTick >= 0 && now >= startAtTick) {
            startAtTick = -1;
            MinefBot.Result result = MinefBot.start(server);
            MinefMod.LOGGER.info("[Minef] {}", result.message().getString());
            if (result.ok()) {
                // даём боту пару секунд полностью зайти, потом выполняем стартовые команды
                commandsAtTick = now + 40;
            }
        }

        if (commandsAtTick >= 0 && now >= commandsAtTick) {
            commandsAtTick = -1;
            for (String command : Config.STARTUP_COMMANDS.get()) {
                String text = command.startsWith("/") ? command : "/" + command;
                MinefBot.Result result = MinefBot.say(server, null, text);
                MinefMod.LOGGER.info("[Minef] стартовая команда '{}': {}", text, result.message().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // убираем бота заранее, чтобы его данные корректно сохранились при остановке
        MinefBot.stop(event.getServer());
    }
}
