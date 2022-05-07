package net.runelite.client.plugins.oneclickfletch;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("oneclickfletch")
public interface OneClickFletchConfig extends Config {

    @ConfigItem(
            position = 0,
            keyName = "TrueOneClick",
            name = "Click anywhere on screen",
            description = "If this is active you can click anywhere to fletch"
    )
    default boolean TrueOneClick() {
        return false;
    }

    @ConfigItem(
            position = 1,
            keyName = "ConsumeClicks",
            name = "Consume clicks",
            description = "Consume clicks if waiting"
    )
    default boolean ConsumeClicks() {
        return false;
    }
}
