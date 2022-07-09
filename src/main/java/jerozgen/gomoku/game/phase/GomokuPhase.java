package jerozgen.gomoku.game.phase;

import jerozgen.gomoku.game.GomokuGame;
import xyz.nucleoid.plasmid.game.GameActivity;

public abstract class GomokuPhase {
    protected final GomokuGame game;

    protected GomokuPhase(GomokuGame game) {
        this.game = game;
    }

    public final void setup(GameActivity activity) {
        game.setup(activity);
        setupPhase(activity);
    }

    protected abstract void setupPhase(GameActivity activity);
}
