/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.KeyboardHandler
 *  net.minecraft.client.input.KeyEvent
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package linuxime.mixin;

import linuxime.FcitxIntegration;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={KeyboardHandler.class})
public abstract class KeyboardHandlerMixin {
    @Inject(method={"keyPress"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$escClosesImeFirst(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (action != 1) {
            return;
        }
        if (FcitxIntegration.onScreenEsc(event)) {
            ci.cancel();
            return;
        }
        if (FcitxIntegration.onGlobalKeyPressed(event)) {
            ci.cancel();
        }
    }

    @Inject(method={"charTyped"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$swallowRawChar(long window, CharacterEvent event, CallbackInfo ci) {
        if (FcitxIntegration.onCharTyped()) {
            ci.cancel();
        }
    }
}
