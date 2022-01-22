package unibs.it.dii.mhs.model;

public class Matrix {
    private int[][] intMatrix;
    private String name;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
