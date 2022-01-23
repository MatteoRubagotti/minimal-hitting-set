package unibs.it.dii.utility;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BenchmarkFilesDirectoryValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException{
            final Path path = Paths.get(value);

            if (!path.isAbsolute() || !path.toFile().isDirectory() || !path.toFile().exists()) {
                System.err.println("Parameter " + name + " should be the absolute path of directory with benchmark files");
                throw new ParameterException("Parameter " + name + " should be the absolute path of directory with benchmark files");
            }
        }
}
