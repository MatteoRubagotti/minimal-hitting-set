package unibs.it.dii.mhs;

import com.google.common.primitives.Booleans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    public boolean[][] computePreProcessing(boolean[][] matrix) {
        reset();
        boolean[][] newMatrix = removeRows(matrix);
        return removeCols(newMatrix);
    }

    private void reset() {
        this.rowsToRemove.clear();
        this.colsToRemove.clear();
    }

    /**
     * Method to remove rows from the input matrix.
     *
     * @param matrix
     * @return
     */
    private boolean[][] removeRows(boolean[][] matrix) {
        int cols = matrix[0].length;
        // Create the object to collect all rows to removed (without duplicated)
        final Set<Integer> rowsToRemoveSet = new HashSet<>();

        for (int i = 0; i < matrix.length; i++) {
            for (int j = i + 1; j < matrix.length; j++) { // Compare with the all next rows of input matrix

                if (rowsToRemoveSet.contains(i) || rowsToRemoveSet.contains(j)) // Rows is already added
                    continue;

                if(checkRow(matrix[i], matrix[j], cols) == -1)
                {
                    if (debug) {
                        System.out.println("Row" + i + ": " + Arrays.toString(matrix[i]) + "\nRow" + j + ": " + Arrays.toString(matrix[j]));
                        System.out.println("do not remove");
                    }
                }

                if (checkRow(matrix[i], matrix[j], cols) == 1 || checkRow(matrix[i], matrix[j], cols) == 0) // N_j is a subset of N_i or are the same (check=0)
                {
                    if (debug) {
                        System.out.println("Row" + i + ": " + Arrays.toString(matrix[i]) + "\nRow" + j + ": " + Arrays.toString(matrix[j]));
                        System.out.println("remove row" + i);
                    }
                    rowsToRemoveSet.add(i);
                    continue;
                }

                if (checkRow(matrix[i], matrix[j], cols) == 2) // N_i is a subset of N_j
                {
                    if (debug) {
                        System.out.println("Row" + i + ": " + Arrays.toString(matrix[i]) + "\nRow" + j + ": " + Arrays.toString(matrix[j]));
                        System.out.println("remove row" + j);
                    }
                    rowsToRemoveSet.add(j);
                }
            }
        }

        rowsToRemove.clear();
        rowsToRemove.addAll(rowsToRemoveSet); // Update the list with the rows removed

        // Create the new input matrix with <= rows
        boolean[][] newMatrix = new boolean[matrix.length - rowsToRemove.size()][matrix[0].length];

        resizeMatrixWithoutRowsRemoved(matrix, newMatrix);

        return newMatrix;
    }

    private void resizeMatrixWithoutRowsRemoved(boolean[][] matrix, boolean[][] newMatrix) {
        for (int i = 0, rowCount = 0; i < matrix.length; i++) {
            if (rowsToRemove.contains(i)) { // Skip the row to store
                continue;
            }
            for (int j = 0; j < matrix[0].length; j++) {
                newMatrix[rowCount][j] = matrix[i][j];
            }
            ++rowCount;
        }
    }

    /**
     * Method to compare two rows and return if one contains the other (or viceversa, 1 or 2), they are equals (0)
     * or there is not a subset relation.
     *
     * @param row1 The fixed row to compare with row2
     * @param row2 The row after row1
     * @param cols The number of columns to compare
     * @return 1 -> row2 < row1, 2 -> row1 < row2, -1 -> row1 <> row2, 0 -> row1 == row2
     */
    private int checkRow(boolean[] row1, boolean[] row2, int cols) {
        int check = 0; // row1 == row2

        for (int i = 0; i < cols; i++) {
            if (row1[i] && !row2[i]) {
                if (check == 2) {
                    check = -1;
                    break;
                } else {
                    check = 1;
                    continue;
                }
            }

            if (!row1[i] && row2[i]) {
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

    /**
     * Method to print the matrix.
     *
     * @param boolMatrix
     */
    private void printBoolMatrix(boolean[][] boolMatrix) {
        if (!debug) {
            return;
        }

        System.out.println("Size: " + boolMatrix.length + "x" + boolMatrix[0].length);
        for (boolean[] col : boolMatrix) {
            for (int j = 0; j < boolMatrix[0].length; j++) {
                System.out.print(col[j] ? 1 + " " : 0 + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }
    }

    /**
     * Method to remove columns from a matrix.
     *
     * @param matrix
     * @return
     */
    private boolean[][] removeCols(boolean[][] matrix) {
        boolean[][] transposeMatrix = transpose(matrix);

        if (debug) {
            System.out.println("Transpose Matrix:");
            printBoolMatrix(transposeMatrix);
        }

        int rows = transposeMatrix.length; // REMEMBER: matrix is transposed!
        final Set<Integer> rowsToRemove = new HashSet<>();

        for (int i = 0; i < rows; i++) { // rows = columns of original matrix
            if (colIsEmpty(transposeMatrix[i], i))
                colsToRemove.add(i);
        }

        boolean[][] newInputMatrix = new boolean[matrix.length][matrix[0].length - colsToRemove.size()];

        resizeMatrixWithoutColumnsRemoved(matrix, newInputMatrix);

        return newInputMatrix;
    }

    /**
     * Method to resize the matrix.
     *
     * @param matrix
     * @param newInputMatrix
     */
    private void resizeMatrixWithoutColumnsRemoved(boolean[][] matrix, boolean[][] newInputMatrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < matrix[0].length; j++) {
                if (!colsToRemove.contains(j)) {
                    newInputMatrix[i][colCount++] = matrix[i][j];
                }
            }
        }
    }

    /**
     * Method to compute the transpose of a matrix.
     *
     * @param matrix
     * @return
     */
    private boolean[][] transpose(boolean[][] matrix) {
        boolean[][] transposeMatrix = new boolean[matrix[0].length][matrix.length];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                transposeMatrix[j][i] = matrix[i][j];
            }
        }

        return transposeMatrix;
    }

    /**
     * Method to check if the column (col) selected is empty.
     *
     * @param col
     * @param numCol
     * @return
     */
    private boolean colIsEmpty(boolean[] col, int numCol) {
        int count = 0;
        for (boolean b : col) if (b) count++;

        if (debug)
            System.out.println("Column" + numCol + ": " + Booleans.asList(col).toString() + "\nisEmpty: " + (count == 0));

        return (count == 0);
    }

}
