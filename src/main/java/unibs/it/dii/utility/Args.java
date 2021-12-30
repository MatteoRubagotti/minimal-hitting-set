package unibs.it.dii.utility;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.PathConverter;

import java.nio.file.Path;

public class Args {
    @Parameter(names = {"-in", "--input-file"}, description = "Path of the input file .matrix", converter = PathConverter.class)
    private Path inputPath;

    public Path getInputPath() {
        return inputPath;
    }

    /*@Parameter(names = {"-out", "--outfile"}, description = "Path of the output file .output", converter = PathConverter.class)
    private Path outputPath;*/
}
