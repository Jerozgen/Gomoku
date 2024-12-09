package jerozgen.gomoku;

import jerozgen.gomoku.game.GomokuConfig;
import jerozgen.gomoku.game.GomokuGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Gomoku implements ModInitializer {
    public static final String ID = "gomoku";

    @Override
    public void onInitialize() {
        GameType.register(Identifier.of(ID, "gomoku"), GomokuConfig.CODEC, GomokuGame::open);
    }
}
