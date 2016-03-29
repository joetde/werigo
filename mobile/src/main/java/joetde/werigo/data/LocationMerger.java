package joetde.werigo.data;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import static joetde.werigo.Constants.MIN_DISLAY_RADIUS;
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
            yesNoDialogCreateOrDelete("Create new point?", newRecord, false);
        } else {
            yesNoDialogCreateOrDelete("Delete selected point?", closest, true);
        }
    }

    private void yesNoDialogCreateOrDelete(String text, final LocationRecord lr, final boolean delete) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE:
                        if (!delete) {
                            addRecordToMerge(lr);
                        } else {
                            dataSource.delete(lr);
                            locations.remove(lr.getId());
                            aggregationManager.kill(lr);
                            aggregationManager.updateDisplay(lr);
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // nothing to do, user refused to perform the action
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(text).setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
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
