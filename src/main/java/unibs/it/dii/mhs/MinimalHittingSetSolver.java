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
    final static private String LINE = "-------------------------------------------------------------------------------------------------------------------------";
    final static private String DOUBLE_LINE = "=========================================================================================================================";

    final static private String MSG_EXCEPTION_GET_FIRST_ELEMENT = "ATTENTION! Something went wrong with getFirstElement (i.e. get the first lexicographical element available)";
    final static private String MSG_EXCEPTION_CHECK_MODULE = "ATTENTION! Something went wrong with checkModule";

    private boolean debugMode;
    private boolean outOfTime = false;
    private boolean outOfMemory = false;
    private long minCardinality = 0;
    private long maxCardinality = 0;

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

    public void setOutOfTime(boolean outOfTime) {
        this.outOfTime = outOfTime;
    }

    public MinimalHittingSetSolver(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public Matrix computeMinimalHittingSets(Matrix matrix, long timeout, Runtime runtime, OutputFileWriter outputFileWriter) throws Exception {
        final Matrix mhsMatrix = new Matrix(); // Output matrix
        final OutputMatrixBuilder outputMatrixBuilder = new OutputMatrixBuilder();
        final int[][] inputIntMatrix = matrix.getIntMatrix();

        try {
            ArrayList<int[]> mhsList = solve(inputIntMatrix, timeout); // List of all MHS found
            minCardinality = checkCardinality(mhsList.get(0));
            maxCardinality = checkCardinality(mhsList.get(mhsList.size() - 1));

            StringBuilder sbCardinality = new StringBuilder();
            sbCardinality.append("Output Matrix:\n");
            sbCardinality.append("Minimum cardinality: ").append(minCardinality).append("\n");
            sbCardinality.append("Maximum cardinality: ").append(maxCardinality).append("\n");
            outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), sbCardinality);

            if (outOfMemory) {
                System.out.println("Execution interrupted > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), new StringBuilder("Execution interrupted > Cause: OUT OF MEMORY\n"));

                try {
                    printMHSFoundUpToOutOfMemory(mhsList, timeout, outputFileWriter);
                } catch (OutOfMemoryError me) {
                    System.err.println("Write output file interrupted > Cause: OUT OF MEMORY\n");
                    outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), new StringBuilder("Impossible to write the output matrix > Cause: OUT OF MEMORY\n"));
                }
            }

            if (outOfTime) {
                System.out.println("Execution interrupted > Cause: OUT OF TIME");
                StringBuilder sb = new StringBuilder();
                sb.append("Execution interrupted > Cause: OUT OF TIME\n");
                sb.append("Number of MHS found: ").append(mhsList.size()).append("\n");
                outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), sb);

                try {
                    printMHSFoundUpToTimeout(mhsList, timeout, outputFileWriter);
                } catch (OutOfMemoryError me) {
                    System.err.println("Impossible to print output matrix on .out file > Cause : OUT OF MEMORY");
                    outOfMemory = true;
//                System.exit(-1);
                }
            }
            try {
                int[][] mhsIntMatrix = outputMatrixBuilder.getMHSIntOutputMatrix(mhsList, inputIntMatrix[0].length);

                mhsMatrix.setFileName(matrix.getFileName());
                mhsMatrix.setIntMatrix(mhsIntMatrix);
            } catch (OutOfMemoryError me) {
                outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), new StringBuilder("Impossible to get output matrix > Cause: OUT OF MEMORY\n"));
                outOfMemory = true;
//            System.exit(-1);
            }

            calculateUsedMemory(runtime, "Used memory after MBase execution:");

        } catch (OutOfMemoryError me) {
            outOfMemory = true;
        }
        return mhsMatrix;
    }

    private long checkCardinality(int[] e) {
        return Arrays.stream(e).filter(i -> i == 1).count();
    }

    private void printMHSFoundUpToOutOfMemory(ArrayList<int[]> mhsList, long timeout, OutputFileWriter outputFileWriter) throws IOException {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mhsList.size(); i++) {
            int[] mhs = mhsList.get(i);
            for (int j = 0; j < mhs.length; j++) {
                sb.append(mhs[j]).append(" ");
            }
            sb.append("-\n");
        }

