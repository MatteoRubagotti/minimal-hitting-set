package unibs.it.dii.mhs;

import com.google.common.primitives.Booleans;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.mhs.model.SubMatrix;
import unibs.it.dii.utility.OutputFileWriter;
import unibs.it.dii.utility.OutputMatrixBuilder;

import java.util.*;

import static java.lang.Integer.min;
import static unibs.it.dii.mhs.MinimalHittingSetFacade.bytesToMegaBytes;

/**
 *
 */
public class MinimalHittingSetSolver {

    final static private String DOUBLE_LINE = "=========================================================================";
    final static private String LINE = "-------------------------------------------------------------------------";

    final static private String MSG_EXCEPTION_GET_FIRST_ELEMENT = "ATTENTION! Something went wrong with getFirstElement (i.e. get the first lexicographical element available)";
    final static private String MSG_EXCEPTION_CHECK_MODULE = "ATTENTION! Something went wrong with checkModule";

    private boolean outOfTime;
    private boolean outOfMemory;
    private long minCardinality;
    private long maxCardinality;
    private long executionTime;
    private int numberMHSFound;
    private long consumedMemory;
    private boolean debug;

    public MinimalHittingSetSolver(boolean debug) {
        this.outOfTime = false;
        this.outOfMemory = false;
        this.minCardinality = 0;
        this.maxCardinality = 0;
        this.executionTime = 0;
        this.numberMHSFound = 0;
        this.consumedMemory = 0;
        this.debug = debug;
    }

