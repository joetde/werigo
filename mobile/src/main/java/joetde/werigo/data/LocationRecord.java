package joetde.werigo.data;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.SIMILARITY_IN_SPACE_DEGREE;

@Data
@Slf4j
@AllArgsConstructor(suppressConstructorProperties = true)
public class LocationRecord {

    private long id;
    private double latitude;
    private double longitude;
    private double accuracy;
    private long timestamp;
    private int merges = 0;

    public LocationRecord(double latitude, double longitude, double accuracy, long timestamp) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
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
        timestamp = lr.getTimestamp();
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
        }
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

    public LatLngBounds getBounds() {
        LatLng one = new LatLng(latitude + 2 * SIMILARITY_IN_SPACE_DEGREE, longitude + 2 * SIMILARITY_IN_SPACE_DEGREE);
        LatLng two = new LatLng(latitude - 2 * SIMILARITY_IN_SPACE_DEGREE, longitude - 2 * SIMILARITY_IN_SPACE_DEGREE);
        return LatLngBounds.builder().include(one).include(two).build();
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
}
