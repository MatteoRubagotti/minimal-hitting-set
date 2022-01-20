package unibs.it.dii.utility;

import java.util.ArrayList;

public class OutputMatrixBuilder {

    public int[][] getMHSIntOutputMatrix(ArrayList<int[]> mhsList, int cols) {
        int[][] outputMatrix = new int[mhsList.size()][cols]; // K x M, where K is the number of MHS found
        int row = 0;

        for (int[] mhs : mhsList) {
            for (int i = 0; i < mhs.length; i++) {
                if (mhs[i] == 1) {
                    outputMatrix[row][i] = 1;
                }
            }
            row++;
        }
        return outputMatrix;
    }
}
