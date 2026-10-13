/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.client.input.PreeditEvent
 */
package linuxime;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;
import linuxime.ImeLog;
import linuxime.Keysyms;
import linuxime.fcitx.FcitxClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.PreeditEvent;

public final class FcitxIntegration {
    private static final FcitxClient FCITX = new FcitxClient();
    private static final ConcurrentLinkedQueue<String> COMMITS = new ConcurrentLinkedQueue();
    private static final AtomicReference<PreeditUpdate> PENDING_PREEDIT = new AtomicReference();
    private static EditBox activeBox;
    private static EditBox lastFocusedBox;
    private static boolean ready;
    private static volatile boolean swallowChar;
    private static volatile boolean composing;
    private static volatile FcitxClient.UiState ui;

    private FcitxIntegration() {
    }

    public static void init() {
        FCITX.setOnCommit(s -> COMMITS.add((String)s));
        FCITX.setOnPreedit((text, cursor) -> {
            composing = !text.isEmpty();
            PENDING_PREEDIT.set(new PreeditUpdate((String)text, (int)cursor));
        });
        FCITX.setOnFocusOut(() -> {
            composing = false;
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
        if (!(ready && event.isEscape() && composing)) {
            return false;
        }
        FCITX.processKeyEvent(Keysyms.keysym(event.key()), event.scancode(), Keysyms.xState(event.modifiers()), false, FcitxIntegration.now());
        composing = false;
        PENDING_PREEDIT.set(new PreeditUpdate("", 0));
        if (activeBox != null) {
            activeBox.preeditUpdated(null);
        }
        ImeLog.info("Esc: \u53d6\u6d88\u7ec4\u8bcd\u5e76\u541e\u6389\u6309\u952e");
        return true;
    }

    public static boolean onKeyPressed(EditBox self, KeyEvent e) {
        boolean handled;
        if (!ready) {
            return false;
        }
        int keyval = Keysyms.keysym(e.key());
        if (keyval == 0) {
            return false;
        }
        FcitxIntegration.activate(self);
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

    private static void activate(EditBox self) {
        if (self != lastFocusedBox) {
            if (lastFocusedBox != null) {
                lastFocusedBox.preeditUpdated(null);
            }
            FCITX.focusIn();
            FcitxIntegration.updateCursorRect(self);
            lastFocusedBox = self;
        }
        activeBox = self;
    }

    public static void onTick() {
        String c;
        if (!ready) {
            return;
        }
        boolean changed = false;
        while ((c = COMMITS.poll()) != null) {
            if (activeBox == null) continue;
            ImeLog.info("commit: \"" + c + "\"");
            activeBox.insertText(c);
            changed = true;
        }
        PreeditUpdate p = PENDING_PREEDIT.getAndSet(null);
        if (p != null && activeBox != null) {
            if (p.text.isEmpty()) {
                activeBox.preeditUpdated(null);
            } else {
                ImeLog.info("preedit: \"" + p.text + "\" cursor=" + p.cursor);
                activeBox.preeditUpdated(new PreeditEvent(p.text, p.cursor, List.of(p.text), 0));
            }
            changed = true;
        }
        if (changed) {
            FcitxIntegration.updateCursorRect(activeBox);
        }
    }

    private static void updateCursorRect(EditBox box) {
        if (box == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        double scale = mc.getWindow().getGuiScale();
        String value = box.getValue();
        int cursor = Math.min(box.getCursorPosition(), value.length());
        String prefix = value.substring(0, cursor);
        double cursorX = box.getX() + 4 + mc.font.width(prefix);
        int x = (int)Math.round(cursorX * scale);
        int y = (int)Math.round((double)box.getY() * scale);
        int w = Math.max(2, (int)Math.round(2.0 * scale));
        int h = (int)Math.round((double)box.getHeight() * scale);
        FCITX.setCursorRect(x, y, w, h);
    }

    public static void renderCandidate(GuiGraphicsExtractor g) {
        FcitxClient.UiState u = ui;
        if (u == null || u.preedit.isEmpty() && u.candidates.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        EditBox box = activeBox;
        if (box == null) {
            return;
        }
        String value = box.getValue();
        int cursorPos = Math.min(box.getCursorPosition(), value.length());
        int cx = box.getX() + 4 + mc.font.width(value.substring(0, cursorPos));
        int cy = box.getY() + box.getHeight() + 2;
        List<String> cands = u.candidates;
        List<String> labels = u.labels;
        int pad = 3;
        Objects.requireNonNull(mc.font);
        int lineH = 9 + 3;
        int w = 56;
        if (!u.preedit.isEmpty()) {
            w = Math.max(w, mc.font.width(u.preedit) + pad * 2);
        }
        for (int i = 0; i < cands.size(); ++i) {
            w = Math.max(w, mc.font.width(FcitxIntegration.candLabel(i, labels) + " " + cands.get(i)) + pad * 2);
        }
        int rows = (u.preedit.isEmpty() ? 0 : 1) + cands.size();
        int h = Math.max(1, rows) * lineH + pad * 2;
        g.fill(cx - 1, cy - 1, cx + w + 1, cy + h + 1, -16777216);
        g.fill(cx, cy, cx + w, cy + h, -266330072);
        int row = 0;
        if (!u.preedit.isEmpty()) {
            g.text(mc.font, u.preedit, cx + pad, cy + pad + row * lineH, -1);
            ++row;
        }
        for (int i = 0; i < cands.size(); ++i) {
            boolean sel;
            int iy = cy + pad + row * lineH;
            boolean bl = sel = i == u.cursor;
            if (sel) {
                g.fill(cx + 1, iy - 2, cx + w - 1, iy + lineH - 2, -2142992129);
            }
            g.text(mc.font, FcitxIntegration.candLabel(i, labels) + " " + cands.get(i), cx + pad, iy - 1, sel ? -1 : -2236963);
            ++row;
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

