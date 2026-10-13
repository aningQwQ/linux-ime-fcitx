/*
 * Decompiled with CFR 0.152.
 */
package linuxime;

public final class ImeLog {
    private static final Object SLF4J;
    private static final Object SLF4J_LOGGER;
    private static final boolean HAS_SLF4J;

    private ImeLog() {
    }

    public static void info(String msg) {
        ImeLog.emit("INFO ", msg);
    }

    public static void warn(String msg) {
        ImeLog.emit("WARN ", msg);
    }

    private static void emit(String level, String msg) {
        System.out.println("[linux-ime-fcitx] " + msg);
        if (HAS_SLF4J && SLF4J_LOGGER != null) {
            try {
                if ("WARN ".equals(level)) {
                    SLF4J_LOGGER.getClass().getMethod("warn", String.class).invoke(SLF4J_LOGGER, "[linux-ime-fcitx] " + msg);
                } else {
                    SLF4J_LOGGER.getClass().getMethod("info", String.class).invoke(SLF4J_LOGGER, "[linux-ime-fcitx] " + msg);
                }
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
    }

    static {
        Object lg = null;
        Object logger = null;
        boolean ok = false;
        try {
            Class<?> factory = Class.forName("org.slf4j.LoggerFactory");
            lg = logger = factory.getMethod("getLogger", String.class).invoke(null, "linux-ime-fcitx");
            ok = true;
        }
        catch (Throwable t) {
            lg = null;
            logger = null;
            ok = false;
        }
        SLF4J = lg;
        SLF4J_LOGGER = logger;
        HAS_SLF4J = ok;
    }
}

