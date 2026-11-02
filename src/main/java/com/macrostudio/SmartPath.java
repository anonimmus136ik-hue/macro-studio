package com.macrostudio;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Client-only, bounded A* planner for horizontal walkable routes.
 * It does not move the player, teleport, scan unloaded chunks, or bypass reach.
 * Unknown terrain is impassable. A missing floor requires a detour, not a jump.
 */
public final class SmartPath {
    private static final int MAX_NODES = 1600;
    private static final int MAX_RADIUS = 24;
    private static final int[][] OFFSETS = {{1,0},{-1,0},{0,1},{0,-1}};

    public record Result(List<BlockPos> path, String status) {
        public boolean found() { return !path.isEmpty(); }
    }
    private record Node(BlockPos pos, int g, int f) {}

    private SmartPath() {}

    public static boolean walkable(Minecraft mc, BlockPos feet) {
        if (mc.level == null || !mc.level.hasChunkAt(feet) || !mc.level.hasChunkAt(feet.below())) return false;
        BlockState floor = mc.level.getBlockState(feet.below());
        BlockState body = mc.level.getBlockState(feet);
        BlockState head = mc.level.getBlockState(feet.above());
        if (!floor.blocksMotion() || floor.getFluidState().isSource()) return false;
        if (body.blocksMotion() || head.blocksMotion()) return false;
        if (!body.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
        return true;
    }

    public static Result plan(Minecraft mc, BlockPos goal) {
        if (mc == null || mc.player == null || mc.level == null || goal == null) return new Result(List.of(), "NO WORLD");
        int y = mc.player.blockPosition().getY();
        BlockPos start = new BlockPos(mc.player.blockPosition().getX(), y, mc.player.blockPosition().getZ());
        BlockPos dest = new BlockPos(goal.getX(), y, goal.getZ());
        if (Math.abs(start.getX() - dest.getX()) > MAX_RADIUS || Math.abs(start.getZ() - dest.getZ()) > MAX_RADIUS)
            return new Result(List.of(), "TARGET TOO FAR");
        if (!walkable(mc, dest)) return new Result(List.of(), "GOAL UNSAFE");

        Comparator<Node> byScore = Comparator.comparingInt(Node::f).thenComparingInt(Node::g);
        PriorityQueue<Node> open = new PriorityQueue<>(byScore);
        Map<BlockPos, Integer> cost = new HashMap<>();
        Map<BlockPos, BlockPos> parent = new HashMap<>();
        cost.put(start, 0);
        open.add(new Node(start, 0, heuristic(start, dest)));
        int explored = 0;
        while (!open.isEmpty() && explored++ < MAX_NODES) {
            Node current = open.poll();
            if (current.g() != cost.getOrDefault(current.pos(), Integer.MAX_VALUE)) continue;
            if (current.pos().equals(dest)) {
                ArrayList<BlockPos> result = new ArrayList<>();
                BlockPos cursor = dest;
                while (cursor != null) {
                    result.add(cursor);
                    cursor = parent.get(cursor);
                }
                Collections.reverse(result);
                return new Result(List.copyOf(result), "SAFE ROUTE " + (result.size() - 1) + " STEPS");
            }
            for (int[] d : OFFSETS) {
                BlockPos next = current.pos().offset(d[0], 0, d[1]);
                if (Math.abs(start.getX() - next.getX()) > MAX_RADIUS || Math.abs(start.getZ() - next.getZ()) > MAX_RADIUS)
                    continue;
                if (!walkable(mc, next)) continue;
                int newCost = current.g() + 1;
                if (newCost >= cost.getOrDefault(next, Integer.MAX_VALUE)) continue;
                cost.put(next, newCost);
                parent.put(next, current.pos());
                open.add(new Node(next, newCost, newCost + heuristic(next, dest)));
            }
        }
        return new Result(List.of(), "NO SAFE ROUTE");
    }

    public static Result toMiningColumn(Minecraft mc, int x, int z) {
        if (mc == null || mc.player == null) return new Result(List.of(), "NO WORLD");
        int y = mc.player.blockPosition().getY();
        Result best = new Result(List.of(), "NO SAFE ROUTE");
        // Stop beside an unwalkable mining column if necessary.
        for (int[] offset : new int[][] {{0,0},{1,0},{-1,0},{0,1},{0,-1},{2,0},{-2,0},{0,2},{0,-2}}) {
            Result candidate = plan(mc, new BlockPos(x + offset[0], y, z + offset[1]));
            if (candidate.found() && (!best.found() || candidate.path().size() < best.path().size()))
                best = candidate;
        }
        return best;
    }

    private static int heuristic(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }
}
