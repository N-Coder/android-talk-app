package de.unipassau.ieee.weatherDemo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;


public class WeatherIntentService extends IntentService {
    private static final String API_URL_CITY = "http://api.openweathermap.org/data/2.5/weather?q=%s";
    private static final String API_URL_LOCATION = "http://api.openweathermap.org/data/2.5/weather?lat=%d&lon=%d";

    private static final String ACTION_FETCH_WEATHER = "de.unipassau.ieee.weatherDemo.action.FETCH_WEATHER";
    private static final String ACTION_FETCH_WEATHER_GPS = "de.unipassau.ieee.weatherDemo.action.FETCH_WEATHER_GPS";

    private static final String EXTRA_CITY = "de.unipassau.ieee.weatherDemo.extra.CITY";
    private static final String EXTRA_LONGITUDE = "de.unipassau.ieee.weatherDemo.extra.LONGITUDE";
    private static final String EXTRA_LATITUDE = "de.unipassau.ieee.weatherDemo.extra.LATITUDE";

    public static final String INTENT_WEATHER_RESPONSE = "de.unipassau.ieee.weatherDemo.intent.WEATHER_RESPONSE";
    public static final String EXTRA_WEATHER_DATA = "de.unipassau.ieee.weatherDemo.extra.WEATHER_DATA";
    public static final String EXTRA_WEATHER_EXCEPTION = "de.unipassau.ieee.weatherDemo.extra.WEATHER_EXCEPTION";

    public static void updateCityWeather(Context context, String city) {
        Intent intent = new Intent(context, WeatherIntentService.class);
        intent.setAction(ACTION_FETCH_WEATHER);
        intent.putExtra(EXTRA_CITY, city);
        context.startService(intent);
    }

    public static void updateLocationWeather(Context context, int latitude, int longitude) {
        Intent intent = new Intent(context, WeatherIntentService.class);
        intent.setAction(ACTION_FETCH_WEATHER);
        intent.putExtra(EXTRA_LATITUDE, latitude);
        intent.putExtra(EXTRA_LONGITUDE, longitude);
        context.startService(intent);
    }

    public static void updateGPSWeather(Context context) {
        Intent intent = new Intent(context, WeatherIntentService.class);
        intent.setAction(ACTION_FETCH_WEATHER_GPS);
        context.startService(intent);
    }

    public WeatherIntentService() {
        super("WeatherIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_FETCH_WEATHER.equals(action)) {
                if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
                    fetchWeather(String.format(API_URL_LOCATION, intent.getIntExtra(EXTRA_LATITUDE, 0), intent.getIntExtra(EXTRA_LONGITUDE, 0)));
                } else {
                    fetchWeather(String.format(API_URL_CITY, intent.getStringExtra(EXTRA_CITY)));
                }
            } else if (ACTION_FETCH_WEATHER_GPS.equals(action)) {
                //TODO
            }
        }
    }

    void fetchWeather(String url) {
        if (!isNetworkAvailable()) {
            Intent callback = new Intent(INTENT_WEATHER_RESPONSE);
            callback.putExtra(EXTRA_WEATHER_EXCEPTION, new IOException("No internet available!"));
            sendBroadcast(callback);
        }
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(url));
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                Intent callback = new Intent(INTENT_WEATHER_RESPONSE);
                callback.putExtra(EXTRA_WEATHER_DATA, EntityUtils.toString(response.getEntity()));
                sendBroadcast(callback);
            } else {
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException e) {
            Intent callback = new Intent(INTENT_WEATHER_RESPONSE);
            callback.putExtra(EXTRA_WEATHER_EXCEPTION, e);
            sendBroadcast(callback);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isAvailable() && activeNetworkInfo.isConnected();
    }
}
