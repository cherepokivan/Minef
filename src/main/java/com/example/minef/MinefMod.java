package com.example.minef;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(MinefMod.MODID)
public class MinefMod {
    public static final String MODID = "minef";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinefMod(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
