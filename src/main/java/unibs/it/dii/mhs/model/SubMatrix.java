package unibs.it.dii.mhs.model;

import java.util.ArrayList;

/**
 * This class represents a submatrix used to create the representative vector
 * for each subset of lexicographical elements.
 */
public class SubMatrix extends Matrix {
    private ArrayList<Integer> elements;

    public SubMatrix(ArrayList<Integer> elements, boolean[][] matrix) {
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
