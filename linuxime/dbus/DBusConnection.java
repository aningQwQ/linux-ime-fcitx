/*
 * Decompiled with CFR 0.152.
 */
package linuxime.dbus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import linuxime.ImeLog;
import linuxime.dbus.In;
import linuxime.dbus.Message;
import linuxime.dbus.Out;

public final class DBusConnection {
    private SocketChannel channel;
    private InputStream in;
    private OutputStream out;
    private final AtomicInteger serial = new AtomicInteger(1);
    private final Map<Integer, LinkedBlockingQueue<Message>> pending = new ConcurrentHashMap<Integer, LinkedBlockingQueue<Message>>();
    private final List<Consumer<Message>> signalListeners = new CopyOnWriteArrayList<Consumer<Message>>();
    private volatile String uniqueName;
    private Thread reader;

    public void connect() throws IOException {
        Path bus = DBusConnection.resolveBusPath();
        this.channel = SocketChannel.open(StandardProtocolFamily.UNIX);
        this.channel.connect(UnixDomainSocketAddress.of(bus));
        this.in = Channels.newInputStream(this.channel);
        this.out = Channels.newOutputStream(this.channel);
        this.authenticate();
        this.startReader();
        this.hello();
        ImeLog.info("D-Bus connected as " + this.uniqueName);
    }

    private static Path resolveBusPath() {
        String env = System.getenv("DBUS_SESSION_BUS_ADDRESS");
        if (env != null && env.startsWith("unix:path=")) {
            String p = env.substring("unix:path=".length());
            int comma = p.indexOf(44);
            if (comma >= 0) {
                p = p.substring(0, comma);
            }
            return Path.of(p, new String[0]);
        }
        Object runtime = System.getenv("XDG_RUNTIME_DIR");
        if (runtime == null) {
            runtime = "/run/user/" + DBusConnection.uid();
        }
        return Path.of((String)runtime, "bus");
    }

