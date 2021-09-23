package subtick.mixins;

import carpet.helpers.TickSpeed;
import carpet.utils.Messenger;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockEventS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.BlockEvent;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
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
import subtick.variables.Phase;
import subtick.variables.Variables;

import static subtick.variables.Variables.*;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World {

    @Shadow @Final private ObjectLinkedOpenHashSet<BlockEvent> syncedBlockEventQueue;
    @Shadow @Final private MinecraftServer server;

    @Shadow private boolean processBlockEvent(BlockEvent event) {return false;}

    @Shadow public abstract List<ServerPlayerEntity> getPlayers();

    @Shadow public abstract ServerWorld toServerWorld();

    private static int prevSize;

    protected ServerWorldMixin(MutableWorldProperties properties,
                               RegistryKey<World> registryRef,
                               DimensionType dimensionType,
                               Supplier<Profiler> profiler,
                               boolean isClient,boolean debugWorld,
                               long seed) {
        super(properties,registryRef,dimensionType,profiler,isClient,debugWorld,seed);
    }

    private boolean doBlockEvent() {
        final BlockEvent blockEvent = syncedBlockEventQueue.removeFirst();
        final BlockPos bePos = blockEvent.getPos();
        final Block beBlock = blockEvent.getBlock();
        final PlayerManager players = server.getPlayerManager();
        if(processBlockEvent(blockEvent))
            players.sendToAround(
                null,
                bePos.getX(),bePos.getY(),bePos.getZ(),
                64.0D,
                getRegistryKey(),
                new BlockEventS2CPacket(
                    bePos,beBlock,
                    blockEvent.getType(),
                    blockEvent.getData()
                )
            );
        if(SubTickSettings.includeInvalidBlockEvents || getBlockState(bePos).isOf(beBlock)) {
            if(SubTickSettings.highlightBlockEvents)
                addHighlight(
                    bePos.getX(),bePos.getY(),bePos.getZ(),
                    players.getPlayerList(),toServerWorld()
                );
            return horizontalDistance(commandSrcPos,bePos) < SubTickSettings.beRadius;
        }
        return false;
    }

    @Inject(method = "addSyncedBlockEvent",at = @At("HEAD"))
    public void recordBeSize(BlockPos pos,Block block,int type,int data,CallbackInfo ci) {
        prevSize = syncedBlockEventQueue.size();
    }

    @Inject(method = "addSyncedBlockEvent",at = @At("TAIL"))
    public void incrementBe(BlockPos pos,Block block,int type,int data,CallbackInfo ci) {
        if(prevSize < syncedBlockEventQueue.size())
            getData(inWorldTick? currentDimension : recentPlayerDimension).beCount++;
    }

    @Inject(method = "processBlockEvent",at = @At("HEAD"))
    public void decrementBe(BlockEvent event,CallbackInfoReturnable<Boolean> cir) {
        getData(getRegistryKey()).beCount--;
        executedBeCount++;
    }

    @Inject(method = "tick",at = @At("HEAD"),cancellable = true)
    public void tickStart(BooleanSupplier shouldKeepTicking,CallbackInfo ci) {
        defaultTickPhaseLogic(getRegistryKey(),Phase.TICK_FREEZE);
    }

    @Inject(
        method = "tick",
        at = @At(
            target = "Lnet/minecraft/server/world/ServerWorld;processSyncedBlockEvents()V",
            value = "INVOKE"
        ),
        cancellable = true
    )
    public void beforeBlockEvents(BooleanSupplier shouldKeepTicking,CallbackInfo ci) {
        if(defaultTickPhaseLogic(getRegistryKey(),Phase.BLOCK_EVENTS)) {
            TickSpeed.process_entities = true;
            blockEventStep();
            TickSpeed.process_entities = false;
        }
    }

    //returns if this phase should be stepped through
    private static boolean defaultTickPhaseLogic(RegistryKey<World> dimension,Phase phase) {
        TickSpeed.process_entities = false;
        if(isBeforeCurrent(dimension,phase))
            return false;
        if(currentBeforeTarget() && (isBeforeTarget(dimension,phase) || isAtTarget(dimension,phase))) {
            currentDimension = dimension;
            currentTickPhase = phase;
        }
        if(currentAtTarget())
            return isAtCurrent(dimension,phase);
        TickSpeed.process_entities = true;
        return false;
    }

    private void blockEventStep() {
        final boolean played;
        if(played = (frozenTickCount - playStart) % playInterval == 0 && !(bedPlay == 0 || bePlay == 0)) {
            --bePlay;
            ++beStep;
        }
        final List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
        if(beStep != 0 || bedStep != 0)
            clearHighlights(players);

        if(executedBeCount >= bedEnd)
            bedEnd += syncedBlockEventQueue.size();

        if(bedStep != 0) {
            for(int i = 0;i < bedEnd - executedBeCount;++i)
                doBlockEvent();
            --bedStep;
            //TODO prgmTrouble:
            // I think that this section can be improved by simply
            // calling doBlockEvent() until the queue is empty.
            // I don't know enough about what's happening here atm
            // to know for sure.
            for(int i = 0;i < bedStep && !syncedBlockEventQueue.isEmpty();++i) {
                final int nbed = syncedBlockEventQueue.size();
                for(int j = 0;j < nbed && !syncedBlockEventQueue.isEmpty();++j)
                    doBlockEvent();
            }
        } else
            for(int i = 0;i < beStep && !syncedBlockEventQueue.isEmpty();)
                if(doBlockEvent() || SubTickSettings.includeInvalidBlockEvents)
                    ++i;

        beStep = 0;
        bedStep = 0;
        if(getData(getRegistryKey()).beCount == 0) {
            bePlay = 0;
            bedPlay = 0;
        }
        //TODO prgmTrouble:
        // I think that the bePlay and bedPlay checks can be elided somehow,
        // but I need to understand the code better to know for sure.
        if(played && bePlay == 0 && bedPlay == 0)
            for(PlayerEntity player : players)
                Messenger.m(player,"w Finished playing block events");
    }
}