    public long getConsumedMemory() {
        return consumedMemory;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public int getNumberMHSFound() {
        return numberMHSFound;
    }

    public long getMinCardinality() {
        return minCardinality;
    }

    public long getMaxCardinality() {
        return maxCardinality;
    }

    public boolean isOutOfTime() {
        return outOfTime;
    }

    public boolean isOutOfMemory() {
        return outOfMemory;
    }

    /**
     * Method to compute the solution of MHS problem.
     *
     * @param matrix
     * @param timeout
     * @param outputFileWriter
     * @return The output matrix of the all MHS found
     * @throws Exception
     */
    public Matrix execute(Matrix matrix, long timeout, OutputFileWriter outputFileWriter) throws Exception {
        // Reset the variables for each method call
        resetSolverVariables();

        // Create the output matrix object
        final Matrix mhsMatrix = new Matrix();
        // Object to create the matrix (boolean[][]) from an ArrayList
        final OutputMatrixBuilder outputMatrixBuilder = new OutputMatrixBuilder();
        // Object to build the information to write in the output file
        StringBuilder sbHeaderMBase = new StringBuilder();

        boolean[][] inputBoolMatrix = matrix.getBoolMatrix();
        // Matrix error to handle in MHSFacade otherwise it will be set to the output matrix
        boolean[][] mhsBoolMatrix = new boolean[1][1];

        Runtime runtime = Runtime.getRuntime();

        try {

            long startTimeMBase = System.currentTimeMillis();

            // List of all MHS found
            ArrayList<boolean[]> mhsList = solve(inputBoolMatrix, timeout, runtime);

            executionTime = System.currentTimeMillis() - startTimeMBase;

            // Number of MHS found
            numberMHSFound = mhsList.size();

            // Compute the min and max cardinality found
            if (!mhsList.isEmpty()) {
                minCardinality = getCardinality(mhsList.get(0));
                maxCardinality = getCardinality(mhsList.get(mhsList.size() - 1));
            }

            printMBaseExecutionInformation(runtime);

            // MBase execution OUT OF MEMORY
            if (outOfMemory) {
                // Write the cause of interruption
                outputFileWriter.writeOutputFile(new StringBuilder("Execution interrupted > Cause: OUT OF MEMORY\n"));
            }

            // MBase execution OUT OF TIME
            if (outOfTime) {
                // Write the cause of interruption
                outputFileWriter.writeOutputFile(new StringBuilder("Execution interrupted > Cause: OUT OF TIME\n"));
            }

            try {

                if (!mhsList.isEmpty())
                    mhsBoolMatrix = outputMatrixBuilder.getMHSBoolOutputMatrix(mhsList, inputBoolMatrix[0].length);

            } catch (OutOfMemoryError me) {
                System.err.println("Impossible to get output matrix > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(new StringBuilder("Impossible to get output matrix > Cause: OUT OF MEMORY\n"));
            }

            mhsMatrix.setName(matrix.getName());
            mhsMatrix.setBoolMatrix(mhsBoolMatrix);

        } catch (OutOfMemoryError me) {
            System.err.println("Problems with the execution of MBase > Cause: OUT OF MEMORY");
            outputFileWriter.writeOutputFile(new StringBuilder("Problems with the execution of MBase > Cause: OUT OF MEMORY\n"));
            mhsMatrix.setBoolMatrix(new boolean[1][1]); // Error matrix
        }

        return mhsMatrix;
    }

    /**
     * Method to reset the internal state of the solver.
     */
    private void resetSolverVariables() {
        this.outOfMemory = false;
        this.outOfTime = false;
        this.numberMHSFound = 0;
        this.executionTime = 0;
        this.maxCardinality = 0;
        this.minCardinality = 0;
        this.consumedMemory = 0;
    }

//    /**
//     * @param runtime
//     * @param sbHeaderMBase
//     */
//    private void buildMBaseExecutionInformation(Runtime runtime, StringBuilder sbHeaderMBase) {
//        sbHeaderMBase.append(DOUBLE_LINE).append("\n");
//        sbHeaderMBase.append("\t\t\t\tMBase").append("\n");
//        sbHeaderMBase.append(DOUBLE_LINE).append("\n");
//        sbHeaderMBase.append("Consumed memory (MBase): ").append(bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory())).append(" MB\n");
//        sbHeaderMBase.append("MBase time: ").append(executionTime).append(" ms").append("\n");
//        sbHeaderMBase.append("Minimum cardinality: ").append(minCardinality).append("\n");
//        sbHeaderMBase.append("Maximum cardinality: ").append(maxCardinality).append("\n");
//        sbHeaderMBase.append("Number of MHS found: ").append(numberMHSFound).append("\n");
//    }

    /**
     * @param runtime
     */
    private void printMBaseExecutionInformation(Runtime runtime) {
        System.out.println("Number of MHS found (in " + executionTime + " ms)" + ": " + numberMHSFound);
        System.out.println("Minimum cardinality: " + minCardinality);
        System.out.println("Maximum cardinality: " + maxCardinality);
        printUsedMemory(runtime, "Consumed memory (MBase): ");
    }

    /**
     * Method to compute the cardinality of the MHS found.
     *
     * @param e
     * @return
     */
    private int getCardinality(boolean[] e) {
        return Booleans.countTrue(e);
    }

    /**
     * @param runtime
     * @param s
     */
    private void printUsedMemory(Runtime runtime, String s) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        System.out.println(s + bytesToMegaBytes(memoryAfter) + "MB");
    }

    /**
     * MBase procedure
     *
     * @param boolMatrix The input matrix from benchmark file (e.g. ####.####.matrix)
     * @param timeout    The maximum time limit
     * @param runtime
     * @return The list of MHS found
     */
    private ArrayList<boolean[]> solve(boolean[][] boolMatrix, long timeout, Runtime runtime) throws Exception {
//        final int rows = boolMatrix.length; // N = number of rows
        final int cols = boolMatrix[0].length; // number of columns = X <= M

        // Create the list of MHS
        final ArrayList<boolean[]> mhsList = new ArrayList<>();
        // Create the queue to store the sets lexicographical elements
        final Queue<boolean[]> queue = new LinkedList<>();

        // Add empty vector [0 0 ... 0]
        queue.add(new boolean[cols]);

        long startTime = System.currentTimeMillis();

        while (!queue.isEmpty() && (System.currentTimeMillis() - startTime) <= timeout) {
            // Get the first element of the queue
            boolean[] e = queue.poll();

            if (debug) {
                System.out.println(LINE);
                System.out.println("Successor: " + getSucc(getMax(e), cols));
            }

            for (int i = getSucc(getMax(e), cols); i < cols && (System.currentTimeMillis() - startTime) <= timeout; i++) {
                try {
                    boolean[] newE = Arrays.copyOf(e, e.length);
                    newE[i] = true;

                    int currentCardinality = getCardinality(newE);

                    if (minCardinality == 0)
                        minCardinality = currentCardinality;

                    if (maxCardinality < currentCardinality)
                        maxCardinality = currentCardinality;

                    if (debug)
                        System.out.println("Element: " + Arrays.toString(newE));

                    // Create the submatrix object
                    SubMatrix subMatrix = getSubMatrix(newE, boolMatrix);

                    if (debug)
                        System.out.println(subMatrix.toString());

                    // Compute the representative vector
                    final int[] rv = getRepresentativeVector(subMatrix);

                    // Scan the representative vector (subset of M)
                    int result = checkModule(rv, subMatrix.getElements());

                    if (debug) {
                        System.out.println("RV" + i + ": " + Arrays.toString(rv));
                        System.out.println("Result: " + (result == 2 ? "MHS" : (result == 1 ? "OK" : "KO")));
                    }

                    if (result == 1 && i < cols - 1) // OK && NOT(last lexicographical element)
                        queue.add(newE); // Add the element to the queue (MHS aspirant)

                    if (result == 2) // MHS
                        mhsList.add(newE); // Add the element to MHS list

                    if (debug)
                        System.out.println(DOUBLE_LINE);
                } catch (OutOfMemoryError me) {
                    System.err.println("Execution interrupted > Cause: OUT OF MEMORY");
                    consumedMemory = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());
                    outOfMemory = true;
                    queue.clear(); // More free memory space

                    // Return the MHS computed until memory saturation
                    return mhsList;
                }
            }

        }

        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > timeout) {
            System.err.println("Execution interrupted > Cause: OUT OF TIME");
            outOfTime = true;
            consumedMemory = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());
            queue.clear(); // More free memory space

