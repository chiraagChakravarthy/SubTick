package subtick.variables;

public enum Phase {
    TICK_FREEZE("Tick Freeze"),
    BLOCK_EVENTS("Block Events"),
    POST_TICK("");

    final String plural;
    Phase(String plural) {this.plural = plural;}

    boolean isBefore(Phase other) {return ordinal() < other.ordinal();}
    public String plural() {return plural;}
}
