/*
 * Copyright (C) 2014 The Android Open Source Project
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
import android.content.Intent;
//import android.database.Cursor;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
//import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
//import android.support.v4.content.CursorLoader;
//import android.support.v4.content.Loader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.content.Context;
import android.content.ContentValues;
import android.content.Context;
import android.widget.ImageButton;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.Toast;

import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.data.WeatherProvider;
import com.example.android.sunshine.utilities.NetworkUtils;
import com.example.android.sunshine.data.SunshinePreferences;
//import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.OpenWeatherJsonUtils;

import com.squareup.picasso.Picasso;
import com.example.android.sunshine.databinding.ActivityDetailBinding;

import java.io.File;
import java.net.URL;

public class DetailActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>{

    /*
     * In this Activity, you can share the selected day's forecast. No social sharing is complete
     * without using a hashtag. #BeTogetherNotTheSame
     */
    private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

    /*
     * The columns of data that we are interested in displaying within our DetailActivity's
     * weather display.
     */
    private Cursor mCursor;

    private static final String OWM_MOVIE_ID = "movie_id";
    private static final String OWM_POSTER_PATH = "movie_poster_path";
    private static final String OWM_TITLE = "movie_title";
    private static final String OWM_SYNOPSIS = "movie_synopsis";
    private static final String OWM_RATING = "movie_rating";
    private static final String OWM_RELEASE_DATE = "movie_release_date";
    private static final String OWM_REVIEWS = "movie_reviews";
    private static final String OWM_TRAILERS = "movie_trailers";

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
     * We store the indices of the values in the array of Strings above to more quickly be able
     * to access the data from our query. If the order of the Strings above changes, these
     * indices must be adjusted to match the order of the Strings.
     */


    /*
     * This ID will be used to identify the Loader responsible for loading the weather details
     * for a particular day. In some cases, one Activity can deal with many Loaders. However, in
     * our case, there is only one. We will still use this ID to initialize the loader and create
     * the loader for best practice. Please note that 353 was chosen arbitrarily. You can use
     * whatever number you like, so long as it is unique and consistent.
     */
    private static final int ID_DETAIL_LOADER = 353;

    /* A summary of the forecast that can be shared by clicking the share button in the ActionBar */
    private String mForecastSummary;

    /* The URI that is used to access the chosen day's weather details */
    private Uri mUri;

    private ContentValues mMovieData;
    private ContentValues mReviewData;
    private ContentValues mTrailerData;
    private String mMovieKey;
    private Context mContext;
    private AppCompatButton favoritesButton;

    public static final String DATABASE_NAME = "favorites.db";



    /*
     * This field is used for data binding. Normally, we would have to call findViewById many
     * times to get references to the Views in this Activity. With data binding however, we only
     * need to call DataBindingUtil.setContentView and pass in a Context and a layout, as we do
     * in onCreate of this class. Then, we can access all of the Views in our layout
     * programmatically without cluttering up the code with findViewById.
     */
    private ActivityDetailBinding mDetailBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mContext = this;
        System.out.println("Detail view created");
        super.onCreate(savedInstanceState);

        Intent intentThatStartedThisActivity = getIntent();

        if (intentThatStartedThisActivity != null) {
            if (intentThatStartedThisActivity.hasExtra(Intent.EXTRA_TEXT)) {
                mMovieKey = intentThatStartedThisActivity.getStringExtra(Intent.EXTRA_TEXT);
            }
        }
        loadMovieData();

        mDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);

        getSupportLoaderManager().initLoader(ID_DETAIL_LOADER, null, this);
        //System.out.println("OnCreate working");
        System.out.println();

        //mUri = getIntent().getData();
        //if (mUri == null) throw new NullPointerException("URI for DetailActivity cannot be null");

        /* This connects our Activity into the loader lifecycle. */
        //getSupportLoaderManager().initLoader(ID_DETAIL_LOADER, null, this);

    }

    /**
     * This is where we inflate and set up the menu for this Activity.
     *
     * @param menu The options menu in which you place your items.
     *
     * @return You must return true for the menu to be displayed;
     *         if you return false it will not be shown.
     *
     * @see android.app.Activity#onPrepareOptionsMenu(Menu)
     * @see #onOptionsItemSelected
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        /* Use AppCompatActivity's method getMenuInflater to get a handle on the menu inflater */
        MenuInflater inflater = getMenuInflater();
        /* Use the inflater's inflate method to inflate our menu layout to this menu */
        inflater.inflate(R.menu.detail, menu);
        /* Return true so that the menu is displayed in the Toolbar */
        return true;
    }

    /**
     * Callback invoked when a menu item was selected from this Activity's menu. Android will
     * automatically handle clicks on the "up" button for us so long as we have specified
     * DetailActivity's parent Activity in the AndroidManifest.
     *
     * @param item The menu item that was selected by the user
     *
     * @return true if you handle the menu click here, false otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /* Get the ID of the clicked item */
        int id = item.getItemId();

        /* Settings menu item clicked */
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }

        /* Share menu item clicked */
        if (id == R.id.action_share) {
            Intent shareIntent = createShareForecastIntent();
            startActivity(shareIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Uses the ShareCompat Intent builder to create our Forecast intent for sharing.  All we need
     * to do is set the type, text and the NEW_DOCUMENT flag so it treats our share as a new task.
     * See: http://developer.android.com/guide/components/tasks-and-back-stack.html for more info.
     *
     * @return the Intent to use to share our weather forecast
     */
    private Intent createShareForecastIntent() {
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(mForecastSummary + FORECAST_SHARE_HASHTAG)
                .getIntent();
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return shareIntent;
    }



    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle bundle) {


        switch (loaderId) {

            case ID_DETAIL_LOADER:
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

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {


        //swapCursor(data);
        //if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;
        //mRecyclerView.smoothScrollToPosition(mPosition);
        //if (data.getCount() != 0) showWeatherDataView();
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

        //swapCursor(null);
    }


    //@Override
    public void setInfo() {


        /****************
         * Movie poster
         ****************/


        String movieURLExt = mMovieData.getAsString(OWM_POSTER_PATH);
        String movieURLString = "http://image.tmdb.org/t/p/w342/" + movieURLExt;
        System.out.println(movieURLString);
        ImageView posterView = mDetailBinding.primaryMovieInfo.ivMoviePoster;
        Picasso.with(this)
                .load(movieURLString)
                //.placeholder(R.drawable.ic_placeholder)
                //.error(R.drawable.ic_error_fallback)
                .into(posterView);


        /****************
         * Movie title
         ****************/

        String movieTitle = mMovieData.getAsString(OWM_TITLE);
        System.out.println("Movie title is "+movieTitle);
        mDetailBinding.primaryMovieInfo.tvMovieTitle.setText(movieTitle);

        /****************
         * release date
         ****************/

        String releaseDate = mMovieData.getAsString(OWM_RELEASE_DATE);
        releaseDate = releaseDate.substring(0, 4);
        mDetailBinding.primaryMovieInfo.tvReleaseDate.setText(releaseDate);

        /****************
         * user rating
         ****************/

        String userRating = mMovieData.getAsString(OWM_RATING);
        mDetailBinding.primaryMovieInfo.tvRating.setText(getString(R.string.rating_descriptor)+userRating);

        /****************
         * synopsis
         ****************/

        String synopsis = mMovieData.getAsString(OWM_SYNOPSIS);
        System.out.println("Synopsis: "+synopsis);
        mDetailBinding.primaryMovieInfo.tvSynopsis.setText(synopsis);

        /****************
         * reviews
         *
         * supports up to three reviews; most movies have that many or fewer in the database
         ****************/

        if(mReviewData.getAsString(Integer.toString(0))!=null) {
            int i = 0;
            String reviewAuthor = mReviewData.getAsString(Integer.toString(2*i));
            String reviewContent = mReviewData.getAsString(Integer.toString(2*i+1));
            TextView review = (TextView) findViewById(R.id.tv_review_1);
            mDetailBinding.extraMovieDetails.tvReviewer1.setText(reviewAuthor);
            review.setText(reviewContent);
        } else {
            mDetailBinding.extraMovieDetails.tvReviewer1.setText(R.string.no_reviews);
            mDetailBinding.extraMovieDetails.tvReview1.setVisibility(View.GONE);
        }

        if(mReviewData.getAsString(Integer.toString(1))!=null) {
            int i = 1;
            String reviewAuthor = mReviewData.getAsString(Integer.toString(2*i));
            String reviewContent = mReviewData.getAsString(Integer.toString(2*i+1));
            TextView review = (TextView) findViewById(R.id.tv_review_2);
            mDetailBinding.extraMovieDetails.tvReviewer2.setText(reviewAuthor);
            review.setText(reviewContent);
        } else {
            mDetailBinding.extraMovieDetails.tvReviewer2.setVisibility(View.GONE);
            mDetailBinding.extraMovieDetails.tvReview2.setVisibility(View.GONE);
        }

        if(mReviewData.getAsString(Integer.toString(2))!=null) {
            int i = 2;
            String reviewAuthor = mReviewData.getAsString(Integer.toString(2*i));
            String reviewContent = mReviewData.getAsString(Integer.toString(2*i+1));
            TextView review = (TextView) findViewById(R.id.tv_review_3);
            mDetailBinding.extraMovieDetails.tvReviewer3.setText(reviewAuthor);
            review.setText(reviewContent);
        } else {
            mDetailBinding.extraMovieDetails.tvReviewer3.setVisibility(View.INVISIBLE);
            mDetailBinding.extraMovieDetails.tvReview3.setVisibility(View.INVISIBLE);
        }

        /****************
         * trailers
         *
         * Supports up to three trailers; few movies have that many, let alone more.
         ****************/


        if(mTrailerData.getAsString(Integer.toString(0))!=null) {
            int i=0;
            String trailerTitle = mTrailerData.getAsString(Integer.toString(2*i));
            final String trailerPath = mTrailerData.getAsString(Integer.toString(2*i+1));
            if(trailerTitle!=null) mDetailBinding.extraMovieDetails.tvTrailersTitle1.setText(trailerTitle);
            mDetailBinding.extraMovieDetails.ibTrailer1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchYouTubeIntent(trailerPath);
                }
            });
        } else {
            mDetailBinding.extraMovieDetails.tvTrailersTitle1.setVisibility(View.GONE);
            mDetailBinding.extraMovieDetails.ibTrailer1.setVisibility(View.GONE);
        }

        if(mTrailerData.getAsString(Integer.toString(2))!=null) {
            int i=1;
            String trailerTitle = mTrailerData.getAsString(Integer.toString(2*i));
            final String trailerPath = mTrailerData.getAsString(Integer.toString(2*i+1));
            mDetailBinding.extraMovieDetails.tvTrailersTitle2.setText(trailerTitle);
            mDetailBinding.extraMovieDetails.ibTrailer2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchYouTubeIntent(trailerPath);
                }
            });
        } else {
            mDetailBinding.extraMovieDetails.tvTrailersTitle2.setVisibility(View.GONE);
            mDetailBinding.extraMovieDetails.ibTrailer2.setVisibility(View.GONE);
        }

        if(mTrailerData.getAsString(Integer.toString(0))!=null) {
            int i=2;
            String trailerTitle = mTrailerData.getAsString(Integer.toString(2*i));
            final String trailerPath = mTrailerData.getAsString(Integer.toString(2*i+1));
            if(trailerTitle!=null) mDetailBinding.extraMovieDetails.tvTrailersTitle3.setText(trailerTitle);
            mDetailBinding.extraMovieDetails.ibTrailer3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchYouTubeIntent(trailerPath);
                }
            });
        } else {
            mDetailBinding.extraMovieDetails.tvTrailersTitle3.setVisibility(View.GONE);
            mDetailBinding.extraMovieDetails.ibTrailer3.setVisibility(View.GONE);
        }


        /****************
         * favorite button
         ****************/

        favoritesButton =
                mDetailBinding.primaryMovieInfo.ibFavoriteMovie;
        favoritesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDetailBinding.primaryMovieInfo.ibFavoriteMovie.setText(R.string.added_to_favorite);
                insertMovieIntoFavorites();
            }
        });






    }

    private void launchYouTubeIntent(String trailerPath) {
        Uri launchUri = NetworkUtils.getUriForYoutubeVideo(trailerPath);
        Intent launchIntent = new Intent(Intent.ACTION_VIEW, launchUri);
        startActivity(launchIntent);

    }

    private void loadMovieData() {
        //System.out.println("Loading movie data");
        //System.out.println();
        //showWeatherDataView();

        new FetchWeatherTask().execute(mMovieKey);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, ContentValues[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //mLoadingIndicator.setVisibility(View.VISIBLE);
            //System.out.println("On Pre Execute");
            //System.out.println();
        }

        @Override
        protected ContentValues[] doInBackground(String... params) {
            String movieKey;

    /* There will always be some sort method assigned.*/
            movieKey = params[0];

            URL detailActivityRequestUrl = NetworkUtils.getUrlForDetailActivity(movieKey);
            URL detailReviewsRequestUrl = NetworkUtils.getUrlForDetailReviews(movieKey);
            URL detailTrailersRequestUrl = NetworkUtils.getUrlForDetailTrailers(movieKey);
            //System.out.println("Data request URL for detail activity is "+detailActivityRequestUrl);
            //System.out.println("Reviews request URL for detail activity is "+detailReviewsRequestUrl);
            //System.out.println("Trailers request URL for detail activity is "+detailTrailersRequestUrl);
            try {
                String jsonDetailMovieResponse = NetworkUtils
                        .getResponseFromHttpUrl(detailActivityRequestUrl);
                String jsonReviewsMovieResponse = NetworkUtils.getResponseFromHttpUrl(detailReviewsRequestUrl);
                String jsonTrailersMovieResponse = NetworkUtils.getResponseFromHttpUrl(detailTrailersRequestUrl);

                //System.out.println("Have http responses!");

                ContentValues[] jsonMovieData = OpenWeatherJsonUtils
                        .getFullMovieInfoFromJson(jsonDetailMovieResponse, jsonReviewsMovieResponse, jsonTrailersMovieResponse);
                //System.out.println("Returning JSON MOVIE DATA");

                return jsonMovieData;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ContentValues[] jsonMovieData) {
            //mRecyclerView.setAdapter(mForecastAdapter);

            //mLoadingIndicator.setVisibility(View.INVISIBLE);
            //mRecyclerView.setVisibility(View.VISIBLE);
            if (jsonMovieData != null) {
                mMovieData = jsonMovieData[0];
                mReviewData = jsonMovieData[1];
                mTrailerData = jsonMovieData[2];
                System.out.println("Setting detailed movie info in mData");
                setInfo();
            } else {
                System.out.println("Weather data null. Showing error message. No movie data assigned to ForecastAdapter.");
                //System.out.println();
                //showErrorMessage();
            }

        }
    }

    public void insertMovieIntoFavorites() {
        ContentValues[] movieValuesArray = new ContentValues[1];
        ContentValues movieValues = new ContentValues();

        movieValues.put(MAIN_FORECAST_PROJECTION[INDEX_MOVIE_ID], mMovieData.getAsString(OWM_MOVIE_ID));
        movieValues.put(MAIN_FORECAST_PROJECTION[INDEX_POSTER_PATH], mMovieData.getAsString(OWM_POSTER_PATH));
        movieValues.put(MAIN_FORECAST_PROJECTION[INDEX_MOVIE_TITLE], mMovieData.getAsString(OWM_TITLE));
        movieValuesArray[0] = movieValues;



        //System.out.println("movieValuesArray is null? "+movieValuesArray==null);
        //System.out.println("movieValuesArray[0] is null?"+movieValuesArray[0]==null);
        Uri movieValuesInsertUri = WeatherContract.MovieEntry.CONTENT_URI;

        // Bulk Insert our new weather data into Sunshine's Database
        mContext.getContentResolver().bulkInsert(
                WeatherContract.MovieEntry.CONTENT_URI,
                movieValuesArray);
    }


        //WeatherProvider weatherProvider = new WeatherProvider();
        //weatherProvider.bulkInsert(movieValuesInsertUri, movieValuesArray);

    }
