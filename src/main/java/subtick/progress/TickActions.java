package subtick.progress;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3i;
import subtick.Highlights;
import subtick.SubTickSettings;
import subtick.commands.HighlightCommand;

import java.util.Arrays;

public class TickActions {
    public static final int ANYTHING = -1;
    public static final int DEFAULT = 0;

    public static boolean ttSuccess = false;//previous tt was executed on the right block
    public static ServerWorld tempWorld = null;
    public static int[] bedCount = new int[3];

    /*TODO
        used for additional granularity within a tick phase (if needed)
        block events:
            be: 0
            bed: 1
        tile ticks:
            tt: 0
            priority: 1
     */
    public static int action;

    public static int numActionsStep;//for this tick

    public static int numActionsPlay;//for playing actions
    public static int ticksPerStep;//number of ticks between played actions

    public static int tickCount = 0;//tracking real time

    public static ServerCommandSource actor = null;


    public static void tick(){
        if(numActionsPlay * ticksPerStep >= tickCount){
            if(ticksPerStep == 0){
                numActionsStep = numActionsPlay;
            } else if(tickCount % ticksPerStep == 0){
                numActionsStep++;
            }
            tickCount++;
        }
    }

    public static void finishPlaying(){
        tickCount = numActionsPlay * ticksPerStep + 1;
    }

    public static boolean stillPlaying(){
        return numActionsPlay != 0 && numActionsPlay * ticksPerStep >= tickCount;
    }

    public static void play(int numActions, int ticksPerPlay, ServerCommandSource actor){
        play(numActions, ticksPerPlay, actor, 0);
    }

    public static void play(int numActions, int ticksPerPlay, ServerCommandSource actor, int action){
        TickActions.ticksPerStep = ticksPerPlay;
        TickActions.action = action;
        TickActions.actor = actor;
        numActionsPlay = numActions;
        tickCount = 0;
        Highlights.clearHighlights(actor.getServer());
    }

    public static void reset() {
        action = 0;
        numActionsPlay = 0;
        numActionsStep = 0;
        tickCount = 0;
        actor = null;
        ticksPerStep = 0;
        Arrays.fill(bedCount, 0);
    }

    public static boolean inRange(Vec3i v){
        if(actor==null){
            return false;
        }
        double dx = v.getX()-actor.getPosition().getX(),
                dz = v.getZ()-actor.getPosition().getZ();
        return Math.sqrt(dx*dx+dz*dz)< SubTickSettings.maxEventRadius;
    }
}
