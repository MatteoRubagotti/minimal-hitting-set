package unibs.it.dii.mhs.model;

public class Matrix {
    private int[][] intMatrix;
    private String fileName;

    public Matrix() {

    }

    public Matrix(int[][] matrix) {
        this.intMatrix = matrix;
    }

    public int[][] getIntMatrix() {
        return intMatrix;
    }

    public void setIntMatrix(int[][] intMatrix) {
        this.intMatrix = intMatrix;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
}
