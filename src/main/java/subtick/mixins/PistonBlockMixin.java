package subtick.mixins;

import net.minecraft.block.BlockState;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.network.MessageType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.SubTickSettings;

import static net.minecraft.block.PistonBlock.EXTENDED;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin extends FacingBlock {

    protected PistonBlockMixin(Settings settings) {
        super(settings);
    }

    @Shadow protected abstract boolean shouldExtend(World world, BlockPos pos, Direction pistonFace);

    @Inject(method = "onSyncedBlockEvent", at = @At(
            target = "Lnet/minecraft/block/PistonBlock;shouldExtend(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Z",
    value = "INVOKE"))
    public void instantRetract(BlockState state, World world, BlockPos pos, int type, int data, CallbackInfoReturnable<Boolean> cir){
        if(SubTickSettings.losslessBedrockBreaking) {
            Direction direction = state.get(FACING);
            boolean powered = shouldExtend(world, pos, direction);
            if (!powered && type == 0) {
                world.setBlockState(pos, state.with(EXTENDED, false), 2);
            }
        }
    }
}
