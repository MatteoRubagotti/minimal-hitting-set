package unibs.it.dii.utility;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class OutputFileWriter {

    public static final String OUTPUT_WITH_PRE_ELABORATION = "-with-pre-elaboration";

    private File outputFile;

    public File getOutputFile() {
        return outputFile;
    }

    public File createOutputFile(boolean preProcessing, String name) throws IOException {
        final StringBuilder fullName = new StringBuilder(name);

        if (preProcessing)
            fullName.append(OUTPUT_WITH_PRE_ELABORATION);

        fullName.append(".out");

        final File output = new File(fullName.toString());

        if (!output.createNewFile()) // File already exists
            new FileOutputStream(output).close(); // Delete the content of the file

        return output;
    }

    public void writeOutputFile(File output, StringBuilder sb) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new FileWriter(output.getAbsoluteFile(), true));

        // Start write report information
        bw.append(sb.toString());
        // Close connection
        bw.close();
    }

    public void setFile(File outputFile) {
        this.outputFile = outputFile;
    }
}
