package fiware.smartcity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.GeoPolygon;
import com.here.android.mpa.common.GeoPosition;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.common.PositioningManager;
import com.here.android.mpa.common.Version;
import com.here.android.mpa.common.ViewObject;
import com.here.android.mpa.guidance.NavigationManager;
import com.here.android.mpa.guidance.VoiceCatalog;
import com.here.android.mpa.guidance.VoiceSkin;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;
import com.here.android.mpa.mapping.MapGesture;
import com.here.android.mpa.mapping.MapMarker;
import com.here.android.mpa.mapping.MapObject;
import com.here.android.mpa.mapping.MapPolygon;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.mapping.MapState;
import com.here.android.mpa.routing.Maneuver;
import com.here.android.mpa.routing.Route;
import com.here.android.mpa.routing.RouteTta;
import com.here.android.mpa.search.Address;
import com.here.android.mpa.search.ErrorCode;
import com.here.android.mpa.search.Location;
import com.here.android.mpa.search.ResultListener;
import com.here.android.mpa.search.ReverseGeocodeRequest2;

import org.joda.time.DateTime;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import fiware.smartcity.ambient.AmbientAreaData;
import fiware.smartcity.ambient.AmbientAreaRenderListener;
import fiware.smartcity.ambient.AmbientAreaRenderer;
import fiware.smartcity.marketplace.MarketActivity;
import fiware.smartcity.navigation.LocationListener;
import fiware.smartcity.navigation.LocationTask;
import fiware.smartcity.navigation.RouteData;
import fiware.smartcity.navigation.VoiceNavigation;
import fiware.smartcity.ngsi.CityDataListener;
import fiware.smartcity.ngsi.CityDataRequest;
import fiware.smartcity.ngsi.CityDataRetriever;
import fiware.smartcity.ngsi.Entity;
import fiware.smartcity.parking.ParkingRenderer;
import fiware.smartcity.parking.ParkingRouteCalculator;
import fiware.smartcity.parking.ParkingRouteData;
import fiware.smartcity.parking.RouteCalculationListener;
import fiware.smartcity.navigation.RouteActivity;
import fiware.smartcity.navigation.RouteTransfer;
import fiware.smartcity.render.RenderListener;
import fiware.smartcity.render.SmartCityHandler;
import fiware.smartcity.render.SmartCityRequest;

/**
 *  Main activity for the FIWARE-HERE Smart Navigator
 *
 *
 */
public class MainActivity extends AppCompatActivity implements LocationListener {
    private List<MapObject> mapObjects = Application.mapObjects;

    private double currentZoomLevel, defaultZoomLevel, routeZoomLevel;

    private boolean loopMode = false;

    private String state = "";
    private boolean inParkingMode = false;
    private boolean parkingFound, pendingParkingRequest = false;

    private int parkingRadius;

    private PopupMenu popupMenu;

    private ProgressDialog locationProgress;

    private LinearLayout dataContainer;

    // map embedded in the map fragment
    private Map map = null;
    // map fragment embedded in this activity
    private MapFragment mapFragment = null;

    private ImageButton locationButton, menuButton;
    private ImageView fiwareImage, councilLogo;

    // Oporto downtown
    public static GeoCoordinate DEFAULT_COORDS;

    private GeoCoordinate lastKnownPosition;

    private NavigationManager navMan;
    private PositioningManager posMan;

    private static String destination;

    private TextView nextRoad, currentSpeed, ETA, distance, currentRoad, nextManouverDistance;
    private ImageView turnNavigation;

    private TextView parkingData;
    private ImageView parkingSign;

    private RouteActivity routeWizard;
    private MarketActivity marketplace;

    private RouteData routeData;

    private boolean underSimulation = false;

    private VoiceSkin voiceSkin;

    private long previousDistance, previousDistanceArea;

    private TextToSpeech tts;

    private TextView scityData;

    private AmbientAreaData ambientAreaData = new AmbientAreaData();

    private boolean pendingSmartCityRequest = false;