//        sb.append("Interruption > Cause : OUT OF MEMORY");

        try {
            outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), sb);
        } catch (OutOfMemoryError | IOException me) {
            outOfMemory = true;
            System.err.println("Write output file interrupted > Cause: OUT OF MEMORY");
        }
//        finally {
//            outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), new StringBuilder("Interruption > Cause : OUT OF MEMORY\n"));
//        }

        System.out.println("Execution interrupted > Cause: OUT OF MEMORY");
        System.out.println("Number of MHS found (in " + timeout + " ms)" + ": " + mhsList.size());
        System.out.println("Minimum cardinality: " + minCardinality + "\nMaximum cardinality: " + maxCardinality);
//        System.exit(-1);
    }

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
            outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), sb);
        } catch (OutOfMemoryError me) {
            outOfMemory = true;
            outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), new StringBuilder("Impossible to write the output matrix > Cause: OUT OF MEMORY\n"));
            System.err.println("Write output file interrupted > Cause: OUT OF MEMORY");
//            System.exit(-1);
        }
//        finally {
//            outputFileWriter.writeOutputFile(outputFileWriter.getOutputFile(), new StringBuilder("Interruption > Cause : OUT OF TIME\n"));
//        }

        System.out.println("Execution interrupted > Cause: OUT OF TIME");
        System.out.println("Number of MHS found (in " + timeout + " ms)" + ": " + mhsList.size());
        System.out.println("Minimum cardinality: " + minCardinality + "\nMaximum cardinality: " + maxCardinality);
        System.exit(0);
    }

    private void calculateUsedMemory(Runtime runtime, String s) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
