package unibs.it.dii.mhs;

import com.google.common.primitives.Booleans;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.mhs.model.SubMatrix;
import unibs.it.dii.utility.OutputFileWriter;
import unibs.it.dii.utility.OutputMatrixBuilder;

import java.io.IOException;
import java.util.*;

import static java.lang.Integer.min;
import static unibs.it.dii.mhs.MinimalHittingSet.bytesToMegaBytes;

public class MinimalHittingSetSolver {

    final static private String DOUBLE_LINE = "=========================================================================";
    final static private String LINE = "-------------------------------------------------------------------------";

    final static private String MSG_EXCEPTION_GET_FIRST_ELEMENT = "ATTENTION! Something went wrong with getFirstElement (i.e. get the first lexicographical element available)";
    final static private String MSG_EXCEPTION_CHECK_MODULE = "ATTENTION! Something went wrong with checkModule";

    private boolean outOfTime = false;
    private boolean outOfMemory = false;
    private long minCardinality = 0;
    private long maxCardinality = 0;
    private long executionTime = 0;
    private int numberMHSFound = 0;
    private long consumedMemory = 0;

    public long getConsumedMemory() {
        return consumedMemory;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long getExecutionTime) {
        this.executionTime = getExecutionTime;
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
     * Method to manage the solution of MHS problem.
     *
     * @param matrix
     * @param timeout
     * @param runtime
     * @param outputFileWriter
     * @param debugMode
     * @return The output matrix of the MHS found
     * @throws Exception
     */
    public Matrix computeMinimalHittingSets(Matrix matrix, long timeout, Runtime runtime, OutputFileWriter outputFileWriter, boolean debugMode, ArrayList<Integer> colsRemoved, int initialCols) throws Exception {
        // Reset the variables for each method call
        resetSolverVariables();

        // Create the output matrix object
        final Matrix mhsMatrix = new Matrix();
        // Object to create the matrix (boolean[][]) from an ArrayList
        final OutputMatrixBuilder outputMatrixBuilder = new OutputMatrixBuilder();
        // Object to build the information to write in the output file
        StringBuilder sbHeaderMBase = new StringBuilder();

        boolean[][] inputBoolMatrix = matrix.getBoolMatrix();

        try {

            long startTimeMBase = System.currentTimeMillis();

            // List of all MHS found
            ArrayList<boolean[]> mhsList = solve(inputBoolMatrix, timeout, debugMode, runtime);

            executionTime = System.currentTimeMillis() - startTimeMBase;

            // Number of MHS found
            numberMHSFound = mhsList.size();

            // Compute the min and max cardinality found
            if (!mhsList.isEmpty()) {
                minCardinality = checkCardinality(mhsList.get(0));
                maxCardinality = checkCardinality(mhsList.get(mhsList.size() - 1));
            }

            printMBaseExecutionInformation(runtime, executionTime);

            buildMBaseExecutionInformation(runtime, executionTime, sbHeaderMBase);

            // Write the information built before in the output report
            outputFileWriter.writeOutputFile(sbHeaderMBase);

            // MBase execution OUT OF MEMORY
            if (outOfMemory) {
                // Print and write the cause of interruption
                System.out.println("Execution interrupted > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(new StringBuilder("Execution interrupted > Cause: OUT OF MEMORY\n"));

                boolean[][] mhsBoolMatrix = outputMatrixBuilder.getMHSIntOutputMatrix(mhsList, inputBoolMatrix[0].length);

                try {
                    printMHSFoundUpToInterruption(mhsBoolMatrix, timeout, outputFileWriter, colsRemoved, initialCols);
                } catch (OutOfMemoryError me) {
                    outOfMemory = true;
                    System.err.println("Writing output file interrupted > Cause: OUT OF MEMORY\n");
                    outputFileWriter.writeOutputFile(new StringBuilder("Impossible to write the output matrix > Cause: OUT OF MEMORY\n"));
                }
            }

            // MBase execution OUT OF TIME
            if (outOfTime) {
                // Print and write the cause of interruption
                System.out.println("Execution interrupted > Cause: OUT OF TIME");
                outputFileWriter.writeOutputFile(new StringBuilder("Execution interrupted > Cause: OUT OF TIME\n"));

                boolean[][] mhsBoolMatrix = outputMatrixBuilder.getMHSIntOutputMatrix(mhsList, inputBoolMatrix[0].length);

                try {
                    printMHSFoundUpToInterruption(mhsBoolMatrix, timeout, outputFileWriter, colsRemoved, initialCols);
                } catch (OutOfMemoryError me) {
                    System.err.println("Impossible to print output matrix on .out file > Cause : OUT OF MEMORY");
                    outputFileWriter.writeOutputFile(new StringBuilder("Impossible to print output matrix on .out file > Cause : OUT OF MEMORY\n"));
                    outOfMemory = true;
//                System.exit(-1);
                }
            }

            try {

                boolean[][] mhsIntMatrix = outputMatrixBuilder.getMHSIntOutputMatrix(mhsList, inputBoolMatrix[0].length);

                mhsMatrix.setName(matrix.getName());
                mhsMatrix.getBoolMatrix(mhsIntMatrix);

            } catch (OutOfMemoryError me) {
                System.err.println("Impossible to get output matrix > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(new StringBuilder("Impossible to get output matrix > Cause: OUT OF MEMORY\n"));
                outOfMemory = true;
//            System.exit(-1);
            }

        } catch (OutOfMemoryError me) {
            System.err.println("OUT OF MEMORY");
            outOfMemory = true;
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

    /**
     * @param runtime
     * @param executionTime
     * @param sbHeaderMBase
     */
    private void buildMBaseExecutionInformation(Runtime runtime, long executionTime, StringBuilder sbHeaderMBase) {
        sbHeaderMBase.append(DOUBLE_LINE).append("\n");
        sbHeaderMBase.append("\t\t\t\tMBase").append("\n");
        sbHeaderMBase.append(DOUBLE_LINE).append("\n");
        sbHeaderMBase.append("Consumed memory (MBase): ").append(bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory())).append(" MB\n");
        sbHeaderMBase.append("MBase time: ").append(executionTime).append(" ms").append("\n");
        sbHeaderMBase.append("Minimum cardinality: ").append(minCardinality).append("\n");
        sbHeaderMBase.append("Maximum cardinality: ").append(maxCardinality).append("\n");
        sbHeaderMBase.append("Number of MHS found: ").append(numberMHSFound).append("\n");
    }

    /**
     * @param runtime
     * @param executionTime
     */
    private void printMBaseExecutionInformation(Runtime runtime, long executionTime) {
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
    private long checkCardinality(boolean[] e) {
        return Booleans.countTrue(e);
    }

    /**
     * @param mhsList
     * @param timeout
     * @param outputFileWriter
     * @throws IOException
     */
    private void printMHSFoundUpToOutOfMemory(ArrayList<boolean[]> mhsList, long timeout, OutputFileWriter outputFileWriter) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mhsList.size(); i++) {
            boolean[] mhs = mhsList.get(i);
            for (int j = 0; j < mhs.length; j++) {
                sb.append(mhs[j] ? 1 + " " : 0 + " ").append(" ");
            }
            sb.append("-\n");
        }

        try {
            outputFileWriter.writeOutputFile(sb);
        } catch (OutOfMemoryError | IOException me) {
            outOfMemory = true;
            outputFileWriter.writeOutputFile(new StringBuilder("Impossible to get output matrix > Cause: OUT OF MEMORY\n"));
            System.err.println("Writing output file interrupted > Cause: OUT OF MEMORY");
        }

//        System.exit(200);
    }

    /**
     * @param matrix
     * @param timeout
     * @param outputFileWriter
     * @param colsRemoved
     * @param initialCols
     * @throws IOException
     */
    private void printMHSFoundUpToInterruption(boolean[][] matrix, long timeout, OutputFileWriter outputFileWriter, ArrayList<Integer> colsRemoved, int initialCols) throws IOException {
//        for (int i = 0; i < mhsList.size(); i++) {
//            boolean[] mhs = mhsList.get(i);
//            for (int j = 0; j < mhs.length; j++) {
//                sb.append(mhs[j] ? 1 + " " : 0 + " ").append(" ");
//            }
//            sb.append("-\n");
//        }

        try {
            outputFileWriter.writeOutputMatrix(matrix, colsRemoved, initialCols);
        } catch (OutOfMemoryError me) {
            outOfMemory = true;
            outputFileWriter.writeOutputFile(new StringBuilder("Impossible to write the output matrix > Cause: OUT OF MEMORY\n"));
            System.err.println("Writing output file interrupted > Cause: OUT OF MEMORY");
//            System.exit(-1);
        }

        System.out.println("For more details: " + outputFileWriter.getOutputFile().getAbsolutePath());
//        System.exit(100);
    }

    /**
     * @param runtime
     * @param s
     */
    private void printUsedMemory(Runtime runtime, String s) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
//            System.out.println(s + memoryAfter + "bytes");
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
    private ArrayList<boolean[]> solve(boolean[][] boolMatrix, long timeout, boolean debugMode, Runtime runtime) throws Exception {
        final int rows = boolMatrix.length; // N = number of rows
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

            if (debugMode) {
                System.out.println(LINE);
                System.out.println("Successor: " + getSucc(getMax(e), cols));
            }

            for (int i = getSucc(getMax(e), cols); i < cols && (System.currentTimeMillis() - startTime) <= timeout; i++) {
                try {
                    boolean[] newE = Arrays.copyOf(e, e.length);
                    newE[i] = true;

                    if (debugMode)
                        System.out.println("Element: " + Arrays.toString(newE));

                    // Create the submatrix object
                    SubMatrix subMatrix = getSubMatrix(newE, boolMatrix);

                    if (debugMode)
                        System.out.println(subMatrix.toString());

                    // Compute the representative vector
                    final int[] rv = getRepresentativeVector(subMatrix);

                    // Scan the representative vector (subset of M)
                    int result = checkModule(rv, subMatrix.getElements(), debugMode);

                    if (debugMode) {
                        System.out.println("RV" + i + ": " + Arrays.toString(rv));
                        System.out.println("Result: " + (result == 2 ? "MHS" : (result == 1 ? "OK" : "KO")));
                    }

                    if (result == 1 && i < cols - 1) // OK && NOT(last lexicographical element)
                        queue.add(newE); // Add the element to the queue (MHS aspirant)

                    if (result == 2) // MHS
                        mhsList.add(newE); // Add the element to MHS list

                    if (debugMode)
                        System.out.println(DOUBLE_LINE);
                } catch (OutOfMemoryError me) {
                    System.err.println("Execution interrupted > Cause: OUT OF MEMORY");
                    outOfMemory = true;
                    consumedMemory = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());
                    queue.clear(); // More free memory space

                    // Return the MHS computed until memory saturation
                    return mhsList;
                }
            }

        }

        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > timeout) {
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
    private int checkModule(int[] rv, ArrayList<Integer> elements, boolean debugMode) throws Exception {
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

        if (debugMode)
            System.out.println("elementsFound: " + elementsFound.toString());

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

        if (debugMode)
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

        // This line is reached only if there are some logical problems in the algorithm
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

//                    // Can be removed [?]
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
