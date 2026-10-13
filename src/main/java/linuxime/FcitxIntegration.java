/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.input.KeyEvent
 */
package linuxime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import linuxime.ImeLog;
import linuxime.Keysyms;
import linuxime.fcitx.FcitxClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;

public final class FcitxIntegration {
    private static final FcitxClient FCITX = new FcitxClient();
    private static final ConcurrentLinkedQueue<String> COMMITS = new ConcurrentLinkedQueue();
    private static final AtomicReference<PreeditUpdate> PENDING_PREEDIT = new AtomicReference();
    private static TextTarget active;
    private static TextTarget last;
    private static boolean ready;
    private static volatile boolean swallowChar;
    private static volatile boolean composing;
    private static volatile FcitxClient.UiState ui;
    private static volatile String clientPreedit = "";

    private FcitxIntegration() {
    }

    public interface TextTarget {
        Object id();

        String value();

        int cursor();

        void insert(String text);

        int x();

        int y();

        int height();
    }

    public static TextTarget editBox(final EditBox box) {
        return new TextTarget(){

            public Object id() {
                return box;
            }

            public String value() {
                return box.getValue();
            }

            public int cursor() {
                return box.getCursorPosition();
            }

            public void insert(String text) {
                box.insertText(text);
            }

            public int x() {
                return box.getX();
            }

            public int y() {
                return box.getY();
            }

            public int height() {
                return box.getHeight();
            }
        };
    }

    public static void init() {
        FCITX.setOnCommit(s -> COMMITS.add((String)s));
        FCITX.setOnPreedit((text, cursor) -> {
            composing = !text.isEmpty();
            clientPreedit = (String)text;
            PENDING_PREEDIT.set(new PreeditUpdate((String)text, (int)cursor));
        });
        FCITX.setOnFocusOut(() -> {
            composing = false;
            clientPreedit = "";
            PENDING_PREEDIT.set(new PreeditUpdate("", 0));
        });
        FCITX.setOnUi(u -> {
            ui = u;
            composing = !u.preedit.isEmpty() || !u.candidates.isEmpty();
        });
        ready = FCITX.connect();
        if (ready) {
            FCITX.setCapability(549772591154L);
        }
        ImeLog.info("fcitx integration ready=" + ready);
    }

    public static boolean isComposing() {
        return composing;
    }

    public static boolean onScreenEsc(KeyEvent event) {
        if (!(ready && event.isEscape() && FcitxIntegration.hasUi())) {
            return false;
        }
        FCITX.processKeyEvent(Keysyms.keysym(event.key()), event.scancode(), Keysyms.xState(event.modifiers()), false, FcitxIntegration.now());
        FcitxIntegration.clearUi();
        ImeLog.info("Esc: \u53d6\u6d88\u7ec4\u8bcd\u5e76\u541e\u6389\u6309\u952e");
        return true;
    }

    private static boolean hasUi() {
        FcitxClient.UiState u = ui;
        return composing || u != null && (!u.preedit.isEmpty() || !u.candidates.isEmpty() || !u.auxUp.isEmpty() || !u.auxDown.isEmpty());
    }

    private static void clearUi() {
        composing = false;
        clientPreedit = "";
        PENDING_PREEDIT.set(new PreeditUpdate("", 0));
        ui = null;
    }

    public static boolean onKeyPressed(TextTarget target, KeyEvent e) {
        boolean handled;
        if (!ready || e.isEscape()) {
            return false;
        }
        int keyval = Keysyms.keysym(e.key());
        if (keyval == 0) {
            return false;
        }
        FcitxIntegration.activate(target);
        int keycode = e.scancode();
        int state = Keysyms.xState(e.modifiers());
        swallowChar = handled = FCITX.processKeyEvent(keyval, keycode, state, false, FcitxIntegration.now());
        if (handled) {
            ImeLog.info("key " + e.key() + " consumed by IME");
        }
        return handled;
    }

    public static boolean onCharTyped() {
        if (swallowChar) {
            swallowChar = false;
            return true;
        }
        return false;
    }

    private static void activate(TextTarget target) {
        if (last == null || target.id() != last.id()) {
            FCITX.focusIn();
            FcitxIntegration.updateCursorRect(target);
            last = target;
        }
        active = target;
    }

