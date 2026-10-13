/*
 * Decompiled with CFR 0.152.
 */
package linuxime.dbus;

final class Marshaller {
    private Marshaller() {
    }

    static int alignment(String sig) {
        char c = sig.charAt(0);
        switch (c) {
            case 'g': 
            case 'y': {
                return 1;
            }
            case 'n': 
            case 'q': {
                return 2;
            }
            case 'a': 
            case 'b': 
            case 'i': 
            case 'o': 
            case 's': 
            case 'u': {
                return 4;
            }
        }
        return 8;
    }

    static int singleTypeEnd(String sig, int start) {
        char c = sig.charAt(start);
        switch (c) {
            case 'a': {
                return Marshaller.singleTypeEnd(sig, start + 1);
            }
            case '(': {
                int depth = 0;
                for (int i = start; i < sig.length(); ++i) {
                    if (sig.charAt(i) == '(') {
                        ++depth;
                        continue;
                    }
                    if (sig.charAt(i) != ')' || --depth != 0) continue;
                    return i + 1;
                }
                throw new IllegalArgumentException("unbalanced ( in " + sig);
            }
            case '{': {
                for (int i = start; i < sig.length(); ++i) {
                    if (sig.charAt(i) != '}') continue;
                    return i + 1;
                }
                throw new IllegalArgumentException("unbalanced { in " + sig);
            }
        }
        return start + 1;
    }
}

