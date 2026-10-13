/*
 * Decompiled with CFR 0.152.
 */
package linuxime.mixin;

import linuxime.FcitxIntegration;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={MultiLineEditBox.class})
public abstract class MultiLineEditBoxMixin {
    @Inject(method={"keyPressed"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$forwardToIme(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (FcitxIntegration.onKeyPressed(FcitxIntegration.multiLineEditBox((MultiLineEditBox)(Object)this), event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method={"charTyped"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$swallowRawChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (FcitxIntegration.onCharTyped()) {
            cir.setReturnValue(true);
        }
    }
}
