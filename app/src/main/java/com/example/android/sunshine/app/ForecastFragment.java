package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
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

        // final String zip = "94043"; // Mountain View, CA
        // final String zip = "8223932"; // Hong Kong

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }

        return super.onOptionsItemSelected(item);

        // code analysis suggested rewrite the original empty action into:
        // return id == R.id.action_refresh || super.onOptionsItemSelected(item);
        // which is
        // return  item.getItemId() == R.id.action_refresh || super.onOptionsItemSelected(item);
    }

    private void updateWeather() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = sharedPref.getString(getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        new fetchWeatherTask().execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        // Create some dummy data for the ListView.  Here's a sample weekly forecast
        // no longer needed
        /*
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
        */
        mForecastAdapter =
                new ArrayAdapter<>(
                        getActivity(), // The current context (this activity)
                        R.layout.list_item_forecast, // The name of the layout ID.
                        R.id.list_item_forecast_textview, // The ID of the textview to populate.
                        new ArrayList<String>());
        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // Toast.makeText(getActivity(), (CharSequence) adapterView.getItemAtPosition(i) , Toast.LENGTH_LONG).show();
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, (String) adapterView.getItemAtPosition(i));
                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    private static InetAddress ip() throws SocketException {
        Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();
        NetworkInterface ni;
        while (nis.hasMoreElements()) {
            ni = nis.nextElement();
            if (!ni.isLoopback()/*not loopback*/ && ni.isUp()/*it works now*/) {
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    //filter for ipv4/ipv6
                    if (ia.getAddress().getAddress().length == 4) {
                        //4 for ipv4, 16 for ipv6
                        return ia.getAddress();
                    }
                }
            }
        }
        return null;
    }

    public class fetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = fetchWeatherTask.class.getSimpleName();

        private final int numDays = 7;

        protected String[] doInBackground(String... params) {

            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            String format = "json";
            String units = "metric";
            try {
                //String baseUrl = "http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7";
                //String apiKey = "&APPID=" + BuildConfig.OPEN_WEATHER_MAP_API_KEY;
                //URL url = new URL(baseUrl.concat(apiKey));

                Uri.Builder uriBuilder = new Uri.Builder();
                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";
                final String QUERY_PARAM_Q = "q";
                final String QUERY_PARAM_ID = "id";
                final String QUERY_PARAM_ZIP = "zip";
                final String QUERY_PARAM;
                if (params[0].matches("\\d+")) {
                    QUERY_PARAM = (params[0].length() == 5)? QUERY_PARAM_Q : QUERY_PARAM_ID;
                    // it seems q=zip is interpreted as US ZIP code
                    // a real zip argument should look zip=94043,us
                    // this is not possible here, because the comma will be encoded by addQueryParameter()
                    // and the resulting %2c is not accepted by the API (requires a real comma in the URL)
                } else {
                    QUERY_PARAM = QUERY_PARAM_Q;
                }

                final String FORMAT_PARAM = "mode";
                final String UNITS_PARAM = "units";
                final String DAYS_PARAM = "cnt";
                final String APPID_PARAM = "APPID";
                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                    .appendQueryParameter(QUERY_PARAM, params[0])
                    .appendQueryParameter(FORMAT_PARAM, format)
                    .appendQueryParameter(UNITS_PARAM, units)
                    .appendQueryParameter(DAYS_PARAM, Integer.toString(numDays))
                    .appendQueryParameter(APPID_PARAM, BuildConfig.OPEN_WEATHER_MAP_API_KEY)
                    .build();

                URL url = new URL(builtUri.toString());

                Log.v(LOG_TAG, "Built URI " + builtUri.toString());


                String myIP="";
                try {
                    Log.v(LOG_TAG, "ip()=" + (myIP=ip().toString()));
                } catch (SocketException e) {
                    Log.e(LOG_TAG,"getIP results in exception", e);
                }
                if (myIP.startsWith("/10.15.")) {
                    // EDB WCH LAN
                    Properties systemProperties = System.getProperties();
                    systemProperties.setProperty("http.proxyHost","wch-tmg02.edb.local");
                    systemProperties.setProperty("http.proxyPort","8080");
                    Log.v(LOG_TAG, "proxy set to WCH LAN");
                } else if (myIP.startsWith("/192.168.20.")) {
                    // 3042bb
                    Properties systemProperties = System.getProperties();
                    systemProperties.setProperty("http.proxyHost","192.168.20.250");
                    systemProperties.setProperty("http.proxyPort","8080");
                    Log.v(LOG_TAG, "proxy set to 3042bb");
                } else {
                    Log.v(LOG_TAG, "No proxy set");
                }


                // no idea why it is necessary, proxy setting already in gradle properties
                // Properties systemProperties = System.getProperties();
                // LAN
                //systemProperties.setProperty("http.proxyHost","wch-tmg02.edb.local");
                // 3042bb
                // systemProperties.setProperty("http.proxyHost","192.168.20.250");
                // systemProperties.setProperty("http.proxyPort","8080");

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
                // Log.v(LOG_TAG, "Retrieved: " + forecastJsonStr);
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

            try {
                return getWeatherDataFromJson(forecastJsonStr, numDays);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }
            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        /*
        protected void onProgressUpdate(Integer... progress) {
            // setProgressPercent(progress[0]);
        }
        */

        protected void onPostExecute(String result[]) {
            if (result == null) {
                Log.e(LOG_TAG, "Retrieval failed.");
            } else {
                // Log.v(LOG_TAG, "Post Execute successful.");
                mForecastAdapter.clear();
                // List<String> weekForecast = new ArrayList<>(Arrays.asList(result));
                // mForecastAdapter.addAll(weekForecast);
                // List<String> weekForecast = new ArrayList<>(Arrays.asList(result));
                mForecastAdapter.addAll(new ArrayList<>(Arrays.asList(result)));
                /*
                text book answer loops to add(), instead of addAll()
                addAll() requires SDK v11 (honeycomb)
                addAll() is better because listView updated only once
                OTOh add() refreshes after each string
                for(String dayForecastStr : result) {
                    mForecastAdapter.add(dayForecastStr);
                }
                */
            }
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
         * so for convenience we're breaking it out into its own method now.
         */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            //SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE d MMMM");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low, String unitType) {

            if (unitType.equals(getString(R.string.pref_units_imperial))) {
                high = (high * 1.8) + 32;
                low = (low * 1.8) + 32;
            } else if (!unitType.equals(getString(R.string.pref_units_metric))) {
                Log.d(LOG_TAG, "Unit type not found: " + unitType);
            }

            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }


        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            //JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
            JSONArray weatherArray = forecastJson.optJSONArray(OWM_LIST);
            if (weatherArray == null) {
                // error
                //forecastJson.getString("cod"); error code
                //forecastJson.getString("message"); error message
                Log.e(LOG_TAG, "Retrieval error. Code=" + forecastJson.getString("cod") + ". Message=" + forecastJson.getString("message"));
                return null;
            } else {
                JSONObject forecastCity = forecastJson.getJSONObject("city");
                Log.v(LOG_TAG, "Successfully retrieved forecast for " + forecastCity.getString("name") + " of " + forecastCity.getString("country"));
            }

            // OWM returns daily forecasts based upon the local time of the city that is being
            // asked for, which means that we need to know the GMT offset to translate this data
            // properly.

            // Since this data is also sent in-order and the first day is always the
            // current day, we're going to take advantage of that to get a nice
            // normalized UTC date for all of our weather.

            Time dayTime = new Time();
            dayTime.setToNow();

            // we start at the day returned by local time. Otherwise this is a mess.
            int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

            // now we work exclusively in UTC
            dayTime = new Time();

            String[] resultStrs = new String[numDays];

            // Data is fetched in Celsius by default.
            // If user prefers to see in Fahrenheit, convert the values here.
            // We do this rather than fetching in Fahrenheit so that the user can
            // change this option without us having to re-fetch the data once
            // we start storing the values in a database.
            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPrefs.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));

            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime;
                // Cheating to convert this to UTC time, which is what we want anyhow
                dateTime = dayTime.setJulianDay(julianStartDay+i);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, unitType);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            /*
            for (String s : resultStrs) {
                Log.v(LOG_TAG, "Forecast entry: " + s);
            }
            */
            return resultStrs;

        }
    }
}