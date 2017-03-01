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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.example.android.sunshine.data.SunshinePreferences;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

/**
 * These utilities will be used to communicate with the weather servers.
 */
public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    private static final String DYNAMIC_WEATHER_URL =
            "https://www.themoviedb.org/3/movie/";

    private static final String STATIC_WEATHER_URL =
            "https://api.themoviedb.org/3/movie/";

    private static final String TMDB_BASE_URL = STATIC_WEATHER_URL;

    private static final String YOUTUBE_BASE_URL =
            "https://www.youtube.com/";

    /*
     * NOTE: These values only effect responses from OpenWeatherMap, NOT from the fake weather
     * server. They are simply here to allow us to teach you how to build a URL if you were to use
     * a real API.If you want to connect your app to OpenWeatherMap's API, feel free to! However,
     * we are not going to show you how to do so in this course.
     */

    /* The format we want our API to return */
    private static final int page = 1;
    private static final String api_key = "";
    private static final String include_adult = "false";
    private static final String include_video = "false";
    private static final String language = "en-US";

    final static String LANG_PARAM = "language";
    final static String API_KEY_PARAM = "api_key";
    final static String SORT_BY_PARAM = "sort_by";
    final static String INCLUDE_ADULT_PARAM = "include_adult";
    final static String INCLUDE_VIDEO = "include_video";
    final static String PAGE_PARAM = "page";

    /* The query parameter allows us to provide a location string to the API */
    private static final String QUERY_PARAM = "q";

    private static final String MOVIE_KEY_PARAM = "";
    private static final String LON_PARAM = "lon";

    /* The format parameter allows us to designate whether we want JSON or XML from our API */
    private static final String FORMAT_PARAM = "mode";
    /* The units parameter allows us to designate whether we want metric units or imperial units */
    private static final String UNITS_PARAM = "units";
    /* The days parameter allows us to designate how many days of weather data we want */
    private static final String DAYS_PARAM = "cnt";


    public static URL getUrlForMainActivity(String sortString) {
        Uri builtUri = Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(sortString)
                .appendQueryParameter(API_KEY_PARAM, api_key)
                .appendQueryParameter(LANG_PARAM, language)
                //.appendQueryParameter(SORT_BY_PARAM, sortString)
                //.appendQueryParameter(INCLUDE_ADULT_PARAM, include_adult)
                //.appendQueryParameter(INCLUDE_VIDEO, include_video)
                //.appendQueryParameter(PAGE_PARAM, Integer.toString(page))
                .build();
        URL url = null;
        try {
            url = new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //System.out.println("URL is " + url);
        return url;
    }


    public static URL getUrlForDetailActivity(String movie_key) {
        Uri weatherQueryUri = Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(movie_key)
                .appendQueryParameter(API_KEY_PARAM, api_key)
                .build();

        URL url = null;
        try {
            url = new URL(weatherQueryUri.toString());
            Log.v(TAG, "URL: " + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //System.out.println("URL is " + url);
        return url;
    }

    public static URL getUrlForDetailReviews(String movie_key) {
        Uri weatherQueryUri = Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(movie_key)
                .appendPath("reviews")
                .appendQueryParameter(API_KEY_PARAM, api_key)
                .build();

        URL url = null;
        try {
            url = new URL(weatherQueryUri.toString());
            Log.v(TAG, "URL: " + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //System.out.println("URL is " + url);
        return url;
    }

    public static URL getUrlForDetailTrailers(String movie_key) {
        Uri weatherQueryUri = Uri.parse(TMDB_BASE_URL).buildUpon()
                .appendPath(movie_key)
                .appendPath("videos")
                .appendQueryParameter(API_KEY_PARAM, api_key)
                .build();

        URL url = null;
        try {
            url = new URL(weatherQueryUri.toString());
            Log.v(TAG, "URL: " + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        //System.out.println("URL is " + url);
        return url;
    }

    public static Uri getUriForYoutubeVideo(String trailer_path) {
        Uri trailerUri = Uri.parse(YOUTUBE_BASE_URL).buildUpon()
                .appendPath(trailer_path)
                .build();

        /*URL url = null;
        try {
            url = new URL(trailerUri.toString());
            Log.v(TAG, "URL: " + url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }*/
        //System.out.println("URL is " + url);
        //return url;
        return trailerUri;

    }


    /*public static String getResponseFromHttpUrl(URL url) throws IOException {
        System.out.println("Attempting to get HTTP response");

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        System.out.println("URL connection made");

        try {
            System.out.println("In try segment");
            InputStream in = urlConnection.getInputStream();

            System.out.println("Got input stream");
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");
            System.out.println("Used scanner delimiter");

            boolean hasInput = scanner.hasNext();
            String response = null;
            if (hasInput) {
                response = scanner.next();
            }
            System.out.println("About to close scanner");
            scanner.close();
            System.out.println("Returning http response.");
            return response;
        } finally {
            urlConnection.disconnect();
        }
    }*/



    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response.
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        //System.out.println("Attempting to get HTTP response");

        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        //System.out.println("URL connection made");

        try {
            //System.out.println("In try segment");
            InputStream in = urlConnection.getInputStream();

            //System.out.println("Got input stream");
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");
            //System.out.println("Used scanner delimiter");

            boolean hasInput = scanner.hasNext();
            if (hasInput) {
                //System.out.println("Returning http response.");
                return scanner.next();

            } else {
                //System.out.println("Returning null");
                return null;
            }
        } finally {
            urlConnection.disconnect();
        }
    }
}
