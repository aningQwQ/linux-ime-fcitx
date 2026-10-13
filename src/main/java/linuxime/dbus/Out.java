/*
 * Decompiled with CFR 0.152.
 */
package linuxime.dbus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public final class Out {
    private ByteBuffer buf = ByteBuffer.allocate(512);

    Out() {
        this.buf.order(ByteOrder.LITTLE_ENDIAN);
    }

    private void ensure(int extra) {
        if (this.buf.remaining() < extra) {
            int newCap = Math.max(this.buf.capacity() * 2, this.buf.position() + extra + 64);
            ByteBuffer nb = ByteBuffer.allocate(newCap);
            nb.order(ByteOrder.LITTLE_ENDIAN);
            this.buf.flip();
            nb.put(this.buf);
            this.buf = nb;
        }
    }

    public void align(int a) {
        int pad = (a - this.buf.position() % a) % a;
        this.ensure(pad);
        for (int i = 0; i < pad; ++i) {
            this.buf.put((byte)0);
        }
    }

    public void u8(int v) {
        this.ensure(1);
        this.buf.put((byte)v);
    }

    public void u32(int v) {
        this.align(4);
        this.ensure(4);
        this.buf.putInt(v);
    }

    public void i32(int v) {
        this.align(4);
        this.ensure(4);
        this.buf.putInt(v);
    }

    public void u64(long v) {
        this.align(8);
        this.ensure(8);
        this.buf.putLong(v);
    }

    public void bool(boolean v) {
        this.align(4);
        this.ensure(4);
        this.buf.putInt(v ? 1 : 0);
    }

    public void string(String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        this.u32(b.length);
        this.ensure(b.length + 1);
        this.buf.put(b);
        this.buf.put((byte)0);
    }

    public void objectPath(String s) {
        this.string(s);
    }

    public void signature(String s) {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        this.u8(b.length);
        this.ensure(b.length + 1);
        this.buf.put(b);
        this.buf.put((byte)0);
    }

    public void variantString(String s) {
        this.signature("s");
        this.string(s);
    }

    public void emptyDictSV() {
        this.u32(0);
    }

    public void arrayOfSS(String[][] pairs) {
        Out tmp = new Out();
        for (String[] p : pairs) {
            tmp.align(8);
            tmp.string(p[0]);
            tmp.string(p[1]);
        }
        byte[] content = tmp.toBytes();
        this.u32(content.length);
        this.align(8);
        for (byte b : content) {
            this.u8(b);
        }
    }

    public byte[] toBytes() {
        byte[] out = new byte[this.buf.position()];
        this.buf.flip();
        this.buf.get(out);
        return out;
    }
}

