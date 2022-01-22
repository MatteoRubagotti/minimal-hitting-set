package unibs.it.dii.utility;

import java.io.*;

public class OutputFileWriter {

    public static final String OUTPUT_WITH_PRE_ELABORATION = "-with-pre-elaboration";

    private File outputFile;

    public File getOutputFile() {
        return outputFile;
    }

    public void setFile(File outputFile) {
        this.outputFile = outputFile;
    }

    public File createOutputFile(boolean preProcessing, String name) throws IOException {
        final StringBuilder fullName = new StringBuilder(name);

        if (preProcessing)
            fullName.append(OUTPUT_WITH_PRE_ELABORATION);

        fullName.append(".out");

        outputFile = new File(fullName.toString());

        if (!outputFile.createNewFile()) // File already exists
            new FileOutputStream(outputFile).close(); // Comment these two line if you want to append more report information for the same input matrix

        return outputFile;
    }

    public void writeOutputFile(StringBuilder sb) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile.getAbsoluteFile(), true));

        // Start write report information
        bw.append(sb.toString());
        // Close connection
        bw.close();
    }
}
