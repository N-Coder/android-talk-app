package de.unipassau.ieee.weatherDemo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;


public class MainActivity extends Activity {

    private TextView  textViewTemperature;
    private TextView  textViewWeatherStatus;
    private TextView  textViewCity;
    private TextView  textViewWind;
    private TextView  textViewCloudiness;
    private TextView  textViewHumidity;
    private ImageView imageViewWeatherStatusIcon;
    private Button    buttonRefresh;
    private Location  latestLocation;
    private long      sunrise;
    private long      sunset;

    /**
     * onCreate(...) is called by the Android system when the activity is created. As MainActivity is the launching activity, this method is called upon program startup.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // define the layout for this activity
        setContentView(R.layout.activity_main);

        // get a reference to the view elements
        textViewTemperature = (TextView) findViewById(R.id.textViewTemperature);
        textViewWeatherStatus = (TextView) findViewById(R.id.textViewWeatherStatus);
        textViewCity = (TextView) findViewById(R.id.textViewCity);
        textViewWind = (TextView) findViewById(R.id.textViewWind);
        textViewCloudiness = (TextView) findViewById(R.id.textViewCloudiness);
        textViewHumidity = (TextView) findViewById(R.id.textViewHumidity);
        imageViewWeatherStatusIcon = (ImageView) findViewById(R.id.imageViewWeatherStatusIcon);
        buttonRefresh = (Button) findViewById(R.id.buttonRefresh);

        // react to clicks/taps on the "Refresh" button
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocationForWeather();
            }
        });
    }

    /**
     * onResume(...) is called by the Android system when an activity has been paused and then is resumed
     * (e.g. when the user switches to another app and then switches back to
     * this app). Furthermore, onResume(...) is always called directly after onCreate(...).
     */
    @Override
    protected void onResume() {
        super.onResume();
        updateLocationForWeather();
    }

