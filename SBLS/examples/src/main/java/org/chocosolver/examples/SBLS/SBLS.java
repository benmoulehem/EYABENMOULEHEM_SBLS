package org.chocosolver.examples.SBLS;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.tools.ArrayUtils;

public class SBLS {

    public static void main(String[] args) {
        // Start searching from Order n = 2
        int n = 2;
        boolean solutionFound = true;

        System.out.println("--- Spatially Balanced Latin Square Solver ---");

        // Loop indefinitely until no solution is found for a specific order
        while (solutionFound) {
            System.out.println("\n=========================================");
            System.out.println("Attempting to solve for Order n = " + n);

            // Try to solve for the current n
            solutionFound = solveSBLS(n);

            if (solutionFound) {
                System.out.println(">> SUCCESS: Found a Balanced Latin Square for n = " + n);
                n++; // Move to the next order
            } else {
                System.out.println(">> STOP: No solution possible for n = " + n);
                System.out.println("Highest order found: " + (n - 1));
            }
        }
    }

    /**
     * Builds and solves the SBLS model for a specific order n.
     */
    public static boolean solveSBLS(int n) {
        Model model = new Model("SBLS_" + n);

        // =========================================================================
        // A. DECISION VARIABLES
        // =========================================================================

        // 1. The Grid: grid[i][j] holds the fertilizer color (1..n)
        IntVar[][] grid = model.intVarMatrix("grid", n, n, 1, n);

        // 2. Flattened Grid: Essential for channeling and search strategy
        IntVar[] flatGrid = ArrayUtils.flatten(grid);

        // 3. K: The target balanced distance
        int maxDist = 2 * n * n * n;
        IntVar K = model.intVar("K", 0, maxDist);

        // 4. Dual Variables: Coordinates of each color's occurrences
        // FIX: Size is now [n][n] (0-based) to prevent NullPointer on index 0
        IntVar[][] rowsOfColor = new IntVar[n][n];
        IntVar[][] colsOfColor = new IntVar[n][n];

        for (int c = 1; c <= n; c++) {
            for (int k = 0; k < n; k++) {
                // FIX: Access using [c-1] because array is 0-based
                rowsOfColor[c-1][k] = model.intVar("R_" + c + "_" + k, 0, n - 1);
                colsOfColor[c-1][k] = model.intVar("C_" + c + "_" + k, 0, n - 1);
            }
        }

        // =========================================================================
        // B. LATIN SQUARE CONSTRAINTS
        // =========================================================================

        // 1. Row Uniqueness
        for (int i = 0; i < n; i++) {
            model.allDifferent(grid[i], "AC").post();
        }

        // 2. Column Uniqueness
        for (int j = 0; j < n; j++) {
            IntVar[] colVars = new IntVar[n];
            for (int i = 0; i < n; i++) {
                colVars[i] = grid[i][j];
            }
            model.allDifferent(colVars, "AC").post();
        }

        // =========================================================================
        // C. CHANNELING (Linking Grid <-> Dual Variables)
        // =========================================================================

        for (int c = 1; c <= n; c++) {
            for (int k = 0; k < n; k++) {
                // FIX: Access using [c-1]
                IntVar rVar = rowsOfColor[c-1][k];
                IntVar cVar = colsOfColor[c-1][k];

                // Calculate 1D index: index = (row * n) + col
                IntVar index = rVar.mul(n).add(cVar).intVar();

                // Constraint: flatGrid[index] == c
                model.element(model.intVar(c), flatGrid, index, 0).post();

                // Internal Symmetry Breaking: Order occurrences
                if (k > 0) {
                    IntVar prevR = rowsOfColor[c-1][k-1];
                    IntVar prevC = colsOfColor[c-1][k-1];
                    IntVar prevIndex = prevR.mul(n).add(prevC).intVar();

                    model.arithm(prevIndex, "<", index).post();
                }
            }
        }

        // =========================================================================
        // D. SPATIAL BALANCE (The Distance Constraint)
        // =========================================================================

        for (int a = 1; a <= n; a++) {
            for (int b = a + 1; b <= n; b++) {

                IntVar[] distances = new IntVar[n * n];
                int idx = 0;

                for (int kA = 0; kA < n; kA++) {
                    for (int kB = 0; kB < n; kB++) {
                        // FIX: Access using [a-1] and [b-1]
                        IntVar r1 = rowsOfColor[a-1][kA];
                        IntVar r2 = rowsOfColor[b-1][kB];

                        IntVar c1 = colsOfColor[a-1][kA];
                        IntVar c2 = colsOfColor[b-1][kB];

                        // Manhattan Distance components
                        IntVar rDiff = model.intVar(model.generateName(), 0, n);
                        model.absolute(rDiff, r1.sub(r2).intVar()).post();

                        IntVar cDiff = model.intVar(model.generateName(), 0, n);
                        model.absolute(cDiff, c1.sub(c2).intVar()).post();

                        // Sum components
                        distances[idx] = model.intVar(model.generateName(), 0, 2*n);
                        model.arithm(rDiff, "+", cDiff, "=", distances[idx]).post();
                        idx++;
                    }
                }
                // The sum of all these distances must equal K
                model.sum(distances, "=", K).post();
            }
        }

        // =========================================================================
        // E. SYMMETRY BREAKING
        // =========================================================================

        // 1. Fix first row: 1, 2, ..., n
        for (int j = 0; j < n; j++) {
            model.arithm(grid[0][j], "=", j + 1).post();
        }

        // 2. Fix first column ordering (if n > 2)
        if (n > 2) {
            model.arithm(grid[0][1], "<", grid[1][0]).post();
        }

        // =========================================================================
        // F. SOLVE
        // =========================================================================

        Solver solver = model.getSolver();

        // -------------------------------------------------------------------------
        // STRATEGY (Now Safe from NullPointer)
        // -------------------------------------------------------------------------
        solver.setSearch(
                Search.inputOrderLBSearch(flatGrid),
                // Since rowsOfColor is now [n][n] and fully filled, flatten() is safe
                Search.inputOrderLBSearch(ArrayUtils.flatten(rowsOfColor)),
                Search.inputOrderLBSearch(ArrayUtils.flatten(colsOfColor)),
                Search.inputOrderLBSearch(K)
        );

        solver.limitTime("5m");

        if (solver.solve()) {
            System.out.println("Balanced Constant K = " + K.getValue());
            printGrid(grid, n);
            return true;
        } else {
            return false;
        }
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