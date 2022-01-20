package unibs.it.dii.mhs;

import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.mhs.model.SubMatrix;
import unibs.it.dii.utility.OutputMatrixBuilder;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Integer.min;

public class MinimalHittingSetSolver {

    private boolean debugMode;

    public boolean isOutOfTime() {
        return outOfTime;
    }

    public void setOutOfTime(boolean outOfTime) {
        this.outOfTime = outOfTime;
    }

    private boolean outOfTime = false;

    public MinimalHittingSetSolver(boolean debugMode) {
        this.debugMode = debugMode;
    }

    final static private String MSG_EXCEPTION_GET_FIRST_ELEMENT = "ATTENTION! Something went wrong with getFirstElement (i.e. get the first lexicographical element available)";
    final static private String MSG_EXCEPTION_CHECK_MODULE = "ATTENTION! Something went wrong with checkModule";
    final static private String LINE = "-------------------------------------------------------------------------------------------------------------------------";
    final static private String DOUBLE_LINE = "=========================================================================================================================";

    public Matrix computeMinimalHittingSets(Matrix matrix, long timeout) throws Exception {
        final Matrix mhsMatrix = new Matrix(); // Output matrix
        final OutputMatrixBuilder outputMatrixBuilder = new OutputMatrixBuilder();
        final int[][] inputIntMatrix = matrix.getIntMatrix();
        ArrayList<int[]> mhsList = solve(inputIntMatrix, timeout); // List of all MHS found
        int[][] mhsIntMatrix = outputMatrixBuilder.getMHSIntOutputMatrix(mhsList, inputIntMatrix[0].length);

        if(outOfTime)
            printMHSFoundUpToTimeout(mhsList);

        if (debugMode)
            System.out.println("Number of MHS found: " + mhsList.size());

        mhsMatrix.setFileName(matrix.getFileName() + ".out");
        mhsMatrix.setIntMatrix(mhsIntMatrix);

        return mhsMatrix;
    }

    private void printMHSFoundUpToTimeout(ArrayList<int[]> mhsList) {
        for (int i = 0; i < mhsList.size(); i++) {
            int [] mhs = mhsList.get(i);
            for (int j = 0; j < mhs.length; j++) {
                System.out.print(mhs[j] + " ");
            }
            System.out.println("-");
        }

        System.exit(0);
    }

    /**
     * MBase procedure
     *
     * @param intMatrix input matrix from benchmark file (e.g. ####.####.matrix)
     * @param timeout
     * @return matrix with M columns and X rows, where X is the number of MHS found
     */
    private ArrayList<int[]> solve(int[][] intMatrix, long timeout) throws Exception {
        final int cols = intMatrix[0].length; // M = number of columns
        final int rows = intMatrix.length; // N = number of rows
        final ArrayList<int[]> mhsList = new ArrayList<>();
        final Queue<int[]> queue = new LinkedList<>();

        queue.add(new int[cols]); // Add empty vector [0 0 ... 0]
        long startTime = System.currentTimeMillis();

        while (!queue.isEmpty() && (System.currentTimeMillis() - startTime) <= timeout) {
            int[] e = queue.poll();

            if (debugMode) {
                System.out.println(LINE);
                System.out.println("succ: " + getSucc(getMax(e), cols));
            }

            for (int i = getSucc(getMax(e), cols); i < cols; i++) {
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
            }

        }

        long endTime = System.currentTimeMillis();

        if ((endTime - startTime) > timeout) {
            outOfTime = true;
            queue.clear();
//            System.out.println("queue isEmpty: " + queue.isEmpty());
            System.out.println("#MHS = " + mhsList.size());
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

    private int getSucc(int element, int lengthM) {
        return min(element + 1, lengthM - 1);
    }

    private int getMax(int[] e) {
        int max = e.length;
        do {
            max--;

        } while ((max > -1) && (e[max] == 0));

        return max;
    }


}
