# EYABENMOULEHEM_SBLS

**Spatially Balanced Latin Square (SBLS)**
This project implements a Constraint Programming (CP) model to solve the Spatially Balanced Latin Square (SBLS) problem using the Choco Solver library. 
It includes an optimized "Smart" solver and a "Simple" baseline method to demonstrate the efficiency gains of constructive constraint propagation.

**Project Structure:**
The source code is located in the SBLS package:

### SBLS_Smart.java: 

The main CP model using DomOverWDeg variable ordering and dual-variable channeling.SBLS_Simple.java: A baseline "Generate-and-Test" solver used for performance comparison.

### Benchmark.java: 

A utility class that runs both methods side-by-side to generate comparison tables.

# **How to Run**
Prerequisites

### Java JDK 17 (or higher)
### Choco-Solver 4.10.14

## 1. Run the Main Solver (Smart Method)
To solve SBLS instances efficiently using the optimized CP model:

### Class: SBLS_Smart

Path: SBLS\examples\src\main\java\org\chocosolver\examples\SBLS\SBLS_Smart.java

## 2. Run the Benchmark Experiment
To reproduce the experimental results comparing the Smart Method vs. Simple Method:

### Class: Benchmark

Path: SBLS\examples\src\main\java\org\chocosolver\examples\SBLS\Benchmark.java

You can find the report above the Readme file.
