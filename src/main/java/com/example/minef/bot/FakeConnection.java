package com.example.minef.bot;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;

/**
 * Соединение-пустышка для виртуального игрока (подход как у Carpet):
 * реального сокета нет, все исходящие пакеты уходят в {@code sink},
 * а не копятся в очереди.
 */
public class FakeConnection extends Connection {
    private final Consumer<Packet<?>> sink;

    public FakeConnection(PacketFlow flow, Consumer<Packet<?>> sink) {
        super(flow);
        this.sink = sink;
        // Поле channel открыто через accesstransformer.cfg.
        // Заглушка нужна, т.к. часть кода (в т.ч. NeoForge) обращается к channel.
        this.channel = new EmbeddedChannel();
    }

    // Три перегрузки send без @Override намеренно: если у какой-то из них
    // в этой версии другая сигнатура, сборка не упадёт.
    public void send(Packet<?> packet) {
        sink.accept(packet);
    }

    public void send(Packet<?> packet, @Nullable PacketSendListener listener) {
        sink.accept(packet);
    }

    public void send(Packet<?> packet, @Nullable PacketSendListener listener, boolean flush) {
        sink.accept(packet);
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void setReadOnly() {
    }

    @Override
    public void handleDisconnection() {
    }

    @Override
    public void setListenerForServerboundHandshake(PacketListener packetListener) {
    }

    @Override
    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> protocolInfo, T packetListener) {
    }
}
