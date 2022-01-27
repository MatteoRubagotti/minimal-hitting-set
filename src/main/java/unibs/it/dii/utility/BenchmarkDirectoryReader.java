package unibs.it.dii.utility;

import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

public class BenchmarkDirectoryReader {

    private static final String FILE_MATRIX_EXTENSION = "matrix";

    private Path benchmarkDirectory;

    public BenchmarkDirectoryReader(Path benchmarkDirectory) {
        this.benchmarkDirectory = benchmarkDirectory;
    }

    public Queue<String> getListBenchmarkFileMatrix() throws IOException {
        Queue<String> results = new LinkedList<>();

        // If this pathname does not denote a directory, then listFiles() returns null.
//        File[] files = new File(benchmarkDirectory.toString()).listFiles();

//        String[] filesString = {"74181.032.matrix", "74181.043.matrix", "74181.053.matrix",
//                    "c432.010.matrix", "c432.044.matrix", "c1355.039.matrix",
//                    "c2670.035.matrix", "c5315.055.matrix", "c5315.082.matrix",
//                    "c5315.091.matrix", "c5315.153.matrix", "c5315.189.matrix",
//                    "c7552.001.matrix", "c7552.003.matrix", "c7552.014.matrix",
//                    "c7552.030.matrix"};

//        List<String> filesToProcess = Arrays.asList(filesString);

//        .filter(f -> filesToProcess.contains(FilenameUtils.getName(f.toString())))

        Files.list(benchmarkDirectory)
                .filter(f -> FilenameUtils.getExtension(f.toString()).equals(FILE_MATRIX_EXTENSION))
                .sorted()
                .forEach(s -> results.add(s.toString()));

        if (results.isEmpty()) {
            System.err.println("No benchmark files found in: " + benchmarkDirectory.toString());
            System.exit(-3);
        }

        return results;
    }
}
