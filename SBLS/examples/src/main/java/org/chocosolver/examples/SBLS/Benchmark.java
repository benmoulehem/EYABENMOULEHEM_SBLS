package org.chocosolver.examples.SBLS;

public class Benchmark {

    public static void main(String[] args) {
        System.out.println("=================================================================================");
        System.out.printf("%-5s | %-30s | %-30s%n", "N", "SMART METHOD (CP)", "SIMPLE METHOD (Generate+Test)");
        System.out.printf("%-5s | %-10s %-10s %-8s | %-10s %-10s %-8s%n",
                "Order", "Time(s)", "Nodes", "Status", "Time(s)", "Checks", "Status");
        System.out.println("=================================================================================");

        int n = 2;
        boolean smartMethodAlive = true;

        while (smartMethodAlive) {

            // 1. Run Smart Method
            SBLS_Smart.RunStats smart = SBLS_Smart.solveSBLS(n);

            // 2. Run Simple Method (Only run if n is small, otherwise it hangs forever)
            SBLS_Smart.RunStats simple;
            if (n < 7) {
                simple = SBLS_Simple.solveSimple(n);
            } else {
                // Skip simple method for large N because it will never finish
                simple = new SBLS_Smart.RunStats(0, 0, false);
            }

            // 3. Print Row
            System.out.printf("%-5d | %-10.4f %-10d %-8s | %-10.4f %-10d %-8s%n",
                    n,
                    smart.time, smart.nodes, (smart.solved ? "OK" : "FAIL"),
                    simple.time, simple.nodes, (n >= 31 ? "SKIP" : (simple.solved ? "OK" : "FAIL"))
            );

            // Stop logic
            if (!smart.solved) {
                smartMethodAlive = false;
            } else {
                n++;
            }
        }
    }
}