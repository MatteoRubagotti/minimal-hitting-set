package unibs.it.dii.utility;

import java.util.ArrayList;

public class OutputMatrixBuilder {

    /**
     * Method to create the output matrix containing the all MHS found.
     * @param mhsList
     * @param cols
     * @return
     */
    public boolean[][] getMHSBoolOutputMatrix(ArrayList<boolean[]> mhsList, int cols) {
        boolean[][] outputMatrix = new boolean[mhsList.size()][cols]; // K x X, where K is the number of MHS found and X <= M
        int row = 0;

        for (boolean[] mhs : mhsList) {
            for (int i = 0; i < mhs.length; i++) {
                outputMatrix[row][i] = mhs[i];
            }
            row++;
        }
        return outputMatrix;
    }
}