//            System.out.println(s + memoryAfter + "bytes");
        System.out.println(s + bytesToMegaBytes(memoryAfter) + "MB");
    }

    /**
     * MBase procedure
     *
     * @param intMatrix input matrix from benchmark file (e.g. ####.####.matrix)
     * @param timeout   maximum time limit
     * @return matrix with M columns and X rows, where X is the number of MHS found
     */
    private ArrayList<int[]> solve(int[][] intMatrix, long timeout) throws Exception {
        final int cols = intMatrix[0].length; // M = number of columns
        final int rows = intMatrix.length; // N = number of rows
        final ArrayList<int[]> mhsList = new ArrayList<>();
        final Queue<int[]> queue = new LinkedList<>();

        queue.add(new int[cols]); // Add empty vector [0 0 ... 0]
        long startTime = System.currentTimeMillis();

        whileLoop:
        while (!queue.isEmpty() && (System.currentTimeMillis() - startTime) <= timeout) {
            int[] e = queue.poll();

            if (debugMode) {
                System.out.println(LINE);
                System.out.println("succ: " + getSucc(getMax(e), cols));
            }

            for (int i = getSucc(getMax(e), cols); i < cols && (System.currentTimeMillis() - startTime) <= timeout; i++) {
                try {
                    int[] newE = Arrays.copyOf(e, e.length);
                    newE[i] = 1;

                    if (debugMode)
                        System.out.println("e: " + Arrays.toString(newE));

                    SubMatrix subMatrix = getSubMatrix(newE, intMatrix);

                    if (debugMode)
                        System.out.println(subMatrix.toString());

                    final int[] rv = getRepresentativeVector(subMatrix);
                    int result = checkModule(rv, subMatrix.getElements());

                    if (debugMode) {
                        System.out.println("RV" + i + ": " + Arrays.toString(rv));
                        System.out.println("Result: " + (result == 2 ? "MHS" : (result == 1 ? "OK" : "KO")));
                    }

                    if (result == 1 && i < cols - 1) // OK && NOT(last lexicographical element)
                        queue.add(newE);

                    if (result == 2) // MHS
                        mhsList.add(newE);

                    if (debugMode)
                        System.out.println(DOUBLE_LINE);
                } catch (OutOfMemoryError me) {
                    System.err.println("Execution interrupted > Cause: OUT OF MEMORY");
                    outOfMemory = true;
                    queue.clear();

                    return mhsList;
                }
            }

        }

        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > timeout) {
            outOfTime = true;
            queue.clear(); // more free space
//            System.out.println("queue isEmpty: " + queue.isEmpty());
//            System.out.println("#MHS = " + mhsList.size());
        }

        return mhsList;
    }

    /**
     * DEBUG METHOD
     *
     * @param subMatrix
     */
    private void printSubMatrix(int[][] subMatrix) {
        for (int i = 0; i < subMatrix.length; i++) {
            for (int j = 0; j < subMatrix[0].length; j++) {
                System.out.print(subMatrix[i][j]);
            }
            System.out.println(" -");
        }
    }

    /**
     * @param e
     * @param matrix
     * @return
     * @throws Exception
     */
    private SubMatrix getSubMatrix(int[] e, int[][] matrix) throws Exception {
        int numOfCols = getNumberOfElements(e);

        int[][] intSubMatrix = new int[matrix.length][numOfCols];
        ArrayList<Integer> elements = new ArrayList<>();
        int col = 0;

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
     * @param e
     * @return
     */
    private int getNumberOfElements(int[] e) {
        List<Integer> eList = Arrays.stream(e).boxed().collect(Collectors.toList());

        return Collections.frequency(eList, 1);
    }

    /**
     * @param rv
     * @param elements
     * @return MHS = 2, OK = 1, KO = 0
     */
    private int checkModule(int[] rv, ArrayList<Integer> elements) throws Exception {
        boolean empty = false;
        ArrayList<Integer> elementsFound = new ArrayList<>();

        for (int j = 0; j < elements.size(); j++) {
            for (int i = 0; i < rv.length; i++) {
                if (rv[i] == 0 && !empty) {
                    empty = true;
                    continue;
                }

                if (!elementsFound.contains(elements.get(j)) && rv[i] == elements.get(j) + 1) { // lexicographical element
                    elementsFound.add(elements.get(j));
                }
            }
        }

        if (debugMode)
            System.out.println("elementsFound: " + elementsFound.toString());

        boolean rvFullProjected = true;
        for (int i = 0; i < elements.size(); i++) {
            if (elementsFound.contains(elements.get(i)))
                continue;
            rvFullProjected = false;
            break;
        }

        if (debugMode)
            System.out.println("empty: " + empty + "\nprojection: " + rvFullProjected);

        if (!empty && rvFullProjected) // MHS
            return 2;

        if (empty && rvFullProjected) // OK
            return 1;

        if (!rvFullProjected) // KO
            return 0;

        throw new Exception(MSG_EXCEPTION_CHECK_MODULE);
    }

    /**
     * Method to compute a representative vector
     *
     * @param subMatrix
     * @return
     * @throws Exception
     */
    private int[] getRepresentativeVector(SubMatrix subMatrix) throws Exception {
        int[][] intSubMatrix = subMatrix.getIntMatrix();
        int[] rv = new int[intSubMatrix.length];

        for (int j = 0; j < intSubMatrix[0].length; j++) { // j = cols
            for (int i = 0; i < intSubMatrix.length; i++) { // i = rows
                if (!subMatrix.getElements().isEmpty()) {
                    if (rv[i] == 0 && intSubMatrix[i][j] == 1) {
                        rv[i] = subMatrix.getElements().get(j) + 1;
                        continue;
                    }

                    if (rv[i] != (subMatrix.getElements().get(j) + 1) && intSubMatrix[i][j] == 1) {
                        rv[i] = -1; // x-value
                        continue;
                    }

                    if (rv[i] == (subMatrix.getElements().get(j) + 1) && intSubMatrix[i][j] == 1) {
                        rv[i] = subMatrix.getElements().get(j) + 1;
                        continue;
                    }

                    if (rv[i] == -1 && intSubMatrix[i][j] == 1) {
                        rv[i] = -1;
                    }
                }

            }
        }

        return rv;
    }

    /**
     * Method to compute the first element in lexicographical order
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

        throw new Exception(MSG_EXCEPTION_GET_FIRST_ELEMENT);
    }

    /**
     * @param element
     * @param lengthM
     * @return
     */
    private int getSucc(int element, int lengthM) {
        return min(element + 1, lengthM - 1);
    }

    /**
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
