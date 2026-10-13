/*
 * Decompiled with CFR 0.152.
 */
package linuxime.dbus;

import java.util.function.Consumer;
import linuxime.dbus.DBusConnection;
import linuxime.dbus.Message;
import linuxime.dbus.Out;

public final class Probe {
    public static void main(String[] args) throws Exception {
        DBusConnection c = new DBusConnection();
        c.connect();
        Probe.probe(c, "org.freedesktop.portal.Fcitx", "/org/freedesktop/portal/inputmethod", "org.fcitx.Fcitx.InputMethod1", "CreateInputContext", "a(ss)", o -> o.arrayOfSS(new String[][]{{"program", "linux-ime-probe"}}));
        Probe.probe(c, "org.freedesktop.IBus", "/org/freedesktop/IBus", "org.freedesktop.IBus", "CreateInputContext", "s", o -> o.string("linux-ime-probe"));
        System.out.println("PROBE DONE");
        System.exit(0);
    }

    private static void probe(DBusConnection c, String dest, String path, String iface, String member, String sig, Consumer<Out> args) {
        try {
            Message ret = c.call(dest, path, iface, member, sig, args);
            System.out.println("== " + dest + "/" + member + " reply type=" + String.valueOf(ret == null ? "null" : Integer.valueOf(ret.type)));
            if (ret == null) {
                return;
            }
            if (ret.type == 3) {
                System.out.println("   ERROR " + ret.errorName);
                return;
            }
            System.out.println("   reply signature=" + ret.signature + " args=" + String.valueOf(ret.args));
            if (ret.args.isEmpty()) {
                return;
            }
            Object first = ret.args.get(0);
            if (!(first instanceof String)) {
                return;
            }
            String icPath = (String)first;
            System.out.println("   IC path = " + icPath);
            Message intro = c.call(dest, icPath, "org.freedesktop.DBus.Introspectable", "Introspect", "", null);
            if (intro != null && !intro.args.isEmpty()) {
                System.out.println("   ---- IC introspection XML ----");
                System.out.println("   " + String.valueOf(intro.args.get(0)));
                System.out.println("   ---- end XML ----");
            } else {
                System.out.println("   (no introspection)");
            }
        }
        catch (Exception e) {
            System.out.println("   probe exception: " + String.valueOf(e));
        }
    }
}

