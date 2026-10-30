package com.example.minef;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Файл настроек: config/minef-common.toml (создаётся при первом запуске).
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> BOT_NAME = BUILDER
            .comment("Ник виртуального игрока (латиница, цифры, _ ; до 16 символов).")
            .define("botName", "VoiceBridgeBot", o -> o instanceof String s && s.matches("[A-Za-z0-9_]{1,16}"));

    public static final ModConfigSpec.BooleanValue AUTO_START = BUILDER
            .comment("Запускать виртуального игрока автоматически при старте сервера.")
            .define("autoStart", false);

    public static final ModConfigSpec.IntValue STARTUP_DELAY_SECONDS = BUILDER
            .comment("Через сколько секунд после старта сервера запускать бота (если autoStart = true).")
            .defineInRange("startupDelaySeconds", 5, 0, 600);

    public static final ModConfigSpec.IntValue COMMAND_PERMISSION_LEVEL = BUILDER
            .comment("Уровень прав (0-4), с которым бот выполняет команды через /minef say /...")
            .defineInRange("commandPermissionLevel", 2, 0, 4);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> STARTUP_COMMANDS = BUILDER
            .comment("Команды, которые бот выполняет сам через ~2 секунды после запуска (без начального слэша).",
                    "Пример: [\"dvc group create discord-bridge \\\"\\\" normal true\", \"dvc start\"]")
            .defineListAllowEmpty("startupCommands", List.of(), () -> "", o -> o instanceof String);

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
