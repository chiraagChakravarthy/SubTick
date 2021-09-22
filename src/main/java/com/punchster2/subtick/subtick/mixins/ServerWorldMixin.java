package subtick.mixins;

import carpet.helpers.TickSpeed;
import carpet.network.ServerNetworkHandler;
import carpet.patches.NetworkManagerFake;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import subtick.SubTickSettings;
import subtick.variables.Variables;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {

    @Shadow @Final private ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue;
    @Shadow @Final private MinecraftServer server;

    @Shadow private boolean processBlockEvent(BlockEvent event){return false;}

    @Shadow public abstract List<ServerPlayerEntity> getPlayers();

    @Shadow protected abstract boolean addEntity(Entity entity);

    @Shadow public abstract ServerWorld toServerWorld();

    private static int prevSize;

    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DimensionType dimensionType, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed) {
        super(properties, registryRef, dimensionType, profiler, isClient, debugWorld, seed);
    }

    private boolean doBlockEvent(){
        BlockEvent blockEvent = this.syncedBlockEventQueue.removeFirst();
        boolean out = getBlockState(blockEvent.getPos()).isOf(blockEvent.getBlock());
        if (this.processBlockEvent(blockEvent)) {
            this.server.getPlayerManager().sendToAround(null, blockEvent.getPos().getX(), blockEvent.getPos().getY(), blockEvent.getPos().getZ(), 64.0D, this.getRegistryKey(), new BlockEventS2CPacket(blockEvent.getPos(), blockEvent.getBlock(), blockEvent.getType(), blockEvent.getData()));
        }
        BlockPos pos = blockEvent.getPos();
        if((out||SubTickSettings.includeInvalidBlockEvents)&&SubTickSettings.highlightBlockEvents) {
            Variables.addHighlight(pos.getX(), pos.getY(), pos.getZ(), server.getPlayerManager().getPlayerList(), toServerWorld());
        }
        return out;
    }

    @Inject(method = "addSyncedBlockEvent",
            at = @At("HEAD"))
    public void recordBeSize(BlockPos pos, Block block, int type, int data, CallbackInfo ci){
        prevSize = syncedBlockEventQueue.size();
    }

    @Inject(method = "addSyncedBlockEvent",
            at=@At("TAIL"))
    public void incrementBe(BlockPos pos, Block block, int type, int data, CallbackInfo ci){
        RegistryKey<World> dimension = Variables.inWorldTick?Variables.currentDimension:Variables.recentPlayerDimension;
        if(prevSize<syncedBlockEventQueue.size()){
            Variables.getData(dimension).beCount++;
        }
    }

    @Inject(method = "processBlockEvent",
            at=@At("HEAD"))
    public void decrementBe(BlockEvent event, CallbackInfoReturnable<Boolean> cir){
        Variables.getData(getRegistryKey()).beCount--;
        Variables.executedBeCount++;
    }

    @Inject(method="tick",
            at = @At("HEAD"),
            cancellable = true)
    public void tickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci){
        defaultTickPhaseLogic(getRegistryKey(), Variables.TICK_FREEZE);
    }

    @Inject(method = "tick",
            at=@At(target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V", value = "INVOKE"),
            cancellable = true)
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
        if(Variables.getData(getRegistryKey()).beCount==0){
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
