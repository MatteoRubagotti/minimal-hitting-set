package unibs.it.dii.utility;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class OutputFileWriter {

    public static final String OUTPUT_WITH_PRE_ELABORATION = "-with-pre-elaboration";

    private File outputFile;

    public OutputFileWriter(Path outputPath) throws IOException {
        if (!outputPath.toFile().exists())
            Files.createDirectories(outputPath);
    }

    public File getOutputFile() {
        return outputFile;
    }

    public void setFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public File createOutputFile(boolean preProcessing, String fileName) throws IOException {
        final StringBuilder fullName = new StringBuilder(fileName);

        if (preProcessing)
            fullName.append(OUTPUT_WITH_PRE_ELABORATION);

        fullName.append(".out");

        outputFile = new File(fullName.toString());

        if (!outputFile.createNewFile()) // File already exists
            new FileOutputStream(outputFile).close(); // Comment these two line if you want to append more report information for the same input matrix

        return outputFile;
    }

    public void writeOutputFile(StringBuilder sb) throws IOException {
        final FileWriter fw = new FileWriter(outputFile.getAbsoluteFile(), true);
        // Start write report information
        fw.append(sb.toString());
        // Close connection
        fw.close();
    }

    public void writeOutputMatrix(boolean[][] matrix, ArrayList<Integer> colsRemoved, int initialCols) throws IOException {
        final FileWriter fw = new FileWriter(outputFile.getAbsoluteFile(), true);
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < matrix.length; i++) {
            sb.setLength(0); // Reset the StringBuilder
            for (int j = 0, count = 0; j < initialCols; j++) {
                if (colsRemoved.contains(count)) {
                    sb.append("0 ");
                    continue;
                }
                sb.append(matrix[i][count++] ? "1 " : "0 "); // Output matrix has less column if pre-processed
            }
            sb.append("-\n"); // End of the row
            // Write the row of the matrix
            fw.append(sb.toString());
        }

        // Close connection
        fw.close();
    }
}
