/*
 * Decompiled with CFR 0.152.
 */
package linuxime.mixin;

import net.minecraft.client.gui.components.MultilineTextField;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(net.minecraft.client.gui.components.MultiLineEditBox.class)
public interface MultiLineEditBoxAccessor {
    @Accessor("textField")
    MultilineTextField linuxime$textField();
}
