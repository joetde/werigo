package joetde.werigo.datasource;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

import joetde.werigo.data.LocationRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationRecordDataSource extends SQLiteOpenHelper {
    public static final int DB_VERSION = 3;
    public static final String DB_NAME = "werigo.locations.db";

    public LocationRecordDataSource(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        log.info("Creating DB");
        db.execSQL(QueryUtils.CREATE_TABLE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        log.info("Updating DB");
        db.execSQL(QueryUtils.DROP_TABLE_QUERY);
        onCreate(db);
    }

    public void writeNewLocation(LocationRecord lr) {
        log.debug("Writing location: {}", lr);
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationRecordEntry.COLUMN_NAME_LATITUDE, lr.getLatitude());
        values.put(LocationRecordEntry.COLUMN_NAME_LONGITUDE, lr.getLongitude());
        values.put(LocationRecordEntry.COLUMN_NAME_ACCURACY, lr.getAccuracy());
        values.put(LocationRecordEntry.COLUMN_NAME_TIMESTAMP, lr.getTimestamp());
        values.put(LocationRecordEntry.COLUMN_NAME_MERGES, lr.getMerges());
        long id = db.insert(LocationRecordEntry.TABLE_NAME, null, values);
        lr.setId(id);
    }

    public void updateLocation(LocationRecord lr) {
        log.debug("Updating location: {}", lr);
        SQLiteDatabase db = getReadableDatabase();
        ContentValues values = new ContentValues();
        values.put(LocationRecordEntry.COLUMN_NAME_LATITUDE, lr.getLatitude());
        values.put(LocationRecordEntry.COLUMN_NAME_LONGITUDE, lr.getLongitude());
        values.put(LocationRecordEntry.COLUMN_NAME_ACCURACY, lr.getAccuracy());
        values.put(LocationRecordEntry.COLUMN_NAME_TIMESTAMP, lr.getTimestamp());
        values.put(LocationRecordEntry.COLUMN_NAME_MERGES, lr.getMerges());
        String idFilter = "_id = " + lr.getId();
        db.update(LocationRecordEntry.TABLE_NAME, values, idFilter, null);
    }

    public List<LocationRecord> loadLocations(LatLngBounds bounds) {
        List<LocationRecord> locations = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        String[] allArgs = {LocationRecordEntry._ID,
                            LocationRecordEntry.COLUMN_NAME_LATITUDE,
                            LocationRecordEntry.COLUMN_NAME_LONGITUDE,
                            LocationRecordEntry.COLUMN_NAME_ACCURACY,
                            LocationRecordEntry.COLUMN_NAME_TIMESTAMP,
                            LocationRecordEntry.COLUMN_NAME_MERGES};
        Cursor c = db.query(LocationRecordEntry.TABLE_NAME,
                            allArgs,
                            QueryUtils.WHERE_IN_BOUNDS,
                            new String[] {Double.toString(bounds.southwest.latitude),
                                          Double.toString(bounds.northeast.latitude),
                                          Double.toString(bounds.southwest.longitude),
                                          Double.toString(bounds.northeast.longitude)},
                            null, null, null);
        if (c.moveToFirst()) {
            do {
                LocationRecord lr = new LocationRecord(c.getLong(0), c.getDouble(1), c.getDouble(2), c.getDouble(3), c.getLong(4), c.getInt(5));
                locations.add(lr);
            } while (c.moveToNext());
        }
        c.close();
        return locations;
    }

    public long countLocations(LatLngBounds bounds) {
        SQLiteDatabase db = getReadableDatabase();
        return DatabaseUtils.queryNumEntries(db, LocationRecordEntry.TABLE_NAME,
                QueryUtils.WHERE_IN_BOUNDS,
                new String[]{Double.toString(bounds.southwest.latitude),
                             Double.toString(bounds.northeast.latitude),
                             Double.toString(bounds.southwest.longitude),
                             Double.toString(bounds.northeast.longitude)});
    }

    public void delete(LocationRecord lr) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(LocationRecordEntry.TABLE_NAME,
                  LocationRecordEntry._ID + " = " + lr.getId(), null);
    }

    private static abstract class LocationRecordEntry implements BaseColumns {
        public static final String TABLE_NAME = "LocationRecords";
        public static final String COLUMN_NAME_LATITUDE = "latitude";
        public static final String COLUMN_NAME_LONGITUDE = "longitude";
        public static final String COLUMN_NAME_ACCURACY = "accuracy";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
        public static final String COLUMN_NAME_MERGES = "merges";
    }

    private static abstract class QueryUtils {
        public static final String CREATE_TABLE_QUERY =
                "CREATE TABLE IF NOT EXISTS " + LocationRecordEntry.TABLE_NAME + " (" +
                LocationRecordEntry._ID + " INTEGER PRIMARY KEY," +
                LocationRecordEntry.COLUMN_NAME_LATITUDE + " DOUBLE, " +
                LocationRecordEntry.COLUMN_NAME_LONGITUDE + " DOUBLE, " +
                LocationRecordEntry.COLUMN_NAME_ACCURACY + " DOUBLE, " +
                LocationRecordEntry.COLUMN_NAME_TIMESTAMP + " INTEGER, " +
                LocationRecordEntry.COLUMN_NAME_MERGES + " INTEGER);" +
                "CREATE INDEX coordinate_index ON " + LocationRecordEntry.TABLE_NAME + " " +
                "("+ LocationRecordEntry.COLUMN_NAME_LATITUDE + ", " + LocationRecordEntry.COLUMN_NAME_LONGITUDE + ");";
        public static final String DROP_TABLE_QUERY = "DROP TABLE " + LocationRecordEntry.TABLE_NAME + ";";
        // note: will probably do something funny at extreme latlng
        public static final String WHERE_IN_BOUNDS = LocationRecordEntry.COLUMN_NAME_LATITUDE + " > ? AND " +
                                                     LocationRecordEntry.COLUMN_NAME_LATITUDE + " < ? AND " +
                                                     LocationRecordEntry.COLUMN_NAME_LONGITUDE + " > ? AND " +
                                                     LocationRecordEntry.COLUMN_NAME_LONGITUDE + " < ? ";
    }
}
