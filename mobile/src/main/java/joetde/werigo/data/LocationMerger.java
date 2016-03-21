package joetde.werigo.data;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import joetde.werigo.MapsActivity;
import joetde.werigo.datasource.LocationRecordDataSource;
import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.SIMILARITY_IN_SPACE;
import static joetde.werigo.Constants.SIMILIRARITY_IN_TIME;

@Slf4j
public class LocationMerger {
    private MapsActivity context;
    private LocationRecordDataSource dataSource;
    private Map<Long, LocationRecord> locations = new HashMap<>(); // id -> location
    private AggregationManager aggregationManager = new AggregationManager();

    public boolean addLocationToMerge(Location location) {

        LocationRecord newRecord = new LocationRecord(location.getLatitude(),
                location.getLongitude(), location.getAccuracy(), System.currentTimeMillis());

        List<LocationRecord> locationsInRange = dataSource.loadLocations(newRecord.getBounds());
        LocationRecord closestRecordInRange = getClosestRecordsInRange(locationsInRange, newRecord);
        if (closestRecordInRange != null) {
            if (closestRecordInRange.delay(newRecord) < SIMILIRARITY_IN_TIME) {
                closestRecordInRange.dedupe(newRecord);
                dataSource.updateLocation(closestRecordInRange);
                return false;
            } else {
                closestRecordInRange.merge(newRecord);
                dataSource.updateLocation(closestRecordInRange);
                return false;
            }
        }

        dataSource.writeNewLocation(newRecord);
        locations.put(newRecord.getId(), newRecord);
        aggregationManager.add(newRecord, context);
        return true;
    }

    /**
     * Get the records in range of the given point.
     * For now brute force. Possible optimization 2d-tree.
     * @param lr
     * @return list of points in range
     */
    private static LocationRecord getClosestRecordsInRange(List<LocationRecord> locations, LocationRecord lr) {
        LocationRecord closestRecordInRange = null;
        double minDistance = SIMILARITY_IN_SPACE;
        for (LocationRecord record : locations) {
            double currentDistance = record.distance(lr);
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                closestRecordInRange = record;
            }
        }
        return closestRecordInRange;
    }

    public void setContextAndLoadDataSource(MapsActivity context) {
        this.context = context;
        this.dataSource = new LocationRecordDataSource(context);
    }

    public void refreshLocations(LatLngBounds bounds, float zoom) {
        if (context != null) {
            aggregationManager.updateZoom(zoom);

            // remove points outside screen
            for (Iterator<Map.Entry<Long, LocationRecord>> it = locations.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, LocationRecord> entry = it.next();
                if (!bounds.contains(new LatLng(entry.getValue().getLatitude(), entry.getValue().getLongitude()))) {
                    //entry.getValue().getCircle().remove();
                    aggregationManager.remove(entry.getValue());
                    it.remove();
                }
            }

            // add new points
            List<LocationRecord> newLocations = dataSource.loadLocations(bounds);
            for (LocationRecord lr : newLocations) {
                if (!locations.containsKey(lr.getId())) {
                    //lr.setCircle(context.createAndSetCircle());
                    aggregationManager.add(lr, context);
                    locations.put(lr.getId(), lr);
                }
            }
        }
    }
}