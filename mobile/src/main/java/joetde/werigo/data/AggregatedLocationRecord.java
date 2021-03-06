package joetde.werigo.data;

import android.graphics.Color;

import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;

import lombok.Getter;
import lombok.Setter;

import static joetde.werigo.Constants.LOCATION_HEAT_RGB;

public class AggregatedLocationRecord {
    private double latitude;
    private double longitude;
    @Setter private double radius;
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
            latitude = (nbAggregatedPoints * latitude + lr.getLatitude() * (lr.getMerges()+1)) / (nbAggregatedPoints + lr.getMerges() + 1);
            longitude = (nbAggregatedPoints * longitude + lr.getLongitude() * (lr.getMerges()+1)) / (nbAggregatedPoints + lr.getMerges() + 1);
        }
        nbAggregatedPoints += lr.getMerges() + 1;
    }

    public void removePoint(LocationRecord lr) {
        nbAggregatedPoints -= lr.getMerges() + 1;
    }

    public void refreshDisplay() {
        if (circle != null) {
            circle.setCenter(new LatLng(latitude, longitude));
            circle.setRadius(radius);
            circle.setFillColor(getColor());
        }
    }

    private int getColor() {
        int colorSet = Math.min((int) nbAggregatedPoints / (int)(2 * radius / 20), 4);
        return Color.argb(125, LOCATION_HEAT_RGB[colorSet][0], LOCATION_HEAT_RGB[colorSet][1], LOCATION_HEAT_RGB[colorSet][2]);
    }
}
