import java.util.ArrayList;
import java.util.List;

/**
 * Smoother - Responsible for applying smoothing algorithms to data.
 * Implements the Solter smoothing algorithm which uses adaptive weighted averaging.
 */
public class Smoother {
    
    // Constants for the Solter smoothing algorithm
    private static final int DEFAULT_WINDOW_SIZE = 5;
    private static final double ALPHA = 0.3;  // Controls distance weight decay
    private static final double BETA = 0.7;   // Controls overall smoothing intensity
    
    /**
     * Apply the Solter smoothing algorithm to a list of data points.
     * 
     * @param data List of data points to smooth
     * @return List of smoothed data points
     */
    public List<DataPoint> applySmoothing(List<DataPoint> data) {
        return applySmoothing(data, DEFAULT_WINDOW_SIZE, ALPHA, BETA);
    }
    
    /**
     * Apply the Solter smoothing algorithm with custom parameters.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the sliding window (odd number recommended)
     * @param alpha Controls how quickly the weight decreases with distance
     * @param beta Controls the overall smoothing intensity
     * @return List of smoothed data points
     */
    public List<DataPoint> applySmoothing(List<DataPoint> data, int windowSize, 
                                         double alpha, double beta) {
        List<DataPoint> smoothedData = new ArrayList<>();
        int n = data.size();
        
        if (n <= 1) {
            // If there's 0 or 1 point, just return a copy of the input
            for (DataPoint point : data) {
                smoothedData.add(point.copy());
            }
            return smoothedData;
        }
        
        // Ensure window size is valid
        windowSize = Math.min(windowSize, n);
        int halfWindow = windowSize / 2;
        
        // Apply Solter smoothing to each point
        for (int i = 0; i < n; i++) {
            DataPoint originalPoint = data.get(i);
            double x = originalPoint.getX();
            double originalY = originalPoint.getY();
            double smoothedY;
            
            // For points at the edges, use smaller windows
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(n - 1, i + halfWindow);
            
            // Apply the Solter smoothing algorithm
            if (start == end) {
                smoothedY = originalY;  // Only one point, no smoothing
            } else {
                double sum = 0;
                double totalWeight = 0;
                
                for (int j = start; j <= end; j++) {
                    double y = data.get(j).getY();
                    double distance = Math.abs(i - j);
                    double weight = Math.exp(-distance * alpha) * beta;
                    
                    // We increase the central point's weight
                    if (j == i) {
                        weight *= 1.5;
                    }
                    
                    sum += y * weight;
                    totalWeight += weight;
                }
                
                // Calculate weighted average
                smoothedY = sum / totalWeight;
            }
            
            // Create a new point with the smoothed value
            smoothedData.add(new DataPoint(x, smoothedY));
        }
        
        return smoothedData;
    }
    
    /**
     * Apply an alternative smoothing method - Moving Average.
     * This is a simpler method compared to Solter smoothing.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the sliding window (odd number recommended)
     * @return List of smoothed data points
     */
    public List<DataPoint> applyMovingAverage(List<DataPoint> data, int windowSize) {
        List<DataPoint> smoothedData = new ArrayList<>();
        int n = data.size();
        
        if (n <= 1) {
            // If there's 0 or 1 point, just return a copy of the input
            for (DataPoint point : data) {
                smoothedData.add(point.copy());
            }
            return smoothedData;
        }
        
        // Ensure window size is valid
        windowSize = Math.min(windowSize, n);
        int halfWindow = windowSize / 2;
        
        // Apply moving average to each point
        for (int i = 0; i < n; i++) {
            DataPoint originalPoint = data.get(i);
            double x = originalPoint.getX();
            double originalY = originalPoint.getY();
            double smoothedY;
            
            // For points at the edges, use smaller windows
            int start = Math.max(0, i - halfWindow);
            int end = Math.min(n - 1, i + halfWindow);
            
            // Calculate simple average
            double sum = 0;
            for (int j = start; j <= end; j++) {
                sum += data.get(j).getY();
            }
            smoothedY = sum / (end - start + 1);
            
            // Create a new point with the smoothed value
            smoothedData.add(new DataPoint(x, smoothedY));
        }
        
        return smoothedData;
    }
    
    /**
     * Apply exponential smoothing to data.
     * This method is good for data with trends.
     * 
     * @param data List of data points to smooth
     * @param alpha Smoothing factor (0 < alpha < 1)
     * @return List of smoothed data points
     */
    public List<DataPoint> applyExponentialSmoothing(List<DataPoint> data, double alpha) {
        List<DataPoint> smoothedData = new ArrayList<>();
        int n = data.size();
        
        if (n <= 1) {
            // If there's 0 or 1 point, just return a copy of the input
            for (DataPoint point : data) {
                smoothedData.add(point.copy());
            }
            return smoothedData;
        }
        
        // Ensure alpha is within valid range
        alpha = Math.max(0.01, Math.min(0.99, alpha));
        
        // Initialize with first point
        DataPoint firstPoint = data.get(0);
        smoothedData.add(new DataPoint(firstPoint.getX(), firstPoint.getY()));
        
        // Apply exponential smoothing to remaining points
        for (int i = 1; i < n; i++) {
            DataPoint currentPoint = data.get(i);
            double previousSmoothed = smoothedData.get(i-1).getY();
            double currentValue = currentPoint.getY();
            
            // Exponential smoothing formula: St = alpha * Yt + (1-alpha) * St-1
            double smoothedValue = alpha * currentValue + (1 - alpha) * previousSmoothed;
            
            smoothedData.add(new DataPoint(currentPoint.getX(), smoothedValue));
        }
        
        return smoothedData;
    }
}