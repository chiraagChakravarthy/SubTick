package subtick;

import carpet.settings.Rule;

public class SubTickSettings {
    @Rule(desc = "tells carpet clients the game isn't frozen, allowing piston animations to play immediately", category = "SUBTICK")
    public static boolean vanillaPistonAnimations = false;

    @Rule(desc = "changes one line of code that makes lossless bedrock breaking possible again (barely)", category = "SUBTICK")
    public static boolean losslessBedrockBreaking = false;

    @Rule(desc = "skip stepping over events executed on the wrong block", category = "SUBTICK")
    public static boolean skipInvalidEvents = true;
}