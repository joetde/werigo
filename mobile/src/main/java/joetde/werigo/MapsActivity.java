package joetde.werigo;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import joetde.werigo.data.LocationMerger;
import joetde.werigo.display.CircleCreator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static joetde.werigo.Constants.*;

@Slf4j
public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback,
                   GoogleMap.OnCameraChangeListener,
                   GoogleMap.OnMapLongClickListener,
                   CircleCreator {

    @Getter private LatLngBounds cameraBounds;
    private GoogleMap map;
    private LocationMerger locationMerger = new LocationMerger();
    private boolean isFirstLocation = true;

    private static final LatLng NULL_LATLNG = new LatLng(0,0);
    private static final int PREM_CODE = 141;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        havePermissionsOrDie();
    }

    private void havePermissionsOrDie() {
        int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
            if (!shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showMessageGetPermissionsOrGoToHell();
                return;
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PREM_CODE);
            return;
        }

        startYourWorkBuddy();
    }

    private void showMessageGetPermissionsOrGoToHell() {
        new AlertDialog.Builder(this)
            .setMessage("You need location permissions for this application to work.")
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int which) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PREM_CODE);
                }
            })
            .setNegativeButton("Quit app", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int which) {
                    android.os.Process.killProcess(android.os.Process.myPid());
                    System.exit(1);
                }
            })
            .create()
            .show();
    }

    private void startYourWorkBuddy() {
        // set UI
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // configure location engine
        locationMerger.setContextAndLoadDataSource(this);

        // configure maps stuff
        setLocationListener();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PREM_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startYourWorkBuddy();
                } else {
                    showMessageGetPermissionsOrGoToHell();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
                isFirstLocation = false;
            }
            // Only record point that is in the screen
            if (loc.getAccuracy() < MIN_ACCURACY_TO_RECORD) {
                if (locationMerger.addLocationToMerge(loc)) {
                    log.debug("Add new point: {}", loc);
                }
            } else {
                log.debug("Skipping point because of bad accuracy.");
            }
        }
    }

    @Override
    public void onCameraChange(CameraPosition position) {
        cameraBounds = map.getProjection().getVisibleRegion().latLngBounds;
        locationMerger.refreshLocations(cameraBounds, position.zoom);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        locationMerger.longClick(latLng);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnCameraChangeListener(this);
        map.setOnMapLongClickListener(this);
        map.setMyLocationEnabled(true);
    }

    public Circle createAndSetCircle() {
        return map.addCircle(createCircle());
    }

    private CircleOptions createCircle() {
        CircleOptions circle = new CircleOptions();
        circle.center(NULL_LATLNG);
        circle.strokeWidth(0);
        circle.visible(true);
        return circle;
    }

}
