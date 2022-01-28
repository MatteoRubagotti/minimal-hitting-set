package unibs.it.dii.mhs.builder;

import unibs.it.dii.mhs.MinimalHittingSetFacade;

import java.io.IOException;
import java.nio.file.Path;

public class MinimalHittingSetFacadeBuilder implements FacadeBuilder {
    private boolean preProcessing;
    private boolean verbosity;
    private Path inputPath;
    private Path outputPath;
    private Path inputDirectoryPath;
    private long timeout;
    private boolean automaticMode;

    public MinimalHittingSetFacade getMHS() throws IOException {
        return new MinimalHittingSetFacade(preProcessing, verbosity, inputPath, outputPath, inputDirectoryPath, timeout, automaticMode);
    }

    @Override
    public void setPreProcessing(boolean preProcessing) {
        this.preProcessing = preProcessing;
    }

    @Override
    public void setVerbosity(boolean verbosity) {
        this.verbosity = verbosity;
    }

    @Override
    public void setInputPath(Path inputPath) {
        this.inputPath = inputPath;
    }

    @Override
    public void setOutputPath(Path outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void setInputDirectoryPath(Path inputDirectoryPath) {
        this.inputDirectoryPath = inputDirectoryPath;
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public void setAutomaticMode(boolean automaticMode) {
        this.automaticMode = automaticMode;
    }

}
