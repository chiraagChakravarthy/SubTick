package subtick.variables;

import carpet.network.ServerNetworkHandler;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.*;

public class Variables {
    //public static final int TICK_FREEZE = 0, BLOCK_EVENTS = 1, POST_TICK = 2;

    public static boolean actuallyProcessEntities;
    public static RegistryKey<World> currentDimension;
    public static Phase currentTickPhase;
    public static RegistryKey<World> targetDimension;
    public static Phase targetTickPhase;
    public static Map<RegistryKey<World>,WorldData> worldVariables;

    public static String[] tickPhasePluralNames = new String[]{
        "Tick Freeze",
        "Block Events"
    };

    public static int beStep = 0, bedStep = 0;
    public static int bePlay = 0, bedPlay = 0;
    public static int playInterval = 1;

    public static int bedEnd = 0, executedBeCount = 0;

    public static Queue<int[]> clientHighlights;

    public static Vec3d commandSrcPos;

    static {
        worldVariables = new HashMap<>();
        worldVariables.put(World.OVERWORLD,new WorldData("Overworld"));
        worldVariables.put(World.NETHER,new WorldData("Nether"));
        worldVariables.put(World.END,new WorldData("End"));
        clientHighlights = new ArrayDeque<>();
        commandSrcPos = Vec3d.ZERO;//so that not null
    }

    public static WorldData getData(RegistryKey<World> dimension) {
        return worldVariables.get(dimension);
    }

    public static boolean isBeforeTarget(RegistryKey<World> dimension,Phase phase) {
        return aBeforeB(dimension,phase,targetDimension,targetTickPhase);
    }

    public static boolean targetBefore(RegistryKey<World> dimension,Phase phase) {
        return aBeforeB(targetDimension,targetTickPhase,dimension,phase);
    }

    public static boolean isBeforeCurrent(RegistryKey<World> dimension,Phase phase) {
        return aBeforeB(dimension,phase,currentDimension,currentTickPhase);
    }

    public static boolean currentAtTarget() {
        return currentDimension == targetDimension && currentTickPhase == targetTickPhase;
    }

    public static boolean currentBeforeTarget() {
        return isBeforeTarget(currentDimension,currentTickPhase);
    }

    public static boolean isAtTarget(RegistryKey<World> dimension,Phase phase) {
        return dimension == targetDimension && phase == targetTickPhase;
    }

    public static boolean isAtCurrent(RegistryKey<World> dimension,Phase phase) {
        return dimension == currentDimension && phase == currentTickPhase;
    }

    public static boolean aBeforeB(RegistryKey<World> dimA,Phase phaseA,RegistryKey<World> dimB,Phase phaseB) {
        return (dimA == dimB && phaseA.isBefore(phaseB)) ||
                   dimB == World.END ||
                   (dimB == World.NETHER && dimA == World.OVERWORLD);
    }

    //ensures play variables are cleared when changing target phase
    public static void setTargetPhase(RegistryKey<World> dimension,Phase phase,MinecraftServer server) {
        if(!isAtTarget(dimension,phase)) {
            onTargetChange(server);
            targetTickPhase = phase;
            targetDimension = dimension;
        }
    }

    private static void onTargetChange(MinecraftServer server) {
        bedPlay = 0;
        bePlay = 0;
        bedEnd = 0;
        executedBeCount = 0;
        clearHighlights(server.getPlayerManager().getPlayerList());
    }

    //debug
    public static void stackTraceVariables() {
        Phase targetTickPhase = Variables.targetTickPhase;
        RegistryKey<World> targetDimension = Variables.targetDimension;
        Phase currentTickPhase = Variables.currentTickPhase;
        RegistryKey<World> currentDimension = Variables.currentDimension;
        int i = 0;
    }

    public static void addHighlight(int x,int y,int z,List<ServerPlayerEntity> players,ServerWorld world) {
        FallingBlockEntity entity = new FallingBlockEntity(world,(double) x + .5,y - 1 / 48d,(double) z + .5,Blocks.GLASS.getDefaultState());
        entity.setNoGravity(true);
        entity.setGlowing(true);
        Packet<?> packet = entity.createSpawnPacket();
        EntityTrackerUpdateS2CPacket dataPacket = new EntityTrackerUpdateS2CPacket(entity.getId(),entity.getDataTracker(),true);
        for(ServerPlayerEntity player : players) {
            if(ServerNetworkHandler.isValidCarpetPlayer(player)) {
                player.networkHandler.sendPacket(packet);
                player.networkHandler.sendPacket(dataPacket);
            }
        }
        clientHighlights.add(new int[]{entity.getId(),Variables.frozenTickCount});
    }

    public static double horizontalDistance(Vec3d srcPos,BlockPos block) {
        double dx = srcPos.x - block.getX();
        double dz = srcPos.z - block.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static void clearHighlights(List<ServerPlayerEntity> players) {
        EntitiesDestroyS2CPacket packet = new EntitiesDestroyS2CPacket();
        IntList ids = packet.getEntityIds();
        while(clientHighlights.size() > 0) {
            ids.add(clientHighlights.poll()[0]);
        }

        for(ServerPlayerEntity player : players) {
            if(ServerNetworkHandler.isValidCarpetPlayer(player)) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }

    public static RegistryKey<World> recentPlayerDimension = null;
    public static int frozenTickCount = 0;
    public static int playStart = 0;
    public static boolean inWorldTick = false;
}
