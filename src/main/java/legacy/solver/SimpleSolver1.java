package legacy.solver;

import ilog.concert.*;
import ilog.cplex.*;
import legacy.utils.Matrix;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

public class SimpleSolver1 implements MySolver {
    @Override
    public void close() throws IOException {

    }
    // data class:

    private static class Variables {
        public final IloNumVar[] a;
        public final IloNumVar[] p;

        public Variables(int D, int N) {
            this.a = new IloNumVar[D];
            this.p = new IloNumVar[N];
        }
    }

    // variables:

    private final double INF;

    private final Matrix matrix;
    private final int D;
    private final int N;

    private final Variables v;

    private final IloCplex cplex;

    // constructor:

    public SimpleSolver1(Matrix matrix, int TIME_LIMIT, double INF) throws IloException {
        this.INF = INF;

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
            v.p[i] = (cplex.numVar(-INF, INF, IloNumVarType.Float, varNameOf("p", i)));
        }
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
            cplex.addEq(cplex.scalProd(matrix.getRow(i), v.a), v.p[i]);
        }

        IloNumExpr[] l1normP = new IloNumExpr[N];
        for (int i = 0; i < l1normP.length; i++) {
            l1normP[i] = cplex.abs(v.p[i]);
        }
        cplex.addEq(cplex.sum(l1normP), N);
    }

    // public methods:

    public boolean solve() throws IloException {
        return cplex.solve();
    }

    @Override
    public void writeVarsToFiles(PrintWriter q, PrintWriter x, PrintWriter t, PrintWriter y) throws Exception {

    }

    //    @Override
    public void writeVarsToFiles(PrintWriter q, PrintWriter x, Matrix matrix12, ArrayList<String> listList, ArrayList<Double> icaDirection) throws Exception {
        writeVarsToFiles(q, icaDirection);
    }

    public void writeVarsToFiles(PrintWriter out, ArrayList<Double> icaDirection) throws Exception {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < D; i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a[i]));
        }
        for (int i = 0; i < N; i++) {
            double r = cplex.getValue(v.p[i]);
            out.println(cplex.getValue(v.p[i]));
            System.out.println("OUTPUT 114");
            System.out.println(r);
            out.println(r);
//            icaDirection.add(r);
        }
    }

    public void writeVars() throws Exception {
        System.out.println("obj = " + cplex.getObjValue());
        for (int i = 0; i < D; i++) {
            System.out.println(varNameOf("a", i) + " = " + cplex.getValue(v.a[i]));
        }
        for (int i = 0; i < N; i++) {
            double r = cplex.getValue(v.p[i]);
            System.out.print(r + " ");
//            icaDirection.add(r);
        }
    }

    private static String varNameOf(String arg1, int arg2) {
        return arg1 + arg2;
    }
}
