package fiware.smartcity.ngsi;

import android.os.AsyncTask;
import android.util.Log;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fiware.smartcity.Application;
import fiware.smartcity.parking.ParkingAttributes;
import fiware.smartcity.weather.WeatherAttributes;

/**
 *
 *   Retrieves data from the city by calling FIWARE-HERE Adaptor
 *
 *
 */
public class CityDataRetriever extends AsyncTask<CityDataRequest, Integer, Map<String,List<Entity>> > {
    private CityDataListener listener;

    protected Map<String,List<Entity>> doInBackground(CityDataRequest... request) {
        String urlString = createRequestURL(request[0]);

        StringBuffer output = new StringBuffer("");
        Map<String, List<Entity>> out = new HashMap<>();
        List<Entity> resultSet = new ArrayList<>();

        /*
        Entity ent = new Entity();

        ent.id = "12345";
        ent.type = "EnvironmentEvent";
        ent.attributes = new HashMap<String, Object>();
        ent.location = new double[] { 41.1500167847, -8.60708522797 };
        ent.attributes.put("temperature", new Double(22.5));

        out.add(ent); */
        InputStream inputStream = null;

        try {
            URL url = new URL(urlString);

            Log.d(Application.TAG, "URL: " + urlString);

            HttpURLConnection connection = (HttpURLConnection)url.openConnection();
            connection.setRequestProperty("User-Agent", "FIWARE-HERE-Navigator");
            connection.setRequestMethod("GET");
            connection.setDoInput(true);
            connection.connect();

            inputStream = connection.getInputStream();

            BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = rd.readLine()) != null) {
                output.append(line);
            }

            Log.d(Application.TAG, "Response: " + output.toString());
            JSONArray array = new JSONArray(output.toString());

