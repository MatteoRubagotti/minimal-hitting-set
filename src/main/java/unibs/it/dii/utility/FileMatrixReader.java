package unibs.it.dii.utility;

import unibs.it.dii.mhs.model.Matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;

/**
 * Class to read the matrix within a benchmark file.
 */
public class FileMatrixReader {

    private static final String FILE_MATRIX_EXTENSION = "matrix";
    public static final String MSG_NO_SUCH_FILE_EXCEPTION = "Enter a correct absolute path for the input file matrix";
    private final static String MSG_WRONG_FILE_EXTENSION = "The input file extension is not correct. Please choose .matrix file";

    /**
     * Method to read the matrix within a file
     * @param file the file with .matrix extension
     * @return the matrix read
     * @throws IOException
     */
    public Matrix readMatrixFromFile(File file) throws IOException {
        final Matrix matrix = new Matrix();
        matrix.setName(FilenameUtils.removeExtension(file.getName()));

        try (Stream<String> stream = Files.lines(file.toPath())) {

            if (!FilenameUtils.getExtension(file.getName()).equals(FILE_MATRIX_EXTENSION)) {
                System.err.println(MSG_WRONG_FILE_EXTENSION);
                System.exit(100);
            }

            final boolean[][] boolMatrix = stream
                    .filter(line -> !line.startsWith(";")) // remove comments
                    .map(this::readRow) // read each row of the matrix
                    .toArray(boolean[][]::new);

            matrix.setBoolMatrix(boolMatrix);

        } catch (NoSuchFileException fe) {
            System.err.println(MSG_NO_SUCH_FILE_EXCEPTION);
            System.exit(200);
        }

        return matrix;
    }

    private boolean[] readRow(String s) {
        final String[] rowString = s.split(" ");
        boolean[] row = new boolean[rowString.length - 1]; // exclude '-' (i.e. end of the row)
        int count = 0;

        for (int j = 0; j < row.length; j++) {
            row[count++] = Integer.parseInt(rowString[j]) == 1;
        }

        return row;
    }
}
