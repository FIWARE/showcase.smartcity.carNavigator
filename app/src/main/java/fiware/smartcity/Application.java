package fiware.smartcity;

import com.here.android.mpa.mapping.MapObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *   Global Application object for common context and constants
 *
 *
 */
public class Application {
    public static MainActivity mainActivity = null;

    public static int THRESHOLD_DISTANCE = 4000;
    public static int DEFAULT_RADIUS = 400;

    // Parking mode is on from 700 ms far away from destination
    public static int PARKING_DISTANCE = 700;

    public static List<MapObject> mapObjects = new ArrayList<MapObject>();

    public static Map<String, String> renderedEntities = new HashMap<>();

    public static String TAG = "FIWARE-HERE";
    public static String TRANSFER_RESULT = "Transfer-Result";

    // We will look for parkings not further than 2 km
    public static int MAX_PARKING_DISTANCE = 2000;

    // Average radius for Ambient areas
    public static int AMBIENT_AREA_RADIUS = 800;

    /* Every 250 meters we ask about changes on ambient area */
    public static int DISTANCE_FREQ_AMBIENT_AREA = 250;

    /* NGSI types used for querying data */
    public static String AMBIENT_OBSERVED_TYPE = "AmbientObserved";
    public static String AMBIENT_AREA_TYPE     = "AmbientArea";
    public static String PARKING_TYPE          = "Parking";
    public static String STREET_PARKING_TYPE   = "StreetParking";
    public static String PARKING_LOT_TYPE      = "ParkingLot";
    public static String WEATHER_FORECAST_TYPE = "WeatherForecast";
    public static String GAS_STATION_TYPE      = "GasStation";
    public static String GARAGE_TYPE           = "Garage";

    public static String PARKING_RESTRICTION_TYPE = "ParkingRestriction";

    public static String PARKING_LOT_ZONE_TYPE = "ParkingLotZone";

    /* 14 m/s == 50 kms/h */
    public static int DEFAULT_SPEED = 14;


    public static String[] POLLUTANTS = {
            "SO2",
            "CO",
            "NO",
            "NO2",
            "PM2.5",
            "PM10",
            "NOx",
            "O3",
            "TOL",
            "BEN",
            "EBE",
            "MXY",
            "PXY",
            "OXY",
            "TCH",
            "CH4",
            "NHMC"
    };

    public static String[] POLLUTION_LEVELS = {
      "VeryLow", "Low", "Medium", "High", "VeryHigh"
    };

    public static String[] POLLUTION_COLORS = {
      "#3B388E3C", "#3B8BC34A", "#3BFFC107", "#3BFF9800", "#3BFF5722"
    };

    public static String RESULT_SET_KEY = "__Result__";

    public static String WEATHER_FORECAST_ENTITY = "Forecast";
    public static String WEATHER_OBSERVED_REFRESH = "nextWeatherObserved";

    public static long lastTimeSpeak = -1;

    public static long lastTimeBubble = -1;

    // 30 seconds
    public static long SPEAK_INTERVAL = 30 * 1000;

    public static boolean isSpeaking = false;

    public static String LAST_CITY_VISITED = "Last_City";

    public static String MARKET_URL = "https://demo-mwc.conwet.com/";

    public static String BF_TOKEN = "bf_token";
    public static String BF_USER  = "bf_user";
}
