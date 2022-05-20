package com.matyrobbrt.adminmode;

import net.minecraftforge.common.ForgeConfigSpec;

public class ServerConfig {

    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENTER_ON_GAMEMODE_CHANGE;

    static {
        final var builder = new ForgeConfigSpec.Builder();

        ENTER_ON_GAMEMODE_CHANGE = builder.comment("If Admin Mode should be entered on gamemode change from creative -> survival automatically.")
                .define("enter_on_gamemode_change", true);

        SPEC = builder.build();
    }

}
