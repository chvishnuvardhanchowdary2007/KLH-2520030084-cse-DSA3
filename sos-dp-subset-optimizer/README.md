# SOS-DP-Subset-Optimizer

A DSA-3 course project (Java 17/21, Maven, JavaFX). It answers many subset queries with
**Sum Over Subsets (SOS) Dynamic Programming**, checks the answers against **brute-force submask
enumeration**, times both, and visualizes subset relationships and performance.

## Problem
Input: an integer set of `n <= 20` elements and several subset queries given as bitmasks
(element `i` = bit `i`).
For a query mask `Q`:
* `sum(Q)` = sum of the chosen elements (the "subset sum").
* `F(Q)`  = sum of `sum(S)` over **every submask S of Q** (the aggregate subset value).

Example: set `{1,2}`, `Q = 0b11`. Submasks: `{}`,`{1}`,`{2}`,`{1,2}` -> sums 0+1+2+3, so `F(Q)=6`.

## Complexity (explained)
| Method | Cost | Reason |
|---|---|---|
| Brute-force submask enumeration | one query `O(2^k)` (k = bits in Q); all 2^n masks **`O(3^n)`** | `sub = (sub-1) & Q` visits each submask once; sum over masks of `2^popcount` = `(1+2)^n` |
| SOS DP preprocessing | **`O(n x 2^n)`** | n passes over 2^n masks |
| Query after preprocessing | **`O(1)`** | answer is `dp[Q]` |
| Space | **`O(2^n)`** | one `long[2^n]` array |

**SOS recurrence** (bit `i` processed in turn, array updated in place):
```
for i in 0..n-1:
  for mask in 0..2^n-1:
    if mask has bit i:  dp[mask] += dp[mask ^ (1<<i)]
```
After bits `0..i`, `dp[mask]` = sum of `f[S]` over submasks `S` differing from `mask` only in bits `0..i`.

**Algorithm selection:** with `q` queries of size `k`, brute force costs `q*2^k`, SOS costs `n*2^n + q`.
For a handful of small queries brute force can win; for many queries or all masks SOS wins hugely.
The app shows both cases honestly (Performance tab vs Scaling tab).

## Project layout
```
pom.xml
src/main/java/com/dsa3/sosdp/core/   Bitmasks, SubsetSums, BruteForce, SosDp, InputParser, Benchmark, ConsoleDemo
src/main/java/com/dsa3/sosdp/ui/     MainApp (JavaFX)
src/main/resources/sample-input.txt
src/test/java/com/dsa3/sosdp/core/   SosDpTest, InputParserTest (JUnit 5)
.vscode/                              tasks + extension recommendations
```
The `core` package uses only arrays (no `java.util` collections), following the handout's
"hand-build every algorithm" constraint. Only the GUI and tests use library classes.

## GUI tabs
1. **Results** - table of every query: mask, binary, elements, `sum(Q)`, brute `F(Q)`, SOS `F(Q)`, equality check.
2. **Subset relationships** - click a result row; draws the lattice of all submasks of that query
   (levels = number of elements, edges = add one element, each node shows its subset sum). Queries with up to 5 elements.
3. **SOS DP steps** - for `n <= 5`, the dp array after each bit, so you can trace it by hand.
4. **Performance** - bar chart: brute force vs SOS (preprocessing vs answering) for your queries.
5. **Scaling** - line chart for `n = 4..maxN`, answering all `2^n` queries: `O(3^n)` vs `O(n*2^n)`.
6. **Complexity** - the explanation above.

Input formats for queries (one per line or `;`): `45`, `0b101101`, `{0,2,3,5}`.
`Open file...` loads a file like `sample-input.txt`.

## VS Code setup
1. Install **JDK 17 or 21** (`java -version`) and **Maven 3.8+** (`mvn -v`). Both must be on `PATH`.
2. Install VS Code and the **Extension Pack for Java** (Maven for Java is included). VS Code will suggest them from `.vscode/extensions.json`.
3. `File > Open Folder...` and choose the `sos-dp-subset-optimizer` folder. Wait for the Java project to import.
4. JavaFX comes from Maven (`org.openjfx`), so no separate JavaFX SDK is needed. The first build needs internet.

## Run
In the VS Code terminal (`Ctrl+`` `) from the project folder:
```bash
mvn javafx:run                 # launch the GUI
mvn test                       # run unit tests
mvn compile
java -cp target/classes com.dsa3.sosdp.core.ConsoleDemo   # text-only demo, no JavaFX
```
Or use `Terminal > Run Task > Run GUI` / `Run tests`.
Do not use the editor's "Run" button on `MainApp`: it does not put JavaFX on the module path. Use `mvn javafx:run`.

Troubleshooting: "JavaFX runtime components are missing" -> run via `mvn javafx:run`.
Scaling with max n = 18 takes a few seconds (brute force is 3^18 steps).
Timings are wall-clock `System.nanoTime()` after a small JIT warm-up; tiny inputs give noisy microsecond results.

## Syllabus mapping (course handout 25CS2103E)
**Module 3 - Advanced Dynamic Programming (CO3)**
| Handout topic | Where in the project |
|---|---|
| DP framework: overlapping subproblems, optimal substructure | `SosDp` states `dp_i[mask]` reused by later masks; `SubsetSums` reuses `f[mask without lowest bit]` |
| Bitmask DP and state compression | subsets of an `n`-set stored as ints; `2^n` states in one array (`Bitmasks`) |
| DP on subsets | `SubsetSums.compute`, `SosDp` |
| Sum-over-subsets in `O(n * 2^n)` | `SosDp` constructor; "SOS DP steps" tab |
| Submask processing | `BruteForce.query` with `(sub-1) & mask`; lattice tab |
| When DP is the wrong tool / exponential state space | Performance tab (few queries) and the `n <= 20` limit |

Related handout entries: Lecture Session 12 (Bitmask DP, DP on Subsets, SOS DP), Practical Session 6 (Bitmask DP),
Skilling Sessions 11-12, Self-learning topic 6 (SOS DP), textbook TB1 Ch. 15.

**Module 1 - complexity and algorithm selection (CO1)**
| Handout topic | Where in the project |
|---|---|
| Computational complexity | `O(3^n)` vs `O(n*2^n)` derived above and shown in the Complexity tab |
| Algorithm selection | `q*2^k` vs `n*2^n + q` break-even discussion; Performance vs Scaling tabs |
| Complexity-based evaluation | measured times, step counts, and the scaling chart; results verified equal |
| `java.util` forbidden inside the engine | `core` package uses arrays only |

## Viva questions
* **Why is brute force `O(3^n)`?** Each element is in Q's complement, in Q but not in S, or in S: 3 choices, so `3^n` (mask, submask) pairs.
* **Why does the loop order matter in SOS?** Processing one bit at a time ensures each submask is added exactly once; the outer loop must be the bit.
* **Why in-place updates are safe?** `dp[mask ^ (1<<i)]` has bit `i` cleared, so it is not changed during pass `i`.
* **Independent check?** `F(Q) = 2^(k-1) * sum(Q)` (each element lies in half of the `2^k` submasks); the unit tests use this.
* **Why `long`?** `F(Q)` can reach about `2^19 * 20 * 2^31`, far above `int`.
* **Why `n <= 20`?** `2^20` longs is about 8 MB; larger `n` grows exponentially.
* **When is brute force better?** A few small queries: it skips the `n*2^n` preprocessing.
