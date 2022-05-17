package subtick.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import subtick.SubTick;
import subtick.SubTickSettings;
import subtick.variables.Variables;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {

    @Shadow @Final public ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue;
    @Shadow @Final private MinecraftServer server;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> registryEntry, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, registryEntry, profiler, isClient, debugWorld, seed);
    }

    @Shadow private boolean processBlockEvent(BlockEvent event){return false;}

    @Shadow public abstract List<ServerPlayerEntity> getPlayers();

    @Shadow public abstract ServerWorld toServerWorld();

    private boolean doBlockEvent(){
        BlockEvent blockEvent = this.syncedBlockEventQueue.removeFirst();
        boolean validBe = getBlockState(blockEvent.pos()).isOf(blockEvent.block())||SubTickSettings.includeInvalidBlockEvents;

        if (this.processBlockEvent(blockEvent)) {
            this.server.getPlayerManager().sendToAround(null, blockEvent.pos().getX(), blockEvent.pos().getY(), blockEvent.pos().getZ(), 64.0D, this.getRegistryKey(), new BlockEventS2CPacket(blockEvent.pos(), blockEvent.block(), blockEvent.type(), blockEvent.data()));
        }
        BlockPos pos = blockEvent.pos();
        if(validBe&&SubTickSettings.highlightBlockEvents) {
            Variables.addHighlight(pos.getX(), pos.getY(), pos.getZ(), server.getPlayerManager().getPlayerList(), toServerWorld());
        }
        return validBe&&Variables.horizontalDistance(Variables.commandSource.getPosition(), blockEvent.pos())<SubTickSettings.beRadius;
    }

    @Inject(method="tick",
            at = @At("HEAD"))
    public void tickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        defaultTickPhaseLogic(getRegistryKey(), Variables.TICK_FREEZE);
    }

    @Inject(method = "tick",
            at=@At(target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V", value = "INVOKE"))
    public void beforeBlockEvents(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        if(defaultTickPhaseLogic(getRegistryKey(), Variables.BLOCK_EVENTS)){
            TickSpeed.process_entities = true;
            blockEventStep();
            TickSpeed.process_entities = false;
        }
    }

    //returns if this phase should be stepped through
    private static boolean defaultTickPhaseLogic(RegistryKey<World> dimension, int phase){
        TickSpeed.process_entities = false;
        if(Variables.isBeforeCurrent(dimension, phase)){
            return false;
        }
        if(Variables.currentBeforeTarget()&&(Variables.isBeforeTarget(dimension, phase)||Variables.isAtTarget(dimension, phase))){
            Variables.currentDimension = dimension;
            Variables.currentTickPhase = phase;
        }
        if(Variables.currentAtTarget()){
            return Variables.isAtCurrent(dimension, phase);
        }
        TickSpeed.process_entities = true;
        return false;
    }

    private void blockEventStep(){
        boolean played = false;

        if((Variables.frozenTickCount-Variables.playStart)%Variables.playInterval==0) {
            if (Variables.bedPlay == 0) {
                if (Variables.bePlay != 0) {
                    Variables.bePlay--;
                    Variables.beStep++;
                    played = true;
                }
            } else {
                Variables.bedPlay--;
                Variables.bedStep++;
                played = true;
            }
        }
        if(Variables.beStep!=0||Variables.bedStep!=0){
            Variables.clearHighlights(server.getPlayerManager().getPlayerList());
        }

        if(Variables.executedBeCount >= Variables.bedEnd){
            Variables.bedEnd += syncedBlockEventQueue.size();
        }

        if(Variables.bedStep==0){
            for (int i = 0; i < Variables.beStep; i++) {
                if(syncedBlockEventQueue.size()==0){
                    break;
                }
                if(!doBlockEvent()&&!SubTickSettings.includeInvalidBlockEvents){
                    i--;
                }
            }
        } else {
            int bedSize = Variables.bedEnd-Variables.executedBeCount;
            for (int i = 0; i < bedSize; i++) {
                doBlockEvent();
            }
            Variables.bedStep--;
            for (int i = 0; i < Variables.bedStep; i++) {
                bedSize = syncedBlockEventQueue.size();
                for (int j = 0; j < bedSize; j++) {
                    if(syncedBlockEventQueue.size()==0){
                        break;
                    }
                    doBlockEvent();
                }
            }
        }
        Variables.beStep = 0;
        Variables.bedStep = 0;
        if(syncedBlockEventQueue.size()==0){
            Variables.bePlay = 0;
            Variables.bedPlay = 0;
        }
        if (Variables.bePlay==0&&Variables.bedPlay==0&&played) {
            for(PlayerEntity player : server.getPlayerManager().getPlayerList()){

                Messenger.m(player, "w Finished playing block events");
            }
        }
    }
}
