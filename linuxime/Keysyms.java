/*
 * Decompiled with CFR 0.152.
 */
package linuxime;

public final class Keysyms {
    private static final int GLFW_MOD_SHIFT = 1;
    private static final int GLFW_MOD_CONTROL = 2;
    private static final int GLFW_MOD_ALT = 4;
    private static final int GLFW_MOD_SUPER = 8;
    private static final int GLFW_MOD_CAPS_LOCK = 16;
    private static final int GLFW_MOD_NUM_LOCK = 32;

    private Keysyms() {
    }

    public static int xState(int glfwMods) {
        int s = 0;
        if ((glfwMods & 1) != 0) {
            s |= 1;
        }
        if ((glfwMods & 0x10) != 0) {
            s |= 2;
        }
        if ((glfwMods & 2) != 0) {
            s |= 4;
        }
        if ((glfwMods & 4) != 0) {
            s |= 8;
        }
        if ((glfwMods & 0x20) != 0) {
            s |= 0x10;
        }
        if ((glfwMods & 8) != 0) {
            s |= 0x40;
        }
        return s;
    }

    public static int keysym(int glfwKey) {
        if (glfwKey >= 65 && glfwKey <= 90) {
            return glfwKey + 32;
        }
        if (glfwKey >= 32 && glfwKey <= 126) {
            return glfwKey;
        }
        switch (glfwKey) {
            case 256: {
                return 65307;
            }
            case 257: {
                return 65293;
            }
            case 258: {
                return 65289;
            }
            case 259: {
                return 65288;
            }
            case 260: {
                return 65379;
            }
            case 261: {
                return 65535;
            }
            case 262: {
                return 65363;
            }
            case 263: {
                return 65361;
            }
            case 264: {
                return 65364;
            }
            case 265: {
                return 65362;
            }
            case 266: {
                return 65365;
            }
            case 267: {
                return 65366;
            }
            case 268: {
                return 65360;
            }
            case 269: {
                return 65367;
            }
        }
        return 0;
    }
}

