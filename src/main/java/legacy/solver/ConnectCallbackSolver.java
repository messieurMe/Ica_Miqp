package legacy.solver;

import legacy.algo.MST;
import legacy.graph.Graph;
import ilog.concert.*;
import ilog.cplex.*;
import legacy.utils.Matrix;
import legacy.utils.Pair;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ConnectCallbackSolver implements MySolver {
    @Override
    public void close() {
        log.close();
    }

    // data class:

    private static class Variables {
        public final IloNumVar[] a;
        public final IloNumVar[] f;
        public final IloNumVar[] g;
        public final IloNumVar[] alpha;
        public final IloNumVar[] beta;

        public final IloNumVar[] r;
        public final IloNumVar[] q;

        public final IloNumVar[] x;

        public final IloNumVar[] s;
        public final IloNumVar[] t;

        public final IloNumVar[] y;

        public final IloNumVar[] allVars;

        public Variables(int D, int N, int E) {
            this.a = new IloNumVar[D];
            this.f = new IloNumVar[N];
            this.g = new IloNumVar[N];
            this.alpha = new IloNumVar[N];
            this.beta = new IloNumVar[N];

            this.r = new IloNumVar[N];
            this.q = new IloNumVar[N];

            this.x = new IloNumVar[E];

            this.s = new IloNumVar[N];
            this.t = new IloNumVar[N];

            this.y = new IloNumVar[E];

            this.allVars = new IloNumVar[D + 8 * N + 2 * E];
        }
    }

    // variables:

    private final double INF;
    private final double STEP;

    private final PrintWriter log;

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Graph graph;
    private final int E;

    private final Variables v;

    private final IloCplex cplex;

    private int cnt_ans = 0;

    // constructor:

    public ConnectCallbackSolver(Matrix matrix, Graph graph, int TIME_LIMIT, double INF, double STEP) throws IloException, IOException {
        this.INF = INF;
        this.STEP = STEP;

        this.log = new PrintWriter("./logs/connect_callback_solver.txt", StandardCharsets.UTF_8);

        this.matrix = matrix;
        this.N = matrix.numRows();
        this.D = matrix.numCols();

        this.graph = graph;
        this.E = graph.getEdges().size();

        if (graph.getNodesCount() != N) {
            throw new RuntimeException("vertex count not equals with row count");
        }

        this.v = new Variables(D, N, E);

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT);

        addVariables();
        addObjective();
        addConstraint();
        addConnectConstraint();

        tuning();
    }

    // private methods:

    private void addVariables() throws IloException {
        for (int i = 0; i < D; i++) {
            v.a[i] = (cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("a", i)));
        }
        for (int i = 0; i < N; i++) {
            v.f[i] = (cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("f", i)));
            v.g[i] = (cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("g", i)));
            v.alpha[i] = (cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("alpha", i)));
            v.beta[i] = (cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("beta", i)));
        }
        for (int i = 0; i < N; i++) {
            v.r[i] = (cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("r", i)));
            v.q[i] = (cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("q", i)));
            v.s[i] = (cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("s", i)));
            v.t[i] = (cplex.numVar(0, INF, IloNumVarType.Float, varNameOf("t", i)));
        }
        for (int i = 0; i < E; i++) {
            v.x[i] = (cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("x", i)));
            v.y[i] = (cplex.numVar(0, 1, IloNumVarType.Int, varNameOf("y", i)));
        }

        int ind_var = 0;
        for (IloNumVar z : v.a) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.f) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.g) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.alpha) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.beta) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.r) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.q) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.x) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.s) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.t) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.y) v.allVars[ind_var++] = z;
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(cplex.prod(v.a[i], v.a[i]), N * 10);
        }
        IloNumExpr[] err_f = new IloNumExpr[N];
        for (int i = 0; i < err_f.length; i++) {
            err_f[i] = cplex.diff(v.f[i], v.q[i]);
            err_f[i] = cplex.prod(err_f[i], err_f[i]);
        }
        IloNumExpr[] err_g = new IloNumExpr[N];
        for (int i = 0; i < err_g.length; i++) {
            err_g[i] = cplex.diff(v.g[i], v.t[i]);
            err_g[i] = cplex.prod(err_g[i], err_g[i]);
        }
        cplex.addMaximize(cplex.diff(cplex.diff(cplex.sum(squares), cplex.sum(err_f)), cplex.sum(err_g)));
    }

    private double calcObjective(RawSolution sol) {
        double sum = 0;
        for (int i = 0; i < D; i++) {
            sum += sol.a[i] * sol.a[i] * N * 10;
        }
        for (int i = 0; i < N; i++) {
            double val = (sol.f[i] - sol.q[i]);
            sum -= val * val;
        }
        for (int i = 0; i < N; i++) {
            double val = (sol.g[i] - sol.t[i]);
            sum -= val * val;
        }
        return sum;
    }

    private void addConstraint() throws IloException {
        for (int i = 0; i < N; i++) {
            cplex.addEq(
                    cplex.scalProd(matrix.getRow(i), v.a),
                    cplex.diff(v.f[i], v.g[i])
            );
        }

        IloNumExpr[] l1normP = new IloNumExpr[N];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.sum(v.f[i], v.g[i]);
        }
        cplex.addEq(cplex.sum(l1normP), N);

        for (int i = 0; i < N; i++) {
            cplex.addLe(v.f[i], cplex.prod(v.alpha[i], INF));
            cplex.addLe(v.g[i], cplex.prod(v.beta[i], INF));
            cplex.addEq(cplex.sum(v.alpha[i], v.beta[i]), 1);
        }
    }

    private void addConnectConstraint() throws IloException {
        cplex.addEq(cplex.sum(v.r), 1);
        cplex.addEq(cplex.sum(v.s), 1);

        for (int num = 0; num < E; num += 2) {
            int back_num = Graph.companionEdge(num);
            if (back_num != num + 1) {
                throw new RuntimeException("unexpected back_num");
            }

            //Graph.checkEdges(graph, num, back_num);

            cplex.addLe(
                    cplex.sum(v.x[num], v.x[back_num]),
                    1
            );
            cplex.addLe(
                    cplex.sum(v.y[num], v.y[back_num]),
                    1
            );
        }

        for (int vertex = 0; vertex < N; vertex++) {
            IloNumVar[] input_edges_x = new IloNumVar[graph.edgesOf(vertex).size()];
            IloNumVar[] input_edges_y = new IloNumVar[graph.edgesOf(vertex).size()];
            int i_1 = 0;
            for (Pair<Integer, Long> to : graph.edgesOf(vertex)) {
                int num = to.second.intValue();
                int back_num = Graph.companionEdge(num);

                //Graph.checkEdges(graph, num, back_num);
                //Graph.checkDest(graph, back_num, vertex);

                input_edges_x[i_1] = (v.x[back_num]);
                input_edges_y[i_1] = (v.y[back_num]);
                i_1++;
            }

            cplex.addEq(
                    cplex.sum(cplex.sum(input_edges_x), v.r[vertex]),
                    1
            );
            cplex.addEq(
                    cplex.sum(cplex.sum(input_edges_y), v.s[vertex]),
                    1
            );
        }

        for (int num = 0; num < E; num++) {
            Pair<Integer, Integer> edge = graph.getEdges().get(num);
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(v.q[edge.first], v.q[edge.second])),
                    cplex.sum(cplex.prod(INF, v.x[num]), STEP)
            );
            cplex.addGe(
                    cplex.sum(INF, cplex.diff(v.t[edge.first], v.t[edge.second])),
                    cplex.sum(cplex.prod(INF, v.y[num]), STEP)
            );
        }
    }

    private void tuning() throws IloException {
        cplex.use(new ICACallback());
    }

    // callback:

    private class RawSolution {
        private static final double eps = 1e-6;
        public final double[] a;
        public final double[] f;
        public final double[] g;
        public final double[] alpha;
        public final double[] beta;
        public final double[] r;
        public final double[] q;
        public final double[] x;
        public final double[] s;
        public final double[] t;
        public final double[] y;

        private RawSolution(
                double[] a,
                double[] f,
                double[] g,
                double[] alpha,
                double[] beta,
                double[] r,
                double[] q,
                double[] x,
                double[] s,
                double[] t,
                double[] y
        ) {
            this.a = a;
            this.f = f;
            this.g = g;
            this.alpha = alpha;
            this.beta = beta;
            this.r = r;
            this.q = q;
            this.x = x;
            this.s = s;
            this.t = t;
            this.y = y;
        }

        public boolean adapt() {
            double[] p = mul(matrix, a);

            double l1norm = calcL1Norm(p);

            if (l1norm < 0.1) {
                return false;
            }

            double cff = 1;
            if (Math.abs(l1norm - matrix.numRows()) > eps) {
                cff = matrix.numRows() / l1norm;
            }

            for (int i = 0; i < a.length; i++) {
                a[i] *= cff;
            }

            double[] new_p = mul(matrix, a);

            for (int i = 0; i < new_p.length; i++) {
                if (new_p[i] > 0) {
                    f[i] = Math.abs(new_p[i]);
                    g[i] = 0;
                    alpha[i] = 1;
                    beta[i] = 0;
                } else {
                    f[i] = 0;
                    g[i] = Math.abs(new_p[i]);
                    alpha[i] = 0;
                    beta[i] = 1;
                }
            }

            if (Math.abs(calcL1Norm(new_p) - matrix.numRows()) > eps) {
                throw new RuntimeException("unexpected l1norm after adapt");
            }

            System.arraycopy(f, 0, q, 0, f.length);

            System.arraycopy(g, 0, t, 0, g.length);

//            for (int i = 0; i < graph.getEdges().size(); i++) {
//                Pair<Integer, Integer> edge = graph.getEdges().get(i);
//                x[i] = x[i] + q[edge.first] + q[edge.second];
//            }
//
//            for (int i = 0; i < graph.getEdges().size(); i++) {
//                Pair<Integer, Integer> edge = graph.getEdges().get(i);
//                y[i] = y[i] + t[edge.first] + t[edge.second];
//            }

            MST.solve(graph, x, q, r, STEP);

            MST.solve(graph, y, t, s, STEP);

            return true;
        }

        private static double calcL1Norm(double[] p) {
            double l1norm = 0;
            for (double val : p) {
                l1norm += Math.abs(val);
            }
            return l1norm;
        }

        private static double[] mul(Matrix matrix, double[] a) {
            return matrix.mult(new Matrix(a).transpose()).transpose().getRow(0);
        }

        @Override
        public String toString() {
            return "RawSolution{" +
                    "\n| l1norm = " + calcL1Norm(mul(matrix, a)) +
                    "\n| obj = " + calcObjective(this) +
                    "\n| a = " + Arrays.toString(a) +
                    "\n| f = " + Arrays.toString(f) +
                    "\n| g = " + Arrays.toString(g) +
                    "\n| alpha = " + Arrays.toString(alpha) +
                    "\n| beta = " + Arrays.toString(beta) +
                    "\n| r = " + Arrays.toString(r) +
                    "\n| q = " + Arrays.toString(q) +
                    "\n| x = " + Arrays.toString(x) +
                    "\n| s = " + Arrays.toString(s) +
                    "\n| t = " + Arrays.toString(t) +
                    "\n| y = " + Arrays.toString(y) +
                    "\n}";
        }
    }

    private class ICACallback extends IloCplex.HeuristicCallback {
        @Override
        protected void main() throws IloException {

            RawSolution sol = new RawSolution(
                    this.getValues((v.a)),
                    this.getValues((v.f)),
                    this.getValues((v.g)),
                    this.getValues((v.alpha)),
                    this.getValues((v.beta)),
                    this.getValues((v.r)),
                    this.getValues((v.q)),
                    this.getValues((v.x)),
                    this.getValues((v.s)),
                    this.getValues((v.t)),
                    this.getValues((v.y))
            );

            String oldStr = sol.toString();

            if (sol.adapt()) {

                String newStr = sol.toString();

                double calcObj = calcObjective(sol);

                cnt_ans++;

                log.println(cnt_ans);
                log.println("before: " + oldStr);
                log.println("after: " + newStr);
                log.println();

                try {
                    try (PrintWriter out_q = new PrintWriter("./answers/q.txt")) {
                        for (int i = 0; i < sol.q.length; i++) {
                            out_q.println(sol.q[i]);
                        }
                    }
                    try (PrintWriter out_x = new PrintWriter("./answers/x.txt")) {
                        for (int i = 0; i < sol.x.length; i++) {
                            out_x.println(sol.x[i]);
                        }
                    }
                    try (PrintWriter out_t = new PrintWriter("./answers/t.txt")) {
                        for (int i = 0; i < sol.t.length; i++) {
                            out_t.println(sol.t[i]);
                        }
                    }
                    try (PrintWriter out_y = new PrintWriter("./answers/y.txt")) {
                        for (int i = 0; i < sol.y.length; i++) {
                            out_y.println(sol.y[i]);
                        }
                    }
                    //DrawUtils.newDraw("./answers/", "tmp_ans" + cnt_ans++, graph);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                double[] vals = new double[v.allVars.length];
                int ind_var = 0;
                for (double z : sol.a) vals[ind_var++] = z;
                for (double z : sol.f) vals[ind_var++] = z;
                for (double z : sol.g) vals[ind_var++] = z;
                for (double z : sol.alpha) vals[ind_var++] = z;
                for (double z : sol.beta) vals[ind_var++] = z;
                for (double z : sol.r) vals[ind_var++] = z;
                for (double z : sol.q) vals[ind_var++] = z;
                for (double z : sol.x) vals[ind_var++] = z;
                for (double z : sol.s) vals[ind_var++] = z;
                for (double z : sol.t) vals[ind_var++] = z;
                for (double z : sol.y) vals[ind_var++] = z;

                if (calcObj >= getIncumbentObjValue()) {
                    //System.out.println("found new solution: " + calcObj);
                    setSolution(v.allVars, vals);
                }

            }
        }
    }


    // public methods:

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    public void writeVarsToFiles(PrintWriter out_q, PrintWriter out_x, PrintWriter out_t, PrintWriter out_y) throws IloException {
//        System.out.println("obj = " + cplex.getObjValue());
//        for (int i = 0; i < D; i++) {
//            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a.get(i)));
//        }
        // to file:
        for (int i = 0; i < v.q.length; i++) {
            out_q.println(cplex.getValue(v.q[i]));
        }
        for (int i = 0; i < v.x.length; i++) {
            out_x.println(cplex.getValue(v.x[i]));
        }
        for (int i = 0; i < v.t.length; i++) {
            out_t.println(cplex.getValue(v.t[i]));
        }
        for (int i = 0; i < v.y.length; i++) {
            out_y.println(cplex.getValue(v.y[i]));
        }
    }

    // private static methods:

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }

}