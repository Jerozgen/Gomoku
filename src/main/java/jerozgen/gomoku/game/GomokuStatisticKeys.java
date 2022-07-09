package jerozgen.gomoku.game;

import jerozgen.gomoku.Gomoku;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.stats.StatisticKey;

public final class GomokuStatisticKeys {
    // global
    public static final StatisticKey<Float> TIME_ELAPSED = StatisticKey.floatKey(id("time_elapsed"));
    public static final StatisticKey<Integer> PLAYERS = StatisticKey.intKey(id("players")).hidden(true);

    // per-player
    public static final StatisticKey<Float> TIME_SPENT = StatisticKey.floatKey(id("time_spent"));
    public static final StatisticKey<Integer> GAMES_WITH_A_DRAW =  StatisticKey.intKey(id("games_with_a_draw"));
    public static final StatisticKey<Integer> BLOCKS_PLACED = StatisticKey.intKey(id("blocks_placed"));

    private static Identifier id(String path) {
        return new Identifier(Gomoku.ID, path);
    }

    private GomokuStatisticKeys() {}
}
