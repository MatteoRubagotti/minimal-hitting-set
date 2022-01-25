package unibs.it.dii.utility;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Args {
    @Parameter(
            names = {"-h", "--help"},
            description = "Print this help message and exit",
            help = true
    )

    boolean help = false;

    public boolean isHelp() {
        return help;
    }

    @Parameter(
            names = {"-v", "--verbose"},
            description = "Print additional information on standard output"
    )

    private boolean verbose = false;

    public boolean isVerbose() {
        return verbose;
    }

    @Parameter(
            names = {"-t", "--timeout"},
            description = "Maximum time limit in seconds (s)",
            required = true
    )

    private long timeout;

    public long getTimeout() {
        return timeout;
    }

    @Parameter(
            names = {"-pe", "-pre", "--pre-elaboration"},
            description = "Compute the Pre-Elaboration before execute MBase procedure"
    )

    private boolean preProcessing = false;

    public boolean isPreProcessing() {
        return preProcessing;
    }

    @Parameter(
            names = {"-in", "--input-file"},
            description = "Absolute path of the input file .matrix",
            converter = PathConverter.class
    )

    private Path inputPath = Paths.get("");

    public Path getInputPath() {
        return inputPath;
    }

    public boolean isManualMode() {
        return inputPath.toString().length() > 0;
    }

    @Parameter(
            names = {"-d", "-dir", "--directory"},
            description = "Absolute path of the directory that contains benchmark files",
            converter = PathConverter.class,
            validateWith = BenchmarkFilesDirectoryValidator.class
    )

    private Path directoryPath = Paths.get("");

    public Path getDirectoryPath() {
        return directoryPath;
    }

    public boolean isAutomaticMode() {
        return directoryPath.toString().length() > 0;
    }

    @Parameter(
            names = {"-out", "--output-path"},
            description = "Absolute path of the output file (.out) with report information",
            converter = PathConverter.class,
            validateWith = OutputDirectoryValidator.class
    )

    private Path outputPath = Paths.get(System.getProperty("user.home") + "/output-" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));

    public Path getOutputPath() {
        return outputPath;
    }
}
