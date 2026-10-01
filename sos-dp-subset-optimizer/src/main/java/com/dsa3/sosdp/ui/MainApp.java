package com.dsa3.sosdp.ui;

import com.dsa3.sosdp.core.Benchmark;
import com.dsa3.sosdp.core.Bitmasks;
import com.dsa3.sosdp.core.InputParser;
import com.dsa3.sosdp.core.SosDp;
import com.dsa3.sosdp.core.SubsetSums;
import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.function.Function;

/** JavaFX front end. All algorithm work lives in the core package. */
public class MainApp extends Application {

    /** One row of the results table. */
    public record Row(int mask, String binary, String elements, long direct,
                      long brute, long sos, boolean match, long steps) {}

    private final TextField elementsField = new TextField();
    private final TextArea queriesArea = new TextArea();
    private final Label status = new Label("Load the sample or type your own input, then press Run.");
    private final TableView<Row> table = new TableView<>();
    private final Pane latticePane = new Pane();
    private final TextArea stepsArea = new TextArea();
    private final BarChart<String, Number> barChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final Label perfSummary = new Label();
    private final NumberAxis scaleX = new NumberAxis("n (number of elements)", 4, 16, 1);
    private final NumberAxis scaleY = new NumberAxis();
    private final LineChart<Number, Number> lineChart = new LineChart<>(scaleX, scaleY);
    private final Spinner<Integer> maxNSpinner = new Spinner<>(6, 18, 15);
    private final Button scaleButton = new Button("Run scaling benchmark");

    private int[] curValues = new int[0];
    private long[] curF = new long[0];

    @Override
    public void start(Stage stage) {
        elementsField.setPromptText("e.g. 5, 3, 8, 2, 7, 1   (n <= 20)");
        queriesArea.setPromptText("One query per line (or ';').\nFormats: 45 | 0b101101 | {0,2,3}");
        queriesArea.setPrefRowCount(10);

        Button sampleBtn = new Button("Load sample");
        sampleBtn.setOnAction(e -> loadSample());
        Button fileBtn = new Button("Open file...");
        fileBtn.setOnAction(e -> openFile(stage));
        Button runBtn = new Button("Run");
        runBtn.setDefaultButton(true);
        runBtn.setOnAction(e -> onRun());

        Label hint = new Label("Element i = bit i of a mask.\nF(Q) = sum of subset-sums of\nall submasks of Q.");
        VBox left = new VBox(8, new Label("Integer set (n <= 20):"), elementsField,
                new Label("Subset queries (bitmasks):"), queriesArea, sampleBtn, fileBtn, runBtn, hint);
        left.setPadding(new Insets(10));
        left.setPrefWidth(290);

        setupTable();
        Tab results = new Tab("Results", new VBox(6, new Label("Click a row to see its subset lattice."), table));
        ScrollPane latticeScroll = new ScrollPane(latticePane);
        Tab lattice = new Tab("Subset relationships", latticeScroll);

        stepsArea.setEditable(false);
        stepsArea.setFont(Font.font("Monospaced", 12));
        Tab steps = new Tab("SOS DP steps", stepsArea);

        barChart.setAnimated(false);
        barChart.setTitle("Brute force vs SOS DP (current queries)");
        barChart.getYAxis().setLabel("time (microseconds)");
        Tab perf = new Tab("Performance", new VBox(6, perfSummary, barChart));

        lineChart.setAnimated(false);
        lineChart.setTitle("Answering ALL 2^n queries: brute force O(3^n) vs SOS DP O(n * 2^n)");
        scaleY.setLabel("time (ms)");
        scaleButton.setOnAction(e -> runScaling());
        Tab scaling = new Tab("Scaling", new VBox(6,
                new javafx.scene.layout.HBox(8, new Label("Max n:"), maxNSpinner, scaleButton), lineChart));

        TextArea complexity = new TextArea(COMPLEXITY_TEXT);
        complexity.setEditable(false);
        complexity.setWrapText(true);
        Tab cx = new Tab("Complexity", complexity);

        TabPane tabs = new TabPane(results, lattice, steps, perf, scaling, cx);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane();
        root.setLeft(left);
        root.setCenter(tabs);
        status.setPadding(new Insets(6, 10, 6, 10));
        root.setBottom(status);

        stage.setTitle("SOS-DP-Subset-Optimizer");
        stage.setScene(new Scene(root, 1250, 700));
        stage.show();
        loadSample();
    }