            return mhsList;
        }

        consumedMemory = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());
        queue.clear();

        // Return the list of MHS computed anyway
        return mhsList;
    }

    /**
     * Method to print the matrix (debug purpose)
     *
     * @param matrix
     */
    private void printBoolMatrix(boolean[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                System.out.print(matrix[i][j]);
            }
            System.out.println(" -");
        }
    }

    /**
     * Method to compute the submatrix.
     *
     * @param e
     * @param matrix
     * @return
     * @throws Exception
     */
    private SubMatrix getSubMatrix(boolean[] e, boolean[][] matrix) throws Exception {
        // Number of columns of the submatrix (i.e. number of 1 in e[])
        int numOfCols = getNumberOfElements(e);

        boolean[][] boolSubMatrix = new boolean[matrix.length][numOfCols];
        // Elements (columns) that make up the submatrix
        ArrayList<Integer> elements = new ArrayList<>();
        int col = 0; // Columns counter for the submatrix (col < inputMatrix[0].length)

        for (int k = getFirstElement(e); k < e.length; k++) {
            if (e[k] && col < numOfCols) {
                elements.add(k);
                for (int i = 0; i < matrix.length; i++) { // rows
                    boolSubMatrix[i][col] = matrix[i][k];
                }
                col++;
            }
        }

        return new SubMatrix(elements, boolSubMatrix);
    }

    /**
     * Method to compute the number of active lexicographical elements in the element e[].
     *
     * @param e
     * @return
     */
    private int getNumberOfElements(boolean[] e) {
        return Booleans.countTrue(e);
    }

    /**
     * Method to scan the representative vector and get the scalar result.
     *
     * @param rv       The representative vector
     * @param elements The lexicographical elements considered
     * @return MHS = 2, OK = 1, KO = 0
     */
    private int checkModule(int[] rv, ArrayList<Integer> elements) throws Exception {
        // RV does not have 0 inside
        boolean empty = false;

        // List of elements found inside the RV
        ArrayList<Integer> elementsFound = new ArrayList<>();

        for (int j = 0; j < elements.size(); j++) {
            for (int i = 0; i < rv.length; i++) {
                if (rv[i] == 0 && !empty) { // The cell(i) of RV is empty
                    empty = true;
                    continue;
                }
                // The cell(i) is the lexicographical element to find in order to reach OK or MHS result
                if (!elementsFound.contains(elements.get(j)) && rv[i] == elements.get(j) + 1) {
                    elementsFound.add(elements.get(j));
                }
            }
        }

        if (debug)
            System.out.println("elementsFound: " + elementsFound);

        // RV is completely projected on the lexicographical element considered (i.e. P(RV) = E)
        boolean rvFullProjected = true;

        // Check all the elements found
        for (int i = 0; i < elements.size(); i++) {
            if (elementsFound.contains(elements.get(i)))
                continue;

            // The i-th element (i.e. column) is not found
            rvFullProjected = false;
            break;
        }

        if (debug)
            System.out.println("empty: " + empty + "\nprojection: " + rvFullProjected);

        // RV does not contain 0 and P(RV) = E
        if (!empty && rvFullProjected) // MHS
            return 2;

        // RV contains at least a 0 and P(RV) = E
        if (empty && rvFullProjected) // OK (MHS candidate)
            return 1;

        // P(RV) != E
        if (!rvFullProjected) // KO
            return 0;

        // This line is reached only if there are some logical problems in the algorithm!
        throw new Exception(MSG_EXCEPTION_CHECK_MODULE);
    }

    /**
     * Method to compute a representative vector.
     *
     * @param subMatrix
     * @return
     * @throws Exception
     */
    private int[] getRepresentativeVector(SubMatrix subMatrix) throws Exception {
        boolean[][] boolSubMatrix = subMatrix.getBoolMatrix();
        // RV has the submatrix number of rows (also of the input matrix)
        int[] rv = new int[boolSubMatrix.length];

        for (int j = 0; j < boolSubMatrix[0].length; j++) { // j = cols
            for (int i = 0; i < boolSubMatrix.length; i++) { // i = rows
                // Check the elements considered in the submatrix
                if (!subMatrix.getElements().isEmpty()) {
                    if (rv[i] == -1 && boolSubMatrix[i][j]) {
                        rv[i] = -1; // x-value (i.e. the i-th set intersect at least 2 elements of submatrix)
                        continue;
                    }

                    if (rv[i] == 0 && boolSubMatrix[i][j]) {
                        rv[i] = subMatrix.getElements().get(j) + 1; // Store the "real" value of the column
                        continue;
                    }

                    if (rv[i] != (subMatrix.getElements().get(j) + 1) && boolSubMatrix[i][j]) {
                        rv[i] = -1; // x-value (i.e. the i-th set intersect at least 2 elements of submatrix)
//                        continue;
                    }

//                    // Can be removed
//                    if (rv[i] == (subMatrix.getElements().get(j) + 1) && boolSubMatrix[i][j]) {
//                        rv[i] = subMatrix.getElements().get(j) + 1; //
//                    }
                }

            }
        }

        return rv;
    }

    /**
     * Method to compute the first element in lexicographical order.
     *
     * @param e
     * @return the position of the first lexicographical element
     */
    private int getFirstElement(boolean[] e) throws Exception {
        for (int i = 0; i < e.length; i++) {
            if (e[i]) {
                return i;
            }
        }

        // This line is never reached if the algorithm logic is correct!
        throw new Exception(MSG_EXCEPTION_GET_FIRST_ELEMENT);
    }

    /**
     * Method to get the successor of the element considered.
     *
     * @param element
     * @param lengthM
     * @return
     */
    private int getSucc(int element, int lengthM) {
        return min(element + 1, lengthM - 1);
    }

    /**
     * Method to compute the max of the lexicographical element passed.
     *
     * @param e
     * @return
     */
    private int getMax(boolean[] e) {
        int max = e.length;

        do {
            max--;
        } while ((max > -1) && (!e[max]));

        return max;
    }

}
