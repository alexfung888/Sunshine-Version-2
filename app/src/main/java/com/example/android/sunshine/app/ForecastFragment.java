package com.example.android.sunshine.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            // Add this line in order for this fragment to handle menu events.
            setHasOptionsMenu(true);
        }

    @Override
    public void onCreateOptionsMenu (Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            new fetchWeatherTask().execute();
            return true;
        }
        return super.onOptionsItemSelected(item);

        // code analysis suggested rewrite the original empty action into:
        // return id == R.id.action_refresh || super.onOptionsItemSelected(item);
        // which is
        // return  item.getItemId() == R.id.action_refresh || super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        String[] data = {
                "Today 28/7 Sunny - 33/28",
                "Tomorrow 29/7 - Sunny - 33/28",
                "Sat 30/7 - Sunny - 34/28",
                "Sun 31/7 - Showers - 33/28",
                "Mon 1/8 - Showers - 33/28",
                "Tue 2/8 - Rainy - 30/27",
                "Wed 3/8 - Rainy - 30/27"
        };
        List<String> weekForecast = new ArrayList<>(Arrays.asList(data));
        mForecastAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        weekForecast);
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        /*
        URL url = null;
        try {
            url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7&appid=" + @string/OPEN_WEATHER_MAP_API_KEY );
        } catch (MalformedURLException e) {
            Log.e("ForecastFragment", "Error: IOException ", e);
        }
        new fetchWeatherTask().execute();
        */

        return rootView;
    }

    public class fetchWeatherTask extends AsyncTask<Void, Void, String> {

        private final String LOG_TAG = fetchWeatherTask.class.getSimpleName();

        protected String doInBackground(Void... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
                String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
                URL url = new URL(baseUrl.concat(apiKey));

                // no idea why it is necessary, proxy setting already in gradle properties
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("http.proxyHost","wch-tmg02.edb.local");
                systemProperties.setProperty("http.proxyPort","8080");

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                if (inputStream == null) {
                    // nothing to do
                    return null;
                }
                StringBuilder builder   = new StringBuilder();
                reader                  = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line).append("\n");
                }

                if (builder.length() == 0) {
                    // nothing to do
                    return null;
                }

                forecastJsonStr = builder.toString();
                Log.v(LOG_TAG, "Retrieved: " + forecastJsonStr);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error: IOException ", e);
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                        return null;
                    }
                }
            }
            return forecastJsonStr;
        }

        /*
        protected void onProgressUpdate(Integer... progress) {
            // setProgressPercent(progress[0]);
        }
        */

        protected void onPostExecute(String result) {
            if (result == null) {
                Log.e(LOG_TAG, "Retrieval failed");
            } else {
                Log.v(LOG_TAG, "Post Execute not null");
            }
        }
    }
}