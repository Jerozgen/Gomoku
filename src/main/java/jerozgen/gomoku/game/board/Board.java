package jerozgen.gomoku.game.board;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.PillarBlock;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.map_templates.MapTemplate;

import java.util.List;
import java.util.function.IntUnaryOperator;


public class Board {
    private static final Block DEFAULT_BLOCK = Blocks.LODESTONE;
    private static final Block EDGE_BLOCK = Blocks.OAK_LOG;

    private final BoardConfig config;
    private final Block[][] board;
    private final int winSize;
    private final Vec3d spawnPos;

    private int emptySpace;

    public Board(BoardConfig config) {
        this.config = config;
        this.board = new Block[config.width()][config.height()];
        for (int x = 0; x < config.width(); x++) {
            for (int z = 0; z < config.height(); z++) {
                board[z][x] = DEFAULT_BLOCK;
            }
        }
        this.emptySpace = config.width() * config.width();
        this.winSize = config.winConfig().size();
        this.spawnPos = new Vec3d(config.width() / 2.0, 1, config.height() / 2.0);
    }

    public boolean isOut(int x, int z) {
        return x < 0 || z < 0 || x >= config.width() || z >= config.height();
    }

    @Nullable
    public Block block(int x, int z) {
        if (isOut(x, z)) return null;
        return board[z][x];
    }

    public BlockChangeResult block(int x, int z, Block block, WorldAccess world) {
        if (isOut(x, z)) return BlockChangeResult.FAIL;
        if (!DEFAULT_BLOCK.equals(block(x, z))) return BlockChangeResult.FAIL;

        board[z][x] = block;
        emptySpace -= 1;
        world.setBlockState(new BlockPos(x, 0, z), block.getDefaultState(), Block.NOTIFY_LISTENERS);

        if (emptySpace <= 0) return BlockChangeResult.DRAW;

        if (checkWin(block, d -> x + d, d -> z    ) ||
            checkWin(block, d -> x    , d -> z + d) ||
            checkWin(block, d -> x + d, d -> z + d) ||
            checkWin(block, d -> x - d, d -> z + d)) return BlockChangeResult.WIN;

        return BlockChangeResult.SUCCESS;
    }

    private boolean checkWin(Block block, IntUnaryOperator x, IntUnaryOperator z) {
        var max = 0;
        var current = 0;
        for (int d = -winSize; d <= winSize; d++) {
            if (!block.equals(block(x.applyAsInt(d), z.applyAsInt(d)))) current = 0;
            else max = Math.max(max, ++current);
        }
        return !config.winConfig().exact() && max >= winSize || max == winSize;
    }

    public Vec3d spawnPos() {
        return spawnPos;
    }

    public Text raycastText(HitResult hit) {
        if (hit.getType() != HitResult.Type.BLOCK) return Text.empty();

        var pos = ((BlockHitResult) hit).getBlockPos();
        if (pos.getY() != 0) return Text.empty();

        var block = block(pos.getX(), pos.getZ());
        if (block == null) return Text.empty();
        if (block.equals(DEFAULT_BLOCK)) return Text.empty();

        return block.getName();
    }

    public MapTemplate template() {
        MapTemplate template = MapTemplate.createEmpty();
        var width = config.width();
        var height = config.height();

        for (BlockPos pos : BlockPos.iterate(0, 0, 0, width - 1, 0, height - 1)) {
            template.setBlockState(pos, DEFAULT_BLOCK.getDefaultState());
        }

        var edge = EDGE_BLOCK.getDefaultState();
        for (var iterable : List.of(
                BlockPos.iterate(-1, 0, -1, width - 1, 0, -1),
                BlockPos.iterate(0, 0, height, width, 0, height))) {
            iterable.forEach(pos -> template.setBlockState(pos, edge.with(PillarBlock.AXIS, Direction.Axis.X)));
        }
        for (var iterable : List.of(
                BlockPos.iterate(-1, 0, 0, -1, 0, height),
                BlockPos.iterate(width, 0, -1, width, 0, height - 1))) {
            iterable.forEach(pos -> template.setBlockState(pos, edge.with(PillarBlock.AXIS, Direction.Axis.Z)));
        }

        return template;
    }

    public enum BlockChangeResult {
        WIN,
        DRAW,
        SUCCESS,
        FAIL
    }
}
