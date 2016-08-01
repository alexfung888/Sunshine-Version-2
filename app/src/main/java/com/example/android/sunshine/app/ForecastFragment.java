package com.example.android.sunshine.app;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
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

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
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
        new fetchWeatherTask().execute(url);
        */

        return rootView;
    }

    private class fetchWeatherTask extends AsyncTask<URL, Void, String> {

        private final String LOG_TAG = fetchWeatherTask.class.getSimpleName();

        protected String doInBackground(URL... urls) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                urlConnection = (HttpURLConnection) urls[0].openConnection();
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
            Log.v("Retrieved: ", result);
        }
    }
}