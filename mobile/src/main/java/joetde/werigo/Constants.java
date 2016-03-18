package joetde.werigo;

public class Constants {
    public static final long MIN_TIME_LOCATION_UPDATES = 1000; // ms
    public static final float MIN_DISTANCE_LOCATION_UPDATES = 2; // meters
    public static final double MIN_ACCURACY_TO_RECORD = 30; // meters

    public static final int SIMILIRARITY_IN_TIME = 12 * 60* 60 * 1000; // 12 hours in ms
    public static final int SIMILARITY_IN_SPACE = 20; // meters
    public static final double SIMILARITY_IN_SPACE_DEGREE = SIMILARITY_IN_SPACE / 111.19492664455873;

    public static final double MIN_DISLAY_RADIUS = 20; // meters

    public static final int[][] LOCATION_HEAT_RGB = {
            {128,255,128},  // light green
            {204,255,128},
            {255,255,128},  // light yellow
            {255,204,128},
            {255,128,128}}; // light red
}
