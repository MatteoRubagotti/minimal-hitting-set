package unibs.it.dii.utility;

import unibs.it.dii.mhs.model.Matrix;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

public class FileMatrixReader {

    public Matrix readMatrixFromFile(File file) throws IOException {
        final Matrix matrix = new Matrix();
        matrix.setFileName(file.getName());

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
        int[] array = new int[split.length - 1];
        int count = 0;

        for (int j = 0; j < array.length; j++) {
            array[count++] = Integer.parseInt(split[j]);
        }

        return array;
    }
}