    private void checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){

            ActivityCompat.requestPermissions(
                    this, new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, Application.STORAGE_PERMISSION);

        } else {
            initMainActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case Application.LOCATION_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkStoragePermission();
                } else {
                    // Do something interesting
                    Log.e(Application.TAG, "Error: Location permission is required to use the App");
                    this.finish();
                    System.exit(0);
                }
                return;
            }
            case Application.STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initMainActivity();
                } else {
                    // Do something interesting
                    Log.e(Application.TAG, "Error: Storage permission is required to use the App");
                    this.finish();
                    System.exit(0);
                }
                return;
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        Application.mainActivity = this;

        // Check app permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this, new String[] {android.Manifest.permission.ACCESS_FINE_LOCATION}, Application.LOCATION_PERMISSION);

        } else {
            checkStoragePermission();
        }
    }

    private void initMainActivity() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.main);

        ViewGroup rootContainer = (ViewGroup)findViewById(R.id.mainFrame);

        getLayoutInflater().inflate(R.layout.activity_main, rootContainer);

        addMapWidgets();

        parkingData = (TextView)findViewById(R.id.parkingData);
        parkingSign = (ImageView)findViewById(R.id.parkingSign);

        popupMenu = new PopupMenu(MainActivity.this, menuButton);
        popupMenu.getMenuInflater().inflate(R.menu.menu_main, popupMenu.getMenu());
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_simulate) {
                    if (routeWizard == null) {
                        loopMode = false;
                        getDirections(null);
                    }
                } else if (item.getItemId() == R.id.action_home) {
                    ((RelativeLayout) findViewById(R.id.routePlanningLayout)).
                            setVisibility(RelativeLayout.GONE);
                    goHome();
                } else if (item.getItemId() == R.id.action_pause) {
                    pauseSimulation();
                } else if (item.getItemId() == R.id.action_terminate) {
                    loopMode = false;
                    terminateSimulation();
                } else if (item.getItemId() == R.id.action_restart) {
                    clearMap();
                    showRoute();
                } else if (item.getItemId() == R.id.action_loop) {
                    if (routeWizard == null) {
                        loopMode = true;
                        getDirections(null);
                    }
                } else if (item.getItemId() == R.id.action_market) {
                    showMarketplace();
                }
                return true;
            }
        });

        nextRoad = (TextView)findViewById(R.id.nextRoad);
        currentSpeed = (TextView)findViewById(R.id.currentSpeed);
        distance = (TextView)findViewById(R.id.distance2);
        ETA = (TextView)findViewById(R.id.eta);
        currentRoad = (TextView)findViewById(R.id.currentRoad);
        nextManouverDistance = (TextView)findViewById(R.id.manouver);

        turnNavigation = (ImageView)findViewById(R.id.nextTurn);

        hideNavigationUI();

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.mapfragment);

        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    Log.d(Application.TAG, "Version: " + Version.SDK_API_INT);
                    mapFragment.getMapGesture().addOnGestureListener(gestureListener);

                    // retrieve a reference of the map from the map fragment
                    map = mapFragment.getMap();
                    // Oporto downtown
                    String city =
                            getPreferences(MODE_WORLD_READABLE).getString(
                                    Application.LAST_CITY_VISITED, "Santander");
                    double[] coords = RouteActivity.cityCoords.get(city);
                    DEFAULT_COORDS = new GeoCoordinate(coords[0], coords[1]);

                    defaultZoomLevel = map.getMaxZoomLevel() - 7.0;
                    routeZoomLevel = map.getMaxZoomLevel() - 2.5;

                    goTo(map, DEFAULT_COORDS, Map.Animation.NONE);

                    map.setExtrudedBuildingsVisible(true);
                    map.getPositionIndicator().setVisible(true);
                    map.setLandmarksVisible(true);
                    map.setCartoMarkersVisible(true);
                    // map.setVisibleLayers(Map.LayerCategory.STREET_CATEGORY_0, true);

                    map.addTransformListener(transformListener);

                    posMan = PositioningManager.getInstance();
                    posMan.start(PositioningManager.LocationMethod.GPS_NETWORK);

                    VoiceNavigation.downloadTargetVoiceSkin(VoiceCatalog.getInstance());

                } else {
                    Log.e(Application.TAG, "ERROR: Cannot initialize Map Fragment");
                }
            }
        });

        tts = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                tts.setLanguage(Locale.ENGLISH);
                // This is for announcing presence of smart city data
                tts.addEarcon("smart_city", getPackageName(), R.raw.data2);
                tts.addEarcon("parking_mode", getPackageName(), R.raw.parking_mode);
                tts.addEarcon("ambient_area", getPackageName(), R.raw.ambientarea);
            }
        });

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {

            }

            @Override
            public void onDone(String utteranceId) {
                if(utteranceId.equals("Entity_End")) {
                    Application.isSpeaking = false;
                    Application.lastTimeSpeak = new DateTime().getMillis();
                    /*
                    map.setZoomLevel(currentZoomLevel,
                            map.projectToPixel(map.getCenter()).getResult(), Map.Animation.LINEAR);
                            */
                }
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }


    private void onCityDataReadyProcess(java.util.Map<String, List<Entity>> data,
                                        List<String> typesRequested, GeoPosition pos) {
       if (data.get(Application.RESULT_SET_KEY).size() == 0) {
           pendingSmartCityRequest = false;

           if (typesRequested.indexOf(Application.AMBIENT_AREA_TYPE) != -1) {
               Log.d(Application.TAG, "Ambient Area not found");
               ambientAreaData.id = null;
               ambientAreaData.polygon = null;
               if (ambientAreaData.view != null) {
                   map.removeMapObject(ambientAreaData.view);
               }
               ambientAreaData.view = null;
           }
           return;
       }

       if (data.get(Application.AMBIENT_AREA_TYPE) != null) {
           Entity ent = data.get(Application.AMBIENT_AREA_TYPE).get(0);
           Log.d(Application.TAG, "Ambient Area: " + ent.id);

           if (ambientAreaData.id == null || !ent.id.equals(ambientAreaData.id)) {
               if (ambientAreaData.view != null) {
                   map.removeMapObject(ambientAreaData.view);
               }
               ambientAreaData.id = ent.id;
               ambientAreaData.polygon = (GeoPolygon)ent.attributes.get("polygon");

               AmbientAreaRenderer ambientRenderer = new AmbientAreaRenderer(map, tts, ent,
                       findViewById(R.id.oascDataLayout), pos.getCoordinate());
               ambientRenderer.render(new AmbientAreaRenderListener() {
                   @Override
                   public void onRendered(String level, MapPolygon polygonView) {
                       pendingSmartCityRequest = false;
                       if (polygonView != null) {
                           ambientAreaData.view = polygonView;
                           mapObjects.add(polygonView);
                       }

                   }
               });

           } else {
               Log.d(Application.TAG, "Ambient Area remains the same: " + ambientAreaData.id);
               pendingSmartCityRequest = false;
           }
       }

       SmartCityRequest req = new SmartCityRequest();
       req.map = map;
       req.data = data;
       req.tts = tts;
       req.oascView = findViewById(R.id.oascDataLayout);

       Toast.makeText(getApplicationContext(), "SmartCity data on route",
               Toast.LENGTH_LONG).show();

       SmartCityHandler sch = new SmartCityHandler();
       sch.setListener(new RenderListener() {
           @Override
           public void onRendered(Object data, int num) {
               java.util.Map<String,Object> result = (java.util.Map<String,Object>)data;
               Entity forecast = (Entity)result.get(Application.WEATHER_FORECAST_ENTITY);
               if( forecast != null) {
                   Utilities.updateWeather(forecast.attributes, findViewById(R.id.oascDataLayout));
               }
               Utilities.WeatherObservedData weatherObs =
                       (Utilities.WeatherObservedData)result.get(Application.WEATHER_OBSERVED_REFRESH);
               if (weatherObs != null) {
                   Utilities.updateWeatherObserved(weatherObs, findViewById(R.id.oascDataLayout));
               }

               pendingSmartCityRequest = false;
           }
       });

       sch.execute(req);
    }

    public void setVoiceSkin(VoiceSkin vs) {
        voiceSkin = vs;

        if(underSimulation) {
            if(navMan != null) {
                navMan.setVoiceSkin(voiceSkin);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (routeWizard != null) {
                    routeWizard.back();
                } else if (marketplace != null) {
                    marketplace.back();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void goHome() {
        // Set the map center to Oporto center
        goTo(map, DEFAULT_COORDS, Map.Animation.LINEAR);
    }



    /**
     *   Stops navigation manager.
     *
     */
    private void stopNavigationManager() {
        if (navMan == null) {
            return;
        }

        if (navMan.getRunningState() != NavigationManager.NavigationState.IDLE) {
            navMan.stop();
        }
    }

    private void goTo(Map map, GeoCoordinate coordinates, Map.Animation animation) {
        map.setCenter(coordinates, animation, defaultZoomLevel, 0, map.getMaxTilt() / 2);
        map.setMapScheme(Map.Scheme.CARNAV_DAY);

        lastKnownPosition = coordinates;
    }

    private MapGesture.OnGestureListener gestureListener = new MapGesture.OnGestureListener() {
        @Override
        public void onPanStart() {

        }

        @Override
        public void onPanEnd() {

        }

        @Override
        public void onMultiFingerManipulationStart() {

        }

        @Override
        public void onMultiFingerManipulationEnd() {

        }

        @Override
        public boolean onMapObjectsSelected(List<ViewObject> list) {
            return false;
        }

        @Override
        public boolean onTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public boolean onDoubleTapEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onPinchLocked() {

        }

        @Override
        public boolean onPinchZoomEvent(float v, PointF pointF) {
            return false;
        }

        @Override
        public void onRotateLocked() {

        }

        @Override
        public boolean onRotateEvent(float v) {
            return false;
        }

        @Override
        public boolean onTiltEvent(float v) {
            return false;
        }

        @Override
        public boolean onLongPressEvent(PointF pointF) {
            return false;
        }

        @Override
        public void onLongPressRelease() {

        }

        @Override
        public boolean onTwoFingerTapEvent(PointF pointF) {
            return false;
        }
    };

    private void showPopupMenu(ImageButton b) {
        popupMenu.show();//showing popup menu
    }

    private void addMapWidgets() {
        FrameLayout container = (FrameLayout)findViewById(R.id.mainFrame);
        RelativeLayout rl2 = new RelativeLayout(this);
        RelativeLayout.LayoutParams relativeLayoutParams =
                new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
                        RelativeLayout.LayoutParams.MATCH_PARENT);

        container.addView(rl2, relativeLayoutParams);


        /*
        <ImageButton
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:adjustViewBounds="true"
        android:cropToPadding="false"
        android:scaleType="fitXY"
        android:id="@+id/currentLocButton"
        android:src="@drawable/current_location2" /> */

        locationButton = new ImageButton(this);
        locationButton.setAdjustViewBounds(true);
        locationButton.setCropToPadding(false);
        locationButton.setScaleType(ImageView.ScaleType.FIT_XY);
        locationButton.setImageDrawable(getResources().getDrawable(R.drawable.current_location2));
        locationButton.setBackground(null);

        menuButton = new ImageButton(this);
        menuButton.setAdjustViewBounds(true);
        menuButton.setCropToPadding(false);
        menuButton.setScaleType(ImageView.ScaleType.FIT_XY);
        menuButton.setImageDrawable(getResources().getDrawable(R.drawable.menu));
        menuButton.setBackground(null);

        Resources r = getResources();
        int px = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100,
                r.getDisplayMetrics());
        int px2 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60,
                r.getDisplayMetrics());
        RelativeLayout.LayoutParams params2 = new RelativeLayout.LayoutParams(px, px);
        RelativeLayout.LayoutParams params3 = new RelativeLayout.LayoutParams(px2, px2);

        DisplayMetrics metrics = r.getDisplayMetrics();
        params2.leftMargin = metrics.widthPixels - px;
        params2.topMargin = metrics.heightPixels - px;
        rl2.addView(locationButton, params2);

        params3.leftMargin = 0;
        params3.topMargin =  metrics.heightPixels - px2;
        rl2.addView(menuButton, params3);

        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                locationProgress = ProgressDialog.show(Application.mainActivity, "FIWARE-HERE",
                        "Obtaining current location", true);
                calculateCurrentPosition(Application.mainActivity);
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(menuButton);
            }
        });

        TextView scityTitle = new TextView(this);
        scityTitle.setBackgroundColor(Color.argb(80, 0, 0, 0));
        scityTitle.setText("Smart City");

        scityData = new TextView(this);

        scityData.setTextColor(Color.argb(80, 0, 0, 0));

        dataContainer = new LinearLayout(this);
        dataContainer.setBackgroundResource(R.drawable.rounded);
        dataContainer.setOrientation(LinearLayout.VERTICAL);

        RelativeLayout.LayoutParams paramsContainer = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        paramsContainer.leftMargin = 10;
        paramsContainer.topMargin = 100;
        dataContainer.addView(scityTitle);
        dataContainer.addView(scityData);
        // Not needed now
        // rl2.addView(dataContainer, paramsContainer);
        dataContainer.setVisibility(RelativeLayout.GONE);

        fiwareImage = new ImageView(this);
        fiwareImage.setAdjustViewBounds(true);
        fiwareImage.setCropToPadding(false);
        fiwareImage.setScaleType(ImageView.ScaleType.FIT_XY);
        fiwareImage.setImageDrawable(getResources().getDrawable(R.drawable.fiware));
        fiwareImage.setBackground(null);

        RelativeLayout.LayoutParams paramsLogo = new RelativeLayout.LayoutParams(160, 35);

        paramsLogo.leftMargin = 10;
        paramsLogo.topMargin = 65;
        rl2.addView(fiwareImage, paramsLogo);
        fiwareImage.setVisibility(RelativeLayout.GONE);

        councilLogo = new ImageView(this);
        councilLogo.setAdjustViewBounds(true);
        councilLogo.setCropToPadding(false);
        councilLogo.setScaleType(ImageView.ScaleType.FIT_XY);
        councilLogo.setBackground(null);

        RelativeLayout.LayoutParams paramsLogoCouncil = new RelativeLayout.LayoutParams(150, 75);
        paramsLogoCouncil.leftMargin = 10;
        paramsLogoCouncil.topMargin = 110;
        rl2.addView(councilLogo, paramsLogoCouncil);
        councilLogo.setVisibility(RelativeLayout.GONE);
    }

    public void calculateCurrentPosition(LocationListener callback) {
        posMan = PositioningManager.getInstance();
        boolean startResult = true;
        if (!posMan.isActive()) {
            startResult = posMan.start(PositioningManager.LocationMethod.GPS_NETWORK);
        }

        if(startResult == true) {
            try {
                LocationTask lt = new LocationTask();
                lt.setListener(callback);
                lt.execute(posMan);
            }
            catch(Exception e) {
                Log.e("FIWARE", "Error while obtaining location");
            }
        }
        else {
            Toast.makeText(getApplicationContext(),
                    "Location services not yet available", Toast.LENGTH_LONG).show();
        }
        /*
        PositioningManager.OnPositionChangedListener posManListener =
                new PositioningManager.OnPositionChangedListener() {
                    public void onPositionUpdated(PositioningManager.LocationMethod method,
                                                  GeoPosition position, boolean isMapMatched) {
                        if (!underSimulation) {
                            goTo(mapFragment.getMap(), position.getCoordinate(), Map.Animation.BOW);
                        }
                    }

                    public void onPositionFixChanged(PositioningManager.LocationMethod method,
                                                     PositioningManager.LocationStatus status) {
                    }
                };
        posMan.addListener(new WeakReference<PositioningManager.OnPositionChangedListener>(posManListener));
        */
    }

    public void onLocationReady(GeoCoordinate coords) {
        locationProgress.dismiss();
        goTo(mapFragment.getMap(), coords, Map.Animation.BOW);
    }

    @Override
    public void onPause() {
        detachNavigationListeners();
        if(posMan != null && posMan.isActive()) {
            posMan.stop();
        }

        if (navMan != null && navMan.getRunningState() == NavigationManager.NavigationState.RUNNING) {
            navMan.pause();
        }

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        if(posMan != null) {
            posMan.start(PositioningManager.LocationMethod.GPS_NETWORK);
        }

        if (navMan != null && navMan.getRunningState() == NavigationManager.NavigationState.PAUSED) {
            attachNavigationListeners();

            NavigationManager.Error error = navMan.resume();

            if (error != NavigationManager.Error.NONE) {
                Toast.makeText(getApplicationContext(),
                        "NavigationManager resume failed: " + error.toString(), Toast.LENGTH_LONG).show();
                return;
            }
        }
    }

    /**
     * Attaches listeners to navigation manager.
     */
    private void attachNavigationListeners() {
        if (navMan != null) {
            navMan.addPositionListener(
                    new WeakReference<NavigationManager.PositionListener>(m_navigationPositionListener));

            navMan.addNavigationManagerEventListener(
                    new WeakReference<NavigationManager.NavigationManagerEventListener>(m_navigationListener));
        }
    }

    /**
     * Detaches listeners from navigation manager.
     */
    private void detachNavigationListeners() {
        if (navMan != null) {
            navMan.removeNavigationManagerEventListener(m_navigationListener);
            navMan.removePositionListener(m_navigationPositionListener);
        }
    }

    public void terminateSimulation() {
        navMan.stop();
        clearMap();

        doTerminateSimulation();
    }


    public void pauseSimulation() {
        if(navMan != null) {
           if (navMan.getRunningState() == NavigationManager.NavigationState.RUNNING) {
               popupMenu.getMenu().findItem(R.id.action_pause).setTitle("Resume Simulation");
               navMan.pause();
           }
           else if (navMan.getRunningState() == NavigationManager.NavigationState.PAUSED) {
               popupMenu.getMenu().findItem(R.id.action_pause).setTitle("Pause Simulation");
               navMan.resume();
           }
        }
    }

    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        clearMap();

        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);
        routeWizard = new RouteActivity(getApplicationContext());

        routeWizard.start();
    }

    public void showMarketplace() {
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);
        marketplace = new MarketActivity(getApplicationContext());

        marketplace.start();
    }

    public void onRouteReady(RouteData r) {
        double[] newDefaultCoords = RouteActivity.cityCoords.get(r.city);
        DEFAULT_COORDS = new GeoCoordinate(newDefaultCoords[0], newDefaultCoords[1]);

        ViewGroup rootContainer = (ViewGroup)findViewById(R.id.mainFrame);
        rootContainer.removeViewAt(2);

        routeData = r;
        routeWizard = null;

        showRoute();
    }

    private void onMenuBack() {
        ViewGroup rootContainer = (ViewGroup)findViewById(R.id.mainFrame);

        rootContainer.removeViewAt(2);

        InputMethodManager mgr = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(findViewById(android.R.id.content).getWindowToken(), 0);
    }
    public void onMarketplaceClosed() {
        marketplace = null;
        onMenuBack();
    }

    public void onRouteCanceled() {
        routeWizard = null;
        onMenuBack();
    }

    private void clearMap() {
        map.removeMapObjects(mapObjects);
        mapObjects.clear();
        map.setZoomLevel(defaultZoomLevel);
    }

    private void showRoute() {
        GeoBoundingBox gbb = routeData.route.getBoundingBox();

        map.setZoomLevel(defaultZoomLevel);
        map.zoomTo(gbb, Map.Animation.LINEAR, Map.MOVE_PRESERVE_ORIENTATION);

        GeoCoordinate start = routeData.route.getStart();
        Image startImg = new Image();
        try {
            startImg.setImageResource(R.drawable.start);
        }
        catch(IOException e) {
            System.err.println("Cannot load image");
        }
        MapMarker startMarker = new MapMarker(start, startImg);
        map.addMapObject(startMarker);

        Image car = new Image();
        try {
            car.setImageResource(R.drawable.car);
        }
        catch(IOException e) {
            System.err.println("Cannot load image");
        }

        GeoCoordinate end = routeData.route.getDestination();
        Image endImg = new Image();
        try {
            endImg.setImageResource(R.drawable.end);
        }
        catch(IOException e) {
            System.err.println("Cannot load image");
        }
        MapMarker endMarker = new MapMarker(end, endImg);
        map.addMapObject(endMarker);

        MapRoute route = new MapRoute(routeData.route);

        map.addMapObject(route);

        state = "zoomToRouteBB";

        mapObjects.add(startMarker);
        mapObjects.add(endMarker);
        mapObjects.add(route);

        if(loopMode) {
            final int interval2 = 7000; // 7 Second
            Handler handler2 = new Handler();
            Runnable runnable2 = new Runnable() {
                public void run() {
                   startSimulation(null);
                }
            };
            handler2.postDelayed(runnable2, interval2);
        }
    }


    // Called on UI thread
    private final NavigationManager.PositionListener
                        m_navigationPositionListener = new NavigationManager.PositionListener() {
        @Override
        public void onPositionUpdated(final GeoPosition loc) {
            updateNavigationInfo(loc);
        }
    };

    private void showParkingData(String text) {
        parkingData.setText(text);
        parkingData.setVisibility(RelativeLayout.VISIBLE);
        parkingSign.setVisibility(RelativeLayout.VISIBLE);
        nextRoad.setLayoutParams(new LinearLayout.LayoutParams(0,
                RelativeLayout.LayoutParams.MATCH_PARENT, 50));
    }

    private void hideParkingData() {
        parkingData.setText("");
        parkingData.setVisibility(RelativeLayout.GONE);
        parkingSign.setVisibility(RelativeLayout.GONE);
        nextRoad.setLayoutParams(new LinearLayout.LayoutParams(0,
                RelativeLayout.LayoutParams.MATCH_PARENT, 100));
    }

    private void handleParkingMode(final GeoCoordinate coord, final long distance) {
        if (!parkingFound && !pendingParkingRequest
                && parkingRadius < Application.MAX_PARKING_DISTANCE) {
            final CityDataRequest reqData = new CityDataRequest();
            reqData.radius = parkingRadius / 2;
            reqData.coordinates = new double[]{
                    routeData.destinationCoordinates.getLatitude(),
                    routeData.destinationCoordinates.getLongitude()
            };

            reqData.types = new ArrayList<>(routeData.parkingCategory);
            if(reqData.types.size() == 0) {
                reqData.types.add(Application.PARKING_TYPE);
            }
            reqData.types.add(Application.PARKING_RESTRICTION_TYPE);

            reqData.token = getUserToken();

            Log.d(Application.TAG, "Going to retrieve parking data ...");
            CityDataRetriever retriever = new CityDataRetriever();
            retriever.setListener(new CityDataListener() {
                @Override
                public void onCityDataReady(java.util.Map<String, List<Entity>> data) {
                    if(data.size() == 0) {
                        Log.d(Application.TAG, "No parking data found.");
                        pendingParkingRequest = false;
                        parkingRadius += 100;
                        return;
                    }

                    Log.d(Application.TAG, "Parking data available: " + data.size());
                    ParkingRenderer.render(getApplicationContext(), map,
                            data.get(Application.RESULT_SET_KEY));

                    List<Entity> parkingLots = data.get(Application.PARKING_LOT_TYPE);
                    List<Entity> parkingLotZones = data.get(Application.PARKING_LOT_ZONE_TYPE);
                    List<Entity> streetParkings = data.get(Application.STREET_PARKING_TYPE);

                    if (parkingLots == null) {
                        parkingLots = new ArrayList<>();
                        if (parkingLotZones != null) {
                            parkingLots.addAll(parkingLotZones);
                        }
                    }

                    if (streetParkings == null) {
                        streetParkings = new ArrayList<>();
                    }

                    // parking Lots have to be reordered
                    // CB providing parking lots do not order by distance
                    Collections.sort(parkingLots, new Comparator<Entity>() {
                        @Override
                        public int compare(Entity lhs, Entity rhs) {
                            double lhsDistance = (new GeoCoordinate(
                                    lhs.location[0], lhs.location[1])).distanceTo(routeData.destinationCoordinates);
                            double rhsDistance = (new GeoCoordinate(
                                    rhs.location[0], rhs.location[1])).distanceTo(routeData.destinationCoordinates);

                            return Double.compare(lhsDistance, rhsDistance);
                        }
                    });

                    boolean parkingReady = false;
                    int streetIndex = 0; int lotIndex = 0;
                    Entity targetParking = null;
                    Entity alternativeParking = null;
                    String typeUnknown = "";
                    int spotNumberUnknown = -1;

                    while (!parkingReady) {
                        Entity streetParking = null;
                        Entity parkingLot = null;

                        if (streetParkings.size() > streetIndex) {
                            streetParking = streetParkings.get(streetIndex++);
                        }

                        if (parkingLots.size() > lotIndex) {
                            parkingLot = parkingLots.get(lotIndex++);
                        }

                        double distanceToStreet = Double.MAX_VALUE, distanceToLot = Double.MAX_VALUE;

                        // Only distances are calculated if there are both kind of parkings
                        if (streetParking != null && parkingLot != null) {
                            distanceToStreet =  (new GeoCoordinate(streetParking.location[0],
                                    streetParking.location[1])).distanceTo(routeData.destinationCoordinates);
                            distanceToLot = (new GeoCoordinate(parkingLot.location[0],
                                    parkingLot.location[1])).distanceTo(routeData.destinationCoordinates);
                        }
                        else if (parkingLot != null) {
                            distanceToLot = 0;
                            distanceToStreet = Double.MAX_VALUE;
                        }
                        else if (streetParking != null) {
                            distanceToStreet = 0;
                            distanceToLot = Double.MAX_VALUE;
                        }
                        else {
                            break;
                        }

                        Entity candidate = parkingLot;
                        int indexCandidate = lotIndex - 1;
                        if (distanceToStreet < distanceToLot) {
                            candidate = streetParking;
                            indexCandidate = streetIndex - 1;
                        }

                        // Workaround for parkings without attributes
                        if(candidate.attributes == null) {
                            candidate.attributes = new HashMap();
                        }

                        Integer availableSpotNumber =
                                (Integer)candidate.attributes.get("availableSpotNumber");

                        if (availableSpotNumber != null) {
                           if (availableSpotNumber > 1) {
                               targetParking = candidate;
                               parkingReady = true;
                           }
                            else if (availableSpotNumber == 1) {
                                alternativeParking = candidate;
                            }
                        }
                        else {
                            typeUnknown =  candidate.type;
                            spotNumberUnknown = indexCandidate;
                        }
                    }


                    if (targetParking == null) {
                        if (alternativeParking != null) {
                            targetParking = alternativeParking;
                        } else if (spotNumberUnknown != -1) {
                            if (Application.STREET_PARKING_TYPE.equals(typeUnknown)) {
                                targetParking = streetParkings.get(spotNumberUnknown);
                            }
                            else if (Application.PARKING_LOT_TYPE.equals(typeUnknown)) {
                                targetParking = parkingLots.get(spotNumberUnknown);
                            }
                        }
                        else {
                            Log.d(Application.TAG, "No target parking found");
                            pendingParkingRequest = false;
                            parkingRadius += 100;
                            return;
                        }
                    }


                    if (targetParking.type.equals(Application.STREET_PARKING_TYPE)) {
                        ReverseGeocodeRequest2 req = new ReverseGeocodeRequest2(
                                new GeoCoordinate(targetParking.location[0], targetParking.location[1]));
                        req.execute(new ResultListener<Location>() {
                            @Override
                            public void onCompleted(Location location, ErrorCode errorCode) {
                                Address address = location.getAddress();
                                String parkingAddress = address.getStreet();
                                if (address.getHouseNumber() != null && address.getHouseNumber().length() > 0) {
                                    parkingAddress += ", " + address.getHouseNumber();
                                }

                                ParkingRenderer.announceParking(tts, parkingAddress);
                                routeData.parkingAddress = parkingAddress;
                                showParkingData(parkingAddress);
                            }
                        });
                    } else {
                        String parkingName = (String) targetParking.attributes.get("name");
                        String parkingDescr = (String) targetParking.attributes.get("description");
                        if (parkingDescr != null && parkingDescr.length() < 25) {
                            parkingName = parkingDescr;
                        }
                        ParkingRenderer.announceParking(tts, parkingName);
                        routeData.parkingAddress = parkingName;
                        showParkingData(parkingName);
                    }

                    routeData.parkingCoordinates = new GeoCoordinate(targetParking.location[0],
                            targetParking.location[1]);

                    ParkingRouteCalculator parkingRoute = new ParkingRouteCalculator();
                    ParkingRouteData prd = new ParkingRouteData();
                    prd.origin = coord;
                    prd.parkingDestination = routeData.parkingCoordinates;

                    parkingRoute.setListener(new RouteCalculationListener() {
                        @Override
                        public void onRouteReady(Route r) {
                            MapRoute mr = new MapRoute(r);
                            mr.setColor(Color.parseColor("#73C2FB"));
                            map.addMapObject(mr);
                            navMan.setRoute(r);
                            mapObjects.add(mr);
                        }
                    });
                    parkingRoute.execute(prd);

                    parkingFound = true;
                    pendingParkingRequest = false;
                }
            });

            pendingParkingRequest = true;
            Log.d(Application.TAG, "Asking parking data in a radius of: " + parkingRadius);
            retriever.execute(reqData);
        }
        else if (parkingRadius >= Application.MAX_PARKING_DISTANCE) {
            parkingFound = true;
            parkingData.setText("No suitable parking found");
        }
    }

    private void updateNavigationInfo(final GeoPosition loc) {
        // Update the average speed
        int avgSpeed = (int) loc.getSpeed();
        currentSpeed.setText(String.format("%d km/h", (int) (avgSpeed * 3.6)));

        // Update ETA
        SimpleDateFormat sdf = new SimpleDateFormat("k:mm", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());

        Date ETADate = navMan.getEta(true, Route.TrafficPenaltyMode.DISABLED);
        ETA.setText(sdf.format(ETADate));

        distance.setText(Utilities.formatDistance(navMan.getDestinationDistance()));
        Maneuver nextManeuver = navMan.getNextManeuver();

        if (nextManeuver != null) {
            currentRoad.setText(nextManeuver.getRoadName());
            nextRoad.setText(nextManeuver.getNextRoadName());
            nextManouverDistance.setText(
                                    Utilities.formatDistance(navMan.getNextManeuverDistance()));

            int id = getResources().getIdentifier(nextManeuver.getTurn().name().toLowerCase(),
                    "drawable", getPackageName());
            // Returns 0 if not found
            if(id != 0) {
                turnNavigation.setImageResource(id);
            }

        }

        handleSmartCity(loc);
    }

    private void handleSmartCity(GeoPosition loc) {
        long currentDistance = navMan.getDestinationDistance();

        if (currentDistance <= 0) {
            return;
        }

        if(currentDistance <= Application.PARKING_DISTANCE) {
            if(!inParkingMode) {
                ParkingRenderer.announceParkingMode(tts);
                parkingRadius = routeData.parkingDistance;
                showParkingData("Searching ... ");
                double zoom = map.getZoomLevel();
                map.setZoomLevel(zoom + 0.7);
            }
            inParkingMode = true;
        }

        if(inParkingMode) {
            handleParkingMode(loc.getCoordinate(), currentDistance);
        }


        // Depending on whether there is a current ambient area or not the check is different
        int threshold =  Application.DISTANCE_FREQ_AMBIENT_AREA;
        if (ambientAreaData.id == null) {
            threshold = Application.AMBIENT_AREA_RADIUS - 100;
        }

        if ( (previousDistanceArea  - currentDistance) > threshold) {
            if (ambientAreaData.polygon == null ||
                    !ambientAreaData.polygon.contains(loc.getCoordinate())) {

                Log.d(Application.TAG, "Now out of the ambient Area");

                if (!pendingSmartCityRequest) {
                    String[] types = { Application.AMBIENT_AREA_TYPE };
                    doExecuteDataRequest(Arrays.asList(types), -1, loc, "intersects");
                } else {
                    Log.d(Application.TAG,
                            "Not checking ambient area because pending smart city request");
                }
            }
            else {
                Log.d(Application.TAG, "Ambient area remains the same: " + ambientAreaData.id);
            }
            previousDistanceArea = currentDistance;
        }

        if ( (currentDistance < Application.THRESHOLD_DISTANCE &&
                (previousDistance  - currentDistance > (Application.DEFAULT_RADIUS - 100)
                        || previousDistance == 0)) && !pendingSmartCityRequest) {
            previousDistance = currentDistance;

            List<String> types = Arrays.asList(
                    Application.PARKING_TYPE,
                    Application.AMBIENT_OBSERVED_TYPE,
                    Application.ANY_ENTITY_TYPE
            );
            executeDataRequest(types, Application.DEFAULT_RADIUS, loc);
        }
    }

    private void executeDataRequest(final List<String> types, int radius, final GeoPosition loc) {
        doExecuteDataRequest(types, radius, loc, "near");
    }

    private void doExecuteDataRequest(final List<String> types, int radius, final GeoPosition loc,
                                      String georel) {
        CityDataRequest reqData = new CityDataRequest();
        reqData.georel = georel;
        reqData.radius = radius;
        reqData.coordinates = new double[]{loc.getCoordinate().getLatitude(),
                loc.getCoordinate().getLongitude()};

        for (String type : types) {
            reqData.types.add(type);
        }

        Log.d(Application.TAG, "Going to retrieve data ..." + reqData.types);
        CityDataRetriever retriever = new CityDataRetriever();
        retriever.setListener(new CityDataListener() {
            @Override
            public void onCityDataReady(java.util.Map<String, List<Entity>> data) {
                onCityDataReadyProcess(data, types, loc);
            }
        });

        reqData.token = getUserToken();

        pendingSmartCityRequest = true;
        retriever.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, reqData);
    }

    private String getUserToken() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(Application.BF_TOKEN, "");
    }

    private void doTerminateSimulation() {
        underSimulation = false;

        detachNavigationListeners();

        navMan.setMapUpdateMode(NavigationManager.MapUpdateMode.POSITION);
        navMan.setTrafficAvoidanceMode(NavigationManager.TrafficAvoidanceMode.DISABLE);
        navMan.setMap(null);

        hideNavigationUI();
        // Allow to play the same route again
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, true);

        ambientAreaData = new AmbientAreaData();
        Application.lastTimeSpeak = -1;
        Application.isSpeaking = false;
        Application.lastTimeBubble = -1;

        previousDistance = 0;
        if(loopMode) {
            final int interval2 = 7000; // 7 Second
            Handler handler2 = new Handler();
            Runnable runnable2 = new Runnable() {
                public void run() {
                    clearMap();
                    showRoute();
                }
            };
            handler2.postDelayed(runnable2, interval2);
        }
    }

    // Called on UI thread
    private final NavigationManager.NavigationManagerEventListener m_navigationListener =
                                            new NavigationManager.NavigationManagerEventListener() {
        @Override
        public void onEnded(final NavigationManager.NavigationMode mode) {
            // NOTE: this method is called in both cases when destination
            // is reached and when NavigationManager is stopped.
            Toast.makeText(getApplicationContext(),
                    "Destination reached!", Toast.LENGTH_LONG).show();

            doTerminateSimulation();

            doTransferRoute();
        }

        private void doTransferRoute() {
            RouteTransfer transferTask = new RouteTransfer();
            Handler handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    int result = msg.getData().getInt(Application.TRANSFER_RESULT);
                    String text = "";
                    if(result == 0) {
                        text = "Route transferred OK";
                    }
                    else {
                        text = "Route transfer error";
                    }
                    Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                }
            };

            transferTask.setHandler(handler);
            transferTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, routeData);
        }

        @Override
        public void onRouteUpdated(final Route updatedRoute) {
        }
    };

    private void showNavigationUI() {
        findViewById(R.id.routePlanningLayout).setVisibility(RelativeLayout.GONE);

        findViewById(R.id.nextRoadLayout).setVisibility(RelativeLayout.VISIBLE);
        findViewById(R.id.navigationLayout).setVisibility(RelativeLayout.VISIBLE);

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0.82f);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.mainMapLayout);
        mapLayout.setLayoutParams(layoutParams1);

        RelativeLayout innerMapLayout = (RelativeLayout)findViewById(R.id.innerMapLayout);
        LinearLayout.LayoutParams layoutParamsInner = new LinearLayout.LayoutParams(
                 0, RelativeLayout.LayoutParams.MATCH_PARENT, 0.90f);
        innerMapLayout.setLayoutParams(layoutParamsInner);

        popupMenu.getMenu().setGroupVisible(R.id.simulationGroup, true);
        popupMenu.getMenu().setGroupVisible(R.id.initialGroup, false);

        fiwareImage.setVisibility(RelativeLayout.VISIBLE);
        councilLogo.setVisibility(RelativeLayout.VISIBLE);

        int id = getResources().getIdentifier(routeData.city.toLowerCase(), "drawable", getPackageName());
        if (id != 0) {
            councilLogo.setImageDrawable(getDrawable(id));
        }
        else {
            councilLogo.setImageDrawable(null);
        }
    }

    private void hideNavigationUI() {
        locationButton.setVisibility(RelativeLayout.VISIBLE);
        fiwareImage.setVisibility(RelativeLayout.GONE);
        councilLogo.setVisibility(RelativeLayout.GONE);

        findViewById(R.id.nextRoadLayout).setVisibility(RelativeLayout.GONE);
        findViewById(R.id.navigationLayout).setVisibility(RelativeLayout.GONE);

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.mainMapLayout);
        mapLayout.setLayoutParams(layoutParams1);
        mapLayout.requestLayout();

        RelativeLayout innerMapLayout = (RelativeLayout)findViewById(R.id.innerMapLayout);
        LinearLayout.LayoutParams layoutParamsInner = new LinearLayout.LayoutParams(
                0, RelativeLayout.LayoutParams.MATCH_PARENT, 1.0f);
        innerMapLayout.setLayoutParams(layoutParamsInner);
        findViewById(R.id.oascDataLayout).setVisibility(RelativeLayout.GONE);
        findViewById(R.id.airQualityGroup).setVisibility(RelativeLayout.GONE);
        ((LinearLayout)findViewById(R.id.airQualityPollutants)).removeAllViews();

        if(popupMenu != null) {
            popupMenu.getMenu().setGroupVisible(R.id.initialGroup, true);
            popupMenu.getMenu().setGroupVisible(R.id.simulationGroup, false);
        }

        hideParkingData();
    }

    private void showRoutePlanningUI() {
        locationButton.setVisibility(RelativeLayout.GONE);

        ((RelativeLayout)findViewById(R.id.routePlanningLayout)).setVisibility(RelativeLayout.VISIBLE);

        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, 0, 0.88f);

        RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.mainMapLayout);
        mapLayout.setLayoutParams(layoutParams1);
        mapLayout.requestLayout();
        mapLayout.getParent().requestLayout();

        RouteTta tta = routeData.route.getTta(Route.TrafficPenaltyMode.DISABLED, 0);
        int duration = tta.getDuration();
        Date date = new Date();
        date.setTime(date.getTime() + duration * 1000);

        SimpleDateFormat sdf = new SimpleDateFormat("k:mm", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getDefault());

        String arrival = sdf.format(date);
        ((TextView) findViewById(R.id.ETAInfo)).setText(arrival);

        ((TextView) findViewById(R.id.distanceInfo)).setText(Utilities.
                formatDistance(routeData.route.getLength()));

        ((TextView) findViewById(R.id.routeDestination)).setText(routeData.destination);

        state = "";
    }

    public void startSimulation(View v) {
        map.setCenter(routeData.route.getStart(), Map.Animation.BOW, routeZoomLevel,
                map.getOrientation(), map.getTilt());

        state = "GoingToDeparture";
    }

    /**
     *   Starts guidance simulation.
     *
     */
    private void startGuidance(Route route) {
        state = "";
        inParkingMode = false;
        parkingFound = false;

        pendingParkingRequest = false;
        pendingSmartCityRequest = false;

        Application.renderedEntities.clear();

        showNavigationUI();

        if (navMan == null) {
            // Setup navigation manager
            navMan = NavigationManager.getInstance();
        }

        attachNavigationListeners();

        navMan.setMap(map);

        navMan.setMapUpdateMode(NavigationManager.MapUpdateMode.POSITION_ANIMATION);

        if(voiceSkin != null) {
            navMan.setVoiceSkin(voiceSkin);
        }

        // We set this variable to avoid nay kind of request before the area request
        pendingSmartCityRequest = true;

        // Start navigation simulation
        NavigationManager.Error error = navMan.simulate(route, Application.DEFAULT_SPEED);
        if (error != NavigationManager.Error.NONE) {
            Toast.makeText(getApplicationContext(),
                     "Failed to start navigation. Error: " + error, Toast.LENGTH_LONG).show();
            navMan.setMap(null);
            underSimulation = false;
            return;
        }

        // Allow to play the same route again
        popupMenu.getMenu().setGroupVisible(R.id.restartGroup, false);

        underSimulation = true;
        navMan.setNaturalGuidanceMode(
                EnumSet.of(NavigationManager.NaturalGuidanceMode.JUNCTION));

        // We try to obtain the ambient zone and weather, first
        String[] types = { Application.AMBIENT_AREA_TYPE, Application.WEATHER_FORECAST_TYPE };
        previousDistanceArea = routeData.route.getLength();
        executeDataRequest(Arrays.asList(types),
                Application.AMBIENT_AREA_RADIUS, new GeoPosition(route.getStart()));
    }

    private Map.OnTransformListener transformListener = new Map.OnTransformListener() {
        @Override
        public void onMapTransformStart() {
        }

        public void onMapTransformEnd(MapState mapState) {
            if(state.equals("zoomToRouteBB")) {
                showRoutePlanningUI();
                // Workaround to deal with resize problems
                map.pan(new PointF(200,200), new PointF(210,150));
            }
            else if(state.equals("GoingToDeparture")) {
                startGuidance(routeData.route);
                currentZoomLevel = mapState.getZoomLevel();
            }
        }
    };
}
