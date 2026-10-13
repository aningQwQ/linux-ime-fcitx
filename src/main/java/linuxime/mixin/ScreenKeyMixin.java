/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraft.client.input.KeyEvent
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package linuxime.mixin;

import linuxime.FcitxIntegration;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={Screen.class})
public abstract class ScreenKeyMixin {
    @Inject(method={"keyPressed"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$escCancelsComposition(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (FcitxIntegration.onScreenEsc(event)) {
            cir.setReturnValue(true);
        }
    }
}

