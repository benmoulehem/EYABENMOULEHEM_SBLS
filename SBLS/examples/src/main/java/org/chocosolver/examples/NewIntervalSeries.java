package org.chocosolver.examples;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.extension.Tuples;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.IntVar;

public class NewIntervalSeries {

    public static void main(String[] args) {

        final int N = 12;
        final int M = N - 1;

        // ---------------- CONFIGURATION ----------------

        String modeling = "table";   // "arithm" or "table"
        String strategy = "DomOverWDeg";
        String type = "AC";          // BC, AC_regin, AC, AC_zhang, default

        // ------------------------------------------------

        Model model = new Model("All Interval Series");

        IntVar[] P = model.intVarArray("P", N, 0, N - 1);
        IntVar[] I = model.intVarArray("I", M, 1, M);

        model.allDifferent(P).post();
        model.allDifferent(I).post();

        // ------------------------------------------------
        // MODELING OPTIONS
        // ------------------------------------------------

        if (modeling.equals("arithm")) {

            // ===== Arithmetic decomposition =====
            for (int k = 0; k < M; k++) {
                IntVar diff = model.intVar("D_" + k, -M, M);
                model.arithm(diff, "=", P[k + 1], "-", P[k]).post();
                model.absolute(I[k], diff).post();
            }

        } else {

            // ===== Table constraint modeling =====

            Tuples t = new Tuples(true);

            for (int a = 0; a < N; a++) {
                for (int b = 0; b < N; b++) {
                    int d = Math.abs(b - a);
                    if (d >= 1 && d <= M) {
                        t.add(a, b, d);
                    }
                }
            }

            String algo = getAlgorithm(type);

            for (int k = 0; k < M; k++) {
                if (algo == null) {
                    model.table(new IntVar[]{P[k], P[k + 1], I[k]}, t).post();
                } else {
                    model.table(new IntVar[]{P[k], P[k + 1], I[k]}, t, algo).post();
                }
            }
        }

        // ------------------------------------------------
        // Symmetry breaking
        // ------------------------------------------------

        model.arithm(P[0], "=", 0).post();

        // ------------------------------------------------
        // Search strategies
        // ------------------------------------------------

        switch (strategy) {

            case "activity":
                model.getSolver().setSearch(Search.activityBasedSearch(P));
                break;

            case "random":
                model.getSolver().setSearch(Search.randomSearch(P, 42));
                break;

            case "smallest":
                model.getSolver().setSearch(Search.minDomLBSearch(P));
                break;

            case "DomOverWDeg":
                model.getSolver().setSearch(Search.domOverWDegSearch(P));
                break;

            default:
                model.getSolver().setSearch(Search.inputOrderLBSearch(P));
        }

        // ------------------------------------------------
        // Solve
        // ------------------------------------------------

        Solution sol = model.getSolver().findSolution();

        if (sol != null) {

            System.out.print("P = [");
            for (int i = 0; i < N; i++)
                System.out.print(P[i].getValue() + (i < N - 1 ? ", " : ""));
            System.out.println("]");

            System.out.print("I = [");
            for (int i = 0; i < M; i++)
                System.out.print(I[i].getValue() + (i < M - 1 ? ", " : ""));
            System.out.println("]");

            model.getSolver().printStatistics();

        } else {
            System.out.println("No solution found.");
        }
    }

    // ------------------------------------------------
    // Constraint type → Choco algorithm mapping
    // ------------------------------------------------

    private static String getAlgorithm(String type) {

        switch (type) {
            case "AC_regin":
                return "AC_REGIN";
            case "AC":
                return "CT+";
            case "AC_zhang":
                return "GAC3rm";
            default:
                return null; // Choco default
        }
    }

}
