package net.swzo.create_blueprinted.config;

import net.createmod.catnip.config.ConfigBase;
import org.jetbrains.annotations.NotNull;

public class OrientationConfig extends ConfigBase {

    public final ConfigFloat defaultYaw = f(-45f, -180f, 180f, "defaultYaw",
            "Default schematic image yaw.");

    public final ConfigFloat defaultPitch = f(35.264f, -180f, 180f, "defaultPitch",
            "Default schematic image pitch.");

    public final ConfigFloat defaultRoll = f(0f, -180f, 180f, "defaultRoll",
            "Default schematic image roll.");

    @Override
    public @NotNull String getName() {
        return "orientation";
    }
}
