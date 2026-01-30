package org.chocosolver.examples.SBLS;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

public class SBLS_Smart {

    public static class RunStats {
        public double time;
        public long nodes;
        public boolean solved;

        public RunStats(double time, long nodes, boolean solved) {
            this.time = time;
            this.nodes = nodes;
            this.solved = solved;
        }
    }

    public static void main(String[] args) {
        // Start searching from Order n = 2
        int n = 2;
        boolean solutionFound = true;

        System.out.println("--- Spatially Balanced Latin Square Solver (Smart Method) ---");

        while (solutionFound) {
            System.out.println("\n=========================================");
            System.out.println("Attempting to solve for Order n = " + n);

            RunStats stats = solveSBLS(n);

            solutionFound = stats.solved;

            if (solutionFound) {
                System.out.printf(">> SUCCESS: Found solution for n=%d in %.3fs (%d nodes)%n", n, stats.time, stats.nodes);
                n++;
            } else {
                System.out.println(">> STOP: No solution possible (or timed out) for n = " + n);
                System.out.println("Highest order found: " + (n - 1));
            }
        }
    }

    public static RunStats solveSBLS(int n) {
        Model model = new Model("SBLS_" + n);

        // Decision Variables
        IntVar[][] grid = model.intVarMatrix("grid", n, n, 1, n);
        IntVar[] flatGrid = ArrayUtils.flatten(grid);
        int maxDist = 2 * n * n * n;
        IntVar K = model.intVar("K", 0, maxDist);
        IntVar[][] rowsOfColor = new IntVar[n][n];
        IntVar[][] colsOfColor = new IntVar[n][n];

        for (int c = 1; c <= n; c++) {
            for (int k = 0; k < n; k++) {
                rowsOfColor[c-1][k] = model.intVar("R_" + c + "_" + k, 0, n - 1);
                colsOfColor[c-1][k] = model.intVar("C_" + c + "_" + k, 0, n - 1);
            }
        }

        // LATIN SQUARE CONSTRAINTS
        for (int i = 0; i < n; i++) model.allDifferent(grid[i], "AC").post();
        for (int j = 0; j < n; j++) {
            IntVar[] colVars = new IntVar[n];
            for (int i = 0; i < n; i++) colVars[i] = grid[i][j];
            model.allDifferent(colVars, "AC").post();
        }


        for (int c = 1; c <= n; c++) {
            for (int k = 0; k < n; k++) {
                IntVar index = rowsOfColor[c-1][k].mul(n).add(colsOfColor[c-1][k]).intVar();
                model.element(model.intVar(c), flatGrid, index, 0).post();
                if (k > 0) {
                    IntVar prevIndex = rowsOfColor[c-1][k-1].mul(n).add(colsOfColor[c-1][k-1]).intVar();
                    model.arithm(prevIndex, "<", index).post();
                }
            }
        }

        // SPATIAL BALANCE
        for (int a = 1; a <= n; a++) {
            for (int b = a + 1; b <= n; b++) {
                IntVar[] distances = new IntVar[n * n];
                int idx = 0;
                for (int kA = 0; kA < n; kA++) {
                    for (int kB = 0; kB < n; kB++) {

                        // --- Row Difference ---
                        IntVar rDelta = rowsOfColor[a-1][kA].sub(rowsOfColor[b-1][kB]).intVar();
                        IntVar rDiff = model.intVar(model.generateName(), 0, n);
                        model.absolute(rDiff, rDelta).post();

                        // --- Column Difference ---
                        IntVar cDelta = colsOfColor[a-1][kA].sub(colsOfColor[b-1][kB]).intVar();
                        IntVar cDiff = model.intVar(model.generateName(), 0, n);
                        model.absolute(cDiff, cDelta).post();

                        // --- Sum into Distances array ---
                        distances[idx] = model.intVar(model.generateName(), 0, 2 * n);
                        model.arithm(rDiff, "+", cDiff, "=", distances[idx]).post();
                        idx++;
                    }
                }
                model.sum(distances, "=", K).post();
            }
        }

        // SYMMETRY BREAKING
        for (int j = 0; j < n; j++) model.arithm(grid[0][j], "=", j + 1).post();
        if (n > 2) model.arithm(grid[0][1], "<", grid[1][0]).post();

        // SOLVE
        Solver solver = model.getSolver();

        solver.setSearch(
                Search.domOverWDegSearch(flatGrid),
                Search.inputOrderLBSearch(ArrayUtils.flatten(rowsOfColor)),
                Search.inputOrderLBSearch(ArrayUtils.flatten(colsOfColor)),
                Search.inputOrderLBSearch(K)
        );

        solver.limitTime("5m");

        long start = System.nanoTime();
        boolean success = solver.solve();
        long end = System.nanoTime();

        double timeInSeconds = (end - start) / 1_000_000_000.0;

        if (success) {
            System.out.println("Balanced Constant K = " + K.getValue());
            printGrid(grid, n);
        }

        return new RunStats(timeInSeconds, solver.getNodeCount(), success);
    }

    private static void printGrid(IntVar[][] grid, int n) {
        for (int i = 0; i < n; i++) {
            System.out.print("| ");
            for (int j = 0; j < n; j++) {
                System.out.printf("%2d ", grid[i][j].getValue());
            }
            System.out.println("|");
        }

        // Manual Verification Check
        if (n > 1) {
            int distSum = 0;
            for(int r1=0; r1<n; r1++) {
                for(int c1=0; c1<n; c1++) {
                    if(grid[r1][c1].getValue() == 1) {
                        for(int r2=0; r2<n; r2++) {
                            for(int c2=0; c2<n; c2++) {
                                if(grid[r2][c2].getValue() == 2) {
                                    distSum += Math.abs(r1-r2) + Math.abs(c1-c2);
                                }
                            }
                        }
                    }
                }
            }
            System.out.println(">> VERIFICATION: Computed distance between 1 & 2 is: " + distSum);
        }
    }
}