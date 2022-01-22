package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.utility.Args;
import unibs.it.dii.utility.FileMatrixReader;
import unibs.it.dii.utility.OutputCSVWriter;
import unibs.it.dii.utility.OutputFileWriter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MinimalHittingSet {

    final static private String DOUBLE_LINE = "==========================================";
    final static private String LINE = "------------------------------------------";
    final static private String MSG_READING_MATRIX_FILE = "\t\tReading .matrix file...";
    final static private String MSG_PRE_PROCESSING_RUNNING = "\t\tRun Pre-Processing...";
    final static private String MSG_MBASE_EXECUTION = "\t\tMBase Execution...";

    final static private String PATH_TO_CSV = "./csv";
    final static private String CSV_FILE_NAME = "mhs-report.csv";

    final static private String CSV_HEADER = "Matrix,Execution time Pre-Elaboration,Execution time MBase,Pre-Elaboration RAM (MB),MBase RAM (MB),Out of time,Out of memory,Rows,Columns,Cardinality Min,Cardinality Max";

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
        // Help call by CLI
        if (arguments.isHelp()) {
            jc.usage();
            System.exit(0);
        }

        // Experimental purpose
        final boolean debugMode = false;

        // Set variables with arguments passed
        final boolean preProcessing = arguments.isPreProcessing();
        final long timeout = getMillis(arguments.getTimeout());
        final boolean verbose = arguments.isVerbose();

        // CSV
        OutputCSVWriter csvWriter = new OutputCSVWriter();
        Path csvPath = Paths.get(PATH_TO_CSV + "/" + CSV_FILE_NAME);

        if (!Files.exists(Paths.get(PATH_TO_CSV))) // ./csv directory does not exist
            Files.createDirectory(Paths.get(PATH_TO_CSV));

        if (!Files.exists(csvPath)) {
            Files.createFile(csvPath);
            // Create the header of csv
            csvWriter.writeCSVHeader(csvPath, CSV_HEADER.split(","));
        }


        if (!Files.exists(arguments.getOutputPath())) {
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

        final Path inputPath = arguments.getInputPath();
        final File file = inputPath.toFile();
        //System.out.println(file.getName());
        final FileMatrixReader reader = new FileMatrixReader();
        final OutputFileWriter outputFileWriter = new OutputFileWriter();

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_READING_MATRIX_FILE);
        System.out.println(DOUBLE_LINE);

        final Matrix inputMatrix = reader.readMatrixFromFile(file);
        final int initialRows = inputMatrix.getIntMatrix().length;
        final int initialCols = inputMatrix.getIntMatrix()[0].length;

        printInputInformation(inputMatrix);

        final PreProcessor preProcess = new PreProcessor(debugMode);

        long endTimePP = 0;
        long startTimePP = 0;

        StringBuilder reportBuilder = new StringBuilder();

        buildOutputHeader(reportBuilder, inputMatrix.getFileName(), initialRows, initialCols, timeout);

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

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_MBASE_EXECUTION);
        System.out.println(DOUBLE_LINE);

        System.out.println(LINE);
        System.out.println("\tInitial memory available: " + bytesToMegaBytes(runtime.totalMemory()) + " MB");
        System.out.println(LINE);

//        calculateUsedMemory(verbose, runtime, "Used memory before MBase execution: ");

        StringBuilder outputFileName = new StringBuilder(arguments.getOutputPath().toString());
        outputFileName.append("/" + inputMatrix.getFileName());
        File outputFile = outputFileWriter.createOutputFile(preProcessing, outputFileName.toString());
        outputFileWriter.setFile(outputFile);

        // Write header information + [Pre-Processing report : optional]
        outputFileWriter.writeOutputFile(outputFile, reportBuilder);

        long startTime = System.currentTimeMillis();

        // Execution of MBase
        final Matrix outputMatrix = solver.computeMinimalHittingSets(inputMatrix, timeout - (endTimePP - startTimePP), runtime, outputFileWriter);

        long endTime = System.currentTimeMillis();

        // Execution time of MBase procedure
        long executionTime = endTime - startTime;

//        System.out.println("Number of MHS found: " + outputMatrix.getIntMatrix().length);

        // TODO -> write on csv and create the ####.####.out

        StringBuilder sbReportMBase = new StringBuilder();

        if (!solver.isOutOfMemory()) {
            buildMBaseReportInformation(outputMatrix, executionTime, runtime, sbReportMBase, solver.getMinCardinality(), solver.getMaxCardinality());

            try {
                outputMatrixToStringBuilder(sbReportMBase, outputMatrix.getIntMatrix(), preProcess.getRowsToRemove(), preProcess.getColsToRemove(), initialCols);
            } catch (OutOfMemoryError me) {
                System.err.println("String building output matrix failed > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(outputFile, new StringBuilder("String building output matrix failed > Cause: OUT OF MEMORY\n"));
            }

            System.out.println("MBase execution time: " + executionTime + " ms");

            outputFileWriter.writeOutputFile(outputFile, sbReportMBase);

            try {
                printOutputInformation(outputMatrix, initialCols, preProcess.getRowsToRemove(), preProcess.getColsToRemove(), debugMode, verbose, solver.getMinCardinality(), solver.getMaxCardinality());
            } catch (OutOfMemoryError me) {
//                System.err.println(me.toString());
                System.err.println("Impossible to write output matrix on file .out > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(outputFile, new StringBuilder("Impossible to print matrix > Cause: OUT OF MEMORY\n"));
//                System.exit(200);
            }
        }

        System.out.println("For more details: " + outputFile.getAbsolutePath());
    }

    private void buildMBaseReportInformation(Matrix outputMatrix, long executionTime, Runtime runtime, StringBuilder sb, long minCard, long maxCard) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();

//        sb.append("Output Matrix:\n");
//        sb.append("Minimum cardinality: ").append(minCard).append("\nMaximum cardinality: ").append(maxCard).append("\n");
        sb.append("Execution time MBase: ").append(executionTime).append(" ms").append("\n");
        sb.append("Memory used (MBase): ").append(bytesToMegaBytes(memoryAfter)).append("MB\n").append("\n");
//        sb.append("Number of MHS found: ").append(outputMatrix.getIntMatrix().length).append("\n");
        sb.append(LINE).append("\n");
        sb.append("Output Matrix:").append("\n");
    }

    private void writePreProcessingMemoryUsed(StringBuilder sb, Runtime runtime) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        sb.append("Memory used (Pre-Elaboration): ").append(bytesToMegaBytes(memoryAfter)).append("MB\n");
    }

    private void buildOutputHeader(StringBuilder sb, String fileName, int initialRows, int initialCols, long timeout) {
        sb.append(DOUBLE_LINE + "\n");
        sb.append("\t\t Matrix ").append(fileName).append("\n");
        sb.append(DOUBLE_LINE + "\n");
        sb.append("Timeout: ").append(timeout).append(" ms\n");
        sb.append("Input Matrix:\n").append("Size: ").append(initialRows).append("x").append(initialCols).append("\n");
        sb.append(LINE + "\n");
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

        try {
            int[][] outputMatrix = new int[matrix.length][initialCols]; // Output matrix with the correct columns (i.e. initialCols)

            for (int i = 0; i < matrix.length; i++) {
                for (int j = 0, colCount = 0; j < initialCols; j++) {
                    if (!colsRemoved.contains(j))
                        outputMatrix[i][j] = matrix[i][colCount++];
                }
            }

            matrixToString(sb, outputMatrix);
        } catch (OutOfMemoryError me) {
            System.err.println("Write output file interrupted > Cause : OUT OF MEMORY");
        }

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
