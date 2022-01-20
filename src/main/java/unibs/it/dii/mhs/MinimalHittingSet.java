package unibs.it.dii.mhs;

import com.beust.jcommander.JCommander;
import unibs.it.dii.mhs.model.Matrix;
import unibs.it.dii.mhs.model.PreProcessor;
import unibs.it.dii.utility.Args;
import unibs.it.dii.utility.FileMatrixReader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

public class MinimalHittingSet {

    final static private String DOUBLE_LINE = "==========================================";
    final static private String MSG_READING_MATRIX_FILE = "\t\t  Reading .matrix file...";
    final static private String MSG_PRE_PROCESSING_RUNNING = "\t\t\tRun Pre-Processing...";
    final static private String MSG_MBASE_EXECUTION = "\t\t\tMBase Execution...";

    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMegaBytes(long bytes) {
        return bytes / MEGABYTE;
    }

    public static void main(String[] args) throws Exception {
        final Args arguments = new Args();
        JCommander.newBuilder()
                .addObject(arguments)
                .build()
                .parse(args);

        MinimalHittingSet main = new MinimalHittingSet();
        main.run(arguments);

    }

    public void run(Args arguments) throws Exception {
        final boolean debugMode = false;
        final boolean preProcessing = true;
        final long timeout = 10000; // seconds

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

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_READING_MATRIX_FILE);

        final Matrix inputMatrix = reader.readMatrixFromFile(file);
        final int initialRows = inputMatrix.getIntMatrix().length;
        final int initialCols = inputMatrix.getIntMatrix()[0].length;

        printInputInformation(inputMatrix);

        final PreProcessor preProcess = new PreProcessor(debugMode);
//        final Matrix newInputMatrix = new Matrix();
//        newInputMatrix.setFileName(inputMatrix.getFileName());

        long endTimePP = 0;
        long startTimePP = 0;

        if (preProcessing)
        {
            System.out.println(DOUBLE_LINE);
            System.out.println(MSG_PRE_PROCESSING_RUNNING);
            System.out.println(DOUBLE_LINE);

            calculateUsedMemory(true, runtime, "Used memory before Pre-Processing: ");
            // Pre-Processing execution
            startTimePP = System.currentTimeMillis();
            int[][] newInputIntMatrix = preProcess.computePreProcessing(inputMatrix.getIntMatrix());
            endTimePP = System.currentTimeMillis();
            calculateUsedMemory(true, runtime, "Used memory after Pre-Processing: ");

            printPreProcessingInformations(preProcess, newInputIntMatrix, endTimePP - startTimePP);

            inputMatrix.setIntMatrix(newInputIntMatrix);
        }
//        else {
//            newInputMatrix.setIntMatrix(inputMatrix.getIntMatrix());
//        }

        System.out.println(DOUBLE_LINE);
        System.out.println(MSG_MBASE_EXECUTION);
        System.out.println(DOUBLE_LINE);

        calculateUsedMemory(true, runtime, "Used memory before MBase execution: ");

        long startTime = System.currentTimeMillis();

        // Execution of MBase
        final Matrix outputMatrix = solver.computeMinimalHittingSets(inputMatrix, timeout - (endTimePP - startTimePP));

        long endTime = System.currentTimeMillis();

        long executionTime = endTime - startTime;

        calculateUsedMemory(true, runtime, "Used memory after MBase execution: ");

        System.out.println("MBase execution time: " + executionTime + " ms");

        printOutputInformation(outputMatrix, initialCols, preProcess.getRowsToRemove(), preProcess.getColsToRemove());

