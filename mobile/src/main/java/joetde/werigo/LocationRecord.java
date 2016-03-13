package joetde.werigo;

import android.graphics.Color;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.*;

@Data
@Slf4j
public class LocationRecord {

    private double latitude;
    private double longitude;
    private double accuracy;
    private long timestamp;
    private transient Circle circle = null;
    private int merges = 0;

    public LocationRecord(double latitude, double longitude, double accuracy, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
    }

    public void setCircle(Circle circle) {
        this.circle = circle;
        refreshDisplay();
    }

    /**
     * Consider the new point as part of the current point
     * @param lr
     */
    public void dedupe(LocationRecord lr) {
        log.debug("Deduping");
        improveAccuracy(lr);
    }

    /**
     * Consider the new point as a new passage on the current location
     * @param lr
     */
    public void merge(LocationRecord lr) {
        log.debug("Merging");
        improveAccuracy(lr);
        merges++;
    }

    /**
     * Compare the new point to see if accuracy can be improved
     * Dummy approach, to be improved
     * @param lr
     */
    private void improveAccuracy(LocationRecord lr) {
        if (lr.getAccuracy() < accuracy) {
            accuracy = lr.getAccuracy();
            latitude = (latitude + lr.getLatitude()) / 2;
            longitude = (longitude + lr.getLongitude()) / 2;
            refreshDisplay();
        }
    }

    private void refreshDisplay() {
        circle.setCenter(new LatLng(latitude, longitude));
        circle.setRadius(accuracy < MIN_DISLAY_RADIUS ? MIN_DISLAY_RADIUS : accuracy);
        circle.setFillColor(getColor());
    }

    /**
     * Distance between two location
     * @param lr
     * @return distance in meters
     */
    public double distance(LocationRecord lr) {
        return distanceFrom(this.getLatitude(), this.getLongitude(), lr.getLatitude(), lr.getLongitude());
    }

    /**
     * Time diff between two recordings
     * @param lr
     * @return
     */
    public long delay(LocationRecord lr) {
        return Math.abs(timestamp - lr.getTimestamp());
    }

    /**
     * Helper method to get the distance between longitude/latitude coordinates
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return distance in meters
     */
    public static double distanceFrom(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * Get alpha to display from the accuracy
     * @return alpha color
     */
    private int getAlpha() {
        return (int)(60 + 125 * (1-getAccuracy()/MIN_ACCURACY_TO_RECORD));
    }

    /**
     * Get the color to use for the given point
     * @return color of the point
     */
    private int getColor() {
        int colorSet = Math.min(merges, 4);
        return Color.argb(getAlpha(), LOCATION_HEAT_RGB[colorSet][0], LOCATION_HEAT_RGB[colorSet][1], LOCATION_HEAT_RGB[colorSet][2]);
    }
}
