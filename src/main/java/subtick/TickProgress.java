package subtick;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

/*TODO brief list of plans
    * subtick step <n>
    * subtick step tt/lt/rd/be/bed/en/te <n>
    * subtick play <rate> <n>
    * subtick play tt/be/bed/en/te <rate> <n>
    * subtick log tt/lt/be/en/te <n>
    * subtick log clear
    * subtick when/list

 */

public class TickProgress {

    public static final int PRE_TICK = 0,
            TILE_TICKS = 1,
            LIQUID_TICKS = 2,
            RAIDS = 3,
            BLOCK_EVENTS = 4,
            ENTITIES = 5,
            TILE_ENTITIES = 6,
            NUM_PHASES = 7;

    public static final int OVERWORLD = 0,
            NETHER = NUM_PHASES,
            END = NUM_PHASES*2,
            NONE = -1;

    public static final int TICK_END = NUM_PHASES*3;
    public static final int PLAYERS = -1;

    public static int currentProgress = PLAYERS, targetProgress = TICK_END;

    private static final Map<RegistryKey<World>, Integer> dimensionCodes;
    private static final RegistryKey<World>[] dimensions;

    public static final String[] dimensionNames = new String[]{"Overworld", "Nether", "End"};
    public static final String[] tickPhaseNames = new String[]{"Pre-tick, Tile Ticks", "Block Events", "Entities", "Tile Entities", "Post-tick"};

    static {
        dimensionCodes = new HashMap<>();
        dimensionCodes.put(ServerWorld.OVERWORLD, OVERWORLD);
        dimensionCodes.put(ServerWorld.NETHER, NETHER);
        dimensionCodes.put(ServerWorld.END, END);
        dimensions = new RegistryKey[]{
                ServerWorld.OVERWORLD,
                ServerWorld.NETHER,
                ServerWorld.END
        };
    }

    /*
   -1: players

    0: pretick in overworld
    1: tile ticks in overworld
    2: block events in overworld
    3: entities in overworld
    4: tile entities in overworld
    5: post tick in overworld

    6: pretick in overworld
    7: tile ticks in nether
    8: block events in nether
    9: entities in nether
    10: tile entities in nether
    11: post tick in overworld

    12: pretick in overworld
    13: tile ticks in end
    14: block events in end
    15: entities in end
    16: tile entities in end
    17: post tick in overworld

    18: tick end
     */

    public static int getDimension(int progress){
        return progress==PLAYERS || progress==TICK_END ? NONE : progress/NUM_PHASES;
    }

    public static int getTickPhase(int progress){
        return progress==PLAYERS || progress==TICK_END ? NONE : progress%NUM_PHASES;
    }

    public static int progressOf(int tickPhase, int dimension){
        return dimension*NUM_PHASES+tickPhase;
    }

    public static int progressOf(int tickPhase, RegistryKey<World> dimension){
        return progressOf(tickPhase, dim(dimension));
    }

    public static int dim(RegistryKey<World> dimension){
        return dimensionCodes.get(dimension);
    }

    public static RegistryKey<World> dim(int dimension){
        return dimensions[dimension];
    }

    public static String progressName(int progress){
        return progress==PLAYERS?"Players":
                progress==TICK_END?"Tick End":
                        tickPhaseNames[getTickPhase(progress)] + " in the " + dimensionNames[getDimension(progress)];
    }

    public static void setTarget(int phase, int dimension){
        targetProgress = progressOf(phase, dimension);
    }

    public static void reset(){
        targetProgress = TICK_END;
        currentProgress = PLAYERS;
        runStatus = RUN_COMPLETELY;
    }

    /*
    RUN_COMPLETELY: call the default minecraft code for the tick phase (ensures lithium is left untouched)
    STEP_TO_FINISH: run subtick code to the end of the tick phase
    STEP_FROM_START: run subtick code for a tick phase, including the beginning part
    STEP: run only the stepping event code
    NO_RUN: skip this tick phase
     */

    public static final int RUN_COMPLETELY = 0,
            STEP_TO_FINISH = 1,
            STEP_FROM_START = 2,
            STEP = 3,
            NO_RUN = 4;

    private static int runStatus = 0;

    public static int update(int phase, RegistryKey<World> dimension){
        return update(phase, dim(dimension));
    }

    public static int update(int phase, int dimension){
        int progress = progressOf(phase, dimension);

        if(progress < currentProgress || progress > targetProgress){
            runStatus = NO_RUN;
            return runStatus;
        }

        if(progress == currentProgress){
            if(currentProgress < targetProgress){
                runStatus = STEP_TO_FINISH;
            } else {
                runStatus = STEP;
            }
            return runStatus;
        }

        currentProgress = progress;
        if(progress < targetProgress){
            runStatus = RUN_COMPLETELY;
        } else {
            runStatus = STEP_FROM_START;
        }
        return runStatus;
    }

    public static int runStatus() {
        return runStatus;
    }
}