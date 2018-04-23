package tk.maikator.mygeoplugin;

import android.Manifest;
import android.content.pm.PackageManager;

import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;

import com.google.gson.JsonObject;
import com.mapbox.mapboxsdk.Mapbox;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.geojson.GeoJsonPlugin;
import com.mapbox.mapboxsdk.plugins.geojson.GeoJsonPluginBuilder;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnLoadingGeoJsonListener;
import com.mapbox.mapboxsdk.plugins.geojson.listener.OnMarkerEventListener;

import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.location.LocationEngineProvider;
import com.mapbox.services.android.telemetry.permissions.PermissionsListener;
import com.mapbox.services.android.telemetry.permissions.PermissionsManager;

import java.io.File;
import java.util.List;


import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback,
        LocationEngineListener,PermissionsListener,
        OnLoadingGeoJsonListener, OnMarkerEventListener, FileChooserDialog.FileCallback {

    private CoordinatorLayout coordinatorLayout;
    private MapView mapView;
    private MapboxMap mapboxMap;
    private GeoJsonPlugin geoJsonPlugin;
    private ProgressBar progressBar;
    private FloatingActionButton urlFab;
    private FloatingActionButton assetsFab;
    private FloatingActionButton pathFab;

    private LocationEngine locationEngine;
    private LocationLayerPlugin locationPlugin;
    private PermissionsManager permissionsManager;

    // 烟台地区的矩形范围
    private static final LatLngBounds YANTAI_CITY_BOUNDS = new LatLngBounds.Builder()
            .include(new LatLng(38.4022, 119.5582))
            .include(new LatLng(36.5732,121.9295)).build();



    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);


        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);
        setUpFabButtons();
        progressBar = (ProgressBar) findViewById(R.id.geoJSONLoadProgressBar);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);
        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {
        MainActivity.this.mapboxMap = mapboxMap;

        // 区域限制为`烟台`
        mapboxMap.setLatLngBoundsForCameraTarget(YANTAI_CITY_BOUNDS);
        mapboxMap.setMinZoomPreference(7); // 级别7，对于烟台是合适

        // 可视化限制区域
        // 在调试时，看效果使用；真正的给客户用到时候，不要显示出来。
        // showBoundsArea();

        // 显示`十字花`
        showCrosshair();


        // add geoJsonPlugin
        geoJsonPlugin = new GeoJsonPluginBuilder()
                .withContext(this)
                .withMap(mapboxMap)
                .withOnLoadingURL(this)
                .withOnLoadingFileAssets(this)
                .withOnLoadingFilePath(this)
                .withMarkerClickListener(this)
                .build();

        // enable LocationPlungin
        enableLocationPlugin();
    }

    private void setUpFabButtons() {
        urlFab = (FloatingActionButton) findViewById(R.id.fabURL);
        assetsFab = (FloatingActionButton) findViewById(R.id.fabAssets);
        pathFab = (FloatingActionButton) findViewById(R.id.fabPath);
        onUrlFabClick();
        onAssetsFabClick();
        onPathFabClick();
    }

    private void onUrlFabClick() {
        urlFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null && geoJsonPlugin != null) {
                    mapboxMap.clear();
                    geoJsonPlugin.setUrl("https://raw.githubusercontent.com/johan/world.geo.json/master/countries/SEN.geo.json");
                }
            }
        });
    }

    private void onAssetsFabClick() {
        assetsFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mapboxMap != null && geoJsonPlugin != null) {
                    mapboxMap.clear();
                    geoJsonPlugin.setAssetsName("boston_police_stations.geojson");
                }
            }
        });
    }

    private void onPathFabClick() {
        pathFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= 23) {
                    if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        Timber.v("Permission is granted");
                        showFileChooserDialog();
                        Toast.makeText(MainActivity.this, R.string.find_file_instruction_toast,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Timber.v("Permission is revoked");
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                    }
                } else { //permission is automatically granted on sdk<23 upon installation
                    Timber.v("Permission is granted");
                    showFileChooserDialog();
                }
            }
        });
    }

    /**
     * Draws GeoJSON file from a specific path. Please add and locate a GeoJSON file in your device to test it.
     *
     * @param file selected file from external storage
     */
    private void drawFromPath(File file) {
        String path = file.getAbsolutePath();
        if (mapboxMap != null && geoJsonPlugin != null) {
            mapboxMap.clear();
            geoJsonPlugin.setFilePath(path);
        }
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (locationPlugin != null) {
            locationPlugin.onStart();
        }
        mapView.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        if (locationPlugin != null) {
            locationPlugin.onStop();
        }
        mapView.onStop();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }



    private void showFileChooserDialog() {
        new FileChooserDialog.Builder(this)
                .extensionsFilter(".geojson", ".json", ".js", ".txt")
                .goUpLabel("Up")
                .show(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case 1: {
                // from GeoJsonPluginActivity.java
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.v("Permission: " + permissions[0] + "was " + grantResults[0]);
                    showFileChooserDialog();
                }
                return;
            }

            case 0:
            {
                // from LocationPluginActivity.java
                permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
                return;
            }
        }
    }

    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {
        drawFromPath(file);
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    @Override
    public void onPreLoading() {
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaded() {
        Toast.makeText(this, "GeoJson data loaded", Toast.LENGTH_LONG).show();
        progressBar.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onLoadFailed(Exception exception) {
        progressBar.setVisibility(View.INVISIBLE);
        Toast.makeText(this, "Error occur during load GeoJson data. see logcat", Toast.LENGTH_LONG).show();
        exception.printStackTrace();
    }

    @Override
    public void onMarkerClickListener(Marker marker, JsonObject properties) {
        Snackbar.make(coordinatorLayout, properties.get("NAME").getAsString(), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            setCameraPosition(location);
            locationEngine.removeLocationEngineListener(this);
        }
    }


    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }


    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            // Create a location engine instance
            initializeLocationEngine();

            locationPlugin = new LocationLayerPlugin(mapView, mapboxMap, locationEngine);
            locationPlugin.setLocationLayerEnabled(LocationLayerMode.TRACKING);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @SuppressWarnings( {"MissingPermission"})
    private void initializeLocationEngine() {
        locationEngine = new LocationEngineProvider(this).obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            setCameraPosition(lastLocation);
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    private void setCameraPosition(Location location) {
        mapboxMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(), location.getLongitude()), 16));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map_style, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection

        if (item.getItemId() == R.id.menu_streets) {
            mapboxMap.setStyleUrl(getString(R.string.MAIKATOR_MAPBOX_STREETS));
            return true;
        } else if (item.getItemId() == R.id.menu_satellite_streets) {
            mapboxMap.setStyleUrl(Style.SATELLITE_STREETS);
            return true;
        }

        return true;
    }


    // 在地图上，显示`+`-十字标记，用于定位地图中央位置。
    private void showCrosshair() {
        View crosshair = new View(this);
        crosshair.setLayoutParams(new FrameLayout.LayoutParams(15, 15, Gravity.CENTER));
        crosshair.setBackgroundColor(Color.GREEN);
        mapView.addView(crosshair);
    }


    // 在地图上，显示矩形区域
    private void showBoundsArea() {
        PolygonOptions boundsArea = new PolygonOptions()
                .add(YANTAI_CITY_BOUNDS.getNorthWest())
                .add(YANTAI_CITY_BOUNDS.getNorthEast())
                .add(YANTAI_CITY_BOUNDS.getSouthEast())
                .add(YANTAI_CITY_BOUNDS.getSouthWest());
        boundsArea.alpha(0.25f);
        boundsArea.fillColor(Color.RED);
        mapboxMap.addPolygon(boundsArea);
    }





}


