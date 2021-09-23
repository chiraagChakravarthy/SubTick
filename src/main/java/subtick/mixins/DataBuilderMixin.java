package subtick.mixins;

import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.SubTickSettings;

@Mixin(targets = "carpet.network.ServerNetworkHandler$DataBuilder")
public class DataBuilderMixin {
    @Shadow private NbtCompound tag;

    @Inject(method = "withFrozenState",at = @At("TAIL"),remap = false)
    public void modifiedFrozenState(CallbackInfoReturnable<?> cir){
        if(SubTickSettings.vanillaPistonAnimations) {
            NbtCompound tickingState = new NbtCompound();
            tickingState.putBoolean("is_paused",false);
            tickingState.putBoolean("deepFreeze",false);
            tag.put("TickingState",tickingState);
        }
    }
}
