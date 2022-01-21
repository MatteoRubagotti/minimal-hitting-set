package unibs.it.dii.utility;

import unibs.it.dii.mhs.model.Matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

public class FileMatrixReader {

    private static final String MATRIX_EXTENSION = "matrix";
    public static final String MSG_NO_SUCH_FILE_EXCEPTION = "Enter a correct path for the input file matrix";
    private final static String MSG_WRONG_FILE_EXTENSION = "The input file extension is not correct. Please choose .matrix file";

    public Matrix readMatrixFromFile(File file) throws IOException {
        final Matrix matrix = new Matrix();
        matrix.setFileName(FilenameUtils.removeExtension(file.getName()));

        try (Stream<String> stream = Files.lines(file.toPath())) {
            if (!FilenameUtils.getExtension(file.getName()).equals(MATRIX_EXTENSION)) {
                System.err.println(MSG_WRONG_FILE_EXTENSION);
                System.exit(-1);
            }

            final int[][] intMatrix = stream
                    .filter(line -> !line.startsWith(";"))
                    .map(this::readRow)
                    .toArray(int[][]::new);

            matrix.setIntMatrix(intMatrix);
        } catch (NoSuchFileException fe) {
            System.err.println(MSG_NO_SUCH_FILE_EXCEPTION);
            System.exit(-1);
        }

        return matrix;
    }

    private int[] readRow(String s) {
        final String[] split = s.split(" ");
        int[] row = new int[split.length - 1];
        int count = 0;

        for (int j = 0; j < row.length; j++) {
            row[count++] = Integer.parseInt(split[j]);
        }

        return row;
    }
}
