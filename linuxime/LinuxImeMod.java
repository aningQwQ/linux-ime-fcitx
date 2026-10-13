/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 */
package linuxime;

import linuxime.FcitxIntegration;
import linuxime.ImeLog;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class LinuxImeMod
implements ClientModInitializer {
    public static final String MOD_ID = "linux-ime-fcitx";

    public void onInitializeClient() {
        ImeLog.info("Linux IME (fcitx5 D-Bus) loading");
        LinuxImeMod.logEnv("XMODIFIERS");
        LinuxImeMod.logEnv("XDG_SESSION_TYPE");
        LinuxImeMod.logEnv("WAYLAND_DISPLAY");
        LinuxImeMod.logEnv("DISPLAY");
        FcitxIntegration.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> FcitxIntegration.onTick());
    }

    private static void logEnv(String key) {
        String v = System.getenv(key);
        ImeLog.info("env " + key + " = " + (v == null ? "<unset>" : v));
    }
}

