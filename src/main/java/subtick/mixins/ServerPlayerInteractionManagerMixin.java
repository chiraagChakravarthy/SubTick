package subtick.mixins;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.variables.Variables;

@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {

    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "tryBreakBlock", at=@At("HEAD"))
    public void onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir){
        Variables.recentPlayerDimension = player.world.getRegistryKey();
    }

    @Inject(method = "interactItem", at=@At("HEAD"))
    public void onInteractItem(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, CallbackInfoReturnable<ActionResult> cir){
        Variables.recentPlayerDimension = this.player.world.getRegistryKey();
    }

    @Inject(method = "interactBlock", at=@At("HEAD"))
    public void onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir){
        Variables.recentPlayerDimension = this.player.world.getRegistryKey();
    }
}
