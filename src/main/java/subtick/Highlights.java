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

    public static Map<Vec3i, FallingBlockEntity> currentShowed = new HashMap<>();

    public static boolean showingExecuted = true;
    public static boolean[] showingNew = new boolean[TickProgress.NUM_PHASES];

    public static final Team EXECUTED_AND_NEW = new Team(dummyScoreboard, "exec and new");

    static {
        teams = new Team[]{
                new Team(dummyScoreboard, "executed"),
                new Team(dummyScoreboard, "scheduled"),
                new Team(dummyScoreboard, "new"),
                EXECUTED_AND_NEW
        };
        teams[0].setColor(Formatting.WHITE);
        teams[1].setColor(Formatting.BLUE);
        teams[2].setColor(Formatting.DARK_GREEN);
        EXECUTED_AND_NEW.setColor(Formatting.GREEN);

        for (int i = 0; i < newHighlights.length; i++) {
            newHighlights[i] = new HashSet<>();
        }
    }

    public static void executedHighlight(Vec3i pos, ServerWorld world){
        executed.add(pos);
        if(showingExecuted && TickSpeed.isPaused())
            showHighlight(pos, EXECUTED, "", world);
    }

    public static void newHighlight(Vec3i pos, int tickPhase, ServerWorld world){
        newHighlights[tickPhase].add(pos);
        if(showingNew[tickPhase] && TickSpeed.isPaused())
            showHighlight(pos, NEW_EVENTS, "", world);
    }

    public static void toggleShowExecuted(ServerWorld world){
        if(showingExecuted){
            showingExecuted = false;
            for (Vec3i pos : executed) {
                hideHighlight(pos, world.getServer());
            }
        } else {
            showingExecuted = true;
            world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(teams[EXECUTED], true));

            for (Vec3i pos : executed) {
                showHighlight(pos, EXECUTED, "", world);
            }
        }
    }

    public static void toggleShowNew(ServerWorld world, int phase){
        if(phase==TickProgress.NUM_PHASES){
            boolean anyFalse = false;
            for (boolean b : showingNew) {
                if (!b) {
                    anyFalse = true;
                    break;
                }
            }
            if(anyFalse){
                world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(teams[NEW_EVENTS], true));
                for (int i = 0; i < showingNew.length; i++) {
                    if(!showingNew[i]){
                        showingNew[i] = true;
                        Set<Vec3i> highlights = newHighlights[i];
                        for(Vec3i pos : highlights){
                            showHighlight(pos, NEW_EVENTS, "", world);
                        }
                    }
                }
            } else {
                for (int i = 0; i < showingNew.length; i++) {
                    if(showingNew[i]){
                        showingNew[i] = false;
                        Set<Vec3i> highlights = newHighlights[i];

                        for(Vec3i pos : highlights){
                            hideHighlight(pos, world.getServer());
                        }
                    }
                }
            }

        } else {
            if(showingNew[phase]){
                showingNew[phase] = false;
                for (Vec3i pos : newHighlights[phase]) {
                    hideHighlight(pos, world.getServer());
                }
            } else {
                showingNew[phase] = true;
                for (Vec3i pos : newHighlights[phase]) {
                    showHighlight(pos, NEW_EVENTS, "", world);
                }
            }

        }
    }

    //to clear highlights when stepping or unfreezing
    public static void clearHighlights(MinecraftServer server){
        hideAllHighlights(server);
        executed.clear();
        for(Set<Vec3i> list : newHighlights){
            list.clear();
        }
    }

    public static void showNone(ServerWorld world) {
        hideAllHighlights(world.getServer());
        showingExecuted = false;
        Arrays.fill(showingNew, false);
    }

    private static void hideAllHighlights(MinecraftServer server){
        for(Team team : teams) {
            for (String name : team.getPlayerList()) {
                server.getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(team, name, TeamS2CPacket.Operation.REMOVE));
            }
            team.getPlayerList().clear();
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
        if(!currentShowed.containsKey(pos)){
            return;
        }

        FallingBlockEntity highlight = currentShowed.get(pos);
        for(Team team : teams){
            if(team.getPlayerList().contains(highlight.getEntityName())){
                server.getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(team, highlight.getEntityName(), TeamS2CPacket.Operation.REMOVE));
                break;
            }
        }

        EntitiesDestroyS2CPacket destroyPacket = new EntitiesDestroyS2CPacket();
        IntList ids = destroyPacket.getEntityIds();
        ids.add(highlight.getId());
        currentShowed.remove(pos);

        server.getPlayerManager().sendToAll(destroyPacket);
    }

    private static void showHighlight(Vec3i pos, int type, String name, ServerWorld world){
        if(!TickSpeed.isPaused()){
            return;
        }
        Team team = teams[type];

        if(currentShowed.containsKey(pos)){
            String otherName = currentShowed.get(pos).getEntityName();
            if((type==EXECUTED && teams[NEW_EVENTS].getPlayerList().contains(otherName)) || (type==NEW_EVENTS && teams[EXECUTED].getPlayerList().contains(otherName))){
                hideHighlight(pos, world.getServer());
                team = EXECUTED_AND_NEW;
            } else {
                return;
            }
        }
        FallingBlockEntity highlight = new FallingBlockEntity(world, pos.getX()+.5, pos.getY()-1/48d, pos.getZ()+.5, Blocks.GLASS.getDefaultState());
        highlight.setNoGravity(true);
        highlight.setGlowing(true);
        highlight.setInvulnerable(true);
        highlight.setInvisible(false);
        if(name != null && !name.isEmpty()) {
            highlight.setCustomNameVisible(true);
            highlight.setCustomName(new LiteralText(name).setStyle(Style.EMPTY.withColor(-1).withBold(true)));
        }

        Packet<?> spawnPacket = highlight.createSpawnPacket();
        EntityTrackerUpdateS2CPacket dataPacket = new EntityTrackerUpdateS2CPacket(highlight.getId(), highlight.getDataTracker(), true);
        List<ServerPlayerEntity> players = world.getPlayers();

        for(ServerPlayerEntity player : players){
            player.networkHandler.sendPacket(spawnPacket);
            player.networkHandler.sendPacket(dataPacket);
        }

        team.getPlayerList().add(highlight.getEntityName());
        world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.updateTeam(team, true));
        world.getServer().getPlayerManager().sendToAll(TeamS2CPacket.changePlayerTeam(team, highlight.getEntityName(), TeamS2CPacket.Operation.ADD));
        currentShowed.put(pos, highlight);
    }

    public static void reset() {
        executed.clear();
        for (int i = 0; i < TickProgress.NUM_PHASES; i++) {
            newHighlights[i].clear();
        }
        Arrays.fill(showingNew, false);
        showingExecuted = true;
    }
}