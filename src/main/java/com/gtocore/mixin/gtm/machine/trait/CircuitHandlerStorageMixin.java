package com.gtocore.mixin.gtm.machine.trait;

import com.gregtechceu.gtceu.api.machine.trait.CircuitHandler;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.IntCircuitBehaviour;

import com.gto.datasynclib.datasream.data.ByteData;
import com.gto.datasynclib.datasream.data.Data;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fixes the programmed (ghost) circuit being reset on every world reload (all standard machines:
 * single-block {@code SimpleTieredMachine} and multiblock input buses alike).
 * <p>
 * Root cause is an upstream gtceu bug (since 1.8.7) in {@code CircuitHandler.ItemStackHandler}: its
 * {@code writeData()} serializes a set circuit as a {@link ByteData}, but its {@code readData()}
 * only recognises {@code IntData} ({@code data instanceof IntData(int value)}). {@code ByteData} is a
 * distinct record, so on load it falls through to the {@code ItemStack.of(decode(data))} branch,
 * which cannot decode a numeric {@code ByteData} and yields an empty stack — the circuit is lost.
 * <p>
 * We intercept {@code readData} and decode the {@code ByteData} branch the way {@code writeData}
 * wrote it. This also recovers worlds already saved with the broken build, since the correct
 * {@code ByteData} value is present on disk and was merely being read wrong.
 * <p>
 * {@code ProgrammableCircuitHandler.ProgrammableHandler} extends this inner class and does not
 * override {@code readData}, so this single fix covers it as well.
 */
@Mixin(CircuitHandler.ItemStackHandler.class)
public abstract class CircuitHandlerStorageMixin {

    @Inject(method = "readData", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtocore$readCircuitByteData(Data data, int dataVersion, CallbackInfo ci) {
        if (dataVersion >= 1 && data instanceof ByteData byteData) {
            ((CustomItemStackHandler) (Object) this).stacks[0] = IntCircuitBehaviour.stack(byteData.value());
            ci.cancel();
        }
    }
}
