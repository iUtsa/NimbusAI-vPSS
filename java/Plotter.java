import java.io.File;
import java.io.IOException;
import java.util.List;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * Plotter - Responsible for creating visualizations from data points.
 * Uses JFreeChart to generate high-quality plots.
 */
public class Plotter {
    
    // Chart dimensions
    private static final int CHART_WIDTH = 800;
    private static final int CHART_HEIGHT = 600;
    
    /**
     * Creates a plot with a single data series.
     * 
     * @param data List of data points
     * @param seriesName Name of the data series
     * @param outputPath Path to save the plot image
     * @throws IOException If an I/O error occurs
     */
    public void createSingleSeriesPlot(List<DataPoint> data, String seriesName, 
                                      String outputPath) throws IOException {
        // Create dataset for the data
        XYSeries series = new XYSeries(seriesName);
        for (DataPoint point : data) {
            series.add(point.getX(), point.getY());
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        
        // Create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(
            seriesName + " Visualization",        // Title
            "X",                                  // X-axis label
            "Y",                                  // Y-axis label
            dataset,                              // Dataset
            PlotOrientation.VERTICAL,             // Orientation
            true,                                 // Show legend
            true,                                 // Show tooltips
            false                                 // Show URLs
        );
        
        // Customize the chart
        customizeChart(chart, seriesName);
        
        // Customize the renderer
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Set series appearance based on type
        if (seriesName.contains("Original")) {
            renderer.setSeriesPaint(0, new Color(0, 0, 255));  // Blue
            renderer.setSeriesStroke(0, new BasicStroke(1.5f));
            renderer.setSeriesShapesVisible(0, true);
        } else if (seriesName.contains("Salted")) {
            renderer.setSeriesPaint(0, new Color(0, 150, 0));  // Green
            renderer.setSeriesStroke(0, new BasicStroke(1.5f));
            renderer.setSeriesShapesVisible(0, true);
        } else if (seriesName.contains("Smoothed")) {
            renderer.setSeriesPaint(0, new Color(255, 0, 0));  // Red
            renderer.setSeriesStroke(0, new BasicStroke(2.0f));
            renderer.setSeriesShapesVisible(0, false);
        }
        
        plot.setRenderer(renderer);
        
        // Save the chart to file
        saveChart(chart, outputPath);
    }
    
    /**
     * Creates a plot with multiple data series (original, salted, and smoothed).
     * 
     * @param originalData Original data points
     * @param saltedData Salted data points
     * @param smoothedData Smoothed data points
     * @param outputPath Path to save the plot image
     * @throws IOException If an I/O error occurs
     */
    public void createMultiSeriesPlot(List<DataPoint> originalData, 
                                     List<DataPoint> saltedData,
                                     List<DataPoint> smoothedData,
                                     String outputPath) throws IOException {
        // Create dataset for the original data
        XYSeries originalSeries = new XYSeries("Original Data");
        for (DataPoint point : originalData) {
            originalSeries.add(point.getX(), point.getY());
        }
        
        // Create dataset for the salted data
        XYSeries saltedSeries = new XYSeries("Salted Data");
        for (DataPoint point : saltedData) {
            saltedSeries.add(point.getX(), point.getY());
        }
        
        // Create dataset for the smoothed data
        XYSeries smoothedSeries = new XYSeries("Smoothed Data");
        for (DataPoint point : smoothedData) {
            smoothedSeries.add(point.getX(), point.getY());
        }
        
        // Add all series to the collection
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(originalSeries);
        dataset.addSeries(saltedSeries);
        dataset.addSeries(smoothedSeries);
        
        // Create the chart
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Complete Data Processing Pipeline",  // Title
            "X",                                  // X-axis label
            "Y",                                  // Y-axis label
            dataset,                              // Dataset
            PlotOrientation.VERTICAL,             // Orientation
            true,                                 // Show legend
            true,                                 // Show tooltips
            false                                 // Show URLs
        );
        
        // Customize the chart
        customizeChart(chart, "Complete Visualization");
        
        // Customize the renderer
        XYPlot plot = chart.getXYPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        
        // Original data: blue with markers
        renderer.setSeriesPaint(0, new Color(0, 0, 255));      // Blue
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer.setSeriesShapesVisible(0, true);
        
        // Salted data: green with markers
        renderer.setSeriesPaint(1, new Color(0, 150, 0));      // Green
        renderer.setSeriesStroke(1, new BasicStroke(1.5f));
        renderer.setSeriesShapesVisible(1, true);
        
        // Smoothed data: red thick line
        renderer.setSeriesPaint(2, new Color(255, 0, 0));      // Red
        renderer.setSeriesStroke(2, new BasicStroke(2.5f));
        renderer.setSeriesShapesVisible(2, false);
        
        plot.setRenderer(renderer);
        
        // Save the chart to file
        saveChart(chart, outputPath);
    }
    
    /**
     * Apply common customizations to a chart.
     * 
     * @param chart The JFreeChart to customize
     * @param title Title for the chart
     */
    private void customizeChart(JFreeChart chart, String title) {
        chart.setBackgroundPaint(Color.WHITE);
        chart.setTitle(new TextTitle(title, new Font("SansSerif", Font.BOLD, 18)));
        chart.addSubtitle(new TextTitle("Generated by Nimbus AI-PSS", 
                         new Font("SansSerif", Font.PLAIN, 12)));
        
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.LIGHT_GRAY);
        plot.setDomainGridlinePaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.WHITE);
        plot.setOutlinePaint(Color.DARK_GRAY);
    }
    
    /**
     * Save a chart to an image file.
     * 
     * @param chart The JFreeChart to save
     * @param outputPath Path to save the image
     * @throws IOException If an I/O error occurs
     */
    private void saveChart(JFreeChart chart, String outputPath) throws IOException {
        File outputFile = new File(outputPath);
        
        // Create parent directories if they don't exist
        if (outputFile.getParentFile() != null) {
            outputFile.getParentFile().mkdirs();
        }
        
        ChartUtils.saveChartAsPNG(outputFile, chart, CHART_WIDTH, CHART_HEIGHT);
    }
}