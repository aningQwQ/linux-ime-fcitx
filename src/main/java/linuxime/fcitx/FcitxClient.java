/*
 * Decompiled with CFR 0.152.
 */
package linuxime.fcitx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import linuxime.ImeLog;
import linuxime.dbus.DBusConnection;
import linuxime.dbus.Message;
import linuxime.dbus.Out;

public final class FcitxClient {
    public static final String DEST = "org.freedesktop.portal.Fcitx";
    public static final String CREATE_PATH = "/org/freedesktop/portal/inputmethod";
    public static final String CREATE_IFACE = "org.fcitx.Fcitx.InputMethod1";
    public static final String IC_IFACE = "org.fcitx.Fcitx.InputContext1";
    public static final long CAPABILITY = 549772591154L;
    private DBusConnection conn;
    private String icPath;
    private Consumer<String> onCommit = s -> {};
    private BiConsumer<String, Integer> onPreedit = (s, c) -> {};
    private Runnable onFocusOut = () -> {};
    private Consumer<UiState> onUi = s -> {};

    public void setOnCommit(Consumer<String> c) {
        this.onCommit = c;
    }

    public void setOnPreedit(BiConsumer<String, Integer> p) {
        this.onPreedit = p;
    }

    public void setOnFocusOut(Runnable r) {
        this.onFocusOut = r;
    }

    public void setOnUi(Consumer<UiState> u) {
        this.onUi = u;
    }

    public boolean connect() {
        try {
            this.conn = new DBusConnection();
            this.conn.connect();
            this.createIc();
            this.conn.addSignalListener(this::dispatch);
            return this.icPath != null;
        }
        catch (Exception e) {
            ImeLog.warn("fcitx connect failed: " + String.valueOf(e));
            return false;
        }
    }

    private void createIc() throws Exception {
        Message ret = this.conn.call(DEST, CREATE_PATH, CREATE_IFACE, "CreateInputContext", "a(ss)", o -> o.arrayOfSS(new String[][]{{"program", "minecraft"}, {"name", "Minecraft"}}));
        if (ret != null && ret.type != 3 && !ret.args.isEmpty() && ret.args.get(0) instanceof String) {
            this.icPath = (String)ret.args.get(0);
            ImeLog.info("fcitx IC created at " + this.icPath);
        } else {
            ImeLog.warn("fcitx CreateInputContext failed: " + (ret == null ? "null" : ret.errorName));
        }
    }

    private void dispatch(Message msg) {
        if (this.icPath == null || !this.icPath.equals(msg.path)) {
            return;
        }
        if (!IC_IFACE.equals(msg.iface)) {
            return;
        }
        switch (msg.member) {
            case "CommitString": {
                if (msg.args.isEmpty()) break;
                this.onCommit.accept((String)msg.args.get(0));
                break;
            }
            case "UpdateFormattedPreedit": {
                String text = FcitxClient.joinSI(msg.args.isEmpty() ? null : msg.args.get(0));
                int cursor = msg.args.size() > 1 && msg.args.get(1) instanceof Integer ? ((Integer)msg.args.get(1)).intValue() : text.length();
                this.onPreedit.accept(text, cursor);
                break;
            }
            case "UpdateClientSideUI": {
                String text = FcitxClient.joinSI(msg.args.isEmpty() ? null : msg.args.get(0));
                int pc = msg.args.size() > 1 && msg.args.get(1) instanceof Integer ? ((Integer)msg.args.get(1)).intValue() : text.length();
                String auxUp = FcitxClient.joinSI(msg.args.size() > 2 ? msg.args.get(2) : null);
                String auxDown = FcitxClient.joinSI(msg.args.size() > 3 ? msg.args.get(3) : null);
                Object candidateArray = msg.args.size() > 4 ? msg.args.get(4) : null;
                List<String> labels = FcitxClient.joinSS(candidateArray, 0);
                List<String> cands = FcitxClient.joinSS(candidateArray, 1);
                int cur = msg.args.size() > 5 && msg.args.get(5) instanceof Integer ? (Integer)msg.args.get(5) : 0;
                this.onUi.accept(new UiState(text, pc, cands, labels, cur, auxUp, auxDown));
                break;
            }
            case "NotifyFocusOut": {
                this.onFocusOut.run();
                break;
            }
        }
    }

    private static String joinSI(Object aSi) {
        if (!(aSi instanceof List)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Object seg : (List)aSi) {
            if (!(seg instanceof Object[]) || ((Object[])seg).length <= 0) continue;
            sb.append(String.valueOf(((Object[])seg)[0]));
        }
        return sb.toString();
    }

    private static List<String> joinSS(Object aSs, int index) {
        ArrayList<String> out = new ArrayList<String>();
        if (!(aSs instanceof List)) {
            return out;
        }
        for (Object seg : (List)aSs) {
            if (!(seg instanceof Object[]) || ((Object[])seg).length <= index) continue;
            out.add(String.valueOf(((Object[])seg)[index]));
        }
        return out;
    }

    public boolean isReady() {
        return this.conn != null && this.icPath != null;
    }

    public void focusIn() {
        this.setCapability(549772591154L);
        this.call("FocusIn", "");
    }

    public void focusOut() {
        this.call("FocusOut", "");
    }

    public void reset() {
        this.call("Reset", "");
    }

    public void setCursorRect(int x, int y, int w, int h) {
        this.call("SetCursorRect", "iiii", o -> {
            o.i32(x);
            o.i32(y);
            o.i32(w);
            o.i32(h);
        });
    }

    public void setCapability(long cap) {
        this.call("SetCapability", "t", o -> o.u64(cap));
    }

    public void setSupportedCapability(long cap) {
        this.call("SetSupportedCapability", "t", o -> o.u64(cap));
    }

    public void selectCandidate(int index) {
        this.call("SelectCandidate", "i", o -> o.i32(index));
    }

    public void nextPage() {
        this.call("NextPage", "");
    }

    public void prevPage() {
        this.call("PrevPage", "");
    }

    public boolean processKeyEvent(int keyval, int keycode, int state, boolean release, int time) {
        try {
            Message ret = this.conn.call(DEST, this.icPath, IC_IFACE, "ProcessKeyEvent", "uuubu", o -> {
                o.u32(keyval);
                o.u32(keycode);
                o.u32(state);
                o.bool(release);
                o.u32(time);
            });
            if (ret != null && ret.type != 3 && !ret.args.isEmpty() && ret.args.get(0) instanceof Boolean) {
                return (Boolean)ret.args.get(0);
            }
        }
        catch (Exception e) {
            ImeLog.warn("ProcessKeyEvent error: " + String.valueOf(e));
        }
        return false;
    }

    private void call(String member, String sig) {
        this.call(member, sig, null);
    }

    private void call(String member, String sig, Consumer<Out> args) {
        try {
            this.conn.call(DEST, this.icPath, IC_IFACE, member, sig, args);
        }
        catch (Exception e) {
            ImeLog.warn(member + " error: " + String.valueOf(e));
        }
    }

    public static final class UiState {
        public final String preedit;
        public final int preeditCursor;
        public final List<String> candidates;
        public final List<String> labels;
        public final int cursor;
        public final String auxUp;
        public final String auxDown;

        public UiState(String preedit, int preeditCursor, List<String> candidates, List<String> labels, int cursor, String auxUp, String auxDown) {
            this.preedit = preedit;
            this.preeditCursor = preeditCursor;
            this.candidates = candidates;
            this.labels = labels;
            this.cursor = cursor;
            this.auxUp = auxUp;
            this.auxDown = auxDown;
        }
    }
}

