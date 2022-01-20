package unibs.it.dii.utility;

import unibs.it.dii.mhs.model.Matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;

public class FileMatrixReader {

    public Matrix readMatrixFromFile(File file) throws IOException {
        final Matrix matrix = new Matrix();
        matrix.setFileName(FilenameUtils.removeExtension(file.getName()));

        try (Stream<String> stream = Files.lines(file.toPath())) {
            final int[][] intMatrix = stream
                    .filter(line -> !line.startsWith(";"))
                    .map(this::readRow)
                    .toArray(int[][]::new);

            matrix.setIntMatrix(intMatrix);
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
