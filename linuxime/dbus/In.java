/*
 * Decompiled with CFR 0.152.
 */
package linuxime.dbus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import linuxime.dbus.Marshaller;

final class In {
    private final ByteBuffer buf;

    In(byte[] data, int offset) {
        this.buf = ByteBuffer.wrap(data, offset, data.length - offset).slice();
        this.buf.order(ByteOrder.LITTLE_ENDIAN);
    }

    int pos() {
        return this.buf.position();
    }

    void align(int a) {
        this.buf.position(this.buf.position() + (a - this.buf.position() % a) % a);
    }

    int u8() {
        return this.buf.get() & 0xFF;
    }

    int u32() {
        this.align(4);
        return this.buf.getInt();
    }

    int i32() {
        this.align(4);
        return this.buf.getInt();
    }

    long u64() {
        this.align(8);
        return this.buf.getLong();
    }

    boolean bool() {
        return this.u32() != 0;
    }

    String string() {
        int len = this.u32();
        byte[] b = new byte[len];
        this.buf.get(b);
        this.buf.get();
        return new String(b, StandardCharsets.UTF_8);
    }

    String objectPath() {
        return this.string();
    }

    String signature() {
        int len = this.u8();
        byte[] b = new byte[len];
        this.buf.get(b);
        this.buf.get();
        return new String(b, StandardCharsets.UTF_8);
    }

    Object read(String sig) {
        char c = sig.charAt(0);
        switch (c) {
            case 'y': {
                return (byte)this.u8();
            }
            case 'b': {
                return this.bool();
            }
            case 'n': {
                return (short)this.i32();
            }
            case 'q': {
                return this.u32();
            }
            case 'i': {
                return this.i32();
            }
            case 'u': {
                return this.u32();
            }
            case 'x': {
                return this.u64();
            }
            case 't': {
                return this.u64();
            }
            case 'd': {
                this.align(8);
                return this.buf.getDouble();
            }
            case 's': {
                return this.string();
            }
            case 'o': {
                return this.objectPath();
            }
            case 'g': {
                return this.signature();
            }
            case 'v': {
                String inner = this.signature();
                return this.read(inner);
            }
            case 'a': {
                int byteLen = this.u32();
                String elem = sig.substring(1);
                int elemEnd = In.elemEnd(elem);
                String elemSig = elem.substring(0, elemEnd);
                int align = Marshaller.alignment(elemSig);
                this.align(align);
                int startPos = this.buf.position();
                ArrayList<Object> list = new ArrayList<Object>();
                while (this.buf.position() < startPos + byteLen) {
                    list.add(this.read(elemSig));
                }
                this.buf.position(startPos + byteLen);
                return list;
            }
            case '(': {
                int end = In.parenEnd(sig);
                String inner = sig.substring(1, end);
                this.align(8);
                ArrayList<Object> parts = new ArrayList<Object>();
                int i = 0;
                while (i < inner.length()) {
                    int ce = Marshaller.singleTypeEnd(inner, i);
                    parts.add(this.read(inner.substring(i, ce)));
                    i = ce;
                }
                return parts.toArray();
            }
            case '{': {
                int end = sig.indexOf(125);
                String keySig = String.valueOf(sig.charAt(1));
                String valSig = sig.substring(2, end);
                this.align(8);
                Object k = this.read(keySig);
                Object v = this.read(valSig);
                return new Object[]{k, v};
            }
        }
        throw new IllegalArgumentException("unsupported type: " + sig);
    }

    void skip(String sig) {
        this.read(sig);
    }

    private static int elemEnd(String sig) {
        return Marshaller.singleTypeEnd(sig, 0);
    }

    private static int parenEnd(String sig) {
        int depth = 0;
        for (int i = 0; i < sig.length(); ++i) {
            if (sig.charAt(i) == '(') {
                ++depth;
                continue;
            }
            if (sig.charAt(i) != ')' || --depth != 0) continue;
            return i;
        }
        throw new IllegalArgumentException("unbalanced ( : " + sig);
    }
}

