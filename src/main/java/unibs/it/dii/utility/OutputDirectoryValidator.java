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
//        System.out.println(path.toString());

        if (!path.isAbsolute() || !Files.isDirectory(path)) {
            throw new ParameterException("Check the absolute path for the output file (-out)");
//            System.exit(-1);
        }

        if (!path.toFile().exists()) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
//                System.err.println("Impossible to create the directory: " + path.toFile());
                throw new ParameterException("Impossible to create the directory: " + path.toFile());
//                System.exit(-1);
            }
        }

        }
    }
