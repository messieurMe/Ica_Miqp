package legacy.solver;

import legacy.utils.Matrix;

import java.io.Closeable;
import java.io.PrintWriter;
import java.util.ArrayList;

public interface MySolver extends Closeable {
    boolean solve() throws Exception;

    void writeVarsToFiles(PrintWriter q, PrintWriter x, PrintWriter t, PrintWriter y) throws Exception;
}
