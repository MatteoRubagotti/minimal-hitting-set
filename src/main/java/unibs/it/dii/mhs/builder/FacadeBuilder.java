package unibs.it.dii.mhs.builder;

import java.nio.file.Path;

public interface FacadeBuilder {
    void setPreProcessing(boolean preProcessing);

    void setVerbosity(boolean verbosity);

    void setInputPath(Path inputPath);

    void setOutputPath(Path outputPath);

    void setInputDirectoryPath(Path inputDirectoryPath);

    void setTimeout(long timeout);

    void setAutomaticMode(boolean automaticMode);

}
