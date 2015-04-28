package de.unipassau.ieee.weatherDemo;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;


public class LocationActivity extends Activity {

    public static String KEY_LATITUDE  = "key_latitude";
    public static String KEY_LONGITUDE = "key_longitude";
    public static String KEY_SUNRISE   = "key_sunrise";
    public static String KEY_SUNSET    = "key_sunset";

    private double latitude;
    private double longitude;
    private long   sunrise;
    private long   sunset;

    private TextView textViewAddress;
    private TextView textViewLatitude;
    private TextView textViewLongitude;
    private TextView textViewSunrise;
    private TextView textViewSunset;
    private Button   buttonShowOnMaps;

    /**
     * onCreate(...) is called by the Android system when the activity is created. As LocationActivity is launched from MainActivity, it is called after
     * startActivity(locationIntent); in MainActivity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // define the layout for this activity
        setContentView(R.layout.activity_location);

        // get a reference to the view elements
        textViewAddress = (TextView) findViewById(R.id.textViewAddress);
        textViewLatitude = (TextView) findViewById(R.id.textViewLatitude);
        textViewLongitude = (TextView) findViewById(R.id.textViewLongitude);
        textViewSunrise = (TextView) findViewById(R.id.textViewSunrise);
        textViewSunset = (TextView) findViewById(R.id.textViewSunset);
        buttonShowOnMaps = (Button) findViewById(R.id.buttonShowOnMaps);

        // retrieve the arguments given to this Acitivity when it was started
        latitude = getIntent().getDoubleExtra(KEY_LATITUDE, 48.5717399);
        longitude = getIntent().getDoubleExtra(KEY_LONGITUDE, 13.4623373);
        sunrise = getIntent().getLongExtra(KEY_SUNRISE, -1);
        sunset = getIntent().getLongExtra(KEY_SUNSET, -1);

        updateValues();

        // react to clicks/taps on the "Show on Maps" button
        buttonShowOnMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLocationOnMaps();
            }
        });
    }

    /**
     * Show the current values on the GUI
     */
    private void updateValues() {
        textViewAddress.setText(getAddress());
        textViewLatitude.setText(String.valueOf(latitude));
        textViewLongitude.setText(String.valueOf(longitude));
        textViewSunrise.setText(getTimeFromDate(sunrise));
        textViewSunset.setText(getTimeFromDate(sunset));
    }

    /**
     * Open a map app (such as Google Maps) and show the current location on it.
     */
    private void showLocationOnMaps(){
        // create an Uri with the information about the location to be displayed
        String uriString = "geo:0,0?q=" + latitude + "," + longitude +"(" + "Your+location" + ")" ;
        Uri uri = Uri.parse(uriString);

        // create an intent to view "something"; android will handle to select an appropriate app itself based on the Uri and the installed apps
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    /**
     * Create a 3 line address for the current location by using Geocoder
     */
    private String getAddress() {
        String addressString = "";
        Geocoder geocoder = new Geocoder(this);
        try {
            final List<Address> addressList = geocoder.getFromLocation(latitude, longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                Address address = addressList.get(0);
                String addressLine;

                // first address line, usually street name and number, depends on the locale
                addressLine = address.getAddressLine(0);
                if (addressLine != null && !addressLine.isEmpty()) {
                    addressString = addressLine;
                }

                // second address line, usually ZIP code and city name
                addressLine = address.getAddressLine(1);
                if (addressLine != null && !addressLine.isEmpty()) {
                    addressString += "\n" + addressLine;
                }

                // third address line, usually country name
                addressLine = address.getAddressLine(2);
                if (addressLine != null && !addressLine.isEmpty()) {
                    addressString += "\n" + addressLine;
                }
            }
        }
        catch (IOException e) {
            Log.e("LocationActivity", "Can't fetch address for location.");
        }
        return addressString;
    }

    /**
     * Get a formatted time string from a timestamp
     */
    private String getTimeFromDate(long date) {
        if(date == -1)
        {
            return "N/A";
        }
        return DateFormat.getTimeInstance().format(new Date(date));
    }
}
