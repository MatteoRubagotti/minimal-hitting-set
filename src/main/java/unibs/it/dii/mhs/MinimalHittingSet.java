package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.utility.Args;
import unibs.it.dii.utility.FileMatrixReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class MinimalHittingSet {
    public static void main(String[] args) throws IOException {
        final Args arguments = new Args();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        MinimalHittingSet main = new MinimalHittingSet();
        main.run(arguments);

        /*final String PATH = ".src/main/java/unibs/it/dii/benchmarks/74L85.000.matrix";
        final File file = new File(PATH);*/

    }

    /*private void run() {

    }*/

    public void run(Args arguments) throws IOException {
        Path inputPath = arguments.getInputPath();
        final File file = inputPath.toFile();
        //System.out.println(file.getName());

        final FileMatrixReader reader = new FileMatrixReader();
        final Matrix matrix = reader.readMatrixFromFile(file);

        String matrixName = matrix.getFileName();
        System.out.println("Matrix name: " + matrixName);

        int[][] intMatrix = matrix.getIntMatrix();
        System.out.println("Matrix:");

        for (int[] intCol : intMatrix) {
            for (int j = 0; j < intMatrix[0].length; j++) {
                System.out.print(intCol[j] + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }
    }

}
