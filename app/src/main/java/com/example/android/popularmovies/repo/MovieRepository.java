/*
 * <!--
 *  Copyright (C) 2016 The Android Open Source Project
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 * -->
 */

package com.example.android.popularmovies.repo;

import android.util.Log;

import com.example.android.popularmovies.ApiKeyFile;
import com.example.android.popularmovies.data.Movie;
import com.example.android.popularmovies.data.Movies;
import com.example.android.popularmovies.utilities.MovieDbService;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
/**
 * TODO 7/1/2019 NEW FEAT:
 *  Repo class will be the center of data access.
 *  We are also use to store data in cache and
 *  use that data when offline using Repo class. - Emre
 */
public class MovieRepository {
    private static final String MOVIE_DB_BASE_URL = "http://api.themoviedb.org/3/movie/";
    private static final String SORT_BY_POPULAR = "popular";
    private static final String REVIEWS = "reviews";
    private static final String COMMA_SEPARATOR = ",";
    private static final String TRAILERS = "videos";

    private String pageNumber = "";
    private MovieDbService movieDbService;
    private MutableLiveData<List<Movie>> serverMovies;
    private MutableLiveData<Movie> serverMovieDetails;

    public MovieRepository() {
        serverMovies = new MutableLiveData<>();
        serverMovieDetails = new MutableLiveData<>();
        movieDbService = createMovieDbService();
    }

    public LiveData<List<Movie>> getMoviesFromServer() {
        // Create the Call by calling the @GET method from the Service
        Call<Movies> call = movieDbService
                .getSortedMovies(
                    SORT_BY_POPULAR,
                    ApiKeyFile.MOVIE_DB_API_KEY,
                    pageNumber);
        // Use the method enqueue from the Call to act upon onResponse and onFailure
        call.enqueue(new Callback<Movies>() {
            @Override
            public void onResponse(@NonNull Call<Movies> call, @NonNull Response<Movies> response) {
                Movies movies = response.body();
                if(movies != null) {
                    serverMovies.setValue(movies.getMovies());
                }
            }

            @Override
            public void onFailure(@NonNull Call<Movies> call,@NonNull Throwable t) {
                /**
                 * TODO 7/1/2019 NEW FEAT:
                 *  Instead of creating a log message, we need to use
                 *  either cache or database to display those results
                 *  and may be show a Toast about connection error here - Emre
                 */
                Log.d("MovieRepository", "onFailure: " + t.getMessage());
            }
        });
        return serverMovies;
    }

    public LiveData<Movie> getMovieDetailsFromServer(String movieId) {
        // Create the Call by calling the @GET method from the Service
        Call<Movie> call = movieDbService
                .getDetails(
                        movieId,
                        ApiKeyFile.MOVIE_DB_API_KEY,
                        REVIEWS + COMMA_SEPARATOR + TRAILERS);
        // Use the method enqueue from the Call to act upon onResponse and onFailure
        call.enqueue(new Callback<Movie>() {
            @Override
            public void onResponse(@NonNull Call<Movie> call, @NonNull Response<Movie> response) {
                Movie movie = response.body();
                if(movie != null) {
                    serverMovieDetails.setValue(movie);
                }
            }
            @Override
            public void onFailure(@NonNull Call<Movie> call, @NonNull Throwable t) {
                /**
                 * TODO 7/1/2019 NEW FEAT:
                 *  Instead of creating a log message, we need to use
                 *  either cache or database to display those results
                 *  and may be show a Toast about connection error here - Emre
                 */
                Log.d("DetailsActivity", "onFailure: " + t.getMessage());
            }
        });
        return serverMovieDetails;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = String.valueOf(pageNumber);
    }

    public void loadMoviesFromServer(int pageNumber) {
        setPageNumber(pageNumber);
        getMoviesFromServer();
    }

    private MovieDbService createMovieDbService() {
        // Build Http Client
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        // Build Retrofit Object with Base URL
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(MOVIE_DB_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(httpClient.build())
                .build();
        // Create the Service Object containing the @GET call
        return retrofit.create(MovieDbService.class);
    }
}