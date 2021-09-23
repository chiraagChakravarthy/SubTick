package subtick;

import carpet.settings.Rule;

public class SubTickSettings {
    @Rule(desc = "step through block events executed on the wrong block (these will never do anything)", category = "SUBTICK")
    public static boolean includeInvalidBlockEvents = false;

    @Rule(desc = "tells carpet clients the game isn't frozen, allowing piston animations to play immediately", category = "SUBTICK")
    public static boolean vanillaPistonAnimations = false;

    @Rule(desc = "show where block events occur", category = "SUBTICK")
    public static boolean highlightBlockEvents = true;

    @Rule(desc = "maximum horizontal distance from the player block events will be stepped through", category = "SUBTICK")
    public static int beRadius = 200;

    @Rule(desc = "changes one line of code that makes lossless bedrock breaking possible again (barely)", category = "SUBTICK")
    public static boolean losslessBedrockBreaking = false;
}