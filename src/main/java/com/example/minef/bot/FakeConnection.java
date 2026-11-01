package com.example.minef.bot;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.ChannelAttributes;

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
        // КЛЮЧЕВОЕ: NeoForge перед каждой отправкой кастомного пакета вызывает
        // NetworkRegistry.checkPacket() и бросает UnsupportedOperationException, если у клиента
        // нет нужного канала. Моды (Sable и др.) шлют свои пакеты при входе игрока без проверки,
        // поэтому говорим NeoForge, что у бота "есть все каналы" - пакеты просто уйдут в никуда.
        this.channel.attr(ChannelAttributes.ADHOC_CHANNELS).set(new AcceptAllChannels());
    }

    /** Набор каналов, который "содержит" любой канал. */
    private static final class AcceptAllChannels extends AbstractSet<ResourceLocation> {
        @Override
        public boolean contains(Object o) {
            return true;
        }

        @Override
        public boolean add(ResourceLocation location) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends ResourceLocation> c) {
            return false;
        }

        @Override
        public Iterator<ResourceLocation> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
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
