package de.unipassau.ieee.weatherDemo;

import android.app.Activity;
import android.content.Context;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewTemperature = (TextView) findViewById(R.id.textViewTemperature);
        textViewWeatherStatus = (TextView) findViewById(R.id.textViewWeatherStatus);
        textViewCity = (TextView) findViewById(R.id.textViewCity);
        textViewWind = (TextView) findViewById(R.id.textViewWind);
        textViewCloudiness = (TextView) findViewById(R.id.textViewCloudiness);
        textViewHumidity = (TextView) findViewById(R.id.textViewHumidity);
        imageViewWeatherStatusIcon = (ImageView) findViewById(R.id.imageViewWeatherStatusIcon);
        buttonRefresh = (Button) findViewById(R.id.buttonRefresh);
        buttonRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateLocationForWeather();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLocationForWeather();
        //TODO also call from button or menu
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
        //Request update
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, getMainLooper());
        //getMainLooper() used for executing the callback (ignore for now)

        //Additionally, query the last known location to update UI immediately
        Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        if (location != null) {
            latestLocation = location;
            //Fetch weather for lsat known Location
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
                    //TODO update UI
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

    private void updateWeatherData(JSONObject jsonObject) {
        //Update the view
        String message;
        if (jsonObject != null) {
            try {
                // do not use the coords and city name provided by openweathermaps as they are inaccurate.
                //                JSONObject coord = jsonObject.getJSONObject("coord");
                JSONObject sys = jsonObject.getJSONObject("sys");
                JSONObject conditions = jsonObject.getJSONObject("main");
                JSONObject weather = jsonObject.getJSONArray("weather").getJSONObject(0);
                JSONObject wind = jsonObject.getJSONObject("wind");
                JSONObject clouds = jsonObject.getJSONObject("clouds");

                //                double latitude = coord.getDouble("lat");
                //                double longitude = coord.getDouble("lon");
                double latitude = latestLocation.getLatitude();
                double longitude = latestLocation.getLongitude();
                Geocoder geocoder = new Geocoder(this);
                List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
                String cityName = "N/A";
                if (addressList.size() > 0) {
                    cityName = addressList.get(0).getLocality();
                }
                long sunrise = sys.getLong("sunrise");
                long sunset = sys.getLong("sunset");
                String weatherDescription = weather.getString("description");
                String weatherIconCode = weather.getString("icon");
                // the temperature is given in Kelvin, therefore convert to Celsius (-272.15)
                double temperature = conditions.getDouble("temp") - 272.15;
                int humidity = conditions.getInt("humidity");
                double windSpeed = wind.getDouble("speed");
                double windDegree = wind.getDouble("deg");
                int cloudiness = clouds.getInt("all");
                //TODO: maybe fetch the city name with GeoCoder for better accuracy. Currently, Obersölden is shown for a location in Passau
                //                String cityName = jsonObject.getString("name");

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
                Drawable weatherIcon = getResources().getDrawable(iconId);

                textViewTemperature.setText(String.valueOf(Math.round(temperature)));
                textViewWeatherStatus.setText(weatherDescription);
                textViewCity.setText(cityName);
                //convert from mps to kmps
                textViewWind.setText(String.valueOf(Math.round(windSpeed * 3.6)) + " km/h " + getWindDirection(windDegree));
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
            }
        }
    }

    private String getWindDirection(double degree) {
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
            throw new IllegalArgumentException("degree must be between 0 and 360");
        }
    }

}
