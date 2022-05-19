package subtick.progress;

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

    public static final boolean DEBUG = false;

    public static final int TILE_TICKS = 0,
            LIQUID_TICKS = 1,
            RAIDS = 2,
            BLOCK_EVENTS = 3,
            ENTITIES = 4,
            TILE_ENTITIES = 5,
            NUM_PHASES = 6;

    public static final int
            OVERWORLD = 0,         //0
            END = NUM_PHASES,      //6
            NETHER = NUM_PHASES*2, //12
            NONE = -1;

    public static final int POST_TICK = NUM_PHASES*3;
    public static final int PRE_TICK = -1;

    public static int currentProgress = PRE_TICK, targetProgress = POST_TICK;

    private static final Map<RegistryKey<World>, Integer> dimensionCodes;
    private static final RegistryKey<World>[] dimensions;

    public static final String[] dimensionNames = new String[]{"Overworld", "End", "Nether",};
    public static final String[] tickPhaseNames = new String[]{"Tile Ticks", "Liquid Ticks", "Raids", "Block Events", "Entities", "Tile Entities"};

    static {
        dimensionCodes = new HashMap<>();
        dimensionCodes.put(ServerWorld.OVERWORLD, OVERWORLD);
        dimensionCodes.put(ServerWorld.END, END);
        dimensionCodes.put(ServerWorld.NETHER, NETHER);
        dimensions = new RegistryKey[]{
                ServerWorld.OVERWORLD,
                ServerWorld.END,
                ServerWorld.NETHER
        };
    }

    public static int getDimension(int progress){
        return (progress== PRE_TICK || progress== POST_TICK) ? NONE : ((progress/NUM_PHASES)*NUM_PHASES);
    }

    public static int getTickPhase(int progress){
        return progress== PRE_TICK || progress== POST_TICK ? NONE : (progress%NUM_PHASES);
    }

    public static int progressOf(int tickPhase, int dimension){
        return dimension+tickPhase;
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
        return progress== PRE_TICK ?"Pre Tick":
                progress== POST_TICK ?"Post Tick":
                        (tickPhaseNames[getTickPhase(progress)] + " in the " + dimensionNames[getDimension(progress)/NUM_PHASES]);
    }

    public static void setTarget(int phase, int dimension){
        targetProgress = progressOf(phase, dimension);
    }

    public static void setTarget(int progress){
        targetProgress = progress;
    }

    public static void setCurrent(int progress){
        currentProgress = progress;
    }

    public static void reset(){
        targetProgress = POST_TICK;
        currentProgress = PRE_TICK;
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
        int ret =  update(phase, dim(dimension));
        if(DEBUG) {
            System.out.println(overallStatus() + ", immediate: \"" + progressName(progressOf(phase, dim(dimension))));
        }
        return ret;
    }

    public static int update(int phase, int dimension){
        int progress = progressOf(phase, dimension);
        return update(progress);
    }

    public static int update(int progress){
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

    public static String overallStatus(){
        return "Target: \"" + progressName(targetProgress) + "\", Current: \"" + progressName(currentProgress) + "\", run status: " + runStatus;
    }
}