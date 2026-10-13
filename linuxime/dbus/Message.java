/*
 * Decompiled with CFR 0.152.
 */
package linuxime.dbus;

import java.util.ArrayList;
import java.util.List;
import linuxime.dbus.In;
import linuxime.dbus.Marshaller;

public final class Message {
    public static final int TYPE_METHOD_CALL = 1;
    public static final int TYPE_METHOD_RETURN = 2;
    public static final int TYPE_ERROR = 3;
    public static final int TYPE_SIGNAL = 4;
    public int type;
    int serial;
    public String path;
    public String iface;
    public String member;
    public String errorName;
    public String destination;
    public String sender;
    public String signature;
    public int replySerial;
    public byte[] body;
    public List<Object> args = new ArrayList<Object>();

    boolean isSignal() {
        return this.type == 4;
    }

    void parseArgs() {
        if (this.body == null || this.signature == null || this.signature.isEmpty()) {
            return;
        }
        this.args.clear();
        In in = new In(this.body, 0);
        int i = 0;
        while (i < this.signature.length()) {
            int end = Marshaller.singleTypeEnd(this.signature, i);
            this.args.add(in.read(this.signature.substring(i, end)));
            i = end;
        }
    }
}

