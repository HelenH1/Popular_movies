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
package com.example.android.sunshine.utilities;

import android.content.ContentValues;
import android.content.Context;

import com.example.android.sunshine.data.SunshinePreferences;
import com.example.android.sunshine.data.WeatherContract;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Utility functions to handle OpenWeatherMap JSON data.
 */
public final class OpenWeatherJsonUtils {

    /* Location information */
    private static final String OWM_CITY = "city";
    private static final String OWM_COORD = "coord";

    private static final String OWM_MOVIE_ID = "movie_id";
    private static final String OWM_POSTER_PATH = "movie_poster_path";
    private static final String OWM_TITLE = "movie_title";
    private static final String OWM_SYNOPSIS = "movie_synopsis";
    private static final String OWM_RATING = "movie_rating";
    private static final String OWM_RELEASE_DATE = "movie_release_date";
    private static final String OWM_REVIEWS = "movie_reviews";
    private static final String OWM_TRAILERS = "movie_trailers";

    /* Location coordinate */
    private static final String OWM_LATITUDE = "lat";
    private static final String OWM_LONGITUDE = "lon";

    /* Weather information. Each day's forecast info is an element of the "list" array */
    private static final String OWM_LIST = "list";

    private static final String OWM_PRESSURE = "pressure";
    private static final String OWM_HUMIDITY = "humidity";
    private static final String OWM_WINDSPEED = "speed";
    private static final String OWM_WIND_DIRECTION = "deg";

    /* All temperatures are children of the "temp" object */
    private static final String OWM_TEMPERATURE = "temp";

    /* Max temperature for the day */
    private static final String OWM_MAX = "max";
    private static final String OWM_MIN = "min";

    private static final String OWM_WEATHER = "weather";
    private static final String OWM_WEATHER_ID = "id";

    private static final String OWM_MESSAGE_CODE = "cod";
    private static final int NUM_MOVIES = 20;

    public static ContentValues[] getFullMovieInfoFromJson(String mainJsonStr, String reviewsJsonStr, String trailersJsonStr)
            throws JSONException {


        //System.out.println("Getting full movie info from JSON");
        /* Weather information. Each day's forecast info is an element of the "list" array */
        final String OWM_LIST = "results";

        final String OWM_MESSAGE_CODE = "cod";

        /* String array to hold each day's weather String */

        ContentValues[] fullMovieData = new ContentValues[3];
        JSONObject mainJson = new JSONObject(mainJsonStr);
        JSONObject reviewsJson = new JSONObject(reviewsJsonStr);
        JSONObject trailersJson = new JSONObject(trailersJsonStr);

        JSONArray reviewsJsonArray = reviewsJson.getJSONArray(OWM_LIST);
        JSONArray trailersJsonArray = trailersJson.getJSONArray(OWM_LIST);
        //System.out.println("Have JSON objects and arrays");

        /* Is there an error? */
        if (mainJson.has(OWM_MESSAGE_CODE)) {
            int errorCode = mainJson.getInt(OWM_MESSAGE_CODE);

            switch (errorCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    /* Location invalid */
                    return null;
                default:
                    /* Server probably down */
                    return null;
            }
        }

        //Movie Details layout contains title, release date, movie poster, vote average, and plot synopsis.
        //Movie Details layout contains a section for displaying trailer videos and user reviews.
            ContentValues movieValues = new ContentValues();
            String movieId = mainJson.getString("id");
            movieValues.put(OWM_MOVIE_ID, movieId);

            String posterPath = mainJson.getString("poster_path");
            movieValues.put(OWM_POSTER_PATH, posterPath);

            String title = mainJson.getString("title");
            movieValues.put(OWM_TITLE, title);

            String synopsis = mainJson.getString("overview");
            movieValues.put(OWM_SYNOPSIS, synopsis);

            String rating = mainJson.getString("vote_average");
            movieValues.put(OWM_RATING, rating);

            String releaseDate = mainJson.getString("release_date");
            movieValues.put(OWM_RELEASE_DATE, releaseDate);

            ContentValues reviewValues = new ContentValues();
            for(int i = 0; i<reviewsJsonArray.length(); i++) {
                JSONObject singleReview = reviewsJsonArray.getJSONObject(i);
                String reviewAuthor = singleReview.getString("author");
                String reviewContent = singleReview.getString("content");
                reviewValues.put(Integer.toString(2*i), reviewAuthor);
                reviewValues.put(Integer.toString(2*i+1), reviewContent);
            }

            ContentValues trailerValues = new ContentValues();
            for(int i = 0; i<trailersJsonArray.length(); i++) {
                JSONObject singleTrailer = trailersJsonArray.getJSONObject(i);
                String videoTitle = singleTrailer.getString("name");
                String videoPath = singleTrailer.getString("key");
                trailerValues.put(Integer.toString(2*i), videoTitle);
                trailerValues.put(Integer.toString(2*i+1), videoPath);
            }

            fullMovieData[0] = movieValues;
            fullMovieData[1] = reviewValues;
            fullMovieData[2] = trailerValues;

        return fullMovieData;
    }


    /**
     * This method parses JSON from a web response and returns an array of Strings
     * describing the weather over various days from the forecast.
     * <p/>
     * Later on, we'll be parsing the JSON into structured data within the
     * getFullWeatherDataFromJson function, leveraging the data we have stored in the JSON. For
     * now, we just convert the JSON into human-readable strings.
     *
     * @param forecastJsonStr JSON response from server
     *
     * @return Array of Strings describing weather data
     *
     * @throws JSONException If JSON data cannot be properly parsed
     */
    public static ContentValues[] getBasicMovieInfoFromJson(String forecastJsonStr)
            throws JSONException {

        //System.out.println("Getting basic movie info from JSON");
        /* Weather information. Each day's forecast info is an element of the "list" array */
        final String OWM_LIST = "results";

        final String OWM_MESSAGE_CODE = "cod";

        /* String array to hold each day's weather String */
        ContentValues[] movieContentValues = new ContentValues[NUM_MOVIES];

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        //System.out.println("MovieContentValues and forecastJson now created.");
        /* Is there an error? */
        if (forecastJson.has(OWM_MESSAGE_CODE)) {
            int errorCode = forecastJson.getInt(OWM_MESSAGE_CODE);

            switch (errorCode) {
                case HttpURLConnection.HTTP_OK:
                    break;
                case HttpURLConnection.HTTP_NOT_FOUND:
                    //System.out.println("HTTP URL Connection not found");
                    /* Location invalid */
                    return null;
                default:
                    /* Server probably down */
                    //System.out.println("Defaulted null");
                    return null;
            }
            //System.out.println("Past error switch");
        }

        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

        for (int i = 0; i < movieContentValues.length; i++) {
            ContentValues movieValues = new ContentValues();

            /* Get the JSON object representing the day */
            JSONObject dayForecast = weatherArray.getJSONObject(i);

            movieValues.put(OWM_MOVIE_ID, dayForecast.getString("id"));
            String posterPath = dayForecast.getString("poster_path");
            //System.out.println("Poster path in Json util is "+posterPath);
            movieValues.put("movie_poster_path", posterPath);

            movieContentValues[i] = movieValues;
        }

        return movieContentValues;
    }
}