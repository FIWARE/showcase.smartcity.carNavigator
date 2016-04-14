package fiware.smartcity.ngsi;

import com.here.android.mpa.common.GeoPolygon;

import java.util.ArrayList;
import java.util.List;

/**
 *   Represents the data needed for the Async Task CityDataRetriever
 *
 *
 */
public class CityDataRequest {
    public List<String> types = new ArrayList<String>();
    public double[] coordinates;
    public int radius = -1;
    public String geometry;
    public GeoPolygon polygon;
    public String token;
}
