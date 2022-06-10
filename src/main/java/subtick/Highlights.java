package subtick;

import carpet.helpers.TickSpeed;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3i;
import subtick.progress.TickProgress;

import java.util.*;

/*What will highlights be used for:
    * Showing where events happened (white)
    * Showing the order events happened if multiple
    * Showing newly scheduled events (green)
    * Showing currently scheduled events (blue)
 */
public class Highlights {

    private static final Set<Vec3i> executed = new HashSet<>();
    private static final Set<Vec3i>[] newHighlights = new HashSet[TickProgress.NUM_PHASES];

    private static final Scoreboard dummyScoreboard = new Scoreboard();
    private static final Team[] teams;

    public static final int EXECUTED = 0, SCHEDULED = 1, NEW_EVENTS = 2, NONE = -1;

    public static int type;
    public static Map<Vec3i, FallingBlockEntity> currentShowed = new HashMap<>();

    private static final Block[] highlightStates = new Block[]{
            Blocks.WHITE_STAINED_GLASS,
            Blocks.BLUE_STAINED_GLASS,
            Blocks.GREEN_STAINED_GLASS
    };

    static {
        teams = new Team[]{
                new Team(dummyScoreboard, "executed"),
                new Team(dummyScoreboard, "scheduled"),
                new Team(dummyScoreboard, "new")
        };
        teams[0].setColor(Formatting.WHITE);
        teams[1].setColor(Formatting.BLUE);
        teams[2].setColor(Formatting.GREEN);
    }

    public static void executedHighlight(Vec3i pos, ServerWorld world){
        executed.add(pos);
        if(type==EXECUTED && TickSpeed.isPaused())
            showHighlight(pos, EXECUTED, "", world);
    }

    public static void newHighlight(Vec3i pos, int tickPhase, ServerWorld world){
        newHighlights[tickPhase].add(pos);
        if(type==NEW_EVENTS && TickSpeed.isPaused())
            showHighlight(pos, NEW_EVENTS, "", world);
    }

    public static void showExecuted(ServerWorld world){
        hideHighlights(world.getServer());
        Highlights.type = EXECUTED;
        world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(teams[type], true));

        for(Vec3i pos : executed){
            showHighlight(pos, EXECUTED, "", world);
        }
    }

    public static void showNew(ServerWorld world, int phase){
        hideHighlights(world.getServer());
        Highlights.type = NEW_EVENTS;
        world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(teams[type], true));
        if(phase==TickProgress.NUM_PHASES){
            for (int j = 0; j < TickProgress.NUM_PHASES; j++) {
                Set<Vec3i> highlights = newHighlights[j];
                for(Vec3i pos : highlights){
                    showHighlight(pos, type, "", world);
                }
            }
        } else {
            for(Vec3i pos : newHighlights[phase]){
                showHighlight(pos, type, "", world);
            }
        }
    }

    public static void showScheduled(MinecraftServer server, int phase){
        hideHighlights(server);
        Highlights.type = SCHEDULED;
        server.getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(teams[type], true));
    }

    //to clear highlights when stepping or unfreezing
    public static void clearHighlights(MinecraftServer server){
        hideHighlights(server);
        executed.clear();
        for(Set<Vec3i> list : newHighlights){
            list.clear();
        }
    }

    public static void showNone(ServerWorld world) {
        hideHighlights(world.getServer());
        Highlights.type = NONE;
    }

    private static void hideHighlights(MinecraftServer server){
        if(type==NONE){
            return;
        }
        Team team = teams[type];
        for(String name : team.getPlayerList()){
            server.getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(team, name, TeamS2CPacket.Operation.REMOVE));
        }
        EntitiesDestroyS2CPacket destroyPacket = new EntitiesDestroyS2CPacket();
        IntList ids = destroyPacket.getEntityIds();
        for(FallingBlockEntity highlight : currentShowed.values()){
            ids.add(highlight.getId());
        }
        currentShowed.clear();
        server.getPlayerManager().sendToAll(destroyPacket);
    }

    private static void hideHighlight(Vec3i pos, MinecraftServer server){
        if(type==NONE){
            return;
        }
        if(!currentShowed.containsKey(pos)){
            return;
        }

        FallingBlockEntity highlight = currentShowed.get(pos);
        Team team = teams[type];
        EntitiesDestroyS2CPacket destroyPacket = new EntitiesDestroyS2CPacket();
        IntList ids = destroyPacket.getEntityIds();
        ids.add(highlight.getId());
        currentShowed.remove(pos);

        server.getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(team, highlight.getEntityName(), TeamS2CPacket.Operation.REMOVE));
        server.getPlayerManager().sendToAll(destroyPacket);
    }

    private static void showHighlight(Vec3i pos, int type, String name, ServerWorld world){
        if(currentShowed.containsKey(pos)){
            return;
        }
        Block block = highlightStates[type];
        FallingBlockEntity highlight = new FallingBlockEntity(world, (double)pos.getX()+0.5, pos.getY()-1/48d, (double)pos.getZ()+.5, block.getDefaultState());
        highlight.setNoGravity(true);
        highlight.setGlowing(true);
        highlight.setInvulnerable(true);
        highlight.setCustomNameVisible(true);
        if(name != null && !name.isEmpty()) {
            highlight.setCustomName(new LiteralText(name).setStyle(Style.EMPTY.withColor(-1).withBold(true)));
        }

        Packet<?> spawnPacket = highlight.createSpawnPacket();
        EntityTrackerUpdateS2CPacket dataPacket = new EntityTrackerUpdateS2CPacket(highlight.getId(), highlight.getDataTracker(), true);
        List<ServerPlayerEntity> players = world.getPlayers();

        for(ServerPlayerEntity player : players){
            player.networkHandler.sendPacket(spawnPacket);
            player.networkHandler.sendPacket(dataPacket);
        }

        teams[type].getPlayerList().add(highlight.getEntityName());
        world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(teams[type], highlight.getEntityName(), TeamS2CPacket.Operation.ADD));
        currentShowed.put(pos, highlight);
    }

    public static void reset() {
        type = 0;
        executed.clear();
        for (int i = 0; i < TickProgress.NUM_PHASES; i++) {
            newHighlights[i].clear();
        }
    }
}