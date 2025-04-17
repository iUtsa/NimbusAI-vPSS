import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Main - The main application that orchestrates the data processing pipeline.
 * Process: Load CSV -> Plot -> Salt -> Smooth -> Graph
 */
public class Main {
    /**
     * Main method to run the application.
     * 
     * @param args Command line arguments:
     *            args[0] - Input CSV file path
     *            args[1] - Output directory for visualizations
     *            args[2] - Output processed data CSV path
     */
    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: java -jar NimbusAI-PSS.jar <input_csv> <output_dir> <output_data>");
            System.exit(1);
        }
        
        String inputFilePath = args[0];
        String outputDir = args[1];
        String outputDataPath = args[2];
        
        try {
            // Ensure output directory exists
            File outputDirFile = new File(outputDir);
            if (!outputDirFile.exists()) {
                outputDirFile.mkdirs();
            }
            
            // Define output paths for each stage
            String initialPlotPath = outputDir + "/initial_plot.png";
            String saltedPlotPath = outputDir + "/salted_plot.png";
            String smoothedPlotPath = outputDir + "/smoothed_plot.png";
            String finalPlotPath = outputDir + "/final_plot.png";
            
            // Step 1: Read CSV data
            System.out.println("Reading CSV data...");
            List<DataPoint> originalData = readCSV(inputFilePath);
            
            if (originalData.isEmpty()) {
                System.err.println("No data found in the CSV file.");
                System.exit(1);
            }
            
            // Step 2: Initial Plotting
            System.out.println("Creating initial plot...");
            Plotter plotter = new Plotter();
            plotter.createSingleSeriesPlot(originalData, "Original Data", initialPlotPath);
            
            // Step 3: Apply Salting
            System.out.println("Applying salting procedure...");
            Salter salter = new Salter();
            List<DataPoint> saltedData = salter.applySalting(originalData);
            plotter.createSingleSeriesPlot(saltedData, "Salted Data", saltedPlotPath);
            
            // Step 4: Apply Smoothing
            System.out.println("Applying smoothing algorithm...");
            Smoother smoother = new Smoother();
            List<DataPoint> smoothedData = smoother.applySmoothing(saltedData);
            plotter.createSingleSeriesPlot(smoothedData, "Smoothed Data", smoothedPlotPath);
            
            // Step 5: Create final graph with all data series
            System.out.println("Creating final visualization...");
            plotter.createMultiSeriesPlot(originalData, saltedData, smoothedData, finalPlotPath);
            
            // Save processed data
            saveProcessedData(outputDataPath, originalData, saltedData, smoothedData);
            
            System.out.println("Processing completed successfully.");
            System.out.println("Initial plot: " + initialPlotPath);
            System.out.println("Salted plot: " + saltedPlotPath);
            System.out.println("Smoothed plot: " + smoothedPlotPath);
            System.out.println("Final plot: " + finalPlotPath);
            System.out.println("Processed data: " + outputDataPath);
            
        } catch (Exception e) {
            System.err.println("Error processing data: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Read data from a CSV file.
     * 
     * @param filePath Path to the CSV file
     * @return List of DataPoint objects
     * @throws IOException If an I/O error occurs
     */
    private static List<DataPoint> readCSV(String filePath) throws IOException {
        List<DataPoint> dataPoints = new ArrayList<>();
        String line;
        boolean firstLine = true;
        int xIndex = 0;
        int yIndex = 1;
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    // Try to determine which columns to use
                    String[] headers = line.split(",");
                    for (int i = 0; i < headers.length; i++) {
                        String header = headers[i].trim().toLowerCase();
                        if (header.contains("time") || header.contains("date") || header.contains("x")) {
                            xIndex = i;
                        } else if (header.contains("value") || header.contains("y") || 
                                   header.contains("data") || header.contains("measure")) {
                            yIndex = i;
                        }
                    }
                    firstLine = false;
                    continue;
                }
                
                try {
                    String[] values = line.split(",");
                    if (values.length > Math.max(xIndex, yIndex)) {
                        // Try to parse as double first
                        try {
                            double x = Double.parseDouble(values[xIndex].trim());
                            double y = Double.parseDouble(values[yIndex].trim());
                            dataPoints.add(new DataPoint(x, y));
                        } catch (NumberFormatException e) {
                            // If parsing fails, use index as x value
                            double y = Double.parseDouble(values[yIndex].trim());
                            dataPoints.add(new DataPoint(dataPoints.size(), y));
                        }
                    }
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    // Skip lines that can't be parsed
                    System.err.println("Warning: Skipping line: " + line);
                }
            }
        }
        
        // If no data was read, try a more lenient approach - use any numeric column
        if (dataPoints.isEmpty()) {
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                firstLine = true;
                while ((line = br.readLine()) != null) {
                    if (firstLine) {
                        firstLine = false;
                        continue;
                    }
                    
                    String[] values = line.split(",");
                    for (int i = 0; i < values.length; i++) {
                        try {
                            double value = Double.parseDouble(values[i].trim());
                            dataPoints.add(new DataPoint(dataPoints.size(), value));
                            break;  // Use the first numeric column we find
                        } catch (NumberFormatException e) {
                            // Not a number, try next column
                        }
                    }
                }
            }
        }
        
        return dataPoints;
    }
    
    /**
     * Save all processed data to a CSV file.
     * 
     * @param filePath Path to save the CSV
     * @param originalData Original data points
     * @param saltedData Salted data points
     * @param smoothedData Smoothed data points
     * @throws IOException If an I/O error occurs
     */
    private static void saveProcessedData(String filePath, 
                                        List<DataPoint> originalData,
                                        List<DataPoint> saltedData,
                                        List<DataPoint> smoothedData) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // Write header
            writer.write("x,original_y,salted_y,smoothed_y\n");
            
            // Write data points
            for (int i = 0; i < originalData.size(); i++) {
                double x = originalData.get(i).getX();
                double originalY = originalData.get(i).getY();
                double saltedY = (i < saltedData.size()) ? saltedData.get(i).getY() : 0;
                double smoothedY = (i < smoothedData.size()) ? smoothedData.get(i).getY() : 0;
                
                writer.write(String.format("%.6f,%.6f,%.6f,%.6f\n", x, originalY, saltedY, smoothedY));
            }
        }
    }
}



