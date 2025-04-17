import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Salter - Responsible for applying the salting procedure to data points.
 * The salting procedure adds controlled variability to the data to highlight 
 * certain patterns that might be obscured in the original data.
 */
public class Salter {
    
    // Constants for the salting procedure
    private static final double DEFAULT_SALT_FACTOR = 0.15;  // Default salt intensity (15%)
    private static final int SEED = 42;  // Random seed for reproducibility
    private final Random random;
    
    /**
     * Constructor initializes the random number generator with a fixed seed
     * for reproducible results.
     */
    public Salter() {
        this.random = new Random(SEED);
    }
    
    /**
     * Apply the salting procedure to a list of data points.
     * 
     * @param data List of original data points
     * @return List of salted data points
     */
    public List<DataPoint> applySalting(List<DataPoint> data) {
        return applySalting(data, DEFAULT_SALT_FACTOR);
    }
    
    /**
     * Apply the salting procedure to a list of data points with a specified salt factor.
     * 
     * @param data List of original data points
     * @param saltFactor Salt intensity (as a percentage of data range)
     * @return List of salted data points
     */
    public List<DataPoint> applySalting(List<DataPoint> data, double saltFactor) {
        if (data.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Copy the original data
        List<DataPoint> saltedData = new ArrayList<>(data.size());
        for (DataPoint point : data) {
            saltedData.add(point.copy());
        }
        
        // Calculate data range for scaling the salt
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (DataPoint point : data) {
            double y = point.getY();
            if (y < min) min = y;
            if (y > max) max = y;
        }
        
        double range = max - min;
        double saltAmount = range * saltFactor;
        
        // Apply different salting methods based on data characteristics
        if (detectTrend(data)) {
            applySaltingToTrend(saltedData, saltAmount);
        } else if (detectCycles(data)) {
            applySaltingToCycles(saltedData, saltAmount);
        } else {
            applySaltingRandom(saltedData, saltAmount);
        }
        
        return saltedData;
    }
    
    /**
     * Apply salting designed to enhance trend visibility.
     * 
     * @param data List of data points to modify
     * @param saltAmount Amount of salt to apply
     */
    private void applySaltingToTrend(List<DataPoint> data, double saltAmount) {
        // For trend data, we apply a systematic pattern that enhances the trend
        // This includes some amplification of the overall slope
        
        int n = data.size();
        
        // Find the average slope
        double avgSlope = 0;
        for (int i = 1; i < n; i++) {
            avgSlope += (data.get(i).getY() - data.get(i-1).getY()) / 
                       (data.get(i).getX() - data.get(i-1).getX());
        }
        avgSlope /= (n - 1);
        
        // Apply salting with trend enhancement
        for (int i = 0; i < n; i++) {
            DataPoint point = data.get(i);
            double normalizedPosition = (double) i / (n - 1);  // 0 to 1
            
            // Calculate salt: random component + trend enhancement
            double randomComponent = (random.nextDouble() * 2 - 1) * saltAmount;
            double trendComponent = avgSlope * normalizedPosition * saltAmount * 0.5;
            
            double saltedY = point.getY() + randomComponent + trendComponent;
            point.setY(saltedY);
        }
    }
    
    /**
     * Apply salting designed to enhance cycle visibility.
     * 
     * @param data List of data points to modify
     * @param saltAmount Amount of salt to apply
     */
    private void applySaltingToCycles(List<DataPoint> data, double saltAmount) {
        // For cyclic data, we apply a periodic component to enhance the cycles
        
        int n = data.size();
        double estimatedPeriod = estimatePeriod(data);
        
        for (int i = 0; i < n; i++) {
            DataPoint point = data.get(i);
            
            // Calculate salt: random component + cyclic enhancement
            double randomComponent = (random.nextDouble() * 2 - 1) * saltAmount * 0.5;
            double cyclicComponent = Math.sin(2 * Math.PI * i / estimatedPeriod) * saltAmount * 0.5;
            
            double saltedY = point.getY() + randomComponent + cyclicComponent;
            point.setY(saltedY);
        }
    }
    
    /**
     * Apply random salting to the data.
     * 
     * @param data List of data points to modify
     * @param saltAmount Amount of salt to apply
     */
    private void applySaltingRandom(List<DataPoint> data, double saltAmount) {
        // For general data, we apply controlled random variations
        
        for (DataPoint point : data) {
            // Generate random salt: -saltAmount to +saltAmount
            double salt = (random.nextDouble() * 2 - 1) * saltAmount;
            
            // Apply salt to the y-value
            double saltedY = point.getY() + salt;
            point.setY(saltedY);
        }
    }
    
    /**
     * Detect if the data has a significant trend.
     * 
     * @param data List of data points to analyze
     * @return true if a trend is detected, false otherwise
     */
    private boolean detectTrend(List<DataPoint> data) {
        if (data.size() < 3) {
            return false;
        }
        
        // Simple trend detection: check if the slope is consistently positive or negative
        int positiveSlopes = 0;
        int negativeSlopes = 0;
        
        for (int i = 1; i < data.size(); i++) {
            double slope = data.get(i).getY() - data.get(i-1).getY();
            if (slope > 0) positiveSlopes++;
            else if (slope < 0) negativeSlopes++;
        }
        
        int totalComparisons = data.size() - 1;
        double positiveRatio = (double) positiveSlopes / totalComparisons;
        double negativeRatio = (double) negativeSlopes / totalComparisons;
        
        // If 70% of slopes are in the same direction, consider it a trend
        return positiveRatio > 0.7 || negativeRatio > 0.7;
    }
    
    /**
     * Detect if the data has cyclical patterns.
     * 
     * @param data List of data points to analyze
     * @return true if cycles are detected, false otherwise
     */
    private boolean detectCycles(List<DataPoint> data) {
        if (data.size() < 10) {
            return false;
        }
        
        // Simple cycle detection: look for sign changes in the first derivative
        int signChanges = 0;
        boolean lastPositive = false;
        boolean firstPoint = true;
        
        for (int i = 1; i < data.size(); i++) {
            double slope = data.get(i).getY() - data.get(i-1).getY();
            boolean positive = slope > 0;
            
            if (firstPoint) {
                lastPositive = positive;
                firstPoint = false;
            } else if (positive != lastPositive) {
                signChanges++;
                lastPositive = positive;
            }
        }
        
        // If we have at least 3 sign changes, consider it cyclical
        return signChanges >= 3;
    }
    
    /**
     * Estimate the period of cyclical data.
     * 
     * @param data List of data points
     * @return Estimated period length
     */
    private double estimatePeriod(List<DataPoint> data) {
        if (data.size() < 4) {
            return data.size();  // Default to data size if too few points
        }
        
        // Look for sign changes in slope to estimate period
        List<Integer> changePoints = new ArrayList<>();
        boolean lastPositive = false;
        boolean firstPoint = true;
        
        for (int i = 1; i < data.size(); i++) {
            double slope = data.get(i).getY() - data.get(i-1).getY();
            boolean positive = slope > 0;
            
            if (firstPoint) {
                lastPositive = positive;
                firstPoint = false;
            } else if (positive != lastPositive) {
                changePoints.add(i);
                lastPositive = positive;
            }
        }
        
        // Calculate average distance between change points
        if (changePoints.size() < 2) {
            return data.size() / 2.0;  // Default to half data size if not enough change points
        }
        
        double totalDistance = 0;
        for (int i = 1; i < changePoints.size(); i++) {
            totalDistance += changePoints.get(i) - changePoints.get(i-1);
        }
        
        return (totalDistance / (changePoints.size() - 1)) * 2;  // Multiply by 2 for full cycle
    }
}