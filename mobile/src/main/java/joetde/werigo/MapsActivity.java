package joetde.werigo;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;

import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.*;

@Slf4j
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap map;
    private LocationMerger locationMerger = new LocationMerger();
    private boolean isFirstLocation = true;

    private static final LatLng NULL_LATLNG = new LatLng(0,0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        locationMerger.setContext(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationMerger.load();
        setLocationListener();
    }

    private void setLocationListener() {
        LocationManager locationManager = (LocationManager) this.getSystemService(this.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location loc) { onLocationUpdate(loc); }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_TIME_LOCATION_UPDATES,
                MIN_DISTANCE_LOCATION_UPDATES,
                locationListener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                MIN_TIME_LOCATION_UPDATES,
                MIN_DISTANCE_LOCATION_UPDATES,
                locationListener);
    }

    private void onLocationUpdate(Location loc) {
        if (map != null) {
            LatLng ll = new LatLng(loc.getLatitude(), loc.getLongitude());
            if (isFirstLocation) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(ll, 16));
                for (LocationRecord lr : locationMerger.getLocations()) {
                    lr.setCircle(map.addCircle(createCircle()));
                }
                isFirstLocation = false;
            }
            if (loc.getAccuracy() < MIN_ACCURACY_TO_RECORD) {
                if (locationMerger.addLocationToMerge(loc)) {
                    log.error("Add new point: {}", loc);
                    locationMerger.addCircleToLastLocation(map.addCircle(createCircle()));
                }
            } else {
                log.debug("Skipping point because of bad accuracy.");
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setMyLocationEnabled(true);
    }

    private CircleOptions createCircle() {
        CircleOptions circle = new CircleOptions();
        circle.center(NULL_LATLNG);
        circle.strokeWidth(0);
        circle.visible(true);
        return circle;
    }

}
