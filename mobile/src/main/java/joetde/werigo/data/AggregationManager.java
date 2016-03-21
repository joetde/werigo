package joetde.werigo.data;

import java.util.HashMap;
import java.util.Map;

import joetde.werigo.display.CircleCreator;

import static joetde.werigo.Constants.METERS_IN_A_DEGREE;

public class AggregationManager {
    private static final double AGGREGATION_LEVEL = 300 / METERS_IN_A_DEGREE;

    private int zoom;
    private Map<String, AggregatedLocationRecord> aggregatedLocations = new HashMap<>(); // aggr lat:lng -> location

    public void updateZoom(double zoom) {
        if ((int)zoom != this.zoom) {
            // clean aggregated points
            // update aggregation constants
            this.zoom = (int) zoom;
        }
    }

    public void remove(LocationRecord lr) {
        String key = getLocationAggregationKey(lr);
        if (!aggregatedLocations.containsKey(key)) {
            return;
        }
        AggregatedLocationRecord alr = aggregatedLocations.get(key);
        alr.removePoint(lr);
        if (alr.getNbAggregatedPoints() == 0) {
            alr.getCircle().remove();
            aggregatedLocations.remove(key);
        }
    }

    public void add(LocationRecord lr, CircleCreator circleCreator) {
        String key = getLocationAggregationKey(lr);
        if (!aggregatedLocations.containsKey(key)) {
            AggregatedLocationRecord newAlr = new AggregatedLocationRecord();
            newAlr.setCircle(circleCreator.createAndSetCircle());
            aggregatedLocations.put(key, newAlr);
        }
        AggregatedLocationRecord alr = aggregatedLocations.get(key);
        alr.addPoint(lr);
    }

    private String getLocationAggregationKey(LocationRecord lr) {
        long latAggr = (long) (lr.getLatitude() / AGGREGATION_LEVEL);
        long lngAggr = (long) (lr.getLongitude() / AGGREGATION_LEVEL);
        return String.format("%s:%s", latAggr, lngAggr);
    }
}
