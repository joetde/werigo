package joetde.werigo;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.Circle;

import java.util.ArrayList;
import java.util.List;

import joetde.werigo.data.LocationRecord;
import joetde.werigo.datasource.LocationRecordDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.SIMILARITY_IN_SPACE;
import static joetde.werigo.Constants.SIMILIRARITY_IN_TIME;

@Slf4j
public class LocationMerger {
    private Context context;
    private LocationRecordDataSource dataSource;
    @Getter private List<LocationRecord> locations = new ArrayList<>();

    public boolean addLocationToMerge(Location location) {
        LocationRecord newRecord = new LocationRecord(location.getLatitude(),
                location.getLongitude(), location.getAccuracy(), System.currentTimeMillis());

        LocationRecord closestRecordInRange = getClosestRecordsInRange(newRecord);
        if (closestRecordInRange != null) {
            if (closestRecordInRange.delay(newRecord) < SIMILIRARITY_IN_TIME) {
                closestRecordInRange.dedupe(newRecord);
                return false;
            } else {
                closestRecordInRange.merge(newRecord);
                return false;
            }
        }

        locations.add(newRecord);
        dataSource.writeNewLocation(newRecord);
        return true;
    }

    /**
     * Get the records in range of the given point.
     * For now brute force. Possible optimization 2d-tree.
     * @param lr
     * @return list of points in range
     */
    private LocationRecord getClosestRecordsInRange(LocationRecord lr) {
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

    public void addCircleToLastLocation(Circle circle) {
        locations.get(locations.size()-1).setCircle(circle);
    }

    public void setContextAndLoadDataSource(Context context) {
        this.context = context;
        this.dataSource = new LocationRecordDataSource(context);
    }

    public void load() {
        locations = dataSource.loadLocations();
    }
}
