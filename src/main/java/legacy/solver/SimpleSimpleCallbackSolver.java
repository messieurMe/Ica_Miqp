package legacy.solver;

import ilog.concert.*;
import ilog.cplex.*;
import legacy.utils.Matrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SimpleSimpleCallbackSolver implements MySolver {
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

        public final IloNumVar[] allVars;

        public Variables(int D, int N) {
            this.a = new IloNumVar[D];
            this.f = new IloNumVar[N];
            this.g = new IloNumVar[N];
            this.alpha = new IloNumVar[N];
            this.beta = new IloNumVar[N];

            this.allVars = new IloNumVar[D + 4 * N];
        }
    }

    // variables:

    private final double INF;
    private final double STEP;

    private final PrintWriter log;

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Variables v;

    private final IloCplex cplex;

    // constructor:

    public SimpleSimpleCallbackSolver(Matrix matrix, int TIME_LIMIT, double INF, double STEP) throws IloException, IOException {
        System.out.println(matrix.numCols());
        this.INF = INF;
        this.STEP = STEP;

        this.log = new PrintWriter("./logs/simple_callback_solver.txt", StandardCharsets.UTF_8);

        this.matrix = matrix;
        this.N = matrix.numRows();
        this.D = matrix.numCols();

        this.v = new Variables(D, N);

        this.cplex = new IloCplex();
        this.cplex.setParam(IloCplex.Param.OptimalityTarget, IloCplex.OptimalityTarget.OptimalGlobal);
        this.cplex.setParam(IloCplex.Param.TimeLimit, TIME_LIMIT);

        addVariables();
        addObjective();
        addConstraint();

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

        int ind_var = 0;
        for (IloNumVar z : v.a) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.f) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.g) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.alpha) v.allVars[ind_var++] = z;
        for (IloNumVar z : v.beta) v.allVars[ind_var++] = z;
    }

    private void addObjective() throws IloException {
        IloNumExpr[] squares = new IloNumExpr[D];
        for (int i = 0; i < squares.length; i++) {
            squares[i] = cplex.prod(v.a[i], v.a[i]);
        }
        cplex.addMaximize(cplex.sum(squares));
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

    // public methods:

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    @Override
    public void writeVarsToFiles(PrintWriter q, PrintWriter x, PrintWriter t, PrintWriter y) throws Exception {

    }

    public void writeVarsToFiles(PrintWriter out_f, PrintWriter out_g, Matrix matrix12, ArrayList<String> listList, ArrayList<Double> icaDirection) throws IloException {
        System.out.println("RESULT!!!!!!!!!!!!!");
        System.out.println("D=" + D);
        System.out.println("obj = " + cplex.getObjValue());
        if(icaDirection.size() != 0){
            System.out.println("ERROR NOT EMPTY");
            int a = 1/0;
        }
        for (int i = 0; i < D; i++) {
            double ii = cplex.getValue(v.a[i]);
            System.out.println(varNameOf("a", i) + " = " + ii);
            icaDirection.add(ii);
        }
        for(int i = 0; i < N; i++){
            out_f.println(cplex.getValue(v.f[i]));
            out_g.println(cplex.getValue(v.g[i]));
        }
    }
    // private static methods:

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }
}
