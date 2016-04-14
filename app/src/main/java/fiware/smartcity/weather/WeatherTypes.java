package fiware.smartcity.weather;

import java.util.HashMap;
import java.util.Map;

/**
 *   Weather types
 *
 *
 */
public class WeatherTypes {
   public static String[] VALUES = new String[] {
           "Clear" ,
           "Slightly cloudy",
           "Partly Cloudy",
           "Overcast",
           "High clouds",
           "Light rain",
           "Drizzle",
           "Heavy rain",
           "Rain"
    };

    public static String[] ICONS = new String [] {
            "weather_clear",
            "weather_slightly_cloudy",
            "weather_partly_cloudy",
            "weather_overcast",
            "weather_high_clouds",
            "weather_rain",
            "weather_drizzle",
            "weather_rain",
            "weather_rain"
    };

    public static String[] VALUES_ES = new String[] {
            "Despejado",
            "Poco nuboso",
            "Parcialmente nuboso",
            "Intervalos nubosos",
            "Nubes altas",
            "Intervalos nubosos con lluvia",
            "Intervalos nubosos con lluvia escasa",
            "Nuboso con lluvia",
            "Muy nuboso con lluvia",
            "Cubierto con lluvia",
            "Nuboso",
            "Muy nuboso",
            "Cubierto",
            "Muy nuboso con lluvia escasa",
            "Nuboso con lluvia escasa"
    };

    public static String[] ICONS_ES = new String [] {
            "weather_clear",
            "weather_slightly_cloudy",
            "weather_partly_cloudy",
            "weather_partly_cloudy",
            "weather_high_clouds",
            "weather_rain",
            "weather_drizzle",
            "weather_rain",
            "weather_rain",
            "weather_rain",
            "weather_overcast",
            "weather_overcast",
            "weather_overcast",
            "weather_drizzle",
            "weather_drizzle"
    };

    public static Map<String, String> WEATHER_TYPE_ICONS = new HashMap<>();

    static {
        int index = 0;
        for (String value : VALUES) {
            WEATHER_TYPE_ICONS.put(value, ICONS[index++]);
        }

        index = 0;
        for (String value : VALUES_ES) {
            WEATHER_TYPE_ICONS.put(value, ICONS_ES[index++]);
        }
    }

    public static String getIcon(String weatherType) {
        String out = WEATHER_TYPE_ICONS.get(weatherType);

        if (out == null) {
            out = "Not_Found_Weather_Type";
        }

        return out;
    }
}
