package subtick;

import carpet.settings.Rule;

public class SubTickSettings {
    @Rule(desc = "step through block events executed on the wrong block (these will never do anything)", category = "SUBTICK")
    public static boolean includeInvalidBlockEvents = false;

    @Rule(desc = "tells carpet clients the game isn't frozen, allowing piston animations to play immediately", category = "SUBTICK")
    public static boolean vanillaPistonAnimations = false;

    @Rule(desc = "show where block events occur", category = "SUBTICK")
    public static boolean highlightBlockEvents = true;
}