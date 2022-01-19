package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.utility.Args;
import unibs.it.dii.utility.FileMatrixReader;

import java.io.File;
import java.nio.file.Path;

public class MinimalHittingSet {
    public static void main(String[] args) throws Exception {
        final Args arguments = new Args();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        MinimalHittingSet main = new MinimalHittingSet();
        main.run(arguments);

    }

    public void run(Args arguments) throws Exception {
        final boolean debugMode = true;
        final MinimalHittingSetSolver solver = new MinimalHittingSetSolver(debugMode);
        Path inputPath = arguments.getInputPath();
        final File file = inputPath.toFile();
        //System.out.println(file.getName());

        final FileMatrixReader reader = new FileMatrixReader();
        final Matrix inputMatrix = reader.readMatrixFromFile(file);
        final Matrix outputMatrix = solver.computeMinimalHittingSets(inputMatrix);

        String inputMatrixName = inputMatrix.getFileName();
        System.out.println("Input File: " + inputMatrixName);

        int[][] inputIntMatrix = inputMatrix.getIntMatrix();
        System.out.println("Input Matrix:");

        for (int[] intCol : inputIntMatrix) {
            for (int j = 0; j < inputIntMatrix[0].length; j++) {
                System.out.print(intCol[j] + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }

        String outputMatrixName = outputMatrix.getFileName();
        System.out.println("Output File name: " + outputMatrixName);

        int[][] outputIntMatrix = outputMatrix.getIntMatrix();
        System.out.println("Output Matrix:");

        for (int[] intCol : outputIntMatrix) {
            for (int j = 0; j < outputIntMatrix[0].length; j++) {
                System.out.print(intCol[j] + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }

        System.out.println("Number of MHS found: " + outputIntMatrix.length);
    }

}
