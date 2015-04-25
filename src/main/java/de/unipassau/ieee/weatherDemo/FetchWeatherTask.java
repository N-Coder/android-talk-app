package de.unipassau.ieee.weatherDemo;

import android.os.AsyncTask;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * An asynchronous Task for fetching the weather data for a given latitude and longitude from openweathermap.org.
 */
class FetchWeatherTask extends AsyncTask<Double, Void, JSONObject> {
    private static final String API_URL_LOCATION = "http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f";

    @Override
    protected JSONObject doInBackground(Double... params) {
        try {
            //Build HTTP request
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response = httpclient.execute(new HttpGet(
                    String.format(API_URL_LOCATION, params[0] /*lat*/, params[1] /*long*/)
            ));
            //Execute HTTP request
            StatusLine statusLine = response.getStatusLine();
            if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
                //Parse and return response data
                String responseString = EntityUtils.toString(response.getEntity());
                return new JSONObject(responseString);
            } else {
                //Server did not return 200 SUCCESS, throw as exception
                response.getEntity().getContent().close();
                throw new IOException(statusLine.getReasonPhrase());
            }
        } catch (IOException | JSONException e) {
            //Could not communicate with server
            Log.e("FetchWeatherTask", "Could not fetch weather", e);
            return null;
        }
    }
}
