package jerozgen.gomoku.game;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.KeybindText;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;

public final class GomokuTexts {
    public static MutableText description(GomokuGame game) {
        var chainSize = game.config().boardConfig().winConfig().size();
        var gameName = game.gameSpace().getMetadata().sourceConfig().name();
        return new LiteralText("").formatted(Formatting.GRAY)
                .append("\n").append(gameName.shallowCopy()
                        .formatted(Formatting.BOLD)
                        .formatted(Formatting.WHITE))
                .append("\n").append(new TranslatableText("text.gomoku.desc" + (chainSize == 5 ? "" : ".custom_chain"),
                                chainSize))
                .append("\n")
                .append("\n").append(new TranslatableText("text.gomoku.desc.controls.swap",
                                new TranslatableText("text.gomoku.combined_keybind",
                                        new TranslatableText("%s", new KeybindText("key.sneak")),
                                        new TranslatableText("%s", new KeybindText("key.use")))
                                        .formatted(Formatting.WHITE)))
                .append("\n");

    }

    public static MutableText swapRequest$toExecutor(ServerPlayerEntity target) {
        return new TranslatableText("text.gomoku.swap.request.to_executor",
                target.getDisplayName())
                .formatted(Formatting.GRAY);
    }

    public static MutableText swapRequest$toTarget(ServerPlayerEntity executor) {
        return new TranslatableText("text.gomoku.swap.request.to_target",
                executor.getDisplayName(),
                new TranslatableText("text.gomoku.combined_keybind",
                        new TranslatableText("%s", new KeybindText("key.sneak")),
                        new TranslatableText("%s", new KeybindText("key.use")))
                        .formatted(Formatting.WHITE))
                .formatted(Formatting.GRAY);
    }

    public static MutableText timeIsOver(ServerPlayerEntity player) {
        return new TranslatableText("text.gomoku.time_is_over",
                player.getDisplayName())
                .formatted(Formatting.RED);
    }

    public static MutableText swap(ServerPlayerEntity player, ServerPlayerEntity target) {
        return new TranslatableText("text.gomoku.swap.success",
                player.getDisplayName(),
                target.getDisplayName())
                .formatted(Formatting.GRAY);
    }

    public static MutableText win(ServerPlayerEntity player) {
        return new TranslatableText("text.gomoku.end.winner",
                player.getDisplayName())
                .formatted(Formatting.GOLD);
    }

    public static MutableText draw() {
        return new TranslatableText("text.gomoku.end.no_winner")
                .formatted(Formatting.GOLD);
    }

    public static MutableText finishedError() {
        return new TranslatableText("text.gomoku.error.finished");
    }

    private GomokuTexts() {}
}
