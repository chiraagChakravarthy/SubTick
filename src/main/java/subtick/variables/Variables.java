package subtick.variables;

import carpet.network.ServerNetworkHandler;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Blocks;
import net.minecraft.client.font.Font;
import net.minecraft.entity.Entity;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.*;

public class Variables {
    public static final int TICK_FREEZE = 0, BLOCK_EVENTS = 1, POST_TICK = 2;

    public static boolean actuallyProcessEntities;
    public static RegistryKey<World> currentDimension;
    public static int currentTickPhase;
    public static RegistryKey<World> targetDimension;
    public static int targetTickPhase;
    public static Map<RegistryKey<World>, WorldData> worldVariables;

    private static HashMap<Formatting, Team> teamsByColor = new HashMap<>();

    private static final Scoreboard dummyScoreboard = new Scoreboard();//haha dummy


    public static String[] tickPhasePluralNames = new String[]{
            "Tick Freeze",
            "Block Events"
    };

    public static int beStep = 0, bedStep = 0;
    public static int bePlay = 0, bedPlay = 0;
    public static int playInterval = 1;

    public static int bedEnd = 0, executedBeCount = 0;

    public static Queue<int[]> clientHighlights;

    public static ServerCommandSource commandSource;

    static {
        worldVariables = new HashMap<>();
        worldVariables.put(World.OVERWORLD, new WorldData("Overworld"));
        worldVariables.put(World.NETHER, new WorldData("Nether"));
        worldVariables.put(World.END, new WorldData("End"));
        clientHighlights = new ArrayDeque<>();
        commandSource = null;
    }

    public static WorldData getData(RegistryKey<World> dimension){
        return worldVariables.get(dimension);
    }

    public static boolean isBeforeTarget(RegistryKey<World> dimension, int phase) {
        return aBeforeB(dimension, phase, targetDimension, targetTickPhase);
    }

    public static boolean targetBefore(RegistryKey<World> dimension, int phase){
        return aBeforeB(targetDimension, targetTickPhase, dimension, phase);
    }

    public static boolean isBeforeCurrent(RegistryKey<World> dimension, int phase){
        return aBeforeB(dimension, phase, currentDimension, currentTickPhase);
    }

    public static boolean currentAtTarget(){
        return currentDimension==targetDimension&&currentTickPhase==targetTickPhase;
    }

    public static boolean currentBeforeTarget(){
        return isBeforeTarget(currentDimension, currentTickPhase);
    }

    public static boolean isAtTarget(RegistryKey<World> dimension, int phase){
        return dimension==targetDimension&&phase==targetTickPhase;
    }

    public static boolean isAtCurrent(RegistryKey<World> dimension, int phase) {
        return dimension==currentDimension&&phase==currentTickPhase;
    }

    public static boolean aBeforeB(RegistryKey<World> dimA, int phaseA, RegistryKey<World> dimB, int phaseB){
        if(dimA==dimB){
            return phaseA < phaseB;
        }

        if (dimB == World.END) {
            return true;
        } else if(dimB==World.NETHER) {
            return dimA == World.OVERWORLD;
        }
        return false;
    }

    //ensures play variables are cleared when changing target phase
    public static void setTargetPhase(RegistryKey<World> dimension, int phase, MinecraftServer server) {
        if(!isAtTarget(dimension, phase)){
            onTargetChange(server);
            targetTickPhase = phase;
            targetDimension = dimension;
        }
    }

    private static void onTargetChange(MinecraftServer server){
        bedPlay = 0;
        bePlay = 0;
        bedEnd = 0;
        executedBeCount = 0;
        clearHighlights(server.getPlayerManager());
    }

    //debug
    public static void stackTraceVariables(){
        int targetTickPhase = Variables.targetTickPhase;
        RegistryKey<World> targetDimension = Variables.targetDimension;
        int currentTickPhase = Variables.currentTickPhase;
        RegistryKey<World> currentDimension = Variables.currentDimension;
        int i = 0;
    }

    public static void addHighlight(BlockPos pos, ServerWorld world, Formatting color){

        List<ServerPlayerEntity> players = world.getServer().getPlayerManager().getPlayerList();

        FallingBlockEntity entity = new FallingBlockEntity(world, (double)pos.getX()+.5, pos.getY()-1/48d, (double)pos.getZ()+.5, Blocks.BLUE_STAINED_GLASS.getDefaultState());

        entity.setNoGravity(true);
        entity.setGlowing(true);
        entity.setInvulnerable(true);
        entity.setCustomNameVisible(true);
        entity.setCustomName(new LiteralText("1").setStyle(Style.EMPTY.withColor(-1).withBold(true)));

        Packet<?> packet = entity.createSpawnPacket();
        EntityTrackerUpdateS2CPacket dataPacket = new EntityTrackerUpdateS2CPacket(entity.getId(), entity.getDataTracker(), true);

        for (ServerPlayerEntity player : players) {
            if (ServerNetworkHandler.isValidCarpetPlayer(player)) {
                player.networkHandler.sendPacket(packet);
                player.networkHandler.sendPacket(dataPacket);
            }
        }

        Team team = getTeam(color, world.getServer().getPlayerManager());
        team.getPlayerList().add(entity.getEntityName());

        world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(team, entity.getEntityName(), TeamS2CPacket.Operation.ADD));

        clientHighlights.add(new int[]{entity.getId(), Variables.frozenTickCount});
    }

    private static Team getTeam(Formatting color, PlayerManager players){
        if(teamsByColor.containsKey(color)){
            return teamsByColor.get(color);
        }

        Team team = new Team(dummyScoreboard, "\n" + color.getColorIndex());
        team.setColor(color);
        teamsByColor.put(color, team);
        players.sendToAll(TeamS2CPacket.updateTeam(team, true));
        return team;
    }

    public static double horizontalDistance(Vec3d srcPos, BlockPos block){
        double dx = srcPos.x-block.getX();
        double dz = srcPos.z-block.getZ();
        return Math.sqrt(dx*dx+dz*dz);
    }

    private static void sendAllCarpetPlayers(PlayerManager players, Packet<?> packet){
        for(ServerPlayerEntity player : players.getPlayerList()) {
            if (ServerNetworkHandler.isValidCarpetPlayer(player)) {
                player.networkHandler.sendPacket(packet);
            }
        }
    }

    public static void clearHighlights(PlayerManager players){
        for(Team team : teamsByColor.values()){
            for(String name : team.getPlayerList()){
                sendAllCarpetPlayers(players, TeamS2CPacket.changePlayerTeam(team, name, TeamS2CPacket.Operation.REMOVE));
            }
            sendAllCarpetPlayers(players, TeamS2CPacket.updateRemovedTeam(team));
        }
        teamsByColor.clear();

        EntitiesDestroyS2CPacket packet = new EntitiesDestroyS2CPacket();
        IntList ids = packet.getEntityIds();
        while (clientHighlights.size()>0){
            ids.add(clientHighlights.poll()[0]);
        }

        players.sendToAll(packet);

    }

    public static int frozenTickCount = 0;
    public static int playStart = 0;
    public static boolean inWorldTick = false;
}
