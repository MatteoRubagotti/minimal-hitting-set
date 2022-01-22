package unibs.it.dii.utility;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class OutputCSVWriter {

    public void writeCSVHeader(Path path, String[] headers) throws IOException {
        final CSVWriter writer = new CSVWriter(new FileWriter(path.toFile().toString()));

        // Write the header of CSV
        writer.writeNext(headers);

        // Close the writer
        writer.close();
    }
}
