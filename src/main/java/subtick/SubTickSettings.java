package subtick;

import carpet.settings.Rule;

public class SubTickSettings {
    @Rule(desc = "tells carpet clients the game isn't frozen, allowing piston animations to play immediately", category = "SUBTICK")
    public static boolean vanillaPistonAnimations = false;

    @Rule(desc = "skip stepping over events executed on the wrong block", category = "SUBTICK")
    public static boolean skipInvalidEvents = true;

    @Rule(desc = "max radius around the player where block events will be stepped over", category = "SUBTICK")
    public static int maxEventRadius = 200;

    @Rule(desc = "prints the coordinates of events when stepping over a single event", category = "SUBTICK")
    public static boolean printEventCoords;
}