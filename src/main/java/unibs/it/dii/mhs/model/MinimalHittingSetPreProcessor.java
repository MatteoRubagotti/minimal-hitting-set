package unibs.it.dii.mhs.model;

import com.google.common.primitives.Booleans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class implements the Pre-Processing procedure in order to simplify the input matrix instance.
 */
public class MinimalHittingSetPreProcessor {

    private final ArrayList<Integer> rowsToRemove;
    private final ArrayList<Integer> colsToRemove;
    private final boolean debug;

    public MinimalHittingSetPreProcessor(boolean debug) {
        this.rowsToRemove = new ArrayList<>();
        this.colsToRemove = new ArrayList<>();
        this.debug = debug;
    }

    public ArrayList<Integer> getRowsToRemove() {
        return rowsToRemove;
    }

    public ArrayList<Integer> getColsToRemove() {
        return colsToRemove;
    }

    /**
     * Method to execute the Pre-Elaboration procedure on the input matrix.
     *
     * @param matrix the boolean matrix to pre-process
     * @return a boolean matrix pre-processed
     */
    public boolean[][] execute(boolean[][] matrix) {
        reset();
        boolean[][] newMatrix = removeRows(matrix);
        return removeCols(newMatrix);
    }

    /**
     * Clear the list of rows and columns removed.
     */
    private void reset() {
        this.rowsToRemove.clear();
        this.colsToRemove.clear();
    }

    /**
     * Method to remove rows from the input matrix.
     *
     * @param matrix the input matrix
     * @return a matrix with a number of rows <= inputMatrix.length
     */
    private boolean[][] removeRows(boolean[][] matrix) {
        int cols = matrix[0].length;
        // Create the object to collect all rows to removed (without duplicated)
        final Set<Integer> rowsToRemoveSet = new HashSet<>();

        for (int i = 0; i < matrix.length; i++) {
            for (int j = i + 1; j < matrix.length; j++) { // Compare with the all next rows of input matrix

                if (rowsToRemoveSet.contains(i) || rowsToRemoveSet.contains(j)) // Rows is already added
                    continue;

                if (checkRow(matrix[i], matrix[j], cols) == -1) {
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

    /**
     * Method to create the matrix with new dimension (i.e. fewer rows).
     *
     * @param matrix    the initial input matrix
     * @param newMatrix the matrix with the new dimensions
     */
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
     * @param row1 the fixed row to compare with row2
     * @param row2 the row after row1
     * @param cols the number of columns to compare
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
     * @param boolMatrix a boolean matrix
     */
    private void printBoolMatrix(boolean[][] boolMatrix) {
        if (debug) {
            System.out.println("Size: " + boolMatrix.length + "x" + boolMatrix[0].length);
            for (boolean[] col : boolMatrix) {
                for (int j = 0; j < boolMatrix[0].length; j++) {
                    System.out.print(col[j] ? 1 + " " : 0 + " "); // Print each row of the matrix
                }
                System.out.println("-"); // Print the end of a row
            }
        }
    }

    /**
     * Method to remove empty columns from a matrix.
     *
     * @param matrix a boolean matrix
     * @return a boolean matrix without empty columns
     */
    private boolean[][] removeCols(boolean[][] matrix) {
        boolean[][] transposeMatrix = transpose(matrix);

        if (debug) {
            System.out.println("Matrix:");
            printBoolMatrix(matrix);
            System.out.println("Matrix transpose:");
            printBoolMatrix(transposeMatrix);
        }

        int rows = transposeMatrix.length; // REMEMBER: matrix is transposed!

        for (int i = 0; i < rows; i++) { // rows = columns of original matrix
            if (colIsEmpty(transposeMatrix[i], i))
                colsToRemove.add(i);
        }

        boolean[][] newInputMatrix = new boolean[matrix.length][matrix[0].length - colsToRemove.size()];

        resizeMatrixWithoutColumnsRemoved(matrix, newInputMatrix);

        return newInputMatrix;
    }

    /**
     * Method to resize the matrix without columns empty.
     *
     * @param matrix         the initial input matrix
     * @param newInputMatrix the matrix with dimensions updated
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
     * @param matrix a boolean matrix
     * @return the boolean matrix transposed
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
     * @param col    a boolean array represented the column of a matrix
     * @param numCol the index of the column
     * @return true if the column is empty (i.e. only false values inside the array)
     */
    private boolean colIsEmpty(boolean[] col, int numCol) {
        int count = 0;
        for (boolean b : col) if (b) return false;

        if (debug)
            System.out.println("Column" + numCol + ": " + Booleans.asList(col) + "\nEmpty: " + (count == 0));

        return true;
    }

}
