package jerozgen.gomoku.game;

import jerozgen.gomoku.game.board.Board;
import jerozgen.gomoku.game.phase.GomokuWaitingPhase;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.api.game.GameActivity;
import xyz.nucleoid.plasmid.api.game.GameOpenContext;
import xyz.nucleoid.plasmid.api.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.api.game.GameSpace;
import xyz.nucleoid.plasmid.api.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.api.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.api.game.stats.GameStatisticBundle;
import xyz.nucleoid.plasmid.api.game.world.generator.TemplateChunkGenerator;

import java.util.Collections;
import java.util.function.Consumer;

public record GomokuGame(GomokuConfig config, Board board, GameSpace gameSpace, ServerWorld world,
                         GameStatisticBundle statistics) {
    public static GameOpenProcedure open(GameOpenContext<GomokuConfig> context) {
        var config = context.config();
        var board = new Board(config.board());
        var template = board.template();
        var generator = new TemplateChunkGenerator(context.server(), template);
        var worldConfig = new RuntimeWorldConfig().setGenerator(generator).setTimeOfDay(6000);
        return context.openWithWorld(worldConfig, (activity, world) -> {
            var gameSpace = activity.getGameSpace();
            var namespace = config.statisticBundleNamespace();
            var statistics = namespace.map(value -> gameSpace.getStatistics().bundle(value)).orElse(null);
            var game = new GomokuGame(config, board, gameSpace, world, statistics);
            activity.listen(GameActivityEvents.CREATE, () -> {
                var waitingPhase = new GomokuWaitingPhase(game);
                game.gameSpace.setActivity(waitingPhase::setup);
            });
        });
    }

    public void setup(GameActivity activity) {
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.deny(GameRuleType.BLOCK_DROPS);
        activity.deny(GameRuleType.CRAFTING);
        activity.deny(GameRuleType.FALL_DAMAGE);
        activity.deny(GameRuleType.HUNGER);
        activity.deny(GameRuleType.PORTALS);
        activity.deny(GameRuleType.PVP);
    }

    private void tick() {
        for (var player : gameSpace.getPlayers())
            if (player.getY() < 0) spawn(player);
    }

    public void spawn(ServerPlayerEntity player) {
        var spawnPos = board.spawnPos();
        player.teleport(world, spawnPos.x, spawnPos.y, spawnPos.z, Collections.emptySet(), player.getYaw(), player.getPitch(), true);
    }

    public void stat(Consumer<GameStatisticBundle> consumer) {
        if (statistics != null) consumer.accept(statistics);
    }
}
