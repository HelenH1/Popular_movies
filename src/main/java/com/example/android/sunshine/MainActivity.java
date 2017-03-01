/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.sunshine;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;
import com.example.android.sunshine.ForecastAdapter.ForecastAdapterOnClickHandler;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        ForecastAdapterOnClickHandler {


    private Cursor mCursor;

    private static final String OWM_MOVIE_ID = "movie_id";
    private static final String OWM_POSTER_PATH = "movie_poster_path";
    private static final String OWM_TITLE = "movie_title";

    private final String TAG = MainActivity.class.getSimpleName();

    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of
     * weather data.
     */

    public static final String COLUMN_DATE = "date";

    /* Weather ID as returned by API, used to identify the icon to be used */
    public static final String COLUMN_MOVIE_ID = "movie_id";

    /* Humidity is stored as a float representing percentage */
    public static final String COLUMN_POSTER_PATH = "poster_path";

    /* Pressure is stored as a float representing percentage */
    public static final String COLUMN_MOVIE_TITLE = "movie_title";

    /* Wind speed is stored as a float representing wind speed in mph */
    //public static final String COLUMN_RELEASE_DATE = "release_date";

    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.MovieEntry.COLUMN_MOVIE_ID,
            WeatherContract.MovieEntry.COLUMN_MOVIE_TITLE,
            WeatherContract.MovieEntry.COLUMN_POSTER_PATH,
    };

    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    public static final int INDEX_MOVIE_ID = 0;
    public static final int INDEX_MOVIE_TITLE = 1;
    public static final int INDEX_POSTER_PATH = 2;


    /*
     * This ID will be used to identify the Loader responsible for loading our weather forecast. In
     * some cases, one Activity can deal with many Loaders. However, in our case, there is only one.
     * We will still use this ID to initialize the loader and create the loader for best practice.
     * Please note that 44 was chosen arbitrarily. You can use whatever number you like, so long as
     * it is unique and consistent.
     */
    private static final int ID_FORECAST_LOADER = 44;

    private ForecastAdapter mForecastAdapter;
    private RecyclerView mRecyclerView;
    private int mPosition = RecyclerView.NO_POSITION;

    private ProgressBar mLoadingIndicator;

    private TextView mErrorMessageDisplay;

    private Context mContext;
    private ForecastAdapterOnClickHandler mClickHandler;
    final String SORT_BY_POP = "popular";//"popularity.desc";
    final String SORT_BY_RATE = "top_rated";//"vote_average.desc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);
        getSupportActionBar().setElevation(0f);


        /*
         * Using findViewById, we get a reference to our RecyclerView from xml. This allows us to
         * do things like set the adapter of the RecyclerView and toggle the visibility.
         */
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_forecast);
        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);

        /*
         * The ProgressBar that will indicate to the user that we are loading data. It will be
         * hidden when no data is loading.
         *
         * Please note: This so called "ProgressBar" isn't a bar by default. It is more of a
         * circle. We didn't make the rules (or the names of Views), we just follow them.
         */
        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        /*
         * A LinearLayoutManager is responsible for measuring and positioning item views within a
         * RecyclerView into a linear list. This means that it can produce either a horizontal or
         * vertical list depending on which parameter you pass in to the LinearLayoutManager
         * constructor. In our case, we want a vertical list, so we pass in the constant from the
         * LinearLayoutManager class for vertical lists, LinearLayoutManager.VERTICAL.
         *
         * There are other LayoutManagers available to display your data in uniform grids,
         * staggered grids, and more! See the developer documentation for more details.
         *
         * The third parameter (shouldReverseLayout) should be true if you want to reverse your
         * layout. Generally, this is only true with horizontal lists that need to support a
         * right-to-left layout.
         */
        GridLayoutManager layoutManager =
                new GridLayoutManager(this, 2);

        /* setLayoutManager associates the LayoutManager we created above with our RecyclerView */
        mRecyclerView.setLayoutManager(layoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
        mRecyclerView.setHasFixedSize(true);

        /*
         * The ForecastAdapter is responsible for linking our weather data with the Views that
         * will end up displaying our weather data.
         *
         * Although passing in "this" twice may seem strange, it is actually a sign of separation
         * of concerns, which is best programming practice. The ForecastAdapter requires an
         * Android Context (which all Activities are) as well as an onClickHandler. Since our
         * MainActivity implements the ForecastAdapter ForecastOnClickHandler interface, "this"
         * is also an instance of that type of handler.
         */
        mForecastAdapter = new ForecastAdapter(this, this);

        /* Setting the adapter attaches it to the RecyclerView in our layout. */
       // mRecyclerView.setAdapter(mForecastAdapter);


        showLoading();
        mContext = this;

        /*
         * Ensures a loader is initialized and active. If the loader doesn't already exist, one is
         * created and (if the activity/fragment is currently started) starts the loader. Otherwise
         * the last created loader is re-used.
         */
        getSupportLoaderManager().initLoader(ID_FORECAST_LOADER, null, this);
        //System.out.println("OnCreate working");
        //System.out.println();
        //SunshineSyncUtils.initialize(this);
        loadMovieData(SORT_BY_POP);
    }

    /**
     * This method will get the user's preferred sort method for the movies (popularity or rating) and then tell some
     * background method to get the movie data in the background.
     */
    private void loadMovieData(String sortBy) {
        //System.out.println("Loading movie data with sorting: "+sortBy);
        //System.out.println();
        showWeatherDataView();

        new FetchWeatherTask().execute(sortBy);
    }

    /**
     * Called by the {@link android.support.v4.app.LoaderManagerImpl} when a new Loader needs to be
     * created. This Activity only uses one loader, so we don't necessarily NEED to check the
     * loaderId, but this is certainly best practice.
     *
     * @param loaderId The loader ID for which we need to create a loader
     * @param bundle   Any arguments supplied by the caller
     * @return A new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {


        switch (loaderId) {

            case ID_FORECAST_LOADER:
                /* URI for all rows of weather data in our weather table */
                Uri forecastQueryUri = WeatherContract.MovieEntry.CONTENT_URI;
                /* Sort order: Ascending by date */
                String sortOrder = WeatherContract.MovieEntry.COLUMN_MOVIE_ID + " ASC";
                /*
                 * A SELECTION in SQL declares which rows you'd like to return. In our case, we
                 * want all weather data from today onwards that is stored in our weather table.
                 * We created a handy method to do that in our WeatherEntry class.
                 */
                //String selection = WeatherContract.MovieEntry.getSqlSelectForAll();
                //if(WeatherContract.MovieEntry.)
                return new CursorLoader(this,
                        forecastQueryUri,
                        MAIN_FORECAST_PROJECTION,
                        null,
                        null,
                        null);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    /**
     * Called when a Loader has finished loading its data.
     *
     * NOTE: There is one small bug in this code. If no data is present in the cursor do to an
     * initial load being performed with no access to internet, the loading indicator will show
     * indefinitely, until data is present from the ContentProvider. This will be fixed in a
     * future version of the course.
     *
     * @param loader The Loader that has finished.
     * @param data   The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {


        swapCursor(data);
        if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;
        mRecyclerView.smoothScrollToPosition(mPosition);
        if (data.getCount() != 0) showWeatherDataView();
    }

    /**
     * Called when a previously created loader is being reset, and thus making its data unavailable.
     * The application should at this point remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        /*
         * Since this Loader's data is now invalid, we need to clear the Adapter that is
         * displaying the data.
         */

        swapCursor(null);
    }

    /**
     * This method is for responding to clicks from our list.
     *
     * @param date Normalized UTC time that represents the local date of the weather in GMT time.
     * @see WeatherContract.WeatherEntry#COLUMN_DATE
     */
    /*@Override
    public void onClick(long date) {
        Intent weatherDetailIntent = new Intent(MainActivity.this, DetailActivity.class);
        Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(date);
        weatherDetailIntent.setData(uriForDateClicked);
        startActivity(weatherDetailIntent);
    }*/


    /**
     * This method is overridden by our MainActivity class in order to handle RecyclerView item
     * clicks.
     *
     * @param movieKeyValues The info for the movie that was clicked
     *
     */
    @Override
    public void onClick(ContentValues movieKeyValues) {
        Context context = this;
        String movieKey = movieKeyValues.getAsString("movie_id");
        //System.out.println("Movie key is: "+movieKey);
        Class destinationClass = DetailActivity.class;
        Intent intentToStartDetailActivity = new Intent(context, destinationClass);
        //System.out.println("Launching detail activity");
        intentToStartDetailActivity.putExtra(Intent.EXTRA_TEXT, movieKey);
        startActivity(intentToStartDetailActivity);
    }



    /**
     * This method will make the View for the weather data visible and hide the error message and
     * loading indicator.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't need to check whether
     * each view is currently visible or invisible.
     */
    private void showWeatherDataView() {
        /* First, hide the loading indicator */
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        /* Finally, make sure the weather data is visible */
        mRecyclerView.setVisibility(View.VISIBLE);
    }


    /**
     * This method will make the error message visible and hide the movie
     * View.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showErrorMessage() {
    /* First, hide the currently visible data */
        mRecyclerView.setVisibility(View.INVISIBLE);
    /* Then, show the error */
        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the loading indicator visible and hide the weather View and error
     * message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't need to check whether
     * each view is currently visible or invisible.
     */
    private void showLoading() {
        /* Then, hide the weather data */
        mRecyclerView.setVisibility(View.INVISIBLE);
        /* Finally, show the loading indicator */
        mLoadingIndicator.setVisibility(View.VISIBLE);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, ContentValues[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
            //System.out.println("On Pre Execute");
            //System.out.println();
        }

        @Override
        protected ContentValues[] doInBackground(String... params) {
            String sortBy;

    /* There will always be some sort method assigned.*/
            sortBy = params[0];

            URL dataRequestUrl = NetworkUtils.getUrlForMainActivity(sortBy);
            try {
                String jsonMovieResponse = NetworkUtils
                        .getResponseFromHttpUrl(dataRequestUrl);
                //System.out.println("Got HTTP Response");

                ContentValues[] jsonMovieData = OpenWeatherJsonUtils
                        .getBasicMovieInfoFromJson(jsonMovieResponse);
                //System.out.println("Returning JSON MOVIE DATA");

                return jsonMovieData;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ContentValues[] jsonMovieData) {
            mRecyclerView.setAdapter(mForecastAdapter);

            mLoadingIndicator.setVisibility(View.INVISIBLE);
            mRecyclerView.setVisibility(View.VISIBLE);
            if (jsonMovieData != null) {
                showWeatherDataView();
                //System.out.println("Setting basic movie info in ForecastAdapter");
                //System.out.println();
                mForecastAdapter.setBasicMovieInfo(jsonMovieData);
            } else {
                //System.out.println("Weather data null. Showing error message. No movie data assigned to ForecastAdapter.");
                mErrorMessageDisplay.setText(R.string.error_no_movie_data);
                //System.out.println();
                showErrorMessage();
            }
        }
    }

    public void showFavorites() {

        try {
            int size = mCursor.getCount();
            System.out.println("Database size is "+size);
        ContentValues[] basicMovieData = new ContentValues[size];

        for(int i=0; i<size; i++) {
            ContentValues movieData = new ContentValues();
            mCursor.moveToPosition(i);
            movieData.put(OWM_MOVIE_ID, mCursor.getString(INDEX_MOVIE_ID));
            movieData.put(OWM_TITLE, mCursor.getString(INDEX_MOVIE_TITLE));
            movieData.put(OWM_POSTER_PATH, mCursor.getString(INDEX_POSTER_PATH));
            basicMovieData[i] = movieData;

        }
            mForecastAdapter.setBasicMovieInfo(basicMovieData);
            mForecastAdapter.notifyDataSetChanged();
        }
        catch (NullPointerException e) {
            mErrorMessageDisplay.setText(R.string.error_no_favorites);
            showErrorMessage();

        }


    }

    void swapCursor(Cursor newCursor) {
        mCursor = newCursor;
    }


    /**
     * This is where we inflate and set up the menu for this Activity.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see #onPrepareOptionsMenu
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.forecast, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    /**
     * Callback invoked when a menu item was selected from this Activity's menu.
     *
     * @param item The menu item that was selected by the user
     *
     * @return true if you handle the menu click here, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_favorites) {
            showFavorites();
            //System.out.println("Should show favorites");
            return true;
        }

        if (id == R.id.action_rating) {
            loadMovieData(SORT_BY_RATE);
            //System.out.println("Sorting by rating");
            return true;
        }
        if (id == R.id.action_popular) {
            loadMovieData(SORT_BY_POP);
            //System.out.println("Sorting by popularity");
            return true;
        }


        return super.onOptionsItemSelected(item);
    }
}
