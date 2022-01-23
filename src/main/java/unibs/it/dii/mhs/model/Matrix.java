package unibs.it.dii.mhs.model;

public class Matrix {
    private boolean[][] boolMatrix;
    private String name;

    public Matrix(boolean[][] matrix) {
        this.boolMatrix = matrix;
    }

    public Matrix() {

    }

    public boolean[][] getBoolMatrix() {
        return boolMatrix;
    }

    public void getBoolMatrix(boolean[][] boolMatrix) {
        this.boolMatrix = boolMatrix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
