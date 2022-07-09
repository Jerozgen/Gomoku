package jerozgen.gomoku.game.phase;

import jerozgen.gomoku.game.GomokuGame;
import jerozgen.gomoku.game.GomokuTexts;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;

import java.util.List;

public class GomokuWaitingPhase extends GomokuPhase {
    public GomokuWaitingPhase(GomokuGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        var blockCount = game.config().blocks().stream().mapToInt(List::size).sum();
        var playerConfig = game.config().playerConfig();
        var newPlayerConfig = new PlayerConfig(
                Math.min(playerConfig.minPlayers(), blockCount),
                Math.min(playerConfig.maxPlayers(), blockCount),
                Math.min(playerConfig.thresholdPlayers(), blockCount),
                playerConfig.countdown());
        GameWaitingLobby.addTo(activity, newPlayerConfig);

        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
        activity.listen(GameActivityEvents.REQUEST_START, this::requestStart);
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.accept(game.world(), game.board().spawnPos()).and(() -> {
            offer.player().sendMessage(GomokuTexts.description(game), false);
            offer.player().changeGameMode(GameMode.ADVENTURE);
        });
    }

    private GameResult requestStart() {
        var activePhase = new GomokuActivePhase(game);
        game.gameSpace().setActivity(activePhase::setup);
        return GameResult.ok();
    }
}
