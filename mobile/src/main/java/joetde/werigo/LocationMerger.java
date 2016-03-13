package joetde.werigo;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.Circle;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.SIMILARITY_IN_SPACE;
import static joetde.werigo.Constants.SIMILIRARITY_IN_TIME;

@Slf4j
public class LocationMerger {
    @Setter private Context context;
    @Getter private List<LocationRecord> locations = new ArrayList<>();
    private static final String FILENAME = "LocationMergerData";
    private static Gson SERILIZER = new Gson();

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

        // TODO save on activity actions?
        save();

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

    public void save() {
        try {
            FileOutputStream fos = context.openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(SERILIZER.toJson(locations).getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            // TODO
            log.error("File not found", e);
        } catch (IOException e) {
            // TODO
            log.error("Failure to write to file", e);
        }

    }

    public void load() {
        try {
            FileInputStream fis = context.openFileInput(FILENAME);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();
            String json = new String(buffer);
            if (json != null && !json.isEmpty()) {
                locations = SERILIZER.fromJson(json, new TypeToken<ArrayList<LocationRecord>>() {}.getType());
                log.error("Location size: " + locations.size());
            }
        } catch (FileNotFoundException e) {
            // TODO create the file
            log.error("File not found", e);
        } catch (IOException e) {
            // TODO
            log.error("Failure to write to file", e);
        }
    }
}
