package unibs.it.dii.mhs;

import com.opencsv.CSVWriter;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.utility.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class MinimalHittingSetFacade {
    final static private String DOUBLE_LINE = "=========================================================================";
    final static private String LINE = "-------------------------------------------------------------------------";
    final static private String HEADER = "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~";

    final static private String MSG_READING_MATRIX_FILE = "\t\t\t\tReading .matrix file...";
    final static private String MSG_PRE_PROCESSING_RUNNING = "\t\t\t\tRun Pre-Processing...";
    final static private String MSG_MBASE_EXECUTION = "\t\t\t\tMBase Execution...";
    final static private String MSG_WRITING_FILE = "\t\t\t\tWriting output file...";
    final static private String MSG_WRITING_CSV = "\t\t\t\tWriting CSV...";

    final static private String PATH_TO_CSV = "./csv";
    final static private String CSV_FILE_NAME = "mhs-report-" + LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")) + ".csv";

    final static private String CSV_HEADER = "Date-Time,Matrix,Execution time Pre-Elaboration (ms),Pre-Elaboration RAM (MB),Rows removed,Cols removed,Execution time MBase (ms),MBase RAM (MB),Out of Time,Out of Memory,Rows,Columns,Cardinality Min,Cardinality Max,#MHS";

    private static final long MEGABYTE = 1024L * 1024L;
    final static public int STD_OUT_MHS_LIMIT = 10000;

    /**
     *
     */
    final private boolean preProcessing;
    final private boolean verbose;
    final private Path inputPath;
    final private Path outputPath;
    final private Path inputDirectoryPath;
    final private long timeout;
    final private boolean automaticMode;
    private Queue<String> benchmarkFileQueue = new LinkedList<>();
    final OutputCSVWriter csvWriter = new OutputCSVWriter();
    final FileMatrixReader reader = new FileMatrixReader();
    final OutputFileWriter outputFileWriter;

    public MinimalHittingSetFacade(boolean preProcessing, boolean verbosity, Path inputPath, Path outputPath, Path inputDirectoryPath, long timeout, boolean automaticMode) throws IOException {
        this.preProcessing = preProcessing;
        this.verbose = verbosity;
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.inputDirectoryPath = inputDirectoryPath;
        this.timeout = timeout;
        this.automaticMode = automaticMode;
        this.outputFileWriter = new OutputFileWriter(outputPath);
    }

    /**
     * @throws Exception
     */
    public void find() throws Exception {
        // Experimental purpose
        final boolean debugMode = false;

        // Get the Java run-time for memory (RAM) consumption
        final Runtime runtime = Runtime.getRuntime();
        // Run the garbage collector
        runtime.gc();

        if (verbose)
            System.out.println("Max memory available: " + bytesToMegaBytes(runtime.maxMemory()) + " MB");

        final Path csvPath = Paths.get(PATH_TO_CSV + "/" + CSV_FILE_NAME);
        // Create CSV writer object
        final OutputCSVWriter csvWriter = new OutputCSVWriter();

        checkCSVPath(csvPath, csvWriter);

        fillInputBenchmarkFiles();

        if (debugMode)
            benchmarkFileQueue.forEach(System.out::println);

        printStartingMessage(benchmarkFileQueue);

        int numberFileToProcess = benchmarkFileQueue.size();

        for (int i = 0; !benchmarkFileQueue.isEmpty(); i++) {
            if (i > 0) {
                System.out.println("Remaining benchmark files to process: " + (numberFileToProcess - i));
                TimeUnit.SECONDS.sleep(1);
            }

            // Object to build row to add into the CSV file
            StringJoiner stringJoiner = new StringJoiner(",");
            // Add the first column value, namely the date-time information
            stringJoiner.add(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss")));

            // Create the input file object
            final File inputFile = new File(benchmarkFileQueue.poll());

            printStatusInformation(MSG_READING_MATRIX_FILE);

            // Create the matrix object
            final Matrix inputMatrix = reader.readMatrixFromFile(inputFile);

            printBoolMatrix(inputMatrix.getBoolMatrix(), "Input Matrix:");

            // Get the size of initial input matrix
            int initialRows = inputMatrix.getBoolMatrix().length;
            int initialCols = inputMatrix.getBoolMatrix()[0].length;

            // STDOUT
            printInputMatrixInformation(initialRows, initialCols, inputMatrix.getName());
            // CSV
            stringJoiner.add(inputMatrix.getName());

            // Set the default value for the pre-processing time/memory
            long preProcessingTime = 0;
            long memoryConsumedPP = -1;
            ArrayList<Integer> colsRemoved = new ArrayList<>();
            ArrayList<Integer> rowsRemoved = new ArrayList<>();

            // Create the MHS solver object
            final MinimalHittingSetSolver solver = new MinimalHittingSetSolver(debugMode);

            StringBuilder headerOutputStringBuilder = buildOutputHeaderString(inputMatrix.getName(), initialRows, initialCols, timeout);

            if (preProcessing) {
                printStatusInformation(MSG_PRE_PROCESSING_RUNNING);

                // Create the object to compute the pre-processing operation
                MinimalHittingSetPreProcessor preProcess = new MinimalHittingSetPreProcessor(debugMode);

                if (debugMode)
                    printUsedMemory("Consumed memory before Pre-Processing: ", runtime);

                // Pre-Processing execution
                long startTimePP = System.currentTimeMillis();
                boolean[][] newInputBoolMatrix = preProcess.execute(inputMatrix.getBoolMatrix());
                long endTimePP = System.currentTimeMillis();

                memoryConsumedPP = printUsedMemory("Consumed memory (Pre-Processing): ", runtime);

                // Compute the time to execute the pre-processing operation
                preProcessingTime = endTimePP - startTimePP;

                rowsRemoved = preProcess.getRowsToRemove();
                colsRemoved = preProcess.getColsToRemove();

                buildPreProcessingInformation(rowsRemoved, colsRemoved, newInputBoolMatrix, preProcessingTime, headerOutputStringBuilder, memoryConsumedPP);

                // Set the new input matrix after pre-processing
                inputMatrix.setBoolMatrix(newInputBoolMatrix);
            }

            addPreProcessingInformationToStringJoiner(stringJoiner, preProcessingTime, memoryConsumedPP, colsRemoved.size(), rowsRemoved.size());

            printStatusInformation(MSG_MBASE_EXECUTION);

            printTotalMemoryAvailable(runtime, "\t\t\tInitial memory available: ");

            if (debugMode)
                printUsedMemory("Consumed memory before MBase execution: ", runtime);

            // Create the output file to save report information
            File outputFile = getOutputFile(inputMatrix.getName());

            // Write header information + [Pre-Processing report : optional] in the output report information
            outputFileWriter.writeOutputFile(headerOutputStringBuilder);

            // Compute the residual time to execute MBase
            long residualTime = timeout - preProcessingTime;

            HashMap<String, String> informationMBase = new HashMap<>();

            // Execution of MBase procedure
            Matrix outputMatrix = solver.execute(inputMatrix, residualTime, runtime, outputFileWriter, colsRemoved, initialCols);

            // Execution time of MBase procedure
            informationMBase.put("time", String.valueOf(solver.getExecutionTime()));
            // Consumed memory after MBase execution
            informationMBase.put("memory", String.valueOf(solver.getConsumedMemory()));
            // OutOfTime flag
            informationMBase.put("outOfTime", String.valueOf(solver.isOutOfTime()));
            // OutOfMemory flag
            informationMBase.put("outOfMemory", String.valueOf(solver.isOutOfMemory()));
            // Minimum cardinality found
            informationMBase.put("min", String.valueOf(solver.getMinCardinality()));
            // Maximum cardinality found
            informationMBase.put("max", String.valueOf(solver.getMaxCardinality()));
            // Number of MHS found
            informationMBase.put("mhs", String.valueOf(solver.getNumberMHSFound()));

            addMBaseInformationToStringJoiner(stringJoiner, informationMBase, initialRows, initialCols);

            printStatusInformation(MSG_WRITING_FILE);

            outputFileWriter.writeOutputFile(buildOutputMatrixHeader());

            if (errorWithOutputMatrix(outputMatrix)) {
                System.err.println("Impossible to get output matrix (e.g. empty)");
                outputFileWriter.writeOutputFile(new StringBuilder("Impossible to get output matrix (e.g. empty)\n"));
            } else {
                try {
                    outputFileWriter.writeOutputMatrix(outputMatrix.getBoolMatrix(), colsRemoved, initialCols);
                } catch (OutOfMemoryError me) {
                    System.err.println("Build string of the output matrix failed > Cause: OUT OF MEMORY");
                    outputFileWriter.writeOutputFile(new StringBuilder("Build string of the output matrix failed > Cause: OUT OF MEMORY\n"));
                }
            }

            if (verbose && !errorWithOutputMatrix(outputMatrix)) {
                try {
                    printOutputInformation(outputMatrix.getBoolMatrix(), initialCols, colsRemoved);
                } catch (OutOfMemoryError me) {
                    System.err.println("Impossible to write output matrix on file .out > Cause: OUT OF MEMORY");
                    outputFileWriter.writeOutputFile(new StringBuilder("Impossible to write output matrix on file .out > Cause: OUT OF MEMORY\n"));
                }
            }

            printStatusInformation(MSG_WRITING_CSV);

            writeCSVInformationReport(csvPath.toFile().toString(), stringJoiner);

            // Call the garbage collector
            System.gc();

            // Print the final message on standard output
            System.out.println("For more details: " + outputFile.toString());
        }

    }

    private boolean errorWithOutputMatrix(Matrix outputMatrix) {
        return outputMatrix.getBoolMatrix()[0].length == 1;
    }

    private StringBuilder buildOutputMatrixHeader() {
        return new StringBuilder(LINE + "\n" + "Output Matrix:\n");
    }

    private void printStatusInformation(String status) {
        System.out.println(DOUBLE_LINE);
        System.out.println(status);
        System.out.println(DOUBLE_LINE);
    }

    /**
     * Method to fill the benchmark file(s) queue: if the user provided an input directory path, the method fill the queue
     * with the benchmarks file to process; otherwise it takes the benchmark file from the -in argument.
     *
     * @throws IOException
     */
    private void fillInputBenchmarkFiles() throws IOException {
        if (automaticMode) {
            BenchmarkDirectoryReader directoryReader = new BenchmarkDirectoryReader(inputDirectoryPath);
            benchmarkFileQueue = directoryReader.getListBenchmarkFileMatrix();

//            while (!benchmarkFileQueue.isEmpty())
//                benchmarkFileQueue.add(benchmarkFileQueue.poll());
        } else {
            benchmarkFileQueue.add(inputPath.toString());
        }
    }

    private void printStartingMessage(Queue<String> benchmarkFileQueue) throws InterruptedException {
        System.out.println("Number of benchmark file to process: " + benchmarkFileQueue.size());
        System.out.print("Starting");
        for (int i = 0; i < 3; ++i) {
            TimeUnit.MILLISECONDS.sleep(500);
            System.out.print(".");
        }
        System.out.println();
    }

    private void writeCSVInformationReport(String pathString, StringJoiner stringJoiner) throws IOException {
        csvWriter.setWriter(new CSVWriter(new FileWriter(pathString, true)));
        csvWriter.writeCSV(stringJoiner.toString().split(","));
    }

    private void addMBaseInformationToStringJoiner(StringJoiner stringJoiner, HashMap<String, String> information, int initialRows, int initialCols) {
        stringJoiner.add(information.get("time"));
        stringJoiner.add(information.get("memory"));
        stringJoiner.add(information.get("outOfTime"));
        stringJoiner.add(information.get("outOfMemory"));
        stringJoiner.add(String.valueOf(initialRows));
        stringJoiner.add(String.valueOf(initialCols));
        stringJoiner.add(information.get("min"));
        stringJoiner.add(information.get("max"));
        stringJoiner.add(information.get("mhs"));
    }

    private void addPreProcessingInformationToStringJoiner(StringJoiner stringJoiner, long preProcessingTime, long memory, int rowsRemoved, int colsRemoved) {
        stringJoiner.add(String.valueOf(preProcessingTime));
        stringJoiner.add(String.valueOf(memory));
        stringJoiner.add(String.valueOf(rowsRemoved));
        stringJoiner.add(String.valueOf(colsRemoved));
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
     * @param inputMatrixName
     * @return
     * @throws IOException
     */
    private File getOutputFile(String inputMatrixName) throws IOException {
        String outputFileName = new String(outputPath.toString() + "/" + inputMatrixName);

        // Create the output file
        File outputFile = outputFileWriter.createOutputFile(preProcessing, outputFileName);

        return outputFile;
    }

    /**
     * Method to print the total memory available.
     *
     * @param runtime
     * @param s
     */
    private void printTotalMemoryAvailable(Runtime runtime, String s) {
        System.out.println(LINE);
        System.out.println(s + bytesToMegaBytes(runtime.maxMemory()) + " MB");
        System.out.println(LINE);
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
            Files.createDirectories(Paths.get(PATH_TO_CSV));

        if (!Files.exists(csvPath)) { // The file .csv does not exist
            Files.createFile(csvPath);
        }

        if (csvPath.toFile().length() == 0) {
            writer.setWriter(new CSVWriter(new FileWriter(csvPath.toFile().toString(), true)));
//            writer.setReader(new CSVReader(new FileReader(csvPath.toFile().toString()))); // Not used yet!
            // Create the header of csv
            writer.writeCSV(CSV_HEADER.split(","));
        }
    }

    /**
     * Method to build the output header for report file.
     *
     * @param fileName
     * @param initialRows
     * @param initialCols
     * @param timeout
     * @return
     */
    private StringBuilder buildOutputHeaderString(String fileName, int initialRows, int initialCols, long timeout) {
        StringBuilder sb = new StringBuilder();
        sb.append(";;;\n").append(";;; ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy-HH:mm:ss"))).append("\n");
        sb.append(HEADER + "\n");
        sb.append("\t\t\tMatrix ").append(fileName).append("\n");
        sb.append(HEADER + "\n");
        sb.append("Timeout: ").append(timeout).append(" ms\n");
        sb.append("Input Matrix:\n").append("Size: ").append(initialRows).append("x").append(initialCols).append("\n");
        sb.append(DOUBLE_LINE + "\n");

        return sb;
    }

    /**
     * Method to build the pre-processing operation report information
     *
     * @param rowsRemoved
     * @param colsRemoved
     * @param newInputIntMatrix
     * @param timePP
     * @param sb
     * @param memory
     */
    private void buildPreProcessingInformation(ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, boolean[][] newInputIntMatrix, long timePP, StringBuilder sb, long memory) {
        sb.append("\t\t\tPre-Elaboration").append("\n");
        sb.append(DOUBLE_LINE).append("\n");
        sb.append("Consumed memory (Pre-Elaboration): ").append(memory).append("MB\n");
        sb.append("Pre-Elaboration time: ").append(timePP).append(" ms\n");
        sb.append("#Rows removed " + "(").append(rowsRemoved.size()).append(")").append(": ").append(rowsRemoved).append("\n");
        sb.append("#Columns removed " + "(").append(colsRemoved.size()).append(")").append(": ").append(colsRemoved).append("\n");
        sb.append("Matrix Pre-Processed:\nSize: ").append(newInputIntMatrix.length).append("x").append(newInputIntMatrix[0].length).append("\n");

        if (verbose) {
            System.out.println("Pre-Processing time: " + timePP + " ms");
            System.out.println("#Rows removed " + "(" + rowsRemoved.size() + ")" + ":\n" + rowsRemoved);
            System.out.println("#Columns removed " + "(" + colsRemoved.size() + ")" + ":\n" + colsRemoved);
            System.out.println("Matrix Pre-Processed:\nSize: " + newInputIntMatrix.length + "x" + newInputIntMatrix[0].length);
            printBoolMatrix(newInputIntMatrix, "");
        }
    }

    /**
     * Method to print the input matrix information on the standard output.
     *
     * @param rows
     * @param cols
     * @param name
     */
    private void printInputMatrixInformation(int rows, int cols, String name) {
        System.out.println(DOUBLE_LINE);
        System.out.println("Input file: " + name);
        System.out.println("Size: " + rows + "x" + cols);
    }

    /**
     * Method to print information on the standard output about the outputs of MBase procedure.
     *
     * @param matrix
     * @param initialCols
     * @param colsRemoved
     */
    private void printOutputInformation(boolean[][] matrix, int initialCols, ArrayList<Integer> colsRemoved) {
        System.out.println(LINE);
        checkPrintOutputMatrix(matrix, colsRemoved, initialCols);
    }

    /**
     * Method to build the matrix with the correct number of the initial input matrix columns.
     *
     * @param matrix
     * @param colsRemoved
     * @param initialCols
     */
    private void checkPrintOutputMatrix(boolean[][] matrix, ArrayList<Integer> colsRemoved, int initialCols) {
        if (matrix.length > STD_OUT_MHS_LIMIT) {
            System.out.println("MHS matrix too large to print on standard output. Check the report file, please.");
            return;
        }

        if (!preProcessing) {
            printBoolMatrix(matrix, "Output Matrix:");
            return;
        }

        boolean[][] outputMatrix = new boolean[matrix.length][initialCols];

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < initialCols; j++) {
                if (!colsRemoved.contains(j))
                    outputMatrix[i][j] = matrix[i][colCount++];
            }
        }

        printBoolMatrix(outputMatrix, "Output Matrix:");

    }

    /**
     * Method to print the matrix on the standard output.
     *
     * @param boolMatrix
     */
    private void printBoolMatrix(boolean[][] boolMatrix, String s) {
        if (verbose) {
            System.out.println(s);
            for (boolean[] intCol : boolMatrix) {
                for (int j = 0; j < boolMatrix[0].length; j++) {
                    System.out.print(intCol[j] ? 1 + " " : 0 + " "); // Print each row of the matrix
                }
                System.out.println("-"); // Print the end of a row
            }
        }
    }

    /**
     * Method to print the memory usage.
     *
     * @param msg
     */
    private long printUsedMemory(String msg, Runtime runtime) {
        long memoryAfter = 0;
        memoryAfter = bytesToMegaBytes(runtime.totalMemory() - runtime.freeMemory());

        if (verbose)
            System.out.println(msg + memoryAfter + "MB");

        return memoryAfter;
    }
}
