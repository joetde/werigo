package joetde.werigo.data;

import android.content.Context;
import android.location.Location;
import android.os.Vibrator;

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
import static joetde.werigo.Constants.MIN_DISLAY_RADIUS;

@Slf4j
public class LocationMerger {
    private MapsActivity context;
    private LocationRecordDataSource dataSource;
    private Map<Long, LocationRecord> locations = new HashMap<>(); // id -> location
    private AggregationManager aggregationManager = new AggregationManager();

    public boolean addLocationToMerge(Location location) {
        LocationRecord newRecord = new LocationRecord(location.getLatitude(),
                location.getLongitude(), location.getAccuracy(), System.currentTimeMillis());
        return addRecordToMerge(newRecord);
    }

    private boolean addRecordToMerge(LocationRecord lr) {
        LocationRecord closestRecordInRange = getClosestInRange(lr);

        // point already exist
        if (closestRecordInRange != null) {
            if (closestRecordInRange.delay(lr) < SIMILIRARITY_IN_TIME) {
                closestRecordInRange.dedupe(lr);
            } else {
                closestRecordInRange.merge(lr);
            }
            aggregationManager.updateDisplay(lr);
            dataSource.updateLocation(closestRecordInRange);
            return false;
        }

        // new location
        dataSource.writeNewLocation(lr);
        locations.put(lr.getId(), lr);
        aggregationManager.add(lr, context);
        aggregationManager.updateDisplay(lr);
        return true;
    }

    private LocationRecord getClosestInRange(LocationRecord lr) {
        List<LocationRecord> locationsInRange = dataSource.loadLocations(lr.getBounds());
        return getClosestRecordsInRange(locationsInRange, lr);
    }

    public void longClick(LatLng latLng) {
        LocationRecord newRecord = new LocationRecord(latLng.latitude, latLng.longitude, MIN_DISLAY_RADIUS, System.currentTimeMillis());
        LocationRecord closest = getClosestInRange(newRecord);
        log.info("Long click: found {}", closest);

        // Vibrate!
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(30);

        if (closest == null) {
            // TODO ask to add
            addRecordToMerge(newRecord);
        } else {
            // TODO ask to suppress and suppress (give the number of points that will be deleted)
            dataSource.delete(closest);
            locations.remove(closest.getId());
            aggregationManager.kill(closest);
            aggregationManager.updateDisplay(closest);
        }
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
            aggregationManager.startBuffering();
            aggregationManager.updateZoom(zoom, locations.values(), context);

            // remove points outside screen
            for (Iterator<Map.Entry<Long, LocationRecord>> it = locations.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<Long, LocationRecord> entry = it.next();
                if (!bounds.contains(new LatLng(entry.getValue().getLatitude(), entry.getValue().getLongitude()))) {
                    aggregationManager.remove(entry.getValue());
                    it.remove();
                }
            }

            // add new points
            List<LocationRecord> newLocations = dataSource.loadLocations(bounds);
            for (LocationRecord lr : newLocations) {
                if (!locations.containsKey(lr.getId())) {
                    aggregationManager.add(lr, context);
                    locations.put(lr.getId(), lr);
                }
            }
            aggregationManager.refreshBuffer();
        }
    }
}
