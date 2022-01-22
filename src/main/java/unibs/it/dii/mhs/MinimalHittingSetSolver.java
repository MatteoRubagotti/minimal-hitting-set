package unibs.it.dii.mhs;

import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.mhs.model.SubMatrix;
import unibs.it.dii.utility.OutputFileWriter;
import unibs.it.dii.utility.OutputMatrixBuilder;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private int mhsFound = 0;

    public long getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(long getExecutionTime) {
        this.executionTime = getExecutionTime;
    }

    public int getMhsFound() {
        return mhsFound;
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

    public Matrix computeMinimalHittingSets(Matrix matrix, long timeout, Runtime runtime, OutputFileWriter outputFileWriter, boolean debugMode) throws Exception {
        // Create the output matrix object
        final Matrix mhsMatrix = new Matrix();
        // Object to create the matrix (int[][]) from an ArrayList
        final OutputMatrixBuilder outputMatrixBuilder = new OutputMatrixBuilder();
        // Object to build the information to write in the output file
        final StringBuilder sbHeaderMBase = new StringBuilder();

        final int[][] inputIntMatrix = matrix.getIntMatrix();

        try {

            long startTimeMBase = System.currentTimeMillis();

            // List of all MHS found
            final ArrayList<int[]> mhsList = solve(inputIntMatrix, timeout, debugMode);

            executionTime = System.currentTimeMillis() - startTimeMBase;

            // Number of MHS found
            mhsFound = mhsList.size();

            // Compute the min and max cardinality found
            minCardinality = checkCardinality(mhsList.get(0));
            maxCardinality = checkCardinality(mhsList.get(mhsList.size() - 1));

            printMBaseExecutionInformation(runtime, mhsList, executionTime);

            buildMBaseExecutionInformation(runtime, mhsList, executionTime, sbHeaderMBase);

            // Write the information built before in the output report
            outputFileWriter.writeOutputFile(sbHeaderMBase);

            // MBase execution OUT OF MEMORY
            if (outOfMemory) {
                // Print and write the cause of interruption
                System.out.println("Execution interrupted > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(new StringBuilder("Execution interrupted > Cause: OUT OF MEMORY\n"));

                try {
                    printMHSFoundUpToOutOfMemory(mhsList, timeout, outputFileWriter);
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

                try {
                    printMHSFoundUpToTimeout(mhsList, timeout, outputFileWriter);
                } catch (OutOfMemoryError me) {
                    System.err.println("Impossible to print output matrix on .out file > Cause : OUT OF MEMORY");
                    outputFileWriter.writeOutputFile(new StringBuilder("Impossible to print output matrix on .out file > Cause : OUT OF MEMORY\n"));
                    outOfMemory = true;
//                System.exit(-1);
                }
            }

            try {

                int[][] mhsIntMatrix = outputMatrixBuilder.getMHSIntOutputMatrix(mhsList, inputIntMatrix[0].length);

                mhsMatrix.setName(matrix.getName());
                mhsMatrix.setIntMatrix(mhsIntMatrix);

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
     * @param runtime
     * @param mhsList
     * @param executionTime
     * @param sbHeaderMBase
     */
    private void buildMBaseExecutionInformation(Runtime runtime, ArrayList<int[]> mhsList, long executionTime, StringBuilder sbHeaderMBase) {
        sbHeaderMBase.append(DOUBLE_LINE).append("\n");
        sbHeaderMBase.append("\t\t\t\tMBase").append("\n");
        sbHeaderMBase.append(DOUBLE_LINE).append("\n");
        sbHeaderMBase.append("Consumed memory (MBase): ").append(bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory())).append(" MB\n");
        sbHeaderMBase.append("MBase time: ").append(executionTime).append(" ms").append("\n");
        sbHeaderMBase.append("Minimum cardinality: ").append(minCardinality).append("\n");
        sbHeaderMBase.append("Maximum cardinality: ").append(maxCardinality).append("\n");
        sbHeaderMBase.append("Number of MHS found: ").append(mhsList.size()).append("\n");
    }

    /**
     * @param runtime
     * @param mhsList
     * @param executionTime
     */
    private void printMBaseExecutionInformation(Runtime runtime, ArrayList<int[]> mhsList, long executionTime) {
        System.out.println("Number of MHS found (in " + executionTime + " ms)" + ": " + mhsList.size());
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
    private long checkCardinality(int[] e) {
        return Arrays.stream(e).filter(i -> i == 1).count();
    }

    /**
     * @param mhsList
     * @param timeout
     * @param outputFileWriter
     * @throws IOException
     */
    private void printMHSFoundUpToOutOfMemory(ArrayList<int[]> mhsList, long timeout, OutputFileWriter outputFileWriter) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mhsList.size(); i++) {
            int[] mhs = mhsList.get(i);
            for (int j = 0; j < mhs.length; j++) {
                sb.append(mhs[j]).append(" ");
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
     * @param mhsList
     * @param timeout
     * @param outputFileWriter
     * @throws IOException
     */
    private void printMHSFoundUpToTimeout(ArrayList<int[]> mhsList, long timeout, OutputFileWriter outputFileWriter) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mhsList.size(); i++) {
            int[] mhs = mhsList.get(i);
            for (int j = 0; j < mhs.length; j++) {
                sb.append(mhs[j]).append(" ");
            }
            sb.append("-\n");
        }

        try {
            outputFileWriter.writeOutputFile(sb);
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
     * @param intMatrix The input matrix from benchmark file (e.g. ####.####.matrix)
     * @param timeout   The maximum time limit
     * @return The list of MHS found
     */
    private ArrayList<int[]> solve(int[][] intMatrix, long timeout, boolean debugMode) throws Exception {
        final int rows = intMatrix.length; // N = number of rows
        final int cols = intMatrix[0].length; // number of columns = X <= M

        // Create the list of MHS
        final ArrayList<int[]> mhsList = new ArrayList<>();
        // Create the queue to store the sets lexicographical elements
        final Queue<int[]> queue = new LinkedList<>();

        // Add empty vector [0 0 ... 0]
        queue.add(new int[cols]);

        long startTime = System.currentTimeMillis();

        while (!queue.isEmpty() && (System.currentTimeMillis() - startTime) <= timeout) {
            // Get the first element of the queue
            int[] e = queue.poll();

            if (debugMode) {
                System.out.println(LINE);
                System.out.println("Successor: " + getSucc(getMax(e), cols));
            }

            for (int i = getSucc(getMax(e), cols); i < cols && (System.currentTimeMillis() - startTime) <= timeout; i++) {
                try {
                    int[] newE = Arrays.copyOf(e, e.length);
                    newE[i] = 1;

                    if (debugMode)
                        System.out.println("Element: " + Arrays.toString(newE));

                    // Create the submatrix object
                    SubMatrix subMatrix = getSubMatrix(newE, intMatrix);

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
                    queue.clear(); // more free space

                    // Return the MHS computed until memory saturation
                    return mhsList;
                }
            }

        }

        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > timeout) {
            outOfTime = true;
            queue.clear(); // more free space
        }

        // Return the MHS computed anyway
        return mhsList;
    }

    /**
     * Method to print the matrix (debug purpose)
     *
     * @param matrix
     */
    private void printMatrix(int[][] matrix) {
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
    private SubMatrix getSubMatrix(int[] e, int[][] matrix) throws Exception {
        // Number of columns of the submatrix (i.e. number of 1 in e[])
        int numOfCols = getNumberOfElements(e);

        int[][] intSubMatrix = new int[matrix.length][numOfCols];
        // Elements (columns) that make up the submatrix
        ArrayList<Integer> elements = new ArrayList<>();
        int col = 0; // Columns counter for the submatrix (col < inputMatrix[0].length)

        for (int k = getFirstElement(e); k < e.length; k++) {
            if (e[k] == 1 && col < numOfCols) {
                elements.add(k);
                for (int i = 0; i < matrix.length; i++) { // rows
                    intSubMatrix[i][col] = matrix[i][k];
                }
                col++;
            }
        }

        return new SubMatrix(elements, intSubMatrix);
    }

    /**
     * Method to compute the number of active lexicographical elements in the element e[].
     *
     * @param e
     * @return
     */
    private int getNumberOfElements(int[] e) {
        List<Integer> eList = Arrays.stream(e).boxed().collect(Collectors.toList());

        return Collections.frequency(eList, 1);
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
        int[][] intSubMatrix = subMatrix.getIntMatrix();
        // RV has the submatrix number of rows (also of the input matrix)
        int[] rv = new int[intSubMatrix.length];

        for (int j = 0; j < intSubMatrix[0].length; j++) { // j = cols
            for (int i = 0; i < intSubMatrix.length; i++) { // i = rows
                // Check the elements considered in the submatrix
                if (!subMatrix.getElements().isEmpty()) {
                    if (rv[i] == -1 && intSubMatrix[i][j] == 1) {
                        rv[i] = -1; // x-value (i.e. the i-th set intersect at least 2 elements of submatrix)
                        continue;
                    }

                    if (rv[i] == 0 && intSubMatrix[i][j] == 1) {
                        rv[i] = subMatrix.getElements().get(j) + 1; // Store the "real" value of the column
                        continue;
                    }

                    if (rv[i] != (subMatrix.getElements().get(j) + 1) && intSubMatrix[i][j] == 1) {
                        rv[i] = -1; // x-value (i.e. the i-th set intersect at least 2 elements of submatrix)
                        continue;
                    }

//                    // Can be removed [?]
//                    if (rv[i] == (subMatrix.getElements().get(j) + 1) && intSubMatrix[i][j] == 1) {
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
    private int getFirstElement(int[] e) throws Exception {
        for (int i = 0; i < e.length; i++) {
            if (e[i] == 1) {
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
    private int getMax(int[] e) {
        int max = e.length;

        do {
            max--;
        } while ((max > -1) && (e[max] == 0));

        return max;
    }

}
