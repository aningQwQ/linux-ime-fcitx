/*
 * Decompiled with CFR 0.152.
 */
package linuxime.fcitx;

import linuxime.fcitx.FcitxClient;

public final class SmokeTest {
    public static void main(String[] args) throws Exception {
        FcitxClient c = new FcitxClient();
        StringBuilder got = new StringBuilder();
        c.setOnCommit(s -> {
            System.out.println("SIGNAL CommitString: \"" + s + "\"");
            got.append("commit");
        });
        c.setOnPreedit((t, cur) -> {
            System.out.println("SIGNAL UpdatePreedit: \"" + t + "\" cursor=" + cur);
            got.append("preedit");
        });
        boolean ok = c.connect();
        System.out.println("connect ok=" + ok + " ready=" + c.isReady());
        if (!ok) {
            System.out.println("SMOKE FAIL: could not connect");
            System.exit(1);
        }
        c.focusIn();
        System.out.println("focusIn sent");
        boolean handled = c.processKeyEvent(97, 38, 0, false, 0);
        System.out.println("processKeyEvent('a') -> handled=" + handled);
        Thread.sleep(1500L);
        System.out.println("signals seen: " + (got.length() == 0 ? "<none>" : got.toString()));
        System.out.println("SMOKE DONE");
        System.exit(0);
    }
}