    public static void onTick() {
        String c;
        if (!ready) {
            return;
        }
        boolean changed = false;
        while ((c = COMMITS.poll()) != null) {
            if (active == null) continue;
            ImeLog.info("commit: \"" + c + "\"");
            active.insert(c);
            changed = true;
        }
        PreeditUpdate p = PENDING_PREEDIT.getAndSet(null);
        if (p != null) {
            changed = true;
        }
        if (changed) {
            FcitxIntegration.updateCursorRect(active);
        }
    }

    private static void updateCursorRect(TextTarget target) {
        if (target == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        String value = target.value();
        int cursor = Math.min(target.cursor(), value.length());
        String prefix = value.substring(0, cursor);
        double cursorX = target.x() + 4 + mc.font.width(prefix);
        int x = (int)Math.round(cursorX * scale);
        int y = (int)Math.round((double)target.y() * scale);
        int w = Math.max(2, (int)Math.round(2.0 * scale));
        int h = (int)Math.round((double)target.height() * scale);
        FCITX.setCursorRect(x, y, w, h);
    }

    public static void renderCandidate(GuiGraphicsExtractor g) {
        FcitxClient.UiState u = ui;
        String preview = !clientPreedit.isEmpty() ? clientPreedit : u != null ? u.preedit : "";
        List<String> candidates = u != null ? u.candidates : List.of();
        List<String> labels = u != null ? u.labels : List.of();
        int cursor = u != null ? u.cursor : -1;
        String auxUp = u != null ? u.auxUp : "";
        String auxDown = u != null ? u.auxDown : "";
        if (preview.isEmpty() && candidates.isEmpty() && auxUp.isEmpty() && auxDown.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int pad = 3;
        int lineH = 12;
        int auxColor = -5592406;
        TextTarget target = active;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        List<String> texts = new ArrayList<String>();
        List<Integer> colors = new ArrayList<Integer>();
        List<Integer> candIdx = new ArrayList<Integer>();
        if (!auxUp.isEmpty()) {
            texts.add(auxUp);
            colors.add(auxColor);
            candIdx.add(-1);
        }
        if (!preview.isEmpty()) {
            texts.add(preview);
            colors.add(-1);
            candIdx.add(-1);
        }
        for (int i = 0; i < candidates.size(); ++i) {
            texts.add(FcitxIntegration.candLabel(i, labels) + " " + candidates.get(i));
            colors.add(i == cursor ? -1 : -2236963);
            candIdx.add(i);
        }
        if (!auxDown.isEmpty()) {
            texts.add(auxDown);
            colors.add(auxColor);
            candIdx.add(-1);
        }
        if (texts.isEmpty()) {
            return;
        }
        int w = 56;
        for (String s : texts) {
            w = Math.max(w, mc.font.width(s) + pad * 2);
        }
        int h = texts.size() * lineH + pad * 2;
        int cx;
        int cy;
        if (target != null) {
            String value = target.value();
            int cursorPos = Math.min(target.cursor(), value.length());
            cx = target.x() + 4 + mc.font.width(value.substring(0, cursorPos));
            int below = target.y() + target.height() + 2;
            cy = below + h + 2 <= screenH ? below : target.y() - h - 2;
        } else {
            cx = (screenW - w) / 2;
            cy = screenH - h - 40;
        }
        if (cx + w + 2 > screenW) {
            cx = screenW - w - 2;
        }
        if (cx < 2) {
            cx = 2;
        }
        if (cy + h + 2 > screenH) {
            cy = screenH - h - 2;
        }
        if (cy < 2) {
            cy = 2;
        }
        g.fill(cx - 1, cy - 1, cx + w + 1, cy + h + 1, -16777216);
        g.fill(cx, cy, cx + w, cy + h, -266330072);
        for (int r = 0; r < texts.size(); ++r) {
            int iy = cy + pad + r * lineH;
            int ci = candIdx.get(r);
            if (ci >= 0 && ci == cursor) {
                g.fill(cx + 1, iy - 2, cx + w - 1, iy + lineH - 2, -2142992129);
            }
            g.text(mc.font, texts.get(r), cx + pad, iy - 1, colors.get(r));
        }
    }

    private static String candLabel(int i, List<String> labels) {
        return i < labels.size() ? labels.get(i) : i + 1 + ".";
    }

    private static int now() {
        return (int)(System.currentTimeMillis() & Integer.MAX_VALUE);
    }

    private static final class PreeditUpdate {
        final String text;
        final int cursor;

        PreeditUpdate(String text, int cursor) {
            this.text = text;
            this.cursor = cursor;
        }
    }
}
