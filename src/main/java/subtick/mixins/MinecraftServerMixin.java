package subtick.mixins;

import carpet.helpers.TickSpeed;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.ServerTask;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.util.thread.ReentrantThreadExecutor;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.variables.Variables;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin
        extends ReentrantThreadExecutor<ServerTask>
        implements CommandOutput, AutoCloseable {

    @Shadow private PlayerManager playerManager;

    public MinecraftServerMixin(String string) {
        super(string);
    }

    @Inject(method = "tickWorlds", at=@At("HEAD"))
    public void preWorldsTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        Variables.frozenTickCount++;//used for playing
        Variables.actuallyProcessEntities = TickSpeed.process_entities;//lets target tick phase continue seamlessly

        if(TickSpeed.process_entities){
            Variables.setTargetPhase(World.END, Variables.POST_TICK, playerManager.getServer());
        }
        Variables.inWorldTick = true;
    }

    @Inject(method = "tickWorlds", at=@At(
                target = "Lnet/minecraft/server/MinecraftServer;getNetworkIo()Lnet/minecraft/server/ServerNetworkIo;",
                value = "INVOKE"))
    public void postWorldsTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        Variables.inWorldTick = false;
        if(Variables.actuallyProcessEntities){
            Variables.setTargetPhase(World.OVERWORLD, Variables.TICK_FREEZE, playerManager.getServer());
            Variables.currentTickPhase = Variables.TICK_FREEZE;
            Variables.currentDimension = World.OVERWORLD;
            TickSpeed.process_entities = true;
        }
    }
}