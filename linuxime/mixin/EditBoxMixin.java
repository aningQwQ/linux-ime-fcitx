/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.components.EditBox
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package linuxime.mixin;

import linuxime.FcitxIntegration;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={EditBox.class})
public abstract class EditBoxMixin {
    @Inject(method={"keyPressed"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$forwardToIme(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        EditBox self = (EditBox)(Object)this;
        if (FcitxIntegration.onKeyPressed(self, event)) {
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

