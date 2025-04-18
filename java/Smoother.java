import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Smoother - Responsible for applying smoothing algorithms to data.
 * Implements the Solter smoothing algorithm which uses adaptive weighted averaging.
 * Enhanced with multi-pass smoothing capabilities for greater smoothness.
 */
public class Smoother {
    
    // Constants for the Solter smoothing algorithm
    private static final int DEFAULT_WINDOW_SIZE = 5;
    private static final double ALPHA = 0.3;  // Controls distance weight decay
    private static final double BETA = 0.7;   // Controls overall smoothing intensity
    private static final int DEFAULT_ITERATIONS = 3; // Number of smoothing passes
    
    /**
     * Apply the Solter smoothing algorithm to a list of data points.
     * 
     * @param data List of data points to smooth
     * @return List of smoothed data points
     */
    public List<DataPoint> applySmoothing(List<DataPoint> data) {
        return applySmoothing(data, DEFAULT_WINDOW_SIZE, ALPHA, BETA, DEFAULT_ITERATIONS);
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
        return applySmoothing(data, windowSize, alpha, beta, 1);
    }
    
    /**
     * Apply the Solter smoothing algorithm with custom parameters and multiple iterations.
     * Multiple iterations create a smoother result by repeatedly applying the algorithm.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the sliding window (odd number recommended)
     * @param alpha Controls how quickly the weight decreases with distance
     * @param beta Controls the overall smoothing intensity
     * @param iterations Number of times to apply the smoothing algorithm
     * @return List of smoothed data points
     */
    public List<DataPoint> applySmoothing(List<DataPoint> data, int windowSize, 
                                         double alpha, double beta, int iterations) {
        List<DataPoint> smoothedData = new ArrayList<>(data);
        
        for (int iter = 0; iter < iterations; iter++) {
            smoothedData = applySinglePassSmoothing(smoothedData, windowSize, alpha, beta);
        }
        
        return smoothedData;
    }
    
