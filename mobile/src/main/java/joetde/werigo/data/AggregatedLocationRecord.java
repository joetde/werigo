package joetde.werigo.data;

import android.graphics.Color;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;

import lombok.Getter;

import static joetde.werigo.Constants.LOCATION_HEAT_RGB;

public class AggregatedLocationRecord {
    private double latitude;
    private double longitude;
    @Getter private long nbAggregatedPoints = 0;
    @Getter private transient Circle circle = null;

    public void setCircle(Circle circle) {
        this.circle = circle;
        refreshDisplay();
    }

    public void addPoint(LocationRecord lr) {
        if (nbAggregatedPoints == 0) {
            latitude = lr.getLatitude();
            longitude = lr.getLongitude();
        } else {
            // TODO ponderate with new point
            latitude = (nbAggregatedPoints * latitude + lr.getLatitude() * (lr.getMerges()+1)) / (nbAggregatedPoints + lr.getMerges() + 1);
            longitude = (nbAggregatedPoints * longitude + lr.getLongitude() * (lr.getMerges()+1)) / (nbAggregatedPoints + lr.getMerges() + 1);
        }
        nbAggregatedPoints += lr.getMerges() + 1;
        refreshDisplay();
    }

    public void removePoint(LocationRecord lr) {
        nbAggregatedPoints -= lr.getMerges() + 1;
    }

    private void refreshDisplay() {
        if (circle != null) {
            circle.setCenter(new LatLng(latitude, longitude));
            circle.setRadius(getRadius());
            circle.setFillColor(getColor());
        }
    }

    private double getRadius() {
        return 150;
    }

    private int getColor() {
        int colorSet = Math.min((int) nbAggregatedPoints / 20, 4);
        return Color.argb(125, LOCATION_HEAT_RGB[colorSet][0], LOCATION_HEAT_RGB[colorSet][1], LOCATION_HEAT_RGB[colorSet][2]);
    }
}
