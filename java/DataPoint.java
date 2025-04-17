/**
 * DataPoint class to represent a single data point.
 * This class is shared across all components.
 */
public class DataPoint {
    private final double x;
    private double y;
    
    /**
     * Constructor for DataPoint.
     * 
     * @param x The x-value
     * @param y The y-value
     */
    public DataPoint(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Get the x-value.
     * 
     * @return The x-value
     */
    public double getX() {
        return x;
    }
    
    /**
     * Get the y-value.
     * 
     * @return The y-value
     */
    public double getY() {
        return y;
    }
    
    /**
     * Set the y-value.
     * 
     * @param y The new y-value
     */
    public void setY(double y) {
        this.y = y;
    }
    
    /**
     * Create a copy of this data point.
     * 
     * @return A new DataPoint with the same values
     */
    public DataPoint copy() {
        return new DataPoint(this.x, this.y);
    }
}