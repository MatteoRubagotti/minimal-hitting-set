package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.utility.Args;
import unibs.it.dii.utility.FileMatrixReader;
import unibs.it.dii.utility.OutputFileWriter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MinimalHittingSet {

    final static private String DOUBLE_LINE = "==========================================";
    final static private String LINE = "------------------------------------------";
    final static private String MSG_READING_MATRIX_FILE = "\t\tReading .matrix file...";
    final static private String MSG_PRE_PROCESSING_RUNNING = "\t\tRun Pre-Processing...";
    final static private String MSG_MBASE_EXECUTION = "\t\tMBase Execution...";

    private static final long MEGABYTE = 1024L * 1024L;
    private static final long MILLISECONDS = 1000;

    public static long bytesToMegaBytes(long bytes) {
        return bytes / MEGABYTE;
    }

    public static void main(String[] args) throws Exception {
        final Args arguments = new Args();
        JCommander jc = JCommander.newBuilder()
                .addObject(arguments)
                .build();

        try {
            jc.parse(args);
        } catch (ParameterException pe) {
            jc.usage();
            System.exit(0);
        }


        MinimalHittingSet main = new MinimalHittingSet();

        main.run(arguments, jc);

    }

    public void run(Args arguments, JCommander jc) throws Exception {
        final boolean debugMode = false;

        // Set arguments
        final boolean preProcessing = arguments.isPreProcessing();
        final long timeout = getMillis(arguments.getTimeout());
        final boolean verbose = arguments.isVerbose();

        if (arguments.isHelp()) {
            jc.usage();
            System.exit(0);
        }

        if(!Files.exists(arguments.getOutputPath())) {
            Files.createDirectory(arguments.getOutputPath());
        }

        if (!arguments.getOutputPath().isAbsolute() || !Files.isDirectory(arguments.getOutputPath())) {
            System.err.println("Check the absolute path for the output file");
            System.exit(0);
        }

        // Get the Java runtime
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();

        if (debugMode)
            System.out.println("Total memory available is: " + bytesToMegaBytes(runtime.totalMemory()) + "MB");

        final MinimalHittingSetSolver solver = new MinimalHittingSetSolver(debugMode);
        Path inputPath = arguments.getInputPath();
        final File file = inputPath.toFile();
        //System.out.println(file.getName());

        final FileMatrixReader reader = new FileMatrixReader();
        OutputFileWriter outputFileWriter = new OutputFileWriter();

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_READING_MATRIX_FILE);
        System.out.println(DOUBLE_LINE);

        final Matrix inputMatrix = reader.readMatrixFromFile(file);
        final int initialRows = inputMatrix.getIntMatrix().length;
        final int initialCols = inputMatrix.getIntMatrix()[0].length;

        printInputInformation(inputMatrix);

        final PreProcessor preProcess = new PreProcessor(debugMode);
//        final Matrix newInputMatrix = new Matrix();
//        newInputMatrix.setFileName(inputMatrix.getFileName());

        long endTimePP = 0;
        long startTimePP = 0;

        StringBuilder reportBuilder = new StringBuilder();

        buildOutputHeader(reportBuilder, inputMatrix.getFileName(), initialRows, initialCols);

        if (preProcessing) {
            System.out.println(DOUBLE_LINE);
            System.out.println(MSG_PRE_PROCESSING_RUNNING);
            System.out.println(DOUBLE_LINE);

            calculateUsedMemory(verbose, runtime, "Used memory before Pre-Processing: ");

            // Pre-Processing execution
            startTimePP = System.currentTimeMillis();
            int[][] newInputIntMatrix = preProcess.computePreProcessing(inputMatrix.getIntMatrix());
            endTimePP = System.currentTimeMillis();

            calculateUsedMemory(verbose, runtime, "Used memory after Pre-Processing: ");

            long preProcessingTime = endTimePP - startTimePP;

            writePreProcessingMemoryUsed(reportBuilder, runtime);
            printPreProcessingInformation(preProcess, newInputIntMatrix, preProcessingTime, verbose, debugMode, reportBuilder);

            inputMatrix.setIntMatrix(newInputIntMatrix);
        }
//        else {
//            newInputMatrix.setIntMatrix(inputMatrix.getIntMatrix());
//        }

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_MBASE_EXECUTION);
        System.out.println(DOUBLE_LINE);

        System.out.println(LINE);
        System.out.println("\tInitial memory available: " + bytesToMegaBytes(runtime.totalMemory()) + " MB");
        System.out.println(LINE);

        calculateUsedMemory(verbose, runtime, "Used memory before MBase execution: ");

        long startTime = System.currentTimeMillis();

        // Execution of MBase
        final Matrix outputMatrix = solver.computeMinimalHittingSets(inputMatrix, timeout - (endTimePP - startTimePP), runtime);

        long endTime = System.currentTimeMillis();

        long executionTime = endTime - startTime; // Execution time of MBase procedure

        buildMBaseReportInformation(outputMatrix, executionTime, runtime, reportBuilder, solver.getMinCardinality(), solver.getMaxCardinality());

        outputMatrixToStringBuilder(reportBuilder, outputMatrix.getIntMatrix(), preProcess.getRowsToRemove(), preProcess.getColsToRemove(), initialCols);

        calculateUsedMemory(verbose, runtime, "Used memory after MBase execution: ");

        System.out.println("MBase execution time: " + executionTime + " ms");

        StringBuilder outputFileName = new StringBuilder(arguments.getOutputPath().toString());
        outputFileName.append("/" + outputMatrix.getFileName());
        File outputFile = outputFileWriter.createOutputFile(preProcessing, outputFileName.toString());

        outputFileWriter.writeOutputFile(outputFile, reportBuilder);

        if (!solver.isOutOfTime())
            try {
                printOutputInformation(outputMatrix, initialCols, preProcess.getRowsToRemove(), preProcess.getColsToRemove(), debugMode, verbose, solver.getMinCardinality(), solver.getMaxCardinality());
            } catch (OutOfMemoryError me) {
                System.err.println(me.toString());
                System.err.println("Execution interrupted > Cause: OUT OF MEMORY");
            }

        System.out.println("Number of MHS found: " + outputMatrix.getIntMatrix().length);
    }

    private void buildMBaseReportInformation(Matrix outputMatrix, long executionTime, Runtime runtime, StringBuilder sb, long minCard, long maxCard) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

        sb.append("Output Matrix:\n");
        sb.append("Minimum cardinality: ").append(minCard).append("\nMaximum cardinality: ").append(maxCard).append("\n");
        sb.append("Execution time MBase: ").append(executionTime).append(" ms").append("\n");
        sb.append("Memory used (MBase): ").append(bytesToMegaBytes(memoryAfter)).append("MB\n").append("\n");
        sb.append("Number of MHS found: ").append(outputMatrix.getIntMatrix().length).append("\n");
        sb.append(LINE).append("\n");
        sb.append("Output Matrix:").append("\n");
    }

    private void writePreProcessingMemoryUsed(StringBuilder sb, Runtime runtime) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        sb.append("Memory used (Pre-Elaboration): ").append(bytesToMegaBytes(memoryAfter)).append("MB\n");
    }

    private void buildOutputHeader(StringBuilder reportBuilder, String fileName, int initialRows, int initialCols) {
        reportBuilder.append(DOUBLE_LINE + "\n");
        reportBuilder.append("\t\t Matrix ").append(fileName).append("\n");
        reportBuilder.append(DOUBLE_LINE + "\n");
        reportBuilder.append("Input Matrix:\n").append("Size: ").append(initialRows).append("x").append(initialCols).append("\n");
        reportBuilder.append(LINE + "\n");
    }

    private long getMillis(long time) {
        return time * MILLISECONDS;
    }

    private void printPreProcessingInformation(PreProcessor preProcess, int[][] newInputIntMatrix, long timePP, boolean verbose, boolean debugMode, StringBuilder sb) {
//        System.out.println(DOUBLE_LINE);
        sb.append("Pre-Processing time: ").append(timePP).append(" ms\n");
        sb.append("#Rows removed " + "(").append(preProcess.getRowsToRemove().size()).append(")").append(" : ").append(preProcess.getRowsToRemove().toString()).append("\n");
        sb.append("#Columns removed " + "(").append(preProcess.getColsToRemove().size()).append(")").append(" : ").append(preProcess.getColsToRemove().toString()).append("\n");
        sb.append("Matrix Pre-Processed:\nSize: ").append(newInputIntMatrix.length).append("x").append(newInputIntMatrix[0].length).append("\n");
        sb.append(LINE).append("\n");

        System.out.println("Pre-Processing time: " + timePP + " ms");
        System.out.println("#Rows removed " + "(" + preProcess.getRowsToRemove().size() + ")" + " : " + preProcess.getRowsToRemove().toString());
        System.out.println("#Columns removed " + "(" + preProcess.getColsToRemove().size() + ")" + " : " + preProcess.getColsToRemove().toString());
        System.out.println("Matrix Pre-Processed:\nSize: " + newInputIntMatrix.length + "x" + newInputIntMatrix[0].length);
        printMatrix(newInputIntMatrix, debugMode, verbose);
    }

    private void printInputInformation(Matrix inputMatrix) {
        String inputMatrixName = inputMatrix.getFileName();
        System.out.println(DOUBLE_LINE);
        System.out.println("Input file (.matrix): " + inputMatrixName);
        int[][] inputIntMatrix = inputMatrix.getIntMatrix();
        System.out.println("Size: " + inputIntMatrix.length + "x" + inputIntMatrix[0].length);
//        printMatrix(inputIntMatrix, true);
    }

    private void printOutputInformation(Matrix outputMatrix, int initialCols, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, boolean debug, boolean verbose, long minCard, long maxCard) {
        String outputMatrixName = outputMatrix.getFileName();
        int[][] outputIntMatrix = outputMatrix.getIntMatrix();
//        System.out.println("Output file: " + outputMatrixName);
        System.out.println("Output file: " + outputMatrix.getFileName());
        System.out.println("Output Matrix:");
        System.out.println(outputIntMatrix.length + "x" + outputIntMatrix[0].length);
        System.out.println("Minimum cardinality: " + minCard + "\nMaximum cardinality: " + maxCard);
        printOutputMatrix(outputIntMatrix, debug, verbose, rowsRemoved, colsRemoved, initialCols);
    }

    private void printOutputMatrix(int[][] matrix, boolean debug, boolean verbose, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, int initialCols) {
        if (rowsRemoved.isEmpty() && colsRemoved.isEmpty()) {
            printMatrix(matrix, debug, verbose);
            return;
        }

        int[][] outputMatrix = new int[matrix.length][initialCols];

        if (debug)
            System.out.println("output.matrix Size: " + matrix.length + "x" + initialCols);
//        System.out.println("Size: " + outputMatrix.length + "x" + outputMatrix[0].length);

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < initialCols; j++) {
                if (!colsRemoved.contains(j))
                    outputMatrix[i][j] = matrix[i][colCount++];
            }
        }

        printMatrix(outputMatrix, debug, verbose);

    }

    private void printMatrix(int[][] intMatrix, boolean debugMode, boolean verbose) {
        if (!debugMode && !verbose) {
            return;
        }

        for (int[] intCol : intMatrix) {
            for (int j = 0; j < intMatrix[0].length; j++) {
                System.out.print(intCol[j] + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }
    }

    private void calculateUsedMemory(boolean verbose, Runtime runtime, String s) {
        if (verbose) {
            long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
//            System.out.println(s + memoryAfter + "bytes");
            System.out.println(s + bytesToMegaBytes(memoryAfter) + "MB");
        }
    }

    private void outputMatrixToStringBuilder(StringBuilder sb, int[][] matrix, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, int initialCols) {
        if (rowsRemoved.isEmpty() && colsRemoved.isEmpty()) {
            matrixToString(sb, matrix);
            return;
        }

        int[][] outputMatrix = new int[matrix.length][initialCols]; // Output matrix with the correct columns (i.e. initialCols)

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < initialCols; j++) {
                if (!colsRemoved.contains(j))
                    outputMatrix[i][j] = matrix[i][colCount++];
            }
        }

        matrixToString(sb, outputMatrix);
    }

    private void matrixToString(StringBuilder sb, int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                sb.append(matrix[i][j]).append(" ");
            }
            sb.append("-\n");
        }
    }
}