    private static String uid() {
        try {
            for (String line : Files.readAllLines(Path.of("/proc/self/status", new String[0]))) {
                if (!line.startsWith("Uid:")) continue;
                return line.split("\\s+")[1];
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
        return "0";
    }

    private static String hexUid() {
        String u = DBusConnection.uid();
        StringBuilder sb = new StringBuilder();
        for (char c : u.toCharArray()) {
            sb.append(String.format("%02x", c));
        }
        return sb.toString();
    }

    private void authenticate() throws IOException {
        this.out.write(0);
        this.out.write(("AUTH EXTERNAL " + DBusConnection.hexUid() + "\r\n").getBytes());
        this.out.flush();
        String resp = this.readLine();
        if (resp == null || !resp.startsWith("OK")) {
            throw new IOException("D-Bus auth failed: " + resp);
        }
        this.out.write("BEGIN\r\n".getBytes());
        this.out.flush();
    }

    private String readLine() throws IOException {
        int b;
        StringBuilder sb = new StringBuilder();
        while ((b = this.in.read()) != -1) {
            if (b == 10) {
                String s = sb.toString();
                if (s.endsWith("\r")) {
                    s = s.substring(0, s.length() - 1);
                }
                return s;
            }
            sb.append((char)b);
        }
        return null;
    }

    private void hello() throws IOException {
        Message ret = this.call("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", "", null);
        if (ret != null && !ret.args.isEmpty()) {
            this.uniqueName = (String)ret.args.get(0);
        }
    }

    public void addSignalListener(Consumer<Message> l) {
        this.signalListeners.add(l);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public Message call(String dest, String path, String iface, String member, String signature, Consumer<Out> args) throws IOException {
        int mySerial = this.serial.getAndIncrement();
        LinkedBlockingQueue q = new LinkedBlockingQueue();
        this.pending.put(mySerial, q);
        byte[] body = this.buildBody(signature, args);
        byte[] msg = this.buildMessage(1, 0, mySerial, path, iface, member, dest, null, signature, body);
        this.out.write(msg);
        this.out.flush();
        try {
            Message ret;
            Message message = ret = (Message)q.poll(5L, TimeUnit.SECONDS);
            return message;
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Message message = null;
            return message;
        }
        finally {
            this.pending.remove(mySerial);
        }
    }

    private byte[] buildBody(String signature, Consumer<Out> args) {
        if (args == null) {
            return new byte[0];
        }
        Out o = new Out();
        args.accept(o);
        return o.toBytes();
    }

    private byte[] buildMessage(int type, int flags, int msgSerial, String path, String iface, String member, String dest, String sender, String signature, byte[] body) {
        Out fields = new Out();
        if (path != null) {
            this.writeField(fields, 1, "o", path);
        }
        if (iface != null) {
            this.writeField(fields, 2, "s", iface);
        }
        if (member != null) {
            this.writeField(fields, 3, "s", member);
        }
        if (dest != null) {
            this.writeField(fields, 6, "s", dest);
        }
        if (signature != null && !signature.isEmpty()) {
            this.writeField(fields, 8, "g", signature);
        }
        byte[] fieldsBytes = fields.toBytes();
        Out m = new Out();
        m.u8(108);
        m.u8(type);
        m.u8(flags);
        m.u8(1);
        m.u32(body == null ? 0 : body.length);
        m.u32(msgSerial);
        m.u32(fieldsBytes.length);
        m.align(8);
        byte[] head = m.toBytes();
        Out all = new Out();
        for (byte b : head) {
            all.u8(b);
        }
        all.align(8);
        for (byte b : fieldsBytes) {
            all.u8(b);
        }
        all.align(8);
        if (body != null) {
            for (byte b : body) {
                all.u8(b);
            }
        }
        return all.toBytes();
    }

    private void writeField(Out o, int code, String type, String value) {
        o.align(8);
        o.u8(code);
        o.signature(type);
        if (type.equals("s") || type.equals("o")) {
            o.string(value);
        } else if (type.equals("g")) {
            o.signature(value);
        } else if (type.equals("u")) {
            o.u32(Integer.parseInt(value));
        }
    }

    private void startReader() {
        this.reader = new Thread(this::readLoop, "linux-ime-dbus-reader");
        this.reader.setDaemon(true);
        this.reader.start();
    }

    /*
     * Unable to fully structure code
     */
    private void readLoop() {
lbl1:
        // 2 sources

        try {
            while ((msg = this.readMessage()) != null) {
                block7: {
                    if (msg.type == 2 || msg.type == 3) {
                        q = this.pending.get(msg.replySerial);
                        if (q == null) continue;
                        q.offer(msg);
                        continue;
                    }
                    if (msg.type != 4) break block7;
                    for (Consumer<Message> l : this.signalListeners) {
                        try {
                            l.accept(msg);
                        }
                        catch (Throwable t) {
                            ImeLog.warn("signal listener error: " + String.valueOf(t));
                        }
                    }
                    ** GOTO lbl1
                }
                if (msg.type != 1) continue;
                this.handleIncomingCall(msg);
            }
        }
        catch (IOException e) {
            ImeLog.warn("D-Bus reader stopped: " + String.valueOf(e));
        }
    }

    private void handleIncomingCall(Message msg) {
        try {
            if ("Ping".equals(msg.member)) {
                this.reply(msg, null, "");
            } else {
                this.reply(msg, null, "");
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private void reply(Message call, String errName, String signature) throws IOException {
        int mySerial = this.serial.getAndIncrement();
        int type = errName == null ? 2 : 3;
        byte[] msg = this.buildMessage(type, 0, mySerial, null, null, null, call.sender, null, signature, new byte[0]);
        msg = this.injectReplySerial(msg, call.serial, errName);
        this.out.write(msg);
        this.out.flush();
    }

    private byte[] injectReplySerial(byte[] msg, int replySerial, String errName) {
        Out fields = new Out();
        this.writeField(fields, 5, "u", String.valueOf(replySerial));
        if (errName != null) {
            this.writeField(fields, 4, "s", errName);
        }
        byte[] fieldsBytes = fields.toBytes();
        Out m = new Out();
        m.u8(108);
        m.u8(errName == null ? 2 : 3);
        m.u8(0);
        m.u8(1);
        m.u32(0);
        m.u32(this.serial.getAndIncrement());
        m.u32(fieldsBytes.length);
        m.align(8);
        byte[] head = m.toBytes();
        Out all = new Out();
        for (byte b : head) {
            all.u8(b);
        }
        all.align(8);
        for (byte b : fieldsBytes) {
            all.u8(b);
        }
        return all.toBytes();
    }

    private Message readMessage() throws IOException {
        byte[] fixed = this.readFully(16);
        if (fixed == null) {
            return null;
        }
        In fin = new In(fixed, 0);
        int endian = fin.u8();
        if (endian != 108) {
            throw new IOException("big-endian D-Bus not supported");
        }
        Message msg = new Message();
        msg.type = fin.u8();
        fin.u8();
        fin.u8();
        int bodyLen = fin.u32();
        msg.serial = fin.u32();
        int fieldsLen = fin.u32();
        byte[] fieldsBytes = this.readFully(fieldsLen);
        if (fieldsBytes == null) {
            return null;
        }
        this.parseFields(fieldsBytes, msg);
        int consumed = 16 + fieldsLen;
        int pad = (8 - consumed % 8) % 8;
        if (pad > 0) {
            this.readFully(pad);
        }
        byte[] byArray = msg.body = bodyLen > 0 ? this.readFully(bodyLen) : new byte[]{};
        if (msg.body == null) {
            return null;
        }
        msg.parseArgs();
        return msg;
    }

    private void parseFields(byte[] fieldsBytes, Message msg) {
        In f = new In(fieldsBytes, 0);
        while (f.pos() < fieldsBytes.length) {
            f.align(8);
            if (f.pos() >= fieldsBytes.length) break;
            int code = f.u8();
            String vtype = f.signature();
            Object val = f.read(vtype);
            switch (code) {
                case 1: {
                    msg.path = (String)val;
                    break;
                }
                case 2: {
                    msg.iface = (String)val;
                    break;
                }
                case 3: {
                    msg.member = (String)val;
                    break;
                }
                case 4: {
                    msg.errorName = (String)val;
                    break;
                }
                case 5: {
                    msg.replySerial = (Integer)val;
                    break;
                }
                case 6: {
                    msg.destination = (String)val;
                    break;
                }
                case 7: {
                    msg.sender = (String)val;
                    break;
                }
                case 8: {
                    msg.signature = (String)val;
                    break;
                }
            }
        }
    }

    private byte[] readFully(int n) throws IOException {
        int r;
        byte[] b = new byte[n];
        for (int off = 0; off < n; off += r) {
            r = this.in.read(b, off, n - off);
            if (r >= 0) continue;
            return null;
        }
        return b;
    }
}

