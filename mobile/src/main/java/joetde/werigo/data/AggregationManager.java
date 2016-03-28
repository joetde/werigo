package joetde.werigo.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import joetde.werigo.display.CircleCreator;

import static joetde.werigo.Constants.METERS_IN_A_DEGREE;

public class AggregationManager {
    private static final int MINIMAL_AGGREGATION_ZOOM = 16;

    private int zoom;
    private Map<String, AggregatedLocationRecord> aggregatedLocations = new HashMap<>(); // aggr lat:lng -> location
    private Set<AggregatedLocationRecord> buffer = new HashSet<>(); // buffer for display

    public void startBuffering() {
        buffer.clear();
    }

    public void refreshBuffer() {
        for (AggregatedLocationRecord alr : buffer) {
            alr.refreshDisplay();
        }
    }

    public void updateZoom(double zoom, Collection<LocationRecord> records, CircleCreator circleCreator) {
        int newZoom = Math.min(MINIMAL_AGGREGATION_ZOOM, (int) zoom);
        if (newZoom != this.zoom) {
            this.zoom = newZoom;
            clearAggregation();
            reloadAll(records, circleCreator);
        }
    }

    private void clearAggregation() {
        for (Map.Entry<String, AggregatedLocationRecord> entry : aggregatedLocations.entrySet()) {
            entry.getValue().getCircle().remove();
        }
        aggregatedLocations.clear();
    }

    public void remove(LocationRecord lr) {
        String key = getLocationAggregationKey(lr, zoom);
        if (!aggregatedLocations.containsKey(key)) { return; }

        AggregatedLocationRecord alr = aggregatedLocations.get(key);
        alr.removePoint(lr);
        if (alr.getNbAggregatedPoints() == 0) {
            alr.getCircle().remove();
            aggregatedLocations.remove(key);
        }
    }

    public void add(LocationRecord lr, CircleCreator circleCreator) {
        String key = getLocationAggregationKey(lr, zoom);
        if (!aggregatedLocations.containsKey(key)) {
            AggregatedLocationRecord newAlr = new AggregatedLocationRecord();
            newAlr.setRadius(getAggregationLevelInMeters(zoom));
            newAlr.setCircle(circleCreator.createAndSetCircle());
            aggregatedLocations.put(key, newAlr);
        }
        AggregatedLocationRecord alr = aggregatedLocations.get(key);
        alr.addPoint(lr);
        buffer.add(alr);
    }

    public void updateDisplay(LocationRecord lr) {
        String key = getLocationAggregationKey(lr, zoom);
        AggregatedLocationRecord alr = aggregatedLocations.get(key);
        alr.refreshDisplay();
    }

    private void reloadAll(Collection<LocationRecord> records, CircleCreator circleCreator) {
        for (LocationRecord lr : records) {
            add(lr, circleCreator);
        }
    }

    private static double getAggregationLevelInMeters(int zoom) {
        double zoomDiff = (MINIMAL_AGGREGATION_ZOOM - zoom);
        return 20 * Math.pow(1.85, zoomDiff);
    }

    private static double getAggregationLevelInDegrees(int zoom) {
        return getAggregationLevelInMeters(zoom) / METERS_IN_A_DEGREE;
    }

    private static String getLocationAggregationKey(LocationRecord lr, int zoom) {
        double aggregationLevel = getAggregationLevelInDegrees(zoom);
        long latAggr = (long) (lr.getLatitude() / aggregationLevel);
        long lngAggr = (long) (lr.getLongitude() / aggregationLevel);
        return String.format("%s:%s", latAggr, lngAggr);
    }
}
