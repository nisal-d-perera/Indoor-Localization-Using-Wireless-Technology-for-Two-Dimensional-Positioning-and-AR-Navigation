package com.example.researchproject;

import android.graphics.Point;

import java.util.*;

public class AStarPathfinder {
    private static final int[][] DIRS4 = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[][] DIRS8 = {{1,0},{-1,0},{0,1},{0,-1},{1,1},{1,-1},{-1,1},{-1,-1}};

    /**
     * Node wrapper so each entry in the PQ carries its own f-score
     */
    private static class Node implements Comparable<Node> {
        int x, y, g, f;
        Node(int x, int y, int g, int f) {
            this.x = x; this.y = y; this.g = g; this.f = f;
        }
        @Override
        public int compareTo(Node o) {
            return Integer.compare(this.f, o.f);
        }
        Point toPoint() { return new Point(x, y); }
    }

    private final boolean useDiagonals;
    public AStarPathfinder(boolean useDiagonals) {
        this.useDiagonals = useDiagonals;
    }

    private int heuristic(Point a, Point b) {
        // Manhattan for 4‑way, Diagonal distance for 8‑way
        int dx = Math.abs(a.x - b.x), dy = Math.abs(a.y - b.y);
        if (!useDiagonals) return dx + dy;
        return Math.max(dx, dy);
    }

    /**
     * @param walkable[y][x] grid
     * @param start  must be inside grid
     * @param goal   must be inside grid
     */
    public List<Point> findPath(boolean[][] walkable, Point start, Point goal) {
        int H = walkable.length, W = walkable[0].length;
        // Force start/goal to be walkable:
        walkable[start.y][start.x] = true;
        walkable[goal.y][goal.x]   = true;

        // gScore map
        int[][] gScore = new int[H][W];
        for (int[] row : gScore) Arrays.fill(row, Integer.MAX_VALUE);
        gScore[start.y][start.x] = 0;

        // cameFrom map
        Point[][] cameFrom = new Point[H][W];

        // Open set: PQ + quick-lookup set
        PriorityQueue<Node> openPQ = new PriorityQueue<>();
        boolean[][] inOpenSet = new boolean[H][W];
        Node startNode = new Node(start.x, start.y, 0, heuristic(start, goal));
        openPQ.add(startNode);
        inOpenSet[start.y][start.x] = true;

        // Closed set
        boolean[][] closed = new boolean[H][W];

        int[][] dirs = useDiagonals ? DIRS8 : DIRS4;

        while (!openPQ.isEmpty()) {
            Node cur = openPQ.poll();
            if (closed[cur.y][cur.x]) continue;    // skipped outdated
            closed[cur.y][cur.x] = true;

            // Goal reached
            if (cur.x == goal.x && cur.y == goal.y) {
                List<Point> path = new ArrayList<>();
                Point p = goal;
                while (p != null) {
                    path.add(p);
                    p = cameFrom[p.y][p.x];
                }
                Collections.reverse(path);
                return path;
            }

            // Explore neighbors
            for (int[] d : dirs) {
                int nx = cur.x + d[0], ny = cur.y + d[1];
                if (nx < 0 || nx >= W || ny < 0 || ny >= H) continue;
                if (!walkable[ny][nx] || closed[ny][nx])    continue;

                int tentativeG = cur.g + ((d[0] != 0 && d[1] != 0) ? 14 : 10);
                // (Using 10/14 costs for diagonal gives true Euclidean approx;
                // you can simplify to +1 for all moves if you like)

                if (tentativeG < gScore[ny][nx]) {
                    gScore[ny][nx] = tentativeG;
                    cameFrom[ny][nx] = new Point(cur.x, cur.y);
                    int f = tentativeG + heuristic(new Point(nx,ny), goal);
                    Node next = new Node(nx, ny, tentativeG, f);
                    openPQ.add(next);
                    inOpenSet[ny][nx] = true;
                }
            }
        }

        // No path found
        return null;
    }
}