            for(int j = 0; j < array.length(); j++) {
                Entity ent = new Entity();
                JSONObject obj = array.getJSONObject(j);

                ent.id = obj.getString("id");
                ent.type = obj.getString("type");
                ent.attributes = new HashMap<String, Object>();

                fillLocation(obj, ent);

                fillAttributes(obj, ent.type, ent.attributes);

                if(out.get(ent.type) == null) {
                    out.put(ent.type, new ArrayList<Entity>());
                }
                out.get(ent.type).add(ent);

                resultSet.add(ent);
            }
        } catch (Exception e) {
            Log.e(Application.TAG, "While obtaining data: " + e.toString());
        }
        finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                }
                catch (Throwable thr) {
                    Log.e(Application.TAG, "Error while closing stream: " + thr);
                }
            }
        }

        out.put(Application.RESULT_SET_KEY, resultSet);

        return out;
    }

    private void fillLocation(JSONObject obj, Entity ent) throws JSONException {
        JSONObject location  = null;
        String locValue = null;
        JSONArray locArray = null;

        try {
            location = obj.getJSONObject("location");
        }
        catch(JSONException jse3) {
        }

        // There could be entities (namely weather entities) without location
        if (location != null) {
            try {
                ent.coordinates = location.getJSONArray("coordinates");

                if (location.getString("type").equals("Point")) {
                    double latitude = ent.coordinates.getDouble(1);
                    double longitude = ent.coordinates.getDouble(0);
                    ent.location = new double[]{latitude, longitude};
                }
                else if (location.getString("type").indexOf("Polygon") != -1) {
                    JSONArray polygons = ent.coordinates;
                    if (location.getString("type").equals("MultiPolygon")) {
                        polygons = ent.coordinates.getJSONArray(0);
                    }

                    List<GeoPolygon> locationPolygon = new ArrayList<GeoPolygon>();

                    int total = polygons.length();
                    for (int j = 0; j < total; j++) {
                        JSONArray polygon = polygons.getJSONArray(j);
                        List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
                        for (int x = 0; x < polygon.length(); x++) {
                            double lat = polygon.getJSONArray(x).getDouble(1);
                            double lon = polygon.getJSONArray(x).getDouble(0);
                            geoPolygon.add(new GeoCoordinate(lat, lon));
                        }

                        GeoPolygon newGeoPolygon = new GeoPolygon(geoPolygon);
                        locationPolygon.add(newGeoPolygon);

                        GeoCoordinate coords = newGeoPolygon.getBoundingBox().getCenter();

                        ent.location = new double[]{coords.getLatitude(), coords.getLongitude()};
                    }

                    ent.attributes.put("polygon", locationPolygon);
                }
            }
            catch(JSONException jse) { }
        }
    }

    private void fillAttributes(JSONObject obj, String type,
                                Map<String, Object> attrs) throws Exception {
        if (type.equals("TrafficEvent")) {

        }
        else if (type.equals(Application.AMBIENT_OBSERVED_TYPE)) {
            fillAmbientObserved(obj,type,attrs);
        }
        else if (type.equals(Application.PARKING_LOT_TYPE) ||
                type.equals(Application.STREET_PARKING_TYPE) ||
                type.equals((Application.PARKING_LOT_ZONE_TYPE))) {
            fillParking(obj, type, attrs);
        }
        else if (type.equals("CityEvent")) {

        }
        else if (type.equals(Application.AMBIENT_AREA_TYPE)) {
            fillAmbientArea(obj,type,attrs);
        }
        else if (type.equals(Application.WEATHER_FORECAST_TYPE)) {
            fillWeather(obj,type, attrs);
        }
        else if (type.equals(Application.GARAGE_TYPE)) {
            fillGarage(obj, type, attrs);
        }
        else if (type.equals(Application.GAS_STATION_TYPE)) {
            fillGasStation(obj, type, attrs);
        }
        else if (type.equals(Application.PARKING_RESTRICTION_TYPE)) {
            fillParkingRestriction(obj, type, attrs);
        }
        else if (type.equals(Application.POI_TYPE)) {
            fillPointOfInterest(obj, type, attrs);
        }
    }

    private void fillAmbientObserved(JSONObject obj, String type, Map<String, Object> attrs) throws Exception {
        getDoubleJSONAttr(WeatherAttributes.TEMPERATURE, obj, WeatherAttributes.TEMPERATURE, attrs);
        getDoubleJSONAttr(WeatherAttributes.R_HUMIDITY, obj, WeatherAttributes.R_HUMIDITY, attrs);
        getDoubleJSONAttr("noiseLevel", obj, "noiseLevel", attrs);

        getStringJSONAttr("created", obj, "created", attrs);
        getStringJSONAttr("sensorType", obj, "sensorType", attrs);

        try {
            JSONObject pollutants = obj.getJSONObject("pollutants");
            for(String pollutant : Application.POLLUTANTS) {
                try {
                    double value =
                            pollutants.getJSONObject(pollutant).getDouble("concentration");
                    attrs.put(pollutant, value);
                }
                catch(JSONException jsoe) { }
            }
        }
        catch(JSONException jsoe) {

        }
    }

    private void fillParkingRestriction(JSONObject obj, String type,
                                        Map<String, Object> attrs) throws Exception {
        // We obtain location
        attrs.put("polygon", getPolygon(obj.getString("location")));
    }

    private void fillParking(JSONObject obj, String type,
                             Map<String, Object> attrs) throws Exception {
        getIntegerJSONAttr(ParkingAttributes.AVAILABLE_SPOTS, obj,
                ParkingAttributes.AVAILABLE_SPOTS, attrs);
        getIntegerJSONAttr(ParkingAttributes.TOTAL_SPOTS, obj, ParkingAttributes.TOTAL_SPOTS, attrs);
        getStringJSONAttr("name", obj, "name", attrs);
        getStringJSONAttr("description", obj, "description", attrs);
    }

    private void fillPointOfInterest (JSONObject obj, String type,
                                      Map<String, Object> attrs) throws Exception {

        getStringJSONAttr("name", obj, null, attrs);
        getStringJSONAttr("description", obj, null, attrs);
        getStringListJSONAttr("category", obj, null, attrs);

    }

    private void fillGarage (JSONObject obj, String type,
                             Map<String, Object> attrs) throws Exception {
        getStringJSONAttr("name", obj, null, attrs);
    }

    private void fillGasStation (JSONObject obj, String type,
                                 Map<String, Object> attrs) throws Exception {
        getStringJSONAttr("name", obj, null, attrs);
    }

    private void fillWeather (JSONObject obj, String type,
                              Map<String, Object> attrs) throws Exception {
        getDoubleJSONAttr(WeatherAttributes.TEMPERATURE, obj, null, attrs);
        getDoubleJSONAttr(WeatherAttributes.R_HUMIDITY, obj, null, attrs);
        getCompoundJSONAttr(WeatherAttributes.MAXIMUM, obj, null, attrs);
        getCompoundJSONAttr(WeatherAttributes.MINIMUM, obj, null, attrs);

        getStringJSONAttr(WeatherAttributes.VALID_FROM, obj, null, attrs);
        getStringJSONAttr(WeatherAttributes.VALID_TO, obj, null, attrs);

        getDoubleJSONAttr(WeatherAttributes.WIND_SPEED, obj, null, attrs);
        getIntegerJSONAttr(WeatherAttributes.WIND_DIRECTION, obj, null, attrs);

        getStringJSONAttr(WeatherAttributes.WEATHER_TYPE, obj, null, attrs);

        getDoubleJSONAttr(WeatherAttributes.POP, obj, null, attrs);
    }

    private void fillAmbientArea(JSONObject obj, String type,
                                 Map<String, Object> attrs) throws Exception {

        JSONArray array = obj.getJSONArray("location");
        List<GeoCoordinate> coordinates = new ArrayList<>();

        for(int j = 0; j < array.length(); j++) {
            String coordPair = array.getString(j);
            String[] list = coordPair.split(",");
            GeoCoordinate coord = new GeoCoordinate(Double.parseDouble(list[0]),
                    Double.parseDouble(list[1]));
            coordinates.add(coord);
        }
        attrs.put("polygon", new GeoPolygon(coordinates));
    }

    private GeoPolygon getPolygon(String coords) {
        String[] polygonCoords = coords.split(",");

        List<GeoCoordinate> geoPolygon = new ArrayList<GeoCoordinate>();
        for(int j = 0; j < polygonCoords.length; j+=2) {
            double lat = Double.parseDouble(polygonCoords[j]);
            double lon = Double.parseDouble(polygonCoords[j + 1]);
            geoPolygon.add(new GeoCoordinate(lat, lon));
        }

        if (geoPolygon.size() > 0) {
            GeoCoordinate last = geoPolygon.get(geoPolygon.size() - 1);
            GeoCoordinate first = geoPolygon.get(0);

            if (last.getLatitude() != first.getLatitude() ||
                    last.getLongitude() != first.getLongitude()) {
                geoPolygon.add(new GeoCoordinate(first));
            }
        }

        return new GeoPolygon(geoPolygon);
    }

    private String getTypes(List<String> types) {
        StringBuffer out = new StringBuffer();

        for(int j = 0; j < types.size(); j++) {
            out.append(types.get(j));
            if(j + 1 < types.size()) {
                out.append(",");
            }
        }

        return out.toString();
    }

    private String createRequestURL(CityDataRequest req) {
        String geometry = req.geometry;
        if(geometry == null) {
            geometry = "point";
        }

        String geoRelStr;
        if(req.radius != -1) {
            geoRelStr = "&georel=near;maxDistance:" + req.radius;
        }
        else if ("intersects".equals(req.georel)) {
            geoRelStr = "&georel=intersects";
        }
        else {
            geoRelStr = "&georel=coveredBy";
        }

        String coords = "";
        if(req.coordinates != null) {
            coords = req.coordinates[0] + "," + req.coordinates[1];
        }
        else if (req.polygon != null) {
            for (int j = 0; j < req.polygon.getNumberOfPoints(); j++) {
                GeoCoordinate point = req.polygon.getPoint(j);
                coords += point.getLatitude() + "," + point.getLongitude();
                coords += ",";
            }
            coords = coords.substring(0,coords.length() - 1);
        }

        String out = Application.SERVICE_URL + "?" + "coords=" + coords
                + geoRelStr + "&type=" + getTypes(req.types) + "&geometry=" + geometry;

        if (req.token != null && req.token.length() > 0) {
            out += "&token=" + req.token;
        }

        return out;
    }


    private void getDoubleJSONAttr(String attr, JSONObject obj, String mAttr,
                                     Map<String, Object> attrs) {
        Double out = null;
        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            out = obj.getDouble(attr);
            attrs.put(mappedAttr, out);
        }
        catch(JSONException e) { }
    }

    private void getCompoundJSONAttr(String attr, JSONObject obj, String mAttr,
                                   Map<String, Object> attrs) {

        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            JSONObject data = obj.getJSONObject(attr);
            Iterator<String> keys = data.keys();
            Map<String, Object> values = new HashMap<>();
            while(keys.hasNext()) {
                String key = keys.next();
                Object value = data.get(key);
                if (value instanceof Integer) {
                    Integer ivalue = (Integer)value;
                    value = new Double(ivalue.doubleValue());
                }
                values.put(key, value);
            }

            attrs.put(mappedAttr, values);
        }
        catch(JSONException e) { }
    }

    private void getIntegerJSONAttr(String attr, JSONObject obj, String mAttr,
                                    Map<String, Object> attrs) {
        Integer out = null;
        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            out = obj.getInt(attr);
            attrs.put(mappedAttr, out);
        }
        catch(JSONException e) { }
    }

    private void getStringJSONAttr(String attr, JSONObject obj, String mAttr,
                                    Map<String, Object> attrs) {
        String out = null;
        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            out = obj.getString(attr);
            attrs.put(mappedAttr, out);
        }
        catch(JSONException e) { }
    }

    private void getStringListJSONAttr(String attr, JSONObject obj, String mAttr,
                                       Map<String, Object> attrs) {

        String mappedAttr = mAttr != null ? mAttr : attr;

        try {
            List<String> resultAttrs = new ArrayList<>();
            JSONArray values = obj.getJSONArray(mappedAttr);
            for (int i = 0; i < values.length(); i++) {
                resultAttrs.add(values.getString(i));
            }

            attrs.put(mappedAttr, resultAttrs);
        }
        catch(JSONException e) { }
    }


    public void setListener(CityDataListener listener) {
        this.listener = listener;
    }

    protected void onPostExecute(Map<String, List<Entity>> data) {
        listener.onCityDataReady(data);
    }
}
