package com.example.minef.bot;

import javax.annotation.Nullable;

import com.example.minef.Config;
import com.example.minef.MinefMod;
import com.mojang.authlib.GameProfile;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.level.GameType;

public final class MinefBot {
    /** Сколько тиков после команды /minef say ответы бота пересылаются выдавшему команду (30 сек). */
    private static final int FORWARD_WINDOW_TICKS = 600;

    private static FakeServerPlayer bot;
    @Nullable
    private static CommandSourceStack lastIssuer;
    private static long forwardUntilTick = -1;

    public record Result(boolean ok, Component message) {
        static Result success(String text) {
            return new Result(true, Component.literal("[Minef] " + text));
        }

        static Result failure(String text) {
            return new Result(false, Component.literal("[Minef] " + text));
        }
    }

    private MinefBot() {
    }

    public static synchronized boolean isRunning(MinecraftServer server) {
        return bot != null && server.getPlayerList().getPlayer(bot.getUUID()) == bot;
    }

    public static synchronized Result start(MinecraftServer server) {
        if (isRunning(server)) {
            return Result.failure("Виртуальный игрок уже запущен.");
        }
        String name = Config.BOT_NAME.get();
        if (server.getPlayerList().getPlayerByName(name) != null) {
            return Result.failure("На сервере уже есть игрок с ником " + name + ".");
        }

        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        ServerLevel level = server.overworld();

        FakeServerPlayer player = new FakeServerPlayer(server, level, profile, p -> server.execute(() -> remove(server, p)));
        FakeConnection connection = new FakeConnection(PacketFlow.SERVERBOUND, packet -> onPacket(server, player, packet));

        server.getPlayerList().placeNewPlayer(connection, player,
                new CommonListenerCookie(profile, 0, player.clientInformation(), false));
        player.gameMode.changeGameModeForPlayer(GameType.SPECTATOR);

        bot = player;
        MinefMod.LOGGER.info("[Minef] Виртуальный игрок {} запущен.", name);
        return Result.success("Виртуальный игрок " + name + " запущен (режим наблюдателя).");
    }

    public static synchronized Result stop(MinecraftServer server) {
        if (bot == null) {
            return Result.failure("Виртуальный игрок не запущен.");
        }
        FakeServerPlayer player = bot;
        remove(server, player);
        return Result.success("Виртуальный игрок отключён.");
    }

    public static synchronized Result status(MinecraftServer server) {
        return isRunning(server)
                ? Result.success("Виртуальный игрок " + bot.getGameProfile().getName() + " онлайн.")
                : Result.success("Виртуальный игрок не запущен.");
    }

    /**
     * Текст с "/" выполняется как команда от имени бота, без "/" - уходит в общий чат.
     * issuer может быть null (например, стартовые команды из конфига).
     */
    public static synchronized Result say(MinecraftServer server, @Nullable CommandSourceStack issuer, String text) {
        if (!isRunning(server)) {
            return Result.failure("Виртуальный игрок не запущен. Сначала /minef start");
        }
        lastIssuer = issuer;
        forwardUntilTick = server.getTickCount() + FORWARD_WINDOW_TICKS;

        if (text.startsWith("/")) {
            CommandSourceStack source = bot.createCommandSourceStack().withPermission(Config.COMMAND_PERMISSION_LEVEL.get());
            server.getCommands().performPrefixedCommand(source, text);
            return Result.success("Команда выполнена от имени бота: " + text);
        }

        server.getPlayerList().broadcastChatMessage(PlayerChatMessage.system(text), bot, ChatType.bind(ChatType.CHAT, bot));
        return Result.success("Сообщение отправлено от имени бота.");
    }

    /** Убирает бота с сервера (как обычное отключение игрока: сохраняет данные). */
    private static synchronized void remove(MinecraftServer server, FakeServerPlayer player) {
        if (bot == player) {
            bot = null;
        }
        if (server.getPlayerList().getPlayer(player.getUUID()) == player) {
            player.connection.onDisconnect(new DisconnectionDetails(Component.literal("Minef: бот остановлен")));
        }
    }

    /** Все пакеты, которые сервер "отправляет" боту. */
    private static void onPacket(MinecraftServer server, FakeServerPlayer player, Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket system) {
            onBotMessage(server, system.content());
        } else if (packet instanceof ClientboundDisconnectPacket) {
            // кик (/kick, бан и т.п.): без реального сокета сами убираем бота
            server.execute(() -> remove(server, player));
        }
    }

    private static synchronized void onBotMessage(MinecraftServer server, Component content) {
        if (server.getTickCount() > forwardUntilTick) {
            return;
        }
        MinefMod.LOGGER.info("[Minef] бот получил: {}", content.getString());
        if (lastIssuer != null) {
            lastIssuer.sendSystemMessage(Component.literal("[бот] ").append(content));
        }
    }
}
