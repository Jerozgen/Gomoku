package jerozgen.gomoku.game.phase;

import jerozgen.gomoku.game.GomokuGame;
import jerozgen.gomoku.game.GomokuStatisticKeys;
import jerozgen.gomoku.game.GomokuTexts;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.plasmid.game.stats.StatisticKeys;
import xyz.nucleoid.stimuli.event.block.BlockUseEvent;
import xyz.nucleoid.stimuli.event.entity.EntityUseEvent;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GomokuActivePhase extends GomokuPhase {
    private static final long REQUEST_DURATION = 30 * 1000;

    private final Map<ServerPlayerEntity, Block> playerToBlock = new HashMap<>();
    private final LinkedList<ServerPlayerEntity> queue = new LinkedList<>();
    private final Map<ServerPlayerEntity, Map<ServerPlayerEntity, Long>> swapRequests = new HashMap<>();

    private int currentMovePlaces;
    private long moveStartTime;
    private int moveSeconds;

    private long startTime;

    public GomokuActivePhase(GomokuGame game) {
        super(game);
    }

    @Override
    protected void setupPhase(GameActivity activity) {
        activity.listen(GameActivityEvents.ENABLE, this::start);
        activity.listen(GameActivityEvents.TICK, this::tick);
        activity.listen(GamePlayerEvents.OFFER, this::offerPlayer);
        activity.listen(GamePlayerEvents.LEAVE, this::removePlayer);
        activity.listen(BlockUseEvent.EVENT, this::onBlockUse);
        activity.listen(EntityUseEvent.EVENT, this::onEntityUse);

        activity.deny(GameRuleType.THROW_ITEMS);
        activity.deny(GameRuleType.MODIFY_INVENTORY);
        activity.deny(GameRuleType.SWAP_OFFHAND);
    }

    private void start() {
        var players = game.gameSpace().getPlayers().stream().collect(Collectors.toList());
        Collections.shuffle(players);
        var blocks = game.config().blocks().stream()
                .map(ArrayList::new)
                .flatMap(list -> {
                    Collections.shuffle(list);
                    return list.stream();
                }).iterator();
        playerToBlock.putAll(players.stream().collect(Collectors.toMap(Function.identity(), __ -> blocks.next())));
        queue.addAll(players);
        currentMovePlaces = game.config().firstMovePlaces();
        startTime = Util.getMeasuringTimeMs();
        startTurn(currentPlayer());
    }

    private void tick() {
        for (var player : queue) {
            var hit = player.raycast(8, 0, false);
            player.sendMessage(game.board().raycastText(hit), true);
        }
        updateTimer(currentPlayer());
    }

    private PlayerOfferResult offerPlayer(PlayerOffer offer) {
        return offer.accept(game.world(), game.board().spawnPos()).and(() -> {
            offer.player().changeGameMode(GameMode.SPECTATOR);
        });
    }

    private void removePlayer(ServerPlayerEntity player) {
        if (player.equals(currentPlayer())) nextTurn();
        player.changeGameMode(GameMode.SPECTATOR);
        queue.remove(player);
        playerToBlock.remove(player);
        swapRequests.remove(player);
        swapRequests.forEach((__, requests) -> requests.remove(player));
        game.stat(statistics -> statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1));
        game.stat(statistics -> statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1));
        if (queue.size() == 1) win(currentPlayer());
    }

    private ActionResult onBlockUse(ServerPlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!player.equals(currentPlayer())) return ActionResult.PASS;
        if (hand != Hand.OFF_HAND) return ActionResult.PASS;
        var pos = hit.getBlockPos();
        if (pos.getY() != 0 || game.board().isOut(pos.getX(), pos.getZ())) return ActionResult.PASS;

        switch (game.board().block(pos.getX(), pos.getZ(), playerToBlock.get(player), game.world())) {
            case WIN -> win(player);
            case DRAW -> draw();
            case SUCCESS -> {
                game.world().playSound(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
                        playerToBlock.get(player).getDefaultState().getSoundGroup().getPlaceSound(),
                        SoundCategory.BLOCKS, 1, 1);
                game.stat(statistics -> statistics.forPlayer(player).increment(GomokuStatisticKeys.BLOCKS_PLACED, 1));
                currentMovePlaces -= 1;
                if (currentMovePlaces <= 0) nextTurn();
            }
            case FAIL -> {
                return ActionResult.PASS;
            }
        }
        return ActionResult.SUCCESS;
    }

    private ActionResult onEntityUse(ServerPlayerEntity player, Entity entity, Hand hand, EntityHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!player.isSneaking()) return ActionResult.PASS;
        if (hit == null) return ActionResult.PASS;
        if (hit.getEntity().getType() != EntityType.PLAYER) return ActionResult.PASS;
        if (!queue.contains(player)) return ActionResult.PASS;
        var target = (ServerPlayerEntity) hit.getEntity();
        if (!queue.contains(target)) return ActionResult.PASS;

        // accept request
        var targetRequests = swapRequests.get(target);
        if (targetRequests != null && targetRequests.getOrDefault(player, Long.MAX_VALUE) < Util.getMeasuringTimeMs() + REQUEST_DURATION) {
            targetRequests.remove(player);
            var playerIndex = queue.indexOf(player);
            var targetIndex = queue.indexOf(target);
            var playerBlock = playerToBlock.get(player);
            var targetBlock = playerToBlock.get(target);
            if (playerIndex == 0 || targetIndex == 0) endTurn(currentPlayer());
            queue.set(playerIndex, target);
            queue.set(targetIndex, player);
            playerToBlock.put(target, playerBlock);
            playerToBlock.put(player, targetBlock);
            game.gameSpace().getPlayers().sendMessage(GomokuTexts.swap(player, target));
            if (playerIndex == 0 || targetIndex == 0) startTurn(currentPlayer());
            return ActionResult.SUCCESS;
        }

        // send request
        var requests = swapRequests.computeIfAbsent(player, __ -> new HashMap<>());
        var previousTime = requests.put(target, Util.getMeasuringTimeMs());
        if (previousTime == null || Util.getMeasuringTimeMs() - previousTime > REQUEST_DURATION) {
            player.sendMessage(GomokuTexts.swapRequest$toExecutor(target), false);
            target.sendMessage(GomokuTexts.swapRequest$toTarget(player), false);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    private void win(ServerPlayerEntity winner) {
        var players = game.gameSpace().getPlayers();
        players.sendMessage(GomokuTexts.win(winner));
        players.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP);
        game.stat(statistics -> {
            for (var player : queue) {
                if (player.equals(winner)) continue;
                statistics.forPlayer(player).increment(StatisticKeys.GAMES_LOST, 1);
            }
            statistics.forPlayer(winner).increment(StatisticKeys.GAMES_WON, 1);
        });
        end();
    }

    private void draw() {
        var players = game.gameSpace().getPlayers();
        players.sendMessage(GomokuTexts.draw());
        players.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP);
        game.stat(statistics -> {
            for (var player : queue) {
                statistics.forPlayer(player).increment(GomokuStatisticKeys.GAMES_WITH_A_DRAW, 1);
            }
        });
        end();
    }

    private void end() {
        if (currentPlayer() != null) endTurn(currentPlayer());
        var timeElapsed = (float) ((Util.getMeasuringTimeMs() - startTime) / 100) / 10;
        game.stat(statistics -> {
            for (var player : queue) {
                statistics.forPlayer(player).increment(StatisticKeys.GAMES_PLAYED, 1);
            }
            statistics.global().set(GomokuStatisticKeys.TIME_ELAPSED, timeElapsed);
        });
        var endingPhase = new GomokuEndingPhase(game);
        game.gameSpace().setActivity(endingPhase::setup);
    }

    private ServerPlayerEntity currentPlayer() {
        return queue.peek();
    }

    private void nextTurn() {
        endTurn(currentPlayer());
        queue.add(queue.remove());
        currentMovePlaces = game.config().eachMovePlaces();
        startTurn(currentPlayer());
    }

    private void endTurn(ServerPlayerEntity player) {
        player.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        var timeSpent = (float) ((Util.getMeasuringTimeMs() - moveStartTime) / 100) / 10;
        game.stat(statistics -> statistics.forPlayer(player).increment(GomokuStatisticKeys.TIME_SPENT, timeSpent));
        clearTimer(player);
    }

    private void startTurn(ServerPlayerEntity player) {
        moveStartTime = Util.getMeasuringTimeMs();
        moveSeconds = game.config().moveDuration();
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1, 1);
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1, 1);
        player.setStackInHand(Hand.OFF_HAND, playerToBlock.get(player).asItem().getDefaultStack());
        updateTimer(player);
    }

    private void updateTimer(ServerPlayerEntity player) {
        var time = Util.getMeasuringTimeMs();
        var moveDuration = game.config().moveDuration() * 1000;
        var progress = 1f - (float) (time - moveStartTime) / moveDuration;
        var seconds = (int) Math.max(moveDuration - (time - moveStartTime), 0) / 1000;

        player.experienceProgress = progress;
        player.setExperienceLevel(seconds);

        if (moveSeconds != seconds) {
            if (moveSeconds == -1) {
                game.gameSpace().getPlayers().sendMessage(GomokuTexts.timeIsOver(player));
                removePlayer(player);
            } else {
                moveSeconds = seconds;
                if (seconds <= 10) {
                    player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, .6f, 1);
                    if (seconds == 0) moveSeconds = -1;
                }
            }
        }
    }

    private void clearTimer(ServerPlayerEntity player) {
        player.experienceProgress = 0;
        player.setExperienceLevel(0);
    }
}
