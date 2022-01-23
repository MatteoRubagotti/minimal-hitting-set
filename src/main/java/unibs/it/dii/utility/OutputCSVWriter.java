package unibs.it.dii.utility;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.IOException;

public class OutputCSVWriter {

    private CSVWriter writer;
    private CSVReader reader;

    public CSVReader getReader() {
        return reader;
    }

    public void setReader(CSVReader reader) {
        this.reader = reader;
    }

    public CSVWriter getWriter() {
        return writer;
    }

    public void setWriter(CSVWriter writer) {
        this.writer = writer;
    }

    public void writeCSV(String[] s) throws IOException {
        // Write the header of CSV
        writer.writeNext(s);

        // Close the writer
        writer.close();
    }
}