    /**
     * Apply a single pass of the Solter smoothing algorithm.
     * This is the core smoothing function used by the multi-pass version.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the sliding window
     * @param alpha Controls weight decay with distance
     * @param beta Controls smoothing intensity
     * @return List of smoothed data points
     */
    private List<DataPoint> applySinglePassSmoothing(List<DataPoint> data, int windowSize, 
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
                    
                    // Enhanced weight calculation with higher distance penalty for smoother results
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
     * Advanced smoothing algorithm that combines multiple approaches for maximum smoothness.
     * Uses a combination of Solter smoothing and bilateral filtering concepts.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the sliding window
     * @param spatialSigma Controls spatial weight decay
     * @param valueSigma Controls value similarity weight
     * @param iterations Number of iterations to apply
     * @return List of smoothed data points
     */
    public List<DataPoint> applyAdvancedSmoothing(List<DataPoint> data, int windowSize, 
                                               double spatialSigma, double valueSigma,
                                               int iterations) {
        List<DataPoint> result = new ArrayList<>(data);
        
        for (int iter = 0; iter < iterations; iter++) {
            List<DataPoint> currentIteration = new ArrayList<>();
            int n = result.size();
            
            if (n <= 1) {
                return new ArrayList<>(result);
            }
            
            // Ensure window size is valid
            windowSize = Math.min(windowSize, n);
            int halfWindow = windowSize / 2;
            
            for (int i = 0; i < n; i++) {
                DataPoint center = result.get(i);
                double x = center.getX();
                double centerY = center.getY();
                
                // For points at the edges, use smaller windows
                int start = Math.max(0, i - halfWindow);
                int end = Math.min(n - 1, i + halfWindow);
                
                double sum = 0;
                double totalWeight = 0;
                
                for (int j = start; j <= end; j++) {
                    DataPoint neighbor = result.get(j);
                    double y = neighbor.getY();
                    
                    // Calculate spatial weight (distance in index space)
                    double spatialDist = Math.abs(i - j);
                    double spatialWeight = Math.exp(-(spatialDist * spatialDist) / (2 * spatialSigma * spatialSigma));
                    
                    // Calculate value weight (difference in y values)
                    double valueDist = Math.abs(centerY - y);
                    double valueWeight = Math.exp(-(valueDist * valueDist) / (2 * valueSigma * valueSigma));
                    
                    // Combine weights
                    double weight = spatialWeight * valueWeight;
                    
                    sum += y * weight;
                    totalWeight += weight;
                }
                
                double smoothedY = (totalWeight > 0) ? sum / totalWeight : centerY;
                currentIteration.add(new DataPoint(x, smoothedY));
            }
            
            result = currentIteration;
        }
        
        return result;
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
        return applyMovingAverage(data, windowSize, 1);
    }
    
    /**
     * Apply a Moving Average with multiple iterations for smoother results.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the sliding window (odd number recommended)
     * @param iterations Number of times to apply the smoothing
     * @return List of smoothed data points
     */
    public List<DataPoint> applyMovingAverage(List<DataPoint> data, int windowSize, int iterations) {
        List<DataPoint> result = new ArrayList<>(data);
        
        for (int iter = 0; iter < iterations; iter++) {
            List<DataPoint> currentPass = new ArrayList<>();
            int n = result.size();
            
            if (n <= 1) {
                return new ArrayList<>(result);
            }
            
            // Ensure window size is valid
            windowSize = Math.min(windowSize, n);
            int halfWindow = windowSize / 2;
            
            // Apply moving average to each point
            for (int i = 0; i < n; i++) {
                DataPoint originalPoint = result.get(i);
                double x = originalPoint.getX();
                
                // For points at the edges, use smaller windows
                int start = Math.max(0, i - halfWindow);
                int end = Math.min(n - 1, i + halfWindow);
                
                // Calculate simple average
                double sum = 0;
                for (int j = start; j <= end; j++) {
                    sum += result.get(j).getY();
                }
                double smoothedY = sum / (end - start + 1);
                
                // Create a new point with the smoothed value
                currentPass.add(new DataPoint(x, smoothedY));
            }
            
            result = currentPass;
        }
        
        return result;
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
        return applyExponentialSmoothing(data, alpha, 1);
    }
    
    /**
     * Apply exponential smoothing with multiple iterations.
     * 
     * @param data List of data points to smooth
     * @param alpha Smoothing factor (0 < alpha < 1)
     * @param iterations Number of iterations to apply
     * @return List of smoothed data points
     */
    public List<DataPoint> applyExponentialSmoothing(List<DataPoint> data, double alpha, int iterations) {
        List<DataPoint> result = new ArrayList<>(data);
        
        for (int iter = 0; iter < iterations; iter++) {
            List<DataPoint> currentPass = new ArrayList<>();
            int n = result.size();
            
            if (n <= 1) {
                return new ArrayList<>(result);
            }
            
            // Ensure alpha is within valid range
            alpha = Math.max(0.01, Math.min(0.99, alpha));
            
            // Initialize with first point
            DataPoint firstPoint = result.get(0);
            currentPass.add(new DataPoint(firstPoint.getX(), firstPoint.getY()));
            
            // Apply exponential smoothing to remaining points
            for (int i = 1; i < n; i++) {
                DataPoint currentPoint = result.get(i);
                double previousSmoothed = currentPass.get(i-1).getY();
                double currentValue = currentPoint.getY();
                
                // Exponential smoothing formula: St = alpha * Yt + (1-alpha) * St-1
                double smoothedValue = alpha * currentValue + (1 - alpha) * previousSmoothed;
                
                currentPass.add(new DataPoint(currentPoint.getX(), smoothedValue));
            }
            
            result = currentPass;
        }
        
        return result;
    }
    
    /**
     * Apply Octave/MATLAB-inspired Savitzky-Golay filter smoothing.
     * This method provides polynomial smoothing similar to what's available in Octave.
     * 
     * @param data List of data points to smooth
     * @param windowSize Size of the window (must be odd)
     * @param polynomialOrder Order of the polynomial (typically 2-4)
     * @return List of smoothed data points
     */
    public List<DataPoint> applySavitzkyGolaySmoothing(List<DataPoint> data, int windowSize, int polynomialOrder) {
        List<DataPoint> smoothedData = new ArrayList<>();
        int n = data.size();
        
        if (n <= 1) {
            return new ArrayList<>(data);
        }
        
        // Ensure window size is odd
        if (windowSize % 2 == 0) {
            windowSize++;
        }
        
        // Ensure window size is valid
        windowSize = Math.min(windowSize, n);
        windowSize = Math.max(polynomialOrder + 1, windowSize);
        int halfWindow = windowSize / 2;
        
        // Extract y values
        double[] yValues = new double[n];
        for (int i = 0; i < n; i++) {
            yValues[i] = data.get(i).getY();
        }
        
        // Apply smoothing
        double[] smoothedY = savitzkyGolayFilter(yValues, windowSize, polynomialOrder);
        
        // Create smoothed data points
        for (int i = 0; i < n; i++) {
            smoothedData.add(new DataPoint(data.get(i).getX(), smoothedY[i]));
        }
        
        return smoothedData;
    }
    
    /**
     * Octave-inspired Gaussian smoothing filter.
     * Similar to Octave's 'smoothdata' function with 'gaussian' method.
     * 
     * @param data List of data points to smooth
     * @param windowSize Window size (must be odd)
     * @param sigma Standard deviation of Gaussian kernel
     * @return List of smoothed data points
     */
    public List<DataPoint> applyGaussianSmoothing(List<DataPoint> data, int windowSize, double sigma) {
        List<DataPoint> smoothedData = new ArrayList<>();
        int n = data.size();
        
        if (n <= 1) {
            return new ArrayList<>(data);
        }
        
        // Ensure window size is odd
        if (windowSize % 2 == 0) {
            windowSize++;
        }
        
        // Create Gaussian kernel
        double[] kernel = createGaussianKernel(windowSize, sigma);
        
        // Extract y values
        double[] yValues = new double[n];
        for (int i = 0; i < n; i++) {
            yValues[i] = data.get(i).getY();
        }
        
        // Apply convolution
        double[] smoothedY = applyConvolution(yValues, kernel);
        
        // Create smoothed data points
        for (int i = 0; i < n; i++) {
            smoothedData.add(new DataPoint(data.get(i).getX(), smoothedY[i]));
        }
        
        return smoothedData;
    }
    
    /**
     * LOESS smoothing (Locally Estimated Scatterplot Smoothing).
     * This is similar to Octave's 'smoothdata' function with 'loess' method.
     * 
     * @param data List of data points to smooth
     * @param span Fraction of data used for local regression (0.1 to 1.0)
     * @param degree Polynomial degree (1 or 2)
     * @return List of smoothed data points
     */
    public List<DataPoint> applyLoessSmoothing(List<DataPoint> data, double span, int degree) {
        int n = data.size();
        List<DataPoint> smoothedData = new ArrayList<>();
        
        if (n <= 1) {
            return new ArrayList<>(data);
        }
        
        // Validate parameters
        span = Math.max(0.1, Math.min(1.0, span));
        degree = Math.max(1, Math.min(2, degree));
        
        // Calculate window size
        int windowSize = (int) Math.ceil(span * n);
        
        // Process each point
        for (int i = 0; i < n; i++) {
            DataPoint point = data.get(i);
            double x = point.getX();
            
            // Calculate weights and fit local polynomial
            double smoothedY = calculateLoessFit(data, i, windowSize, degree);
            smoothedData.add(new DataPoint(x, smoothedY));
        }
        
        return smoothedData;
    }
    
    /**
     * Multiple-pass Octave smoothing for extremely smooth results.
     * Combines multiple Octave-inspired algorithms.
     * 
     * @param data List of data points to smooth
     * @return List of extremely smoothed data points
     */
    public List<DataPoint> applyOctaveSupersmoothingPipeline(List<DataPoint> data) {
        // Create a multi-stage smoothing pipeline inspired by Octave techniques
        
        // Stage 1: Apply Savitzky-Golay filter to remove high-frequency noise
        List<DataPoint> stage1 = applySavitzkyGolaySmoothing(data, 7, 3);
        
        // Stage 2: Apply Gaussian smoothing to further smooth the data
        List<DataPoint> stage2 = applyGaussianSmoothing(stage1, 11, 2.0);
        
        // Stage 3: Apply LOESS smoothing for final refinement
        List<DataPoint> stage3 = applyLoessSmoothing(stage2, 0.3, 2);
        
        // Stage 4: One final pass of Savitzky-Golay to ensure smoothness
        return applySavitzkyGolaySmoothing(stage3, 9, 3);
    }
    
    // Helper method to create a Gaussian kernel
    private double[] createGaussianKernel(int size, double sigma) {
        double[] kernel = new double[size];
        int center = size / 2;
        double sum = 0.0;
        
        for (int i = 0; i < size; i++) {
            double x = i - center;
            kernel[i] = Math.exp(-(x * x) / (2 * sigma * sigma));
            sum += kernel[i];
        }
        
        // Normalize the kernel
        for (int i = 0; i < size; i++) {
            kernel[i] /= sum;
        }
        
        return kernel;
    }
    
    // Helper method to apply convolution with a kernel
    private double[] applyConvolution(double[] data, double[] kernel) {
        int n = data.length;
        int kSize = kernel.length;
        int halfK = kSize / 2;
        double[] result = new double[n];
        
        for (int i = 0; i < n; i++) {
            double sum = 0;
            double weightSum = 0;
            
            for (int j = -halfK; j <= halfK; j++) {
                int idx = i + j;
                if (idx >= 0 && idx < n) {
                    double weight = kernel[j + halfK];
                    sum += data[idx] * weight;
                    weightSum += weight;
                }
            }
            
            result[i] = sum / weightSum;
        }
        
        return result;
    }
    
    // Helper method for Savitzky-Golay filter
    private double[] savitzkyGolayFilter(double[] y, int windowSize, int polynomialOrder) {
        int n = y.length;
        double[] result = new double[n];
        int halfWindow = windowSize / 2;
        
        // Apply simple moving average for border points
        for (int i = 0; i < halfWindow; i++) {
            double sum = 0;
            int count = 0;
            for (int j = 0; j <= i + halfWindow; j++) {
                sum += y[j];
                count++;
            }
            result[i] = sum / count;
        }
        
        for (int i = n - halfWindow; i < n; i++) {
            double sum = 0;
            int count = 0;
            for (int j = i - halfWindow; j < n; j++) {
                sum += y[j];
                count++;
            }
            result[i] = sum / count;
        }
        
        // Main S-G filter calculation for central points
        for (int i = halfWindow; i < n - halfWindow; i++) {
            // Extract local window
            double[] xLocal = new double[windowSize];
            double[] yLocal = new double[windowSize];
            
            for (int j = 0; j < windowSize; j++) {
                int idx = i - halfWindow + j;
                xLocal[j] = idx - i; // centered x values
                yLocal[j] = y[idx];
            }
            
            // Fit polynomial using least squares
            double[] coeffs = polynomialFit(xLocal, yLocal, polynomialOrder);
            
            // Evaluate at center point (x = 0)
            result[i] = evaluatePolynomial(coeffs, 0);
        }
        
        return result;
    }
    
    // Helper method to fit polynomial using least squares
    private double[] polynomialFit(double[] x, double[] y, int order) {
        int n = x.length;
        int terms = order + 1;
        
        // Simple implementation for order 2 (most common for S-G)
        if (order == 2 && n >= 5) {
            // For quadratic fit, we can use simplified formulas
            double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0, sumXXX = 0, sumXXXX = 0, sumXXY = 0;
            
            for (int i = 0; i < n; i++) {
                double xi = x[i];
                double yi = y[i];
                double xiSq = xi * xi;
                
                sumX += xi;
                sumY += yi;
                sumXX += xiSq;
                sumXY += xi * yi;
                sumXXX += xiSq * xi;
                sumXXXX += xiSq * xiSq;
                sumXXY += xiSq * yi;
            }
            
            // Solve normal equations for quadratic
            double denominator = n * (sumXX * sumXXXX - sumXXX * sumXXX) - 
                                 sumX * (sumX * sumXXXX - sumXXX * sumXX) +
                                 sumXX * (sumX * sumXXX - sumXX * sumXX);
            
            if (Math.abs(denominator) < 1e-10) {
                // Fallback to linear fit if system is near-singular
                return new double[] {sumY / n, 0, 0};
            }
            
            double a = (sumXXY * (n * sumXXXX - sumXX * sumXX) - 
                        sumY * (sumXX * sumXXXX - sumXXX * sumXXX) + 
                        sumXY * (sumXX * sumXXX - sumX * sumXXXX)) / denominator;
                        
            double b = (n * (sumXY * sumXXXX - sumXXY * sumXXX) - 
                        sumX * (sumY * sumXXXX - sumXXY * sumXX) + 
                        sumXX * (sumY * sumXXX - sumXY * sumXX)) / denominator;
                        
            double c = (n * (sumXX * sumXXY - sumXY * sumXXX) - 
                        sumX * (sumX * sumXXY - sumXY * sumXX) + 
                        sumY * (sumX * sumXXX - sumXX * sumXX)) / denominator;
            
            return new double[] {c, b, a};
        }
        
        // Simple implementation for order 1 (linear fit)
        if (order == 1 || order == 0) {
            double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0;
            
            for (int i = 0; i < n; i++) {
                sumX += x[i];
                sumY += y[i];
                sumXX += x[i] * x[i];
                sumXY += x[i] * y[i];
            }
            
            if (order == 0 || Math.abs(n * sumXX - sumX * sumX) < 1e-10) {
                // Constant fit (order 0) or degenerate case
                return new double[] {sumY / n};
            }
            
            // Linear fit coefficients
            double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            
            return new double[] {intercept, slope};
        }
        
        // Fallback to moving average for higher orders
        double avg = 0;
        for (int i = 0; i < n; i++) {
            avg += y[i];
        }
        avg /= n;
        
        return new double[] {avg};
    }
    
    // Helper method to evaluate polynomial
    private double evaluatePolynomial(double[] coeffs, double x) {
        double result = 0;
        for (int i = 0; i < coeffs.length; i++) {
            result += coeffs[i] * Math.pow(x, i);
        }
        return result;
    }
    
    // Helper method for LOESS calculation
    private double calculateLoessFit(List<DataPoint> data, int centerIdx, int windowSize, int degree) {
        int n = data.size();
        DataPoint centerPoint = data.get(centerIdx);
        double centerX = centerPoint.getX();
        
        // Collect points in window
        List<DataPoint> windowPoints = new ArrayList<>();
        List<Double> distances = new ArrayList<>();
        double maxDistance = 0;
        
        for (int i = 0; i < n; i++) {
            DataPoint point = data.get(i);
            double distance = Math.abs(point.getX() - centerX);
            
            windowPoints.add(point);
            distances.add(distance);
            maxDistance = Math.max(maxDistance, distance);
        }
        
        // Sort points by distance
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            indices.add(i);
        }
        
        // Sort indices by distance
        indices.sort((a, b) -> Double.compare(distances.get(a), distances.get(b)));
        
        // Keep only the closest windowSize points
        int actualSize = Math.min(windowSize, n);
        List<DataPoint> closestPoints = new ArrayList<>();
        List<Double> weights = new ArrayList<>();
        
        for (int i = 0; i < actualSize; i++) {
            int idx = indices.get(i);
            closestPoints.add(windowPoints.get(idx));
            
            // Tri-cubic weight function
            double normalizedDist = distances.get(idx) / (maxDistance > 0 ? maxDistance : 1);
            double weight = Math.pow(1 - Math.pow(normalizedDist, 3), 3);
            weights.add(weight);
        }
        
        // Extract x and y arrays
        double[] xValues = new double[actualSize];
        double[] yValues = new double[actualSize];
        double[] weightValues = new double[actualSize];
        
        for (int i = 0; i < actualSize; i++) {
            DataPoint point = closestPoints.get(i);
            xValues[i] = point.getX() - centerX; // Center x values
            yValues[i] = point.getY();
            weightValues[i] = weights.get(i);
        }
        
        // Weighted least squares polynomial fit
        double[] coeffs = polynomialFitWeighted(xValues, yValues, weightValues, degree);
        
        // Evaluate polynomial at x = 0 (the center point)
        return evaluatePolynomial(coeffs, 0);
    }
    
