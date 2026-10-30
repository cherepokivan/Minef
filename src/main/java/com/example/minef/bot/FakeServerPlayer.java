package com.example.minef.bot;

import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Виртуальный игрок. Логика tick() взята из Carpet (EntityPlayerMPFake):
 * соединение фейка не тикается сервером само, поэтому игрок тикает себя сам.
 */
public class FakeServerPlayer extends ServerPlayer {
    private final Consumer<FakeServerPlayer> onDeath;

    public FakeServerPlayer(MinecraftServer server, ServerLevel level, GameProfile profile,
            Consumer<FakeServerPlayer> onDeath) {
        super(server, level, profile, ClientInformation.createDefault());
        this.onDeath = onDeath;
    }

    @Override
    public void tick() {
        MinecraftServer server = this.getServer();
        if (server != null && server.getTickCount() % 10 == 0) {
            this.connection.resetPosition();
            this.serverLevel().getChunkSource().move(this);
        }
        try {
            super.tick();
            this.doTick();
        } catch (NullPointerException ignored) {
            // как в Carpet: не роняем сервер из-за фейк-игрока
        }
    }

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        this.setHealth(20.0F);
        this.onDeath.accept(this);
    }

    @Override
    public String getIpAddress() {
        return "127.0.0.1";
    }

    @Override
    public boolean allowsListing() {
        return false;
    }
}