        System.out.println("Number of MHS found: " + outputMatrix.getIntMatrix().length);

//        Callable<Matrix> execution = () -> {
//            try {
//                long endTimePP = 0;
//                long startTimePP = 0;
//
//                if (preProcessing)
//                {
//                    calculateUsedMemory(true, runtime, "Used memory before Pre-Processing: ");
//                    // Pre-Processing execution
//                    startTimePP = System.currentTimeMillis();
//                    int[][] newInputIntMatrix = preProcess.computePreProcessing(inputMatrix.getIntMatrix());
//                    endTimePP = System.currentTimeMillis();
//                    calculateUsedMemory(true, runtime, "Used memory after Pre-Processing: ");
//
//                    printPreProcessingInformations(preProcess, newInputIntMatrix, endTimePP - startTimePP);
//
//                    newInputMatrix.setIntMatrix(newInputIntMatrix);
//                } else {
//                    newInputMatrix.setIntMatrix(inputMatrix.getIntMatrix());
//                    newInputMatrix.setFileName(inputMatrix.getFileName());
//                }
//
//                calculateUsedMemory(true, runtime, "Used memory before MBase execution: ");
//
//                long startTime = System.currentTimeMillis();
//
//                // Execution of MBase
//                final Matrix outputMatrix = solver.computeMinimalHittingSets(newInputMatrix, timeout - (endTimePP - startTimePP));
//
//                long endTime = System.currentTimeMillis();
//                long executionTime = endTime - startTime;
//                System.out.println("MBase execution time: " + executionTime + " ms");
//
//                return outputMatrix;
//            } catch (InterruptedException ie) {
//                throw new IllegalStateException("Execution interrupted!", ie);
//            }
//        };

//        final ExecutorService executor = Executors.newSingleThreadExecutor();
////        final Future future = executor.submit(execution);
//        final Future<Matrix> future = executor.submit(execution);
//        executor.shutdown(); // This does not cancel the already-scheduled task.
//
//        try {
//            Matrix outputMatrix = future.get(timeout, TimeUnit.SECONDS);
//
//            calculateUsedMemory(true, runtime, "Used memory after MBase execution is: ");
//
//            printOutputInformation(outputMatrix, inputMatrix, preProcess.getRowsToRemove(), preProcess.getColsToRemove());
//
//            System.out.println("Number of MHS found: " + outputMatrix.getIntMatrix().length);
//
//        } catch (InterruptedException ie) {
//            /* Handle the interruption. Or ignore it. */
//            System.out.println(ie.getCause());
//        } catch (ExecutionException ee) {
//            /* Handle the error. Or ignore it. */
//            System.out.println(ee.getCause());
//        } catch (TimeoutException te) {
//            /* Handle the timeout. Or ignore it. */
//            boolean timeoutCheck = true;
//            System.out.println("Timeout reached!");
////            executor.awaitTermination(10, TimeUnit.SECONDS);
//        }
//
//        if (!executor.isTerminated()) {
//            System.out.println("Not terminated!");
//            executor.shutdownNow(); // If you want to stop the code that hasn't finished.
//        }
////        calculateUsedMemory(true, runtime, "Used memory after execution is: ");
    }

    private void printPreProcessingInformations(PreProcessor preProcess, int[][] newInputIntMatrix, long timePP) {
        System.out.println(DOUBLE_LINE);
        System.out.println("Pre-Processing time: " + timePP + " ms");
        System.out.println("#Rows removed " + "(" + preProcess.getRowsToRemove().size() + ")" + " : " + preProcess.getRowsToRemove().toString());
        System.out.println("#Columns removed " + "(" + preProcess.getColsToRemove().size() + ")" + " : "+ preProcess.getColsToRemove().toString());
        System.out.println("Matrix Pre-Processed:\nSize: " + newInputIntMatrix.length + "x" + newInputIntMatrix[0].length);
        printMatrix(newInputIntMatrix, true);
    }

    private void printInputInformation(Matrix inputMatrix) {
        String inputMatrixName = inputMatrix.getFileName();
        System.out.println(DOUBLE_LINE);
        System.out.println("Input .matrix: " + inputMatrixName);
        int[][] inputIntMatrix = inputMatrix.getIntMatrix();
        System.out.println("Input Matrix:");
        System.out.println("Size: " + inputIntMatrix.length + "x" + inputIntMatrix[0].length);
        printMatrix(inputIntMatrix, true);
    }

    private void printOutputInformation(Matrix outputMatrix, int initialCols, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved) {
        String outputMatrixName = outputMatrix.getFileName();
        int[][] outputIntMatrix = outputMatrix.getIntMatrix();
//        System.out.println("Output file: " + outputMatrixName);
        System.out.println("Output file: " + outputMatrix.getFileName());
        System.out.println("Output Matrix:");
        System.out.println(outputIntMatrix.length + "x" + outputIntMatrix[0].length);
        printOutputMatrix(outputIntMatrix, true, rowsRemoved, colsRemoved, initialCols);
    }

    private void printOutputMatrix(int[][] matrix, boolean debug, ArrayList<Integer> rowsRemoved, ArrayList<Integer> colsRemoved, int initialCols) {
        if (rowsRemoved.isEmpty() && colsRemoved.isEmpty()) {
            printMatrix(matrix, debug);
            return;
        }

        int[][] outputMatrix = new int[matrix.length][initialCols];
        System.out.println("output.matrix Size: " + matrix.length + "x" + initialCols);
//        System.out.println("Size: " + outputMatrix.length + "x" + outputMatrix[0].length);

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0, colCount = 0; j < initialCols; j++) {
                if (!colsRemoved.contains(j))
                    outputMatrix[i][j] = matrix[i][colCount++];
            }
        }

        printMatrix(outputMatrix, debug);

    }

    private void printMatrix(int[][] intMatrix, boolean debugMode) {
        if (!debugMode) {
            return;
        }

        for (int[] intCol : intMatrix) {
            for (int j = 0; j < intMatrix[0].length; j++) {
                System.out.print(intCol[j] + " "); // Print each row of the matrix
            }
            System.out.println("-"); // Print the end of a row
        }
    }

    private void calculateUsedMemory(boolean debugMode, Runtime runtime, String s) {
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        if (debugMode) {
//            System.out.println(s + memoryAfter + "bytes");
            System.out.println(s + bytesToMegaBytes(memoryAfter) + "MB");
        }
    }

}
