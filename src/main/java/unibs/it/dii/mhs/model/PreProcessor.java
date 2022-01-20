package unibs.it.dii.mhs.model;

import java.util.*;
import java.util.stream.Collectors;

public class PreProcessor {

    private final ArrayList<Integer> rowsToRemove;
    private final ArrayList<Integer> colsToRemove;
    private final boolean debug;

    public ArrayList<Integer> getRowsToRemove() {
        return rowsToRemove;
    }

    public ArrayList<Integer> getColsToRemove() {
        return colsToRemove;
    }

    public PreProcessor(boolean debug) {
        rowsToRemove = new ArrayList<>();
        colsToRemove = new ArrayList<>();
        this.debug = debug;
    }

    public int[][] computePreProcessing(int[][] matrix) {
        int[][] newMatrix = removeRows(matrix);
        return removeCols(newMatrix);
    }

    private int[][] removeRows(int[][] matrix) {
        int cols = matrix[0].length;
        final Set<Integer> rowsToRemoveSet = new HashSet<>();

        for (int i = 0; i < matrix.length; i++) {
            for (int j = i + 1; j < matrix.length; j++) {
                if (checkRow(matrix[i], matrix[j], cols) == 1 || checkRow(matrix[i], matrix[j], cols) == 0) // N_j is a subset of N_i or are the same (check=0)
                {
                    rowsToRemoveSet.add(i);
                    continue;
                }

                if (checkRow(matrix[i], matrix[j], cols) == 2) // N_i is a subset of N_j
                    rowsToRemoveSet.add(j);
            }
        }

        rowsToRemove.clear();
        rowsToRemove.addAll(rowsToRemoveSet); // create an ArrayList of rows removed

        int[][] newMatrix = new int[matrix.length - rowsToRemove.size()][matrix[0].length];

        for (int i = 0, rowCount = 0; i < matrix.length; i++) {
            if (rowsToRemove.contains(i)) {
                continue;
            }
            for (int j = 0; j < matrix[0].length; j++) {
                newMatrix[rowCount][j] = matrix[i][j];
            }
            ++rowCount;
        }

        return newMatrix;
    }

    /**
     * @param row1
     * @param row2
     * @param cols
     * @return 1 -> row2 <= row1, 2 -> row1 <= row2, -1 -> row1 <> row2, 0 -> row1 == row2
     */
    private int checkRow(int[] row1, int[] row2, int cols) {
        int check = 0; // row1 == row2

        for (int i = 0; i < cols; i++) {
            if (row1[i] == 1 && row2[i] == 0) {
                if (check == 2) {
                    check = -1;
                    break;
                } else {
                    check = 1;
                    continue;
                }
            }
            if (row1[i] == 0 && row2[i] == 1) {
                if (check == 1) {
                    check = -1;
                    break;
                } else {
                    check = 2;
                }
            }
        }

        return check;
    }

    private void printMatrix(int[][] intMatrix, boolean debugMode) {
        if (!debugMode) {
            return;
        }

        System.out.println("Size: " + intMatrix.length + "x" + intMatrix[0].length);
        for (int[] intCol : intMatrix) {
            for (int j = 0; j < intMatrix[0].length; j++) {
                System.out.print(intCol[j] + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }
    }

    private int[][] removeCols(int[][] matrix) {
        int[][] transposeMatrix = transpose(matrix);
        if (debug)
            printMatrix(transposeMatrix, true);

        int rows = transposeMatrix.length; // REMEMBER: matrix is transposed!
        final Set<Integer> rowsToRemove = new HashSet<>();

        for (int i = 0; i < rows; i++) { // rows = columns of original matrix
            if (colIsEmpty(transposeMatrix[i], i))
                colsToRemove.add(i);
        }

        int[][] newInputMatrix = new int[matrix.length][matrix[0].length - colsToRemove.size()];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < matrix[0].length; j++) {
                if (!colsToRemove.contains(j)) {
                    newInputMatrix[i][colCount++] = matrix[i][j];
                }
            }
        }

        return newInputMatrix;
    }

    private int[][] transpose(int[][] matrix) {
        int[][] transposeMatrix = new int[matrix[0].length][matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                transposeMatrix[j][i] = matrix[i][j];
            }
        }

        return transposeMatrix;
    }

    private boolean colIsEmpty(int[] col, int numCol) {
        ArrayList<Integer> colList = (ArrayList<Integer>) Arrays.stream(col).boxed().collect(Collectors.toList());
        if (debug)
            System.out.println("col" + numCol + ": " + colList.toString() + "\nisEmpty: " + !colList.contains(1));

        return !colList.contains(1);
    }

}
