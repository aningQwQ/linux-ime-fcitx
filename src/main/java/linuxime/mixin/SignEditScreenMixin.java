/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.font.TextFieldHelper
 *  net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 *  net.minecraft.world.level.block.entity.SignBlockEntity
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package linuxime.mixin;

import linuxime.FcitxIntegration;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={AbstractSignEditScreen.class})
public abstract class SignEditScreenMixin {
    @Shadow
    private TextFieldHelper signField;
    @Shadow
    private int line;
    @Shadow
    @Final
    private String[] messages;
    @Shadow
    @Final
    protected SignBlockEntity sign;

    @Shadow
    protected abstract float getSignYOffset();

    @Inject(method={"keyPressed"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$forwardToIme(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (FcitxIntegration.onKeyPressed(this.linuxime$target(), event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method={"charTyped"}, at={@At(value="HEAD")}, cancellable=true)
    private void linuxime$swallowRawChar(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (FcitxIntegration.onCharTyped()) {
            cir.setReturnValue(true);
        }
    }

    private FcitxIntegration.TextTarget linuxime$target() {
        final AbstractSignEditScreen self = (AbstractSignEditScreen)(Object)this;
        return new FcitxIntegration.TextTarget(){

            public Object id() {
                return self;
            }

            public String value() {
                int l = SignEditScreenMixin.this.line;
                String[] m = SignEditScreenMixin.this.messages;
                return l >= 0 && l < m.length && m[l] != null ? m[l] : "";
            }

            public int cursor() {
                return SignEditScreenMixin.this.signField.getCursorPos();
            }

            public void insert(String text) {
                SignEditScreenMixin.this.signField.insertText(text);
            }

            public int x() {
                return self.width / 2;
            }

            public int y() {
                return (int)SignEditScreenMixin.this.getSignYOffset();
            }

            public int height() {
                return SignEditScreenMixin.this.sign.getTextLineHeight();
            }
        };
    }
}
