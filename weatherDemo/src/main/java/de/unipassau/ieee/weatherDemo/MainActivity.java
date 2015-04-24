package de.unipassau.ieee.weatherDemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.Serializable;

import static de.unipassau.ieee.weatherDemo.WeatherIntentService.INTENT_WEATHER_RESPONSE;


public class MainActivity extends ActionBarActivity {
    private final BroadcastReceiver weatherReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.hasExtra(WeatherIntentService.EXTRA_WEATHER_EXCEPTION)){
                Exception exception = (Exception) intent.getSerializableExtra(WeatherIntentService.EXTRA_WEATHER_EXCEPTION);
                exception.printStackTrace();
                ((TextView) findViewById(R.id.weatherText)).setText(
                        exception.toString()
                );
            }else {
                ((TextView) findViewById(R.id.weatherText)).setText(
                        intent.getStringExtra(WeatherIntentService.EXTRA_WEATHER_DATA)
                );
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(weatherReceiver, new IntentFilter(INTENT_WEATHER_RESPONSE));
    }

    @Override
    protected void onResume() {
        super.onResume();
        WeatherIntentService.updateCityWeather(this, "Passau");
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(weatherReceiver);
        super.onDestroy();
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
}
