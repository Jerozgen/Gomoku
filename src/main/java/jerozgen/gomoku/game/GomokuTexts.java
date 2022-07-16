package jerozgen.gomoku.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class GomokuTexts {
    public static MutableText description(GomokuGame game) {
        var chainSize = game.config().boardConfig().winConfig().size();
        var gameName = game.gameSpace().getMetadata().sourceConfig().name();
        return Text.empty().formatted(Formatting.GRAY)
                .append("\n").append(gameName.copy()
                        .formatted(Formatting.BOLD)
                        .formatted(Formatting.WHITE))
                .append("\n").append(Text.translatable("text.gomoku.desc" + (chainSize == 5 ? "" : ".custom_chain"),
                                chainSize))
                .append("\n")
                .append("\n").append(Text.translatable("text.gomoku.desc.controls.swap",
                                Text.translatable("text.gomoku.combined_keybind",
                                        Text.translatable("%s", Text.keybind("key.sneak")),
                                        Text.translatable("%s", Text.keybind("key.use")))
                                        .formatted(Formatting.WHITE)))
                .append("\n");

    }

    public static MutableText swapRequest$toExecutor(ServerPlayerEntity target) {
        return Text.translatable("text.gomoku.swap.request.to_executor",
                target.getDisplayName())
                .formatted(Formatting.GRAY);
    }

    public static MutableText swapRequest$toTarget(ServerPlayerEntity executor) {
        return Text.translatable("text.gomoku.swap.request.to_target",
                executor.getDisplayName(),
                Text.translatable("text.gomoku.combined_keybind",
                        Text.translatable("%s", Text.keybind("key.sneak")),
                        Text.translatable("%s", Text.keybind("key.use")))
                        .formatted(Formatting.WHITE))
                .formatted(Formatting.GRAY);
    }

    public static MutableText timeIsOver(ServerPlayerEntity player) {
        return Text.translatable("text.gomoku.time_is_over",
                player.getDisplayName())
                .formatted(Formatting.RED);
    }

    public static MutableText swap(ServerPlayerEntity player, ServerPlayerEntity target) {
        return Text.translatable("text.gomoku.swap.success",
                player.getDisplayName(),
                target.getDisplayName())
                .formatted(Formatting.GRAY);
    }

    public static MutableText win(ServerPlayerEntity player) {
        return Text.translatable("text.gomoku.end.winner",
                player.getDisplayName())
                .formatted(Formatting.GOLD);
    }

    public static MutableText draw() {
        return Text.translatable("text.gomoku.end.no_winner")
                .formatted(Formatting.GOLD);
    }

    public static MutableText finishedError() {
        return Text.translatable("text.gomoku.error.finished");
    }

    private GomokuTexts() {}
}
