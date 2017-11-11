package com.example.android.popularmovies;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.example.android.popularmovies.data.MovieContract.MovieEntry;
import com.example.android.popularmovies.databinding.ActivityMainBinding;
import com.example.android.popularmovies.utilities.EndlessRecyclerViewScrollListener;
import com.example.android.popularmovies.utilities.JSONDataHandler;
import com.example.android.popularmovies.utilities.NetworkUtils;

import org.json.JSONException;

import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks  {

    private static final String POPULAR = "popular";
    private static final String TOP_RATED = "top_rated";
    private static final String FAVORITES = "favorites";
    private static final String PAGE = "mPage";
    private static final String SORT_BY = "sortBy";
    private static final String MOVIES_ARRAY_LIST = "moviesArrayList";
    private static final String CURRENT_SCROLL_POSITION = "currentScrollPosition";
    private static final String CONNECT_TO_CONTINUE = "Please connect to the Internet to continue";
    private static final String MOVIE_REQUEST_URL = "movie_request_url";

    private static ArrayList<Movie> moviesArrayList = new ArrayList<>();
    private static final String apiKey = ApiKeyFile.MOVIE_DB_API_KEY;
    private String sortBySelectionString = "popular";
    private String mPage = "1";
    private int currentScrollPosition;

    private ActivityMainBinding mainBinding;
    private MoviePosterAdapter moviePosterAdapter;
    private GridLayoutManager gridLayoutManager;

    public static final int MOVIES_LOADER = 1;
    private static final int FAVORITES_LOADER = 3;

    private static final String TAG = MainActivity.class.getSimpleName();

    /**
     * Creates and format the RecyclerView and adds an EndlessRecyclerViewScrollListener to allow endless
     * scrolling.
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //Configure the GridLayoutManager then set it as the layout manager of the RecyclerView
        int noOfColumns = calculateNoOfColumns(this);
        gridLayoutManager = new GridLayoutManager(this, noOfColumns);
        mainBinding.rvMoviePosters.setLayoutManager(gridLayoutManager);

        //Construct and set the adapter for the RecyclerView
        moviePosterAdapter = new MoviePosterAdapter(this);
        mainBinding.rvMoviePosters.setAdapter(moviePosterAdapter);

        //Construct a new Endless Scroll Listener and pass it the GridLayoutManager
        EndlessRecyclerViewScrollListener scrollListener = new EndlessRecyclerViewScrollListener(gridLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                //Use field page instead of the parameter for this method
                //Using the parameter page caused the same page of result to be loaded multiple times
                if (sortBySelectionString != FAVORITES){
                    setmPage(Integer.valueOf(mPage) + 1);
                    Log.d(TAG, "onLoadMore.page: " + String.valueOf(page + 1));
                    Log.d(TAG, "onLoadMore.mPage: " + String.valueOf(mPage + 1));
                    getMovies();

                } else {
                    // TODO: 11/10/2017 onLoadMore() - load more favorites?
                }
            }
        };
        //Add an OnScrollListener to the RecyclerView and pass it the Endless Scroll Listener
        mainBinding.rvMoviePosters.addOnScrollListener(scrollListener);

        checkConnectionStatus();

        //If savedInstanceState isn't null, restore the app's previous state
        if (savedInstanceState != null) {
            mPage = savedInstanceState.getString(PAGE);
            sortBySelectionString = savedInstanceState.getString(SORT_BY);
            currentScrollPosition = savedInstanceState.getInt(CURRENT_SCROLL_POSITION, 0);

            moviesArrayList = savedInstanceState.getParcelableArrayList(MOVIES_ARRAY_LIST);
            moviePosterAdapter.setMoviesArrayList(moviesArrayList, sortBySelectionString);

            //Scroll to the previous position
            mainBinding.rvMoviePosters.smoothScrollToPosition(currentScrollPosition);
        } else {
            moviesArrayList.clear();
            getMovies();
        }
        getSupportLoaderManager().initLoader(MOVIES_LOADER, null, this);
    }

    /**
     * Calculates the number of columns for the GridLayoutManager
     * @param context
     * @return the number of columns
     */
    public static int calculateNoOfColumns (Context context){
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        int scalingFactor = 180;
        return (int) (dpWidth / scalingFactor);
    }

    /**
     * Saves the data needed to restore the RecyclerView's previous state
     * @param outState
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(CURRENT_SCROLL_POSITION, currentScrollPosition);
        outState.putParcelableArrayList(MOVIES_ARRAY_LIST , moviesArrayList);
        outState.putString(PAGE, mPage);
        outState.putString(SORT_BY, sortBySelectionString);
        super.onSaveInstanceState(outState);
    }

    /**
     * Saves the current scroll position
     */
    @Override
    protected void onPause() {
        super.onPause();
        currentScrollPosition = gridLayoutManager.findFirstVisibleItemPosition();
    }

    /**
     * Gets the URL used to request movies then executes doInBackground()
     */
    private void getMovies() {
        URL movieRequestUrl = NetworkUtils.buildMovieDbUrl(sortBySelectionString, mPage);

        Bundle urlBundle = new Bundle();
        urlBundle.putString(MOVIE_REQUEST_URL, movieRequestUrl.toString());

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> movieLoader = loaderManager.getLoader(MOVIES_LOADER);

        if (movieLoader == null){
            loaderManager.initLoader(MOVIES_LOADER, urlBundle, this).forceLoad();
        } else {
            loaderManager.restartLoader(MOVIES_LOADER, urlBundle, this).forceLoad();
        }
        Log.d(TAG, "getMovies: was called");
    }

    /**
     * Sets which mPage of results to display
     * @param mPage
     */
    private void setmPage(int mPage) {
        this.mPage = String.valueOf(mPage);
    }

    @Override
    public Loader onCreateLoader(final int id, final Bundle bundle) {
        Log.d(TAG, "onCreateLoader() returned: " + "was called");

        switch (id){
            case MOVIES_LOADER:
                return new MovieTaskLoader(this, MOVIE_REQUEST_URL, bundle);

            case FAVORITES_LOADER:
                return new CursorLoader(this,
                        MovieEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        MovieEntry.COLUMN_TITLE);
            default:
                return null;
        }

    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {
        switch (loader.getId()){
            case MOVIES_LOADER:
                try {
                    String jsonResultString = (String) data;
                    if (moviesArrayList == null) {
                        moviesArrayList = JSONDataHandler.getMovieArrayList(jsonResultString);
                    } else {
                        ArrayList<Movie> moreMovies = JSONDataHandler.getMovieArrayList(jsonResultString);
                        moviesArrayList.addAll(moreMovies);
                    }

                    moviePosterAdapter.setMoviesArrayList(moviesArrayList, sortBySelectionString);

                } catch (NullPointerException | JSONException e) {
                    e.printStackTrace();
                }
                break;
            case FAVORITES_LOADER:
                if (moviesArrayList != null)
                    moviesArrayList.clear();

                Cursor cursor = (Cursor)data;
                ArrayList<byte[]> posterBytes = new ArrayList<>();
                while (cursor.moveToNext()){
                    Movie movie = new Movie.Builder()
                            .title(cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_TITLE)))
                            .overview(cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_PLOT)))
                            .releaseDate(cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_RELEASE)))
                            .voteAverage(cursor.getString(cursor.getColumnIndex(MovieEntry.COLUMN_RATING)))
                            .build();
                    moviesArrayList.add(movie);
                    posterBytes.add(cursor.getBlob(cursor.getColumnIndex(MovieEntry.COLUMN_POSTER)));
                }
                moviePosterAdapter.setMoviesArrayList(moviesArrayList, sortBySelectionString);
                moviePosterAdapter.setFavoritesPosterArray(posterBytes);
                break;

        }

    }

    @Override
    public void onLoaderReset(Loader loader) {

    }


    private boolean checkConnectionStatus() {
        boolean connected = NetworkUtils.isOnline(this);
        if (!connected) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this)
                    .setMessage("No internet. Favorites can still be viewed. Would you like to continue?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            getFavoriteMovies();
                        }
                    })
                    .setNegativeButton("Connect to Wifi", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                        }
                    });
            final AlertDialog dialog = alert.create();
            dialog.show();
        }
        return connected;
    }

    /**
     * Inflates the main menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        return true;
    }

    /**
     * Changes the selectionString to the value of the selected menu item, clears the current movies
     * from the ArrayList, notifies the Adapter, resets the mPage count, then gets new movie results
     * with the updated selectionString.
     * @param item menu item that was selected
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.popular){
            sortBySelectionString = POPULAR;
            Log.d(TAG, "onOptionsItemSelected: popular was called");
            if (checkConnectionStatus())
                getNewMovieResults();

            return true;
        }

        if (id == R.id.top_rated){
            sortBySelectionString = TOP_RATED;
            Log.d(TAG, "onOptionsItemSelected: top rated was called");
            if (checkConnectionStatus())
                getNewMovieResults();

            return true;
        }
        if (id == R.id.favorites){
            Log.d(TAG, "onOptionsItemSelected: favorites was called");

            getFavoriteMovies();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void getFavoriteMovies() {
        sortBySelectionString = FAVORITES;

        LoaderManager loaderManager = getSupportLoaderManager();
        Loader<String> favoritesLoader = loaderManager.getLoader(FAVORITES_LOADER);

        if (favoritesLoader == null){
            loaderManager.initLoader(FAVORITES_LOADER, null, this);
        } else {
            loaderManager.restartLoader(FAVORITES_LOADER, null, this);
        }
        Log.d(TAG, "getFavoriteMovies: was called");
    }

    /**
     * Clears the ArrayList of Movies, notifies the adapter, then gets new Movies to store in the
     * ArrayList
     */
    private void getNewMovieResults() {
        int size = moviesArrayList.size();

        //removes old movie results before getting new results
        moviesArrayList.clear();
        moviePosterAdapter.notifyItemRangeRemoved(0, size);

        //resets mPage count for new movie results
        setmPage(1);
        getMovies();
    }
}
