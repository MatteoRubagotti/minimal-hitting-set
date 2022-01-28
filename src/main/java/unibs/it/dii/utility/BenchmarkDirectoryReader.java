package unibs.it.dii.utility;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Class to read the benchmark files (i.e. with .matrix extension) within a specified directory path.
 */
public class BenchmarkDirectoryReader {

    private static final String FILE_MATRIX_EXTENSION = "matrix";

    private Path benchmarkDirectory;


    public BenchmarkDirectoryReader(Path benchmarkDirectory) {
        this.benchmarkDirectory = benchmarkDirectory;
    }

    /**
     * Method to create the queue of benchmark files to process
     *
     * @return the queue of file's names to process
     * @throws IOException
     */
    public Queue<String> getListBenchmarkFileMatrix() throws IOException {
        Queue<String> results = new LinkedList<>();

        Files.list(benchmarkDirectory)
                .filter(f -> FilenameUtils.getExtension(f.toString()).equals(FILE_MATRIX_EXTENSION))
                .sorted()
                .forEach(f -> results.add(f.toString()));

        if (results.isEmpty()) {
            System.err.println("No benchmark files found in: " + benchmarkDirectory.toString());
            System.exit(255);
        }

        return results;
    }
}
