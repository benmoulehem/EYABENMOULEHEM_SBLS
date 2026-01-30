package org.chocosolver.examples;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.constraints.extension.Tuples;


public class ConstraintInteger {

        public static void main(String[] args) {

            // 1. Model
            Model model = new Model("Table constraint |x - y| = z (CT+)");

            // 2. Variables [0..4]
            IntVar x = model.intVar("x", 0, 4);
            IntVar y = model.intVar("y", 0, 4);
            IntVar z = model.intVar("z", 0, 4);

            // Variable array
            IntVar[] X = new IntVar[]{x, y, z};

            // 3. Allowed tuples
            Tuples t = new Tuples(true);

            for (int xv = 0; xv <= 4; xv++) {
                for (int yv = 0; yv <= 4; yv++) {
                    int zv = Math.abs(xv - yv);
                    t.add(xv, yv, zv);
                }
            }

            // 4. Table constraint using CT+ algorithm
            model.table(X, t, "CT+").post();

            // 5. Solve
            Solver solver = model.getSolver();
            while (solver.solve()) {
                System.out.println(
                        "x=" + x.getValue() +
                                ", y=" + y.getValue() +
                                ", z=" + z.getValue()
                );
            }
        }

}
