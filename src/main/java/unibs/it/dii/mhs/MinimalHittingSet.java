package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.utility.Args;
import unibs.it.dii.utility.FileMatrixReader;
import unibs.it.dii.utility.OutputCSVWriter;
import unibs.it.dii.utility.OutputFileWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.StringJoiner;

public class MinimalHittingSet {

    final static private String DOUBLE_LINE = "=========================================================================";
    final static private String LINE = "-------------------------------------------------------------------------";
    final static private String HEADER = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~";

    final static private String MSG_READING_MATRIX_FILE = "\t\t\t\tReading .matrix file...";
    final static private String MSG_PRE_PROCESSING_RUNNING = "\t\t\t\tRun Pre-Processing...";
    final static private String MSG_MBASE_EXECUTION = "\t\t\t\tMBase Execution...";

    final static private String PATH_TO_CSV = "./csv";
    final static private String CSV_FILE_NAME = "mhs-report.csv";

    final static private String CSV_HEADER = "Date-Time,Matrix,Execution time Pre-Elaboration (ms),Pre-Elaboration RAM (MB),Rows removed,Cols removed,Execution time MBase (ms),MBase RAM (MB),Out of Time,Out of Memory,Rows,Columns,Cardinality Min,Cardinality Max,#MHS";

    final static private String DATE_TIME = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss"));

    private static final long MEGABYTE = 1024L * 1024L;
    private static final long MILLISECONDS = 1000;

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
        final boolean verbose = arguments.isVerbose();
        final Path inputPath = arguments.getInputPath();
        final Path outputPath = arguments.getOutputPath();
        final long timeout = getMillis(arguments.getTimeout());

        final Path csvPath = Paths.get(PATH_TO_CSV + "/" + CSV_FILE_NAME);

        // Object to build the CSV report informations
        final StringJoiner stringJoiner = new StringJoiner(",");
        stringJoiner.add(DATE_TIME); // Add the first column value, namely the date-time information

        // Create the matrix reader
        final FileMatrixReader reader = new FileMatrixReader();
        // Create the writer for the output report information
        final OutputFileWriter outputFileWriter = new OutputFileWriter();

        // Create CSV writer object
        final OutputCSVWriter csvWriter = new OutputCSVWriter();

        checkCSVPath(csvPath, csvWriter);

        checkOutputPath(outputPath);

        // Get the Java runtime for memory (RAM) consumption
        Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();

        if (debugMode)
            getTotalMemoryAvailable(verbose, runtime, "Total memory available is: ");

        // Create the solver object
        final MinimalHittingSetSolver solver = new MinimalHittingSetSolver(debugMode);