    // Helper method for weighted polynomial fit
    private double[] polynomialFitWeighted(double[] x, double[] y, double[] weights, int degree) {
        int n = x.length;
        
        // For simple cases or when degree is too high relative to points, fall back to weighted average
        if (n <= degree || degree > 2) {
            double sumY = 0, sumWeights = 0;
            for (int i = 0; i < n; i++) {
                sumY += y[i] * weights[i];
                sumWeights += weights[i];
            }
            return new double[] {sumWeights > 0 ? sumY / sumWeights : 0};
        }
        
        // Linear weighted fit (degree 1)
        if (degree == 1) {
            double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0, sumW = 0;
            
            for (int i = 0; i < n; i++) {
                double w = weights[i];
                sumX += x[i] * w;
                sumY += y[i] * w;
                sumXX += x[i] * x[i] * w;
                sumXY += x[i] * y[i] * w;
                sumW += w;
            }
            
            if (Math.abs(sumW * sumXX - sumX * sumX) < 1e-10) {
                return new double[] {sumW > 0 ? sumY / sumW : 0};
            }
            
            double slope = (sumW * sumXY - sumX * sumY) / (sumW * sumXX - sumX * sumX);
            double intercept = (sumY - slope * sumX) / sumW;
            
            return new double[] {intercept, slope};
        }
        
        // Quadratic weighted fit (degree 2)
        if (degree == 2) {
            double sumW = 0;
            double sumX = 0, sumY = 0, sumXX = 0, sumXY = 0, sumXXX = 0, sumXXXX = 0, sumXXY = 0;
            
            for (int i = 0; i < n; i++) {
                double w = weights[i];
                double xi = x[i];
                double yi = y[i];
                double xiSq = xi * xi;
                
                sumW += w;
                sumX += xi * w;
                sumY += yi * w;
                sumXX += xiSq * w;
                sumXY += xi * yi * w;
                sumXXX += xiSq * xi * w;
                sumXXXX += xiSq * xiSq * w;
                sumXXY += xiSq * yi * w;
            }
            
            // Simplified solution for weighted quadratic regression
            double det = sumW * (sumXX * sumXXXX - sumXXX * sumXXX) - 
                         sumX * (sumX * sumXXXX - sumXXX * sumXX) +
                         sumXX * (sumX * sumXXX - sumXX * sumXX);
            
            if (Math.abs(det) < 1e-10) {
                // System is near-singular, fall back to linear fit
                return polynomialFitWeighted(x, y, weights, 1);
            }
            
            double a = (sumXXY * (sumW * sumXXXX - sumXX * sumXX) - 
                        sumY * (sumXX * sumXXXX - sumXXX * sumXXX) + 
                        sumXY * (sumXX * sumXXX - sumX * sumXXXX)) / det;
                        
            double b = (sumW * (sumXY * sumXXXX - sumXXY * sumXXX) - 
                        sumX * (sumY * sumXXXX - sumXXY * sumXX) + 
                        sumXX * (sumY * sumXXX - sumXY * sumXX)) / det;
                        
            double c = (sumW * (sumXX * sumXXY - sumXY * sumXXX) - 
                        sumX * (sumX * sumXXY - sumXY * sumXX) + 
                        sumY * (sumX * sumXXX - sumXX * sumXX)) / det;
            
            return new double[] {c, b, a};
        }
        
        // Should not reach here
        return new double[] {0};
    }
}