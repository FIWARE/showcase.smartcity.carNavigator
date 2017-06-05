package fiware.smartcity.ambient;

import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapOverlayType;
import com.here.android.mpa.mapping.MapPolygon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import fiware.smartcity.Application;
import fiware.smartcity.ngsi.CityDataListener;
import fiware.smartcity.ngsi.CityDataRequest;
import fiware.smartcity.ngsi.CityDataRetriever;
import fiware.smartcity.ngsi.Entity;
import fiware.smartcity.R;
import fiware.smartcity.ResultListener;
import fiware.smartcity.navigation.SpeechMessage;
import fiware.smartcity.Utilities;

/**
 *   Renders an AmbientArea by creating a polygon and querying for sensor data on that area
 *   and knowing about overall air quality there
 *
 */
public class AmbientAreaRenderer implements CityDataListener {

    private Map hereMap;
    private TextToSpeech tts;
    private Entity ambientArea;
    private GeoPolygon polygon;
    private AmbientAreaRenderListener listener;
    private View oascView;
    private GeoCoordinate currentPos;

    public static java.util.Map<String,String> AREA_COLORS = new HashMap<>();

    static {
        int index = 0;
        for(String pollutionLevel : Application.POLLUTION_LEVELS) {
            AREA_COLORS.put(pollutionLevel,Application.POLLUTION_COLORS[index++]);
        }

    }

    public AmbientAreaRenderer(Map hereMap, TextToSpeech tts, Entity ent, View v,
                               GeoCoordinate currentPos) {
        this.hereMap = hereMap;
        this.ambientArea = ent;
        this.tts = tts;
        this.polygon = (GeoPolygon)ent.attributes.get("polygon");
        this.oascView = v;
        this.currentPos = currentPos;
    }

    @Override
    public void onCityDataReady(java.util.Map<String, List<Entity>> data) {
        // When city data is ready, obtain data from all the sensors
        // and then pass the ball to the AirQualityCalculator
        java.util.Map<String,List<Double>> pollutants = new HashMap<>();

        for (Entity ent: data.get(Application.RESULT_SET_KEY)) {
            if(!ent.type.equals(Application.AMBIENT_OBSERVED_TYPE)) {
                continue;
            }

            java.util.Map<String,Object> attributes = ent.attributes;

            for (String pollutant : Application.POLLUTANTS) {
                if (attributes.get(pollutant) != null) {
                    List<Double> accumulated = pollutants.get(pollutant);
                    if(accumulated == null) {
                        accumulated = new ArrayList<>();
                        pollutants.put(pollutant, accumulated);
                    }
                    accumulated.add((Double)attributes.get(pollutant));
                }
            }
        }

        java.util.Map<String, Double> finalValues = new HashMap<>();

        for (String pollutant : pollutants.keySet()) {
            List<Double> values = pollutants.get(pollutant);
            double average = 0;
            for (double value : values) {
                average += value;
            }
            average /= values.size();

            finalValues.put(pollutant, average);
        }

        AirQualityCalculator calculator = new AirQualityCalculator();
        calculator.setListener(new ResultListener<java.util.Map<String,java.util.Map>>() {
            @Override
            public void onResultReady(java.util.Map<String,java.util.Map> result) {
                if (result != null && result.size() > 0) {
                    oascView.findViewById(R.id.airQualityGroup).setVisibility(View.VISIBLE);
                    Utilities.updateAirPollution(result,
                            (LinearLayout) oascView.findViewById(R.id.airQualityPollutants));

                    Utilities.AirQualityData data = Utilities.getAirQualityData(result);

                    String aqiLevelName = (String)data.worstIndex.get("name");

                    MapPolygon polygon = doRender(AREA_COLORS.get(aqiLevelName));

                    tts.playEarcon("ambient_area", TextToSpeech.QUEUE_ADD, null,
                            "ambient_area_announcement");
                    List<SpeechMessage> msgs = new ArrayList<>();
                    msgs.add(new SpeechMessage("You have entered an area with "
                            + data.worstIndex.get("description") + " pollution", 100, "Pollution_Area" ));
                    Utilities.speak(tts, msgs);


                    GeoCoordinate coords = new GeoCoordinate(ambientArea.location[0],
                            ambientArea.location[1]);

                    // PointF f = hereMap.geoToPixel(currentPos);
                    // f.offset(100, 50);
                    // GeoCoordinate coords = hereMap.pixelToGeo(f);

                    MapMarker marker = Utilities.buildSensorMarker(coords, "Air Quality",
                            data.asString);

                    hereMap.addMapObject(marker);
                    Utilities.showBubble(marker);

                    Application.mapObjects.add(marker);

                    listener.onRendered(aqiLevelName, polygon);
                }
                else {
                    Log.w(Application.TAG, "Air quality calculator returned empty object");
                }
            }
        });

        calculator.execute(finalValues);
    }

    private void getDataFromSensors() {
        CityDataRequest req = new CityDataRequest();
        req.geometry = "polygon";
        req.polygon = polygon;
        req.types = Arrays.asList(Application.AMBIENT_OBSERVED_TYPE);

        CityDataRetriever retriever = new CityDataRetriever();
        retriever.setListener(this);

        retriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, req);
    }

    public void render(AmbientAreaRenderListener listener) {
        this.listener = listener;
        polygon = (GeoPolygon)ambientArea.attributes.get("polygon");
        getDataFromSensors();
    }

    private MapPolygon doRender(String targetColor) {
        MapPolygon ambientAreaPolygon = null;

        try {
            ambientAreaPolygon = new MapPolygon(polygon);
            ambientAreaPolygon.setLineColor(Color.parseColor(targetColor));
            ambientAreaPolygon.setFillColor(Color.parseColor(targetColor));

            ambientAreaPolygon.setOverlayType(MapOverlayType.BACKGROUND_OVERLAY);

            hereMap.addMapObject(ambientAreaPolygon);

            GeoBoundingBox bb = hereMap.getBoundingBox();
            GeoBoundingBox box = polygon.getBoundingBox();
            if (false && !bb.contains(box)) {
                Map.PixelResult pr = hereMap.projectToPixel(box.getCenter());
                PointF point = pr.getResult();
                hereMap.setZoomLevel(hereMap.getZoomLevel() - 3, point, Map.Animation.LINEAR);
            }
        }
        catch(Throwable thr) {
            Log.e(Application.TAG, "Error while painting ambient area: " + thr);
        }

        return ambientAreaPolygon;
    }
}
