package de.unipassau.ieee.weatherDemo;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import org.json.JSONObject;

public class MainActivity extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocationForWeather();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // Weather API

    /**
     * Request a single GPS update from the LocationManager.
     * Once the new Location is received by the Listener,
     * fetch the weather for the new location using {@link #fetchWeather(double, double)}.
     */
    private void updateLocationForWeather() {
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //Fetch weather for new Location
                fetchWeather(location.getLatitude(), location.getLongitude());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };
        //Request update
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestSingleUpdate(new Criteria(), listener, getMainLooper());
    }

    /**
     * Use the asynchronous {@link FetchWeatherTask} to get the current weather
     * for the given coordinates and update the UI.
     */
    private void fetchWeather(double lat, double lon) {
        new FetchWeatherTask() {
            /**
             * Cancel the weather update if no internet is available.
             */
            @Override
            protected void onPreExecute() {
                //Cancel if not network is available
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable() || !activeNetworkInfo.isConnected()) {
                    cancel(true);
                }
            }

            /**
             * Update the UI once this Task finishes.
             */
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                //Update the view
                if (jsonObject != null) {
                    ((TextView) findViewById(R.id.weatherText)).setText(jsonObject.toString());
                }
            }
        }.execute(lat, lon);
    }
}