    // ---------------------------------------------------------------- actions

    private void loadSample() {
        try (InputStream in = MainApp.class.getResourceAsStream("/sample-input.txt")) {
            applyFile(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            error("Could not load sample: " + ex.getMessage());
        }
    }

    private void openFile(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open input file");
        java.io.File f = fc.showOpenDialog(stage);
        if (f == null) return;
        try {
            applyFile(Files.readString(f.toPath()));
        } catch (Exception ex) {
            error("Could not read file: " + ex.getMessage());
        }
    }

    private void applyFile(String text) {
        InputParser.SampleFile sf = InputParser.parseFile(text);
        elementsField.setText(sf.elements());
        queriesArea.setText(sf.queries());
    }

    private void onRun() {
        try {
            int[] values = InputParser.parseElements(elementsField.getText());
            int[] masks = InputParser.parseQueries(queriesArea.getText(), values.length);
            int n = values.length;

            curValues = values;
            curF = SubsetSums.compute(values);
            Benchmark.Report r = Benchmark.run(values, masks);

            var rows = FXCollections.<Row>observableArrayList();
            for (Benchmark.QueryResult q : r.results()) {
                rows.add(new Row(q.mask(), Bitmasks.binary(q.mask(), n), Bitmasks.elementsOf(q.mask(), values),
                        q.directSum(), q.bruteValue(), q.sosValue(), q.match(), q.bruteSteps()));
            }
            table.setItems(rows);
            table.getSelectionModel().selectFirst();

            showSteps(n);
            showPerformance(r);
            status.setText((r.allMatch() ? "VERIFIED: brute force and SOS DP agree on all " : "MISMATCH found in ")
                    + masks.length + " queries.  n = " + n);
        } catch (IllegalArgumentException ex) {
            error(ex.getMessage());
        }
    }

    private void runScaling() {
        int maxN = maxNSpinner.getValue();
        scaleButton.setDisable(true);
        status.setText("Running scaling benchmark up to n = " + maxN + " ...");
        Task<Benchmark.ScalePoint[]> task = new Task<>() {
            @Override
            protected Benchmark.ScalePoint[] call() {
                return Benchmark.scaling(4, maxN);
            }
        };
        task.setOnSucceeded(e -> {
            XYChart.Series<Number, Number> brute = new XYChart.Series<>();
            brute.setName("Brute force O(3^n)");
            XYChart.Series<Number, Number> sos = new XYChart.Series<>();
            sos.setName("SOS DP O(n*2^n)");
            boolean ok = true;
            for (Benchmark.ScalePoint p : task.getValue()) {
                brute.getData().add(new XYChart.Data<>(p.n(), p.bruteNanos() / 1e6));
                sos.getData().add(new XYChart.Data<>(p.n(), p.sosNanos() / 1e6));
                ok &= p.match();
            }
            scaleX.setUpperBound(maxN);
            lineChart.getData().clear();
            lineChart.getData().addAll(brute, sos);
            scaleButton.setDisable(false);
            status.setText("Scaling done up to n = " + maxN + (ok ? ". All results verified equal." : ". MISMATCH!"));
        });
        task.setOnFailed(e -> {
            scaleButton.setDisable(false);
            error("Scaling failed: " + task.getException());
        });
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    // ---------------------------------------------------------------- views

    private void setupTable() {
        table.getColumns().add(col("Mask", 60, r -> String.valueOf(r.mask())));
        table.getColumns().add(col("Binary", 110, Row::binary));
        table.getColumns().add(col("Elements", 130, Row::elements));
        table.getColumns().add(col("sum(Q)", 80, r -> String.valueOf(r.direct())));
        table.getColumns().add(col("Brute F(Q)", 110, r -> String.valueOf(r.brute())));
        table.getColumns().add(col("SOS F(Q)", 110, r -> String.valueOf(r.sos())));
        table.getColumns().add(col("Equal?", 70, r -> r.match() ? "OK" : "MISMATCH"));
        table.getColumns().add(col("Brute steps", 90, r -> String.valueOf(r.steps())));
        table.getSelectionModel().selectedItemProperty().addListener((o, a, row) -> {
            if (row != null) drawLattice(row.mask());
        });
    }

    private TableColumn<Row, String> col(String title, double width, Function<Row, String> f) {
        TableColumn<Row, String> c = new TableColumn<>(title);
        c.setPrefWidth(width);
        c.setCellValueFactory(cd -> new ReadOnlyStringWrapper(f.apply(cd.getValue())));
        return c;
    }

    /** Draw the lattice of all submasks of the query (edges: S -> S plus one element). */
    private void drawLattice(int mask) {
        latticePane.getChildren().clear();
        int[] idx = Bitmasks.indices(mask);
        int k = idx.length;
        if (k == 0) {
            note("Empty query: its only subset is the empty set (sum 0), so F(Q) = 0.");
            return;
        }
        if (k > 5) {
            note("Query has " + k + " elements = " + (1 << k) + " submasks. The lattice is drawn only for "
                    + "queries with at most 5 elements. Select a smaller query.");
            return;
        }
        int total = 1 << k;
        double width = 1000, gap = 95, top = 55;
        int[] levelCount = new int[k + 1];
        int[] placed = new int[k + 1];
        for (int s = 0; s < total; s++) levelCount[Bitmasks.popcount(s)]++;
        double[] xs = new double[total];
        double[] ys = new double[total];
        for (int s = 0; s < total; s++) {
            int lv = Bitmasks.popcount(s);
            placed[lv]++;
            xs[s] = placed[lv] * width / (levelCount[lv] + 1);
            ys[s] = top + lv * gap;
        }
        for (int s = 0; s < total; s++) {
            for (int j = 0; j < k; j++) {
                if (((s >> j) & 1) == 0) {
                    int t = s | (1 << j);
                    Line ln = new Line(xs[s], ys[s], xs[t], ys[t]);
                    ln.setStroke(Color.GRAY);
                    latticePane.getChildren().add(ln);
                }
            }
        }
        long sumOfNodes = 0;
        for (int s = 0; s < total; s++) {
            int real = 0;
            for (int j = 0; j < k; j++) if (((s >> j) & 1) == 1) real |= 1 << idx[j];
            long f = curF[real];
            sumOfNodes += f;
            int lv = Bitmasks.popcount(s);
            Rectangle box = new Rectangle(78, 38);
            box.setArcWidth(10);
            box.setArcHeight(10);
            box.setFill(Color.hsb(210, 0.10 + 0.55 * lv / k, 1.0));
            box.setStroke(Color.DIMGRAY);
            Text label = new Text(Bitmasks.elementsOf(real, curValues) + "\nΣ=" + f);
            label.setFont(Font.font(10));
            label.setFill(Color.BLACK);
            StackPane node = new StackPane(box, label);
            node.relocate(xs[s] - 39, ys[s] - 19);
            Tooltip.install(node, new Tooltip("submask " + Bitmasks.binary(real, curValues.length)
                    + "  (decimal " + real + ")\nf = " + f));
            latticePane.getChildren().add(node);
        }
        Text head = new Text(12, 22, "Query " + mask + " = " + Bitmasks.elementsOf(mask, curValues)
                + " : " + total + " submasks.  F(Q) = sum of all node sums = " + sumOfNodes
                + "   (Σ = subset sum of that node; each level adds one element)");
        head.setFont(Font.font(13));
        latticePane.getChildren().add(head);
        latticePane.setPrefSize(width + 20, top + k * gap + 50);
    }

    private void note(String msg) {
        Text t = new Text(20, 30, msg);
        t.setFont(Font.font(14));
        latticePane.getChildren().add(t);
    }

    private void showSteps(int n) {
        if (n > 5) {
            stepsArea.setText("Step table is shown only for n <= 5 (it has 2^n rows).\n"
                    + "Use a smaller set to watch SOS DP process bit 0, bit 1, ... one at a time.");
            return;
        }
        long[][] tr = SosDp.trace(curF, n);
        StringBuilder sb = new StringBuilder();
        sb.append("Each column = the dp array after handling one more bit.\n");
        sb.append("Rule for bit i: if mask has bit i, dp[mask] += dp[mask without bit i].\n\n");
        sb.append(String.format("%-8s %-8s %-10s", "mask", "binary", "f (start)"));
        for (int i = 0; i < n; i++) sb.append(String.format(" %-10s", "bit " + i));
        sb.append("  <- last column = F[mask]\n");
        for (int m = 0; m < (1 << n); m++) {
            sb.append(String.format("%-8d %-8s", m, Bitmasks.binary(m, n)));
            for (int i = 0; i <= n; i++) sb.append(String.format(" %-10d", tr[i][m]));
            sb.append('\n');
        }
        stepsArea.setText(sb.toString());
    }

    private void showPerformance(Benchmark.Report r) {
        XYChart.Series<String, Number> pre = new XYChart.Series<>();
        pre.setName("Preprocessing (base sums, + SOS build)");
        pre.getData().add(new XYChart.Data<>("Brute force", r.baseNanos() / 1000.0));
        pre.getData().add(new XYChart.Data<>("SOS DP", (r.baseNanos() + r.sosBuildNanos()) / 1000.0));
        XYChart.Series<String, Number> qs = new XYChart.Series<>();
        qs.setName("Answering the queries");
        qs.getData().add(new XYChart.Data<>("Brute force", r.bruteQueryNanos() / 1000.0));
        qs.getData().add(new XYChart.Data<>("SOS DP", r.sosQueryNanos() / 1000.0));
        barChart.getData().clear();
        barChart.getData().addAll(pre, qs);

        double ratio = (double) r.bruteTotalNanos() / Math.max(1, r.sosTotalNanos());
        perfSummary.setText(String.format(
                "Queries: %d | Brute-force submask visits: %d | SOS build steps (n*2^n): %d + %d O(1) lookups%n"
                        + "Total time: brute %.1f us vs SOS %.1f us (brute/SOS = %.2f).%n"
                        + "SOS pays a fixed price up front; it wins when there are MANY queries or large queries. "
                        + "Try the Scaling tab.",
                r.results().length, r.bruteSteps(), r.sosPreSteps(), r.results().length,
                r.bruteTotalNanos() / 1000.0, r.sosTotalNanos() / 1000.0, ratio));
    }

    private void error(String msg) {
        status.setText("Error: " + msg);
        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
    }

    private static final String COMPLEXITY_TEXT = """
            PROBLEM
            For a query mask Q, compute F(Q) = sum of f(S) over every submask S of Q,
            where f(S) is the sum of the elements chosen by S.

            BRUTE FORCE - submask enumeration
              sub = Q; repeat { add f[sub]; sub = (sub-1) & Q } until sub = 0
              One query visits 2^popcount(Q) submasks.
              All 2^n masks together: sum over masks of 2^popcount = (1+2)^n = 3^n  ->  O(3^n).

            SOS DP - Sum Over Subsets
              for i in 0..n-1:  for each mask with bit i set:  dp[mask] += dp[mask ^ (1<<i)]
              Preprocessing: n * 2^n steps          ->  O(n * 2^n)
              Query afterwards: return dp[Q]        ->  O(1)
              Space: one array of 2^n longs         ->  O(2^n)

            WHY IT WORKS
              After processing bits 0..i, dp[mask] = sum of f[S] over submasks S that differ from
              mask only in bits 0..i. Handling bit i either keeps bit i (already counted) or
              clears it (add dp[mask without bit i]). After all n bits, every submask is counted once.

            ALGORITHM SELECTION (Module 1)
              q queries of average size k:  brute = q * 2^k    SOS = n * 2^n + q
              Few or tiny queries -> brute force may be faster (no preprocessing).
              Many queries, or all masks (3^n vs n*2^n) -> SOS DP wins by a huge margin.
              The constraint n <= 20 keeps the 2^n array (about 8 MB of longs) in memory.
            """;

    public static void main(String[] args) {
        launch(args);
    }
}