        // Create the input file object
        final File inputFile = inputPath.toFile();

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_READING_MATRIX_FILE);
        System.out.println(DOUBLE_LINE);

        // Create the matrix object
        final Matrix inputMatrix = reader.readMatrixFromFile(inputFile);

        // Get the size of initial input matrix
        final int initialRows = inputMatrix.getIntMatrix().length;
        final int initialCols = inputMatrix.getIntMatrix()[0].length;

        printInputMatrixInformation(inputMatrix);
        stringJoiner.add(inputMatrix.getName());

        // Set the default value for the pre-processing time/memory
        long preProcessingTime = -1;
        long memory = -1;

        // Object to create the header of the output report file
        StringBuilder headerOutputBuilder = new StringBuilder();

        buildOutputHeader(headerOutputBuilder, inputMatrix.getName(), initialRows, initialCols, timeout);

        // Create the object to compute the pre-processing operation
        final PreProcessor preProcess = new PreProcessor(debugMode);

        if (preProcessing) {
            System.out.println(DOUBLE_LINE);
            System.out.println(MSG_PRE_PROCESSING_RUNNING);
            System.out.println(DOUBLE_LINE);

            if (debugMode)
                printUsedMemory(verbose, runtime, "Consumed memory before Pre-Processing: ");

            // Pre-Processing execution
            long startTimePP = System.currentTimeMillis();
            int[][] newInputIntMatrix = preProcess.computePreProcessing(inputMatrix.getIntMatrix());
            long endTimePP = System.currentTimeMillis();

            memory = printUsedMemory(verbose, runtime, "Consumed memory after Pre-Processing: ");

            // Compute the time to execute the pre-processing operation
            preProcessingTime = endTimePP - startTimePP;

            buildPreProcessingInformation(preProcess, newInputIntMatrix, preProcessingTime, verbose, debugMode, headerOutputBuilder, memory);

            // Set the new input matrix after pre-processing
            inputMatrix.setIntMatrix(newInputIntMatrix);
        }

        addPreProcessingInformationToStringJoiner(stringJoiner, preProcessingTime, memory, preProcess);

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_MBASE_EXECUTION);
        System.out.println(DOUBLE_LINE);

        if (verbose) {
            System.out.println(LINE);
            System.out.println("\t\t\tInitial memory available: " + bytesToMegaBytes(runtime.totalMemory()) + "MB");
            System.out.println(LINE);
        }

        if (debugMode)
            printUsedMemory(verbose, runtime, "Consumed memory before MBase execution: ");

        // Create the output file to save report information
        File outputFile = getOutputFile(preProcessing, outputPath, outputFileWriter, inputMatrix.getName());

        // Write header information + [Pre-Processing report : optional] in the output report information
        outputFileWriter.writeOutputFile(headerOutputBuilder);

        // Compute the residual time to execute MBase
        long residualTime = timeout - (preProcessingTime);

        // Execution of MBase procedure
        long startTimeMBase = System.currentTimeMillis();
        final Matrix outputMatrix = solver.computeMinimalHittingSets(inputMatrix, residualTime, runtime, outputFileWriter);
        long endTimeMBase = System.currentTimeMillis();

        // Execution time of MBase procedure
        long executionTimeMBase = endTimeMBase - startTimeMBase;

        // Consumed memory after MBase execution
        long memoryAfterMBase = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());

        addMBaseInformationToStringJoiner(stringJoiner, solver.getMhsFound(), solver, initialRows, initialCols, executionTimeMBase, memoryAfterMBase);

        writeCSVInformationReport(csvPath.toFile().toString(), stringJoiner, csvWriter);

        // Object to build the output report information about MBase execution
        StringBuilder sbReportMBase = new StringBuilder();

        // MBase execution (or building the output matrix) did not exceed the maximum RAM size
        if (!solver.isOutOfMemory()) {

            buildMBaseReportInformation(outputMatrix, executionTimeMBase, memoryAfterMBase, sbReportMBase, solver.getMinCardinality(), solver.getMaxCardinality());

            try {
                outputMatrixToStringBuilder(sbReportMBase, outputMatrix.getIntMatrix(), preProcess.getRowsToRemove(), preProcess.getColsToRemove(), initialCols, outputFileWriter);
            } catch (OutOfMemoryError me) {
                System.err.println("String building output matrix failed > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(new StringBuilder("String building output matrix failed > Cause: OUT OF MEMORY\n"));
            }

            if (verbose)
                System.out.println("MBase execution time: " + executionTimeMBase + " ms");

            // Write information about MBase execution on the output file
            outputFileWriter.writeOutputFile(sbReportMBase);

            try {
                printOutputInformation(outputMatrix, initialCols, preProcess.getRowsToRemove(), preProcess.getColsToRemove(), debugMode, verbose, solver.getMinCardinality(), solver.getMaxCardinality());
            } catch (OutOfMemoryError me) {
//                System.err.println(me.toString());
                System.err.println("Impossible to write output matrix on file .out > Cause: OUT OF MEMORY");
                outputFileWriter.writeOutputFile(new StringBuilder("Impossible to write output matrix on file .out > Cause: OUT OF MEMORY\n"));
//                System.exit(200);
            }
        }

        // Print the final message on standard output
        System.out.println("For more details: " + outputFile.getAbsolutePath());
    }

    private void writeCSVInformationReport(String pathString, StringJoiner stringJoiner, OutputCSVWriter csvWriter) throws IOException {
        csvWriter.setWriter(new CSVWriter(new FileWriter(pathString, true)));
        csvWriter.writeCSV(stringJoiner.toString().split(","));
    }

    private void addMBaseInformationToStringJoiner(StringJoiner stringJoiner, int mhsFound, MinimalHittingSetSolver solver, int initialRows, int initialCols, long executionTimeMBase, long memoryAfterMBase) {
        stringJoiner.add(String.valueOf(executionTimeMBase));
        stringJoiner.add(String.valueOf(memoryAfterMBase));
        stringJoiner.add(String.valueOf(solver.isOutOfTime()));
        stringJoiner.add(String.valueOf(solver.isOutOfMemory()));
        stringJoiner.add(String.valueOf(initialRows));
        stringJoiner.add(String.valueOf(initialCols));
        stringJoiner.add(String.valueOf(solver.getMinCardinality()));
        stringJoiner.add(String.valueOf(solver.getMaxCardinality()));
        stringJoiner.add(String.valueOf(mhsFound));
    }

    private void addPreProcessingInformationToStringJoiner(StringJoiner stringJoiner, long preProcessingTime, long memory, PreProcessor preProcess) {
        stringJoiner.add(String.valueOf(preProcessingTime));
        stringJoiner.add(String.valueOf(bytesToMegaBytes(memory)));
        stringJoiner.add(String.valueOf(preProcess.getRowsToRemove().size()));
        stringJoiner.add(String.valueOf(preProcess.getColsToRemove().size()));
    }

    /**
     * Method to convert bytes into MB.
     *
     * @param bytes
     * @return a value in MB
     */
    public static long bytesToMegaBytes(long bytes) {
        return bytes / MEGABYTE;
    }

    /**
     * Method to create the output file path.
     *
     * @param preProcessing
     * @param outputPath
     * @param outputFileWriter
     * @param inputMatrixName
     * @return
     * @throws IOException
     */
    private File getOutputFile(boolean preProcessing, Path outputPath, OutputFileWriter outputFileWriter, String inputMatrixName) throws IOException {
        StringBuilder outputFileName = new StringBuilder(outputPath.toString());
        outputFileName.append("/" + inputMatrixName);

        // Create the output file
        File outputFile = outputFileWriter.createOutputFile(preProcessing, outputFileName.toString());

        return outputFile;
    }

    /**
     * Method to print the total memory available.
     *
     * @param verbose
     * @param runtime
     * @param s
     */
    private void getTotalMemoryAvailable(boolean verbose, Runtime runtime, String s) {
        if (verbose) {
            System.out.println(s + bytesToMegaBytes(runtime.maxMemory()) + " MB");
        }
    }

    /**
     * Method to check if the output path passed by argument is correct.
     *
     * @param path
     * @throws IOException
     */
    private void checkOutputPath(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }

        if (!path.isAbsolute() || !Files.isDirectory(path)) {
            System.err.println("Check the absolute path for the output file");
            System.exit(-1);
        }
    }

    /**
     * Method to create the CSV file if it does not exist and create the CSV header.
     *
     * @param csvPath
     * @param writer
     * @throws IOException
     */
    private void checkCSVPath(Path csvPath, OutputCSVWriter writer) throws IOException {
        if (!Files.exists(Paths.get(PATH_TO_CSV))) // ./csv directory does not exist
            Files.createDirectory(Paths.get(PATH_TO_CSV));

        if (!Files.exists(csvPath)) { // The file .csv does not exist
            Files.createFile(csvPath);
            writer.setWriter(new CSVWriter(new FileWriter(csvPath.toFile().toString(), true)));
            writer.setReader(new CSVReader(new FileReader(csvPath.toFile().toString()))); // Not used yet!

            // Create the header of csv
            writer.writeHeaderCSV(CSV_HEADER.split(","));

            return;
        }

        writer.setWriter(new CSVWriter(new FileWriter(csvPath.toFile().toString(), true)));
        writer.setReader(new CSVReader(new FileReader(csvPath.toFile().toString()))); // Not used yet!
    }

    /**
     * Method to build the MBase execution information to save into the output file.
     *
     * @param outputMatrix
     * @param executionTime
     * @param memoryAfter
     * @param sb
     * @param minCard
     * @param maxCard
     */
    private void buildMBaseReportInformation(Matrix outputMatrix, long executionTime, long memoryAfter, StringBuilder sb, long minCard, long maxCard) {
//        sb.append("Execution time MBase: ").append(executionTime).append(" ms").append("\n");
        sb.append(LINE).append("\n");
        sb.append("Output Matrix:").append("\n");
    }

    /**
     * Method to build the output header for report file.
     *
     * @param sb
     * @param fileName
     * @param initialRows
     * @param initialCols
     * @param timeout
     */
    private void buildOutputHeader(StringBuilder sb, String fileName, int initialRows, int initialCols, long timeout) {
        sb.append(";;; ").append(DATE_TIME).append("\n");
        sb.append(HEADER + "\n");
        sb.append("\t\t\tMatrix ").append(fileName).append("\n");
        sb.append(HEADER + "\n");
        sb.append("Timeout: ").append(timeout).append(" ms\n");
        sb.append("Input Matrix:\n").append("Size: ").append(initialRows).append("x").append(initialCols).append("\n");
        sb.append(DOUBLE_LINE + "\n");
    }

    /**
     * Method to get the time in milliseconds.
     *
     * @param time a time value in seconds
     * @return
     */
    private long getMillis(long time) {
        return time * MILLISECONDS;
    }

    /**
     * Method to build the pre-processing operation report information
     *
     * @param preProcess
     * @param newInputIntMatrix
     * @param timePP
     * @param verbose
     * @param debugMode
     * @param sb
     */
    private void buildPreProcessingInformation(PreProcessor preProcess, int[][] newInputIntMatrix, long timePP, boolean verbose, boolean debugMode, StringBuilder sb, long memory) {
        sb.append("\t\t\tPre-Elaboration").append("\n");
        sb.append(DOUBLE_LINE).append("\n");
        sb.append("Consumed memory (Pre-Elaboration): ").append(memory).append("MB\n");
        sb.append("Pre-Elaboration time: ").append(timePP).append(" ms\n");
        sb.append("#Rows removed " + "(").append(preProcess.getRowsToRemove().size()).append(")").append(" : ").append(preProcess.getRowsToRemove().toString()).append("\n");
        sb.append("#Columns removed " + "(").append(preProcess.getColsToRemove().size()).append(")").append(" : ").append(preProcess.getColsToRemove().toString()).append("\n");
        sb.append("Matrix Pre-Processed:\nSize: ").append(newInputIntMatrix.length).append("x").append(newInputIntMatrix[0].length).append("\n");

        if (verbose) {
            System.out.println("Pre-Processing time: " + timePP + " ms");
            System.out.println("#Rows removed " + "(" + preProcess.getRowsToRemove().size() + ")" + " : " + preProcess.getRowsToRemove().toString());
            System.out.println("#Columns removed " + "(" + preProcess.getColsToRemove().size() + ")" + " : " + preProcess.getColsToRemove().toString());
            System.out.println("Matrix Pre-Processed:\nSize: " + newInputIntMatrix.length + "x" + newInputIntMatrix[0].length);
            printMatrix(newInputIntMatrix, debugMode, verbose);
        }
    }

    /**
     * Method to print the input matrix information on the standard output.
     *
     * @param inputMatrix
     */
    private void printInputMatrixInformation(Matrix inputMatrix) {
        String inputMatrixName = inputMatrix.getName();

        System.out.println(DOUBLE_LINE);
        System.out.println("Input file (.matrix): " + inputMatrixName);

        int[][] inputIntMatrix = inputMatrix.getIntMatrix();
        System.out.println("Size: " + inputIntMatrix.length + "x" + inputIntMatrix[0].length);
    }

    /**
     * Method to print information on the standard output about the outputs of MBase procedure.
     *
     * @param outputMatrix
     * @param initialCols
     * @param rowsRemoved
     * @param colsRemoved
     * @param debug
     * @param verbose
     * @param minCard
     * @param maxCard
     */
    private void printOutputInformation(Matrix outputMatrix, int initialCols, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, boolean debug, boolean verbose, long minCard, long maxCard) {
        if (verbose) {
            String outputMatrixName = outputMatrix.getName();
            int[][] outputIntMatrix = outputMatrix.getIntMatrix();

//            System.out.println("Output file: " + outputMatrix.getFileName() + ".out");
            System.out.println("Output Matrix:");
//            System.out.println(outputIntMatrix.length + "x" + outputIntMatrix[0].length);
            System.out.println("Minimum cardinality: " + minCard + "\nMaximum cardinality: " + maxCard);
            checkPrintOutputMatrix(outputIntMatrix, debug, verbose, rowsRemoved, colsRemoved, initialCols);
        }
    }

    /**
     * Method to build the matrix with the correct number of the initial input matrix columns.
     *
     * @param matrix
     * @param debug
     * @param verbose
     * @param rowsRemoved
     * @param colsRemoved
     * @param initialCols
     */
    private void checkPrintOutputMatrix(int[][] matrix, boolean debug, boolean verbose, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, int initialCols) {
        if (rowsRemoved.isEmpty() && colsRemoved.isEmpty()) {
            printMatrix(matrix, debug, verbose);
            return;
        }

        int[][] outputMatrix = new int[matrix.length][initialCols];

        if (debug)
            System.out.println("matrix.out > Size: " + matrix.length + "x" + initialCols);

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < initialCols; j++) {
                if (!colsRemoved.contains(j))
                    outputMatrix[i][j] = matrix[i][colCount++];
            }
        }

        printMatrix(outputMatrix, debug, verbose);

    }

    /**
     * Method to print the matrix on the standard output.
     *
     * @param intMatrix
     * @param debugMode
     * @param verbose
     */
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

    /**
     * Method to print the memory usage.
     *
     * @param verbose
     * @param runtime
     * @param s
     */
    private long printUsedMemory(boolean verbose, Runtime runtime, String s) {
        long memoryAfter = 0;
        if (verbose) {
            memoryAfter = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());
            System.out.println(s + (memoryAfter) + "MB");
        }

        return memoryAfter;
    }

    /**
     * Method to build the matrix with the correct number of the initial input matrix columns.
     *
     * @param sb
     * @param matrix
     * @param rowsRemoved
     * @param colsRemoved
     * @param initialCols
     * @param outputWriter
     * @throws IOException
     */
    private void outputMatrixToStringBuilder(StringBuilder sb, int[][] matrix, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, int initialCols, OutputFileWriter outputWriter) throws IOException {
        if (rowsRemoved.isEmpty() && colsRemoved.isEmpty()) {
            matrixToStringBuilder(sb, matrix);
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

            matrixToStringBuilder(sb, outputMatrix);

        } catch (OutOfMemoryError me) {
            System.err.println("Impossible to build the matrix string > Cause : OUT OF MEMORY");
            outputWriter.writeOutputFile(new StringBuilder("Impossible to build the matrix string > Cause : OUT OF MEMORY"));
            System.exit(200);
        }

    }

    /**
     * Method to add the matrix to the StringBuilder.
     *
     * @param sb
     * @param matrix
     */
    private void matrixToStringBuilder(StringBuilder sb, int[][] matrix) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                sb.append(matrix[i][j]).append(" ");
            }
            sb.append("-\n");
        }
    }
}
