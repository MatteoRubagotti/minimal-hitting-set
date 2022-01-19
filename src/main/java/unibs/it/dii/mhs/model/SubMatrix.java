package unibs.it.dii.mhs.model;

import java.util.ArrayList;

public class SubMatrix extends Matrix {
    private ArrayList<Integer> elements;

    public SubMatrix(ArrayList<Integer> elements, int[][] matrix) {
        super(matrix);
        this.elements = elements;
    }

    public ArrayList<Integer> getElements() {
        return elements;
    }

    public void setElements(ArrayList<Integer> elements) {
        this.elements = elements;
    }

    @Override
    public String toString() {
        return "Elements: " + elements.toString();
    }

}