    /**
     * onCreateOptionsMenu(...) is called by the Android system when the menu (usually in the upper right corner) is created.
     * We can choose, which resource file should be used to inflate the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * onOptionsItemSelected(...) is called by the Android system whenever the user clicks on a menu item. The clicked menu item is given as an parameter.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        // If the menu item "location" is selected, start a new activity to display some information about the current location.
        if (id == R.id.action_location) {
            //create an Intent to start the new activity
            Intent locationIntent = new Intent(this, LocationActivity.class);

            // create a Bundle to give some arguments to the activity
            Bundle arguments = new Bundle();
            arguments.putDouble(LocationActivity.KEY_LATITUDE, latestLocation.getLatitude());
            arguments.putDouble(LocationActivity.KEY_LONGITUDE, latestLocation.getLongitude());
            arguments.putLong(LocationActivity.KEY_SUNRISE, sunrise);
            arguments.putLong(LocationActivity.KEY_SUNSET, sunset);

            // start the Activity with the prepared arguments
            locationIntent.putExtras(arguments);
            startActivity(locationIntent);

            // Return true, as the click has been handled/consumed.
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
                //save this location as the latest used location
                latestLocation = location;

                //Fetch weather for new Location
                Toast.makeText(MainActivity.this, "New location: " + location.getLatitude() + " | " + location.getLongitude(), Toast.LENGTH_SHORT).show();
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
        // request a location update
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, getMainLooper());
        //getMainLooper() used for executing the callback (ignore for now)

        // Additionally, query the last known location from a passive provider to update the UI immediately.
        // A passive provider can be any location provider. It does not request a
        Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location != null) {
            //save this location as the latest used location
            latestLocation = location;

            // fetch weather for last known Location
            fetchWeather(location.getLatitude(), location.getLongitude());
        }
    }

    /**
     * Use the asynchronous {@link FetchWeatherTask} to get the current weather
     * for the given coordinates and update the UI.
     */
    private void fetchWeather(double lat, double lon) {
        //Add UI callbacks to the FetchWeatherTask
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask() {
            /**
             * Cancel the weather update if no internet is available.
             * This method is executed on the UI thread before fetching the data in a background thread.
             */
            @Override
            protected void onPreExecute() {
                //Cancel if not network is available
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo == null || !activeNetworkInfo.isAvailable() || !activeNetworkInfo.isConnected()) {
                    cancel(true);

                    // show a Toast on the screen
                    Toast.makeText(MainActivity.this, "No internet connection available.", Toast.LENGTH_LONG).show();
                }
            }

            /**
             * This method is invoked on a background Thread in order to fetch the weather.
             * See super implementation for details on how this HTTP request works.
             */
            @Override
            protected JSONObject doInBackground(Double... params) {
                return super.doInBackground(params); //only here so that I can add an additional JavaDoc comment
            }

            /**
             * Update the UI once this Task finishes.
             * This method is executed on the UI thread after fetching the data in a background thread.
             */
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                updateWeatherData(jsonObject);
            }
        };
        fetchWeatherTask.execute(lat, lon);
    }

    /**
     * Update the view with the data fetched from the internet.
     *
     * @param jsonObject A JSON object containing the current weather information, as provided by openweathermaps.org.
     *                   Exemplary JSON string: {"coord":{"lon":13.45,"lat":48.57},"sys":{"message":0.0129,"country":"DE",
     *                   "sunrise":1430193062,"sunset":1430244978},"weather":[{"id":500,"main":"Rain","description":"light rain","icon":"10d"}]
     *                   ,"base":"stations","main":{"temp":277.945,"temp_min":277.945,"temp_max":277.945,"pressure":976.14,"sea_level":1029.85,
     *                   "grnd_level":976.14,"humidity":100},"wind":{"speed":7.06,"deg":303.003},"clouds":{"all":92},"rain":{"3h":1.3475},
     *                   "dt":1430232044,"id":2855328,"name":"Passau","cod":200}
     */
    private void updateWeatherData(JSONObject jsonObject) {
        String message = null;
        if (jsonObject != null) {
            try {
                // get the second level JSON objects
                JSONObject sys = jsonObject.getJSONObject("sys");
                JSONObject main = jsonObject.getJSONObject("main");
                JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                JSONObject wind = jsonObject.getJSONObject("wind");
                JSONObject clouds = jsonObject.getJSONObject("clouds");

                // retrieve the weather information (temperature, wind speed, etc.) from the JSON objects
                sunrise = sys.getLong("sunrise") * 1000; // *1000 to convert from UNIX timestamp to Java millis
                sunset = sys.getLong("sunset") * 1000; // *1000 to convert from UNIX timestamp to Java millis
                String weatherDescription = weather.getString("description");
                String weatherIconCode = weather.getString("icon");
                double temperature = main.getDouble("temp") - 272.15; // the temperature is given in Kelvin, therefore convert to Celsius (-272.15)
                int humidity = main.getInt("humidity");
                double windSpeed = wind.getDouble("speed");
                double windDegree = wind.getDouble("deg");
                int cloudiness = clouds.getInt("all");

                // use Geocoder to retrieve the city name from the location
                double latitude = latestLocation.getLatitude();
                double longitude = latestLocation.getLongitude();
                Geocoder geocoder = new Geocoder(this);
                List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                String cityName = "N/A"; // default city name
                if (addressList.size() > 0) {
                    cityName = addressList.get(0).getLocality();
                }

                // get the icon for the given weather
                Drawable weatherIcon = getWeatherStatusIcon(weatherIconCode);

                // set the values to the GUI
                textViewTemperature.setText(String.valueOf(Math.round(temperature)));
                textViewWeatherStatus.setText(weatherDescription);
                textViewCity.setText(cityName);
                windSpeed = Math.round(windSpeed * 3.6); // convert from mps to kmps
                textViewWind.setText(String.valueOf(windSpeed + " km/h " + getWindDirection(windDegree)));
                textViewCloudiness.setText(String.valueOf(cloudiness));
                textViewHumidity.setText(String.valueOf(humidity));
                imageViewWeatherStatusIcon.setImageDrawable(weatherIcon);
            }
            catch (JSONException e) {
                //malformed JSON response
                Log.e("Main", "Malformed server response", e);
                message = "Malformed server response";
            }
            catch (IOException e) {
                Log.e("Main", "Can't fetch address for location.");
                message = "Can't fetch address for location.";
            }
            if (message != null && !message.isEmpty()) {
                // if there was an error, show a Toast (notification) on the screen
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Chooses the correct icon for the weatherIconCode.
     *
     * @return The correct icon as Drawable.
     */
    private Drawable getWeatherStatusIcon(String weatherIconCode) {
        int iconId;
        switch (weatherIconCode) {
            case "01d":
                iconId = R.drawable.clear_sky_day;
                break;
            case "01n":
                iconId = R.drawable.clear_sky_night;
                break;
            case "02d":
                iconId = R.drawable.few_clouds_day;
                break;
            case "02n":
                iconId = R.drawable.few_clouds_night;
                break;
            case "03d":
                iconId = R.drawable.scattered_clouds_day;
                break;
            case "04d":
                iconId = R.drawable.broken_clouds_day;
                break;
            case "09d":
                iconId = R.drawable.shower_rain_day;
                break;
            case "10d":
                iconId = R.drawable.rain_day;
                break;
            case "11d":
                iconId = R.drawable.thunderstorm_day;
                break;
            case "13d":
                iconId = R.drawable.snow_day;
                break;
            case "50d":
                iconId = R.drawable.mist_day;
                break;
            default:
                iconId = R.drawable.clear_sky_day;
                break;
        }
        return getResources().getDrawable(iconId);
    }

    /**
     * Get the coarse wind direction (N, NE, E, SE, S, SW, W, NW) from the wind direction angle
     *
     * @param degree The wind direction in degrees (0 - 360)
     * @return A String representing the coarse wind direction.
     */
    private String getWindDirection(double degree) {
        // subtract 22.5 to have "nice borders" such as 45, 90, 135, etc. rather than 22.5, 67.5, 112.5, etc.
        degree = degree - 22.5;
        if (degree <= 0 || degree >= 315) {
            return "N";
        }
        else if (degree <= 45) {
            return "NE";
        }
        else if (degree <= 90) {
            return "E";
        }
        else if (degree <= 135) {
            return "SE";
        }
        else if (degree <= 180) {
            return "S";
        }
        else if (degree <= 225) {
            return "SW";
        }
        else if (degree <= 270) {
            return "W";
        }
        else if (degree <= 315) {
            return "NW";
        }
        else {
            throw new IllegalArgumentException("Degree must be between 0 and 360");
        }
    }

}
