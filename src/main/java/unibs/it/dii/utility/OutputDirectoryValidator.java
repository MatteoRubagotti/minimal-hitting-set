package unibs.it.dii.utility;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputDirectoryValidator implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        Path path = Paths.get(value);

        if (!path.isAbsolute() || !Files.isDirectory(path)) {
            throw new ParameterException("Check the absolute path for the output file (-out)");
        }

        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new ParameterException("Impossible to create the directory: " + path.toFile());
            }
        }

    }
}
