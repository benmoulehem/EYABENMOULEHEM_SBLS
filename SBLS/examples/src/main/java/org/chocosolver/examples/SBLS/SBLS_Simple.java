package org.chocosolver.examples.SBLS;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;

public class SBLS_Simple {

    public static SBLS_Smart.RunStats solveSimple(int n) {
        Model model = new Model("SBLS_Simple_" + n);

        // 1. Basic Latin Square (NO distance constraints)
        IntVar[][] grid = model.intVarMatrix("grid", n, n, 1, n);
        for (int i = 0; i < n; i++) model.allDifferent(grid[i], "AC").post();
        for (int j = 0; j < n; j++) {
            IntVar[] colVars = new IntVar[n];
            for (int i = 0; i < n; i++) colVars[i] = grid[i][j];
            model.allDifferent(colVars, "AC").post();
        }

        // Symmetry Breaking (Fairness)
        for (int j = 0; j < n; j++) model.arithm(grid[0][j], "=", j + 1).post();
        if (n > 2) model.arithm(grid[0][1], "<", grid[1][0]).post();

        Solver solver = model.getSolver();

        // 2. The Naive Search Loop
        long nodes = 0;
        long start = System.nanoTime();
        boolean found = false;

        while (solver.solve()) {
            nodes++;

            if (checkSpatialBalance(grid, n)) {
                found = true;
                break;
            }

            if ((System.nanoTime() - start) > 10_000_000_000L) break;
        }

        double time = (System.nanoTime() - start) / 1_000_000_000.0;
        return new SBLS_Smart.RunStats(time, nodes, found);
    }

    private static boolean checkSpatialBalance(IntVar[][] grid, int n) {
        long targetK = -1;
        for (int c1 = 1; c1 <= n; c1++) {
            for (int c2 = c1 + 1; c2 <= n; c2++) {
                long dist = 0;
                for (int rA = 0; rA < n; rA++) {
                    for (int cA = 0; cA < n; cA++) {
                        if (grid[rA][cA].getValue() == c1) {
                            for (int rB = 0; rB < n; rB++) {
                                for (int cB = 0; cB < n; cB++) {
                                    if (grid[rB][cB].getValue() == c2) {
                                        dist += Math.abs(rA - rB) + Math.abs(cA - cB);
                                    }
                                }
                            }
                        }
                    }
                }
                if (targetK == -1) targetK = dist;
                else if (dist != targetK) return false;
            }
        }
        return true;
    }
}