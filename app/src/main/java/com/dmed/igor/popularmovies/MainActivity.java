package com.dmed.igor.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.dmed.igor.popularmovies.utilities.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;

public class MainActivity extends AppCompatActivity implements MoviesAdapter.GridItemClickListener {

    private RecyclerView mRecyclerView;

    private MoviesAdapter mMoviesAdapter;

    private TextView mErrorMessageDisplay;

    private ProgressBar mLoadingIndicator;

    private static final String POPULARITY_SORT_OPTION = "popular";

    private static final String TOP_RATED_SORT_OPTION = "top_rated";

    private String[] mPosterData;

    private String[] mMovieIdData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerview_movies);

        mErrorMessageDisplay = (TextView) findViewById(R.id.tv_error_message_display);

        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 3);

        mRecyclerView.setLayoutManager(gridLayoutManager);

        mRecyclerView.setHasFixedSize(true);

        mMoviesAdapter = new MoviesAdapter(this);

        mRecyclerView.setAdapter(mMoviesAdapter);

        mLoadingIndicator = (ProgressBar) findViewById(R.id.pb_loading_indicator);

        loadMoviesData(POPULARITY_SORT_OPTION);
    }

    private void loadMoviesData(String sortOption) {
        showMoviesDataView();

        if (isOnline()) {
            new FetchMoviesTask().execute(sortOption);
        } else {
            showErrorMessage();
        }
    }

    private void showMoviesDataView() {
        mErrorMessageDisplay.setVisibility(View.INVISIBLE);

        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        mRecyclerView.setVisibility(View.INVISIBLE);

        mErrorMessageDisplay.setVisibility(View.VISIBLE);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    @Override
    public void onGridItemClick(int clickedItemIndex) {
        if (isOnline()) {
            new FetchMovieDetailTask().execute(mMovieIdData[clickedItemIndex]);
        } else {
            showErrorMessage();
        }
    }

    public class FetchMoviesTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected String[] doInBackground(String... strings) {

            String sortOption = strings[0];
            URL moviesRequestUrl = NetworkUtils.buildUrl(sortOption);

            try {
                String jsonResponse = NetworkUtils
                        .getResponseFromHttpUrl(moviesRequestUrl);

                mPosterData = null;
                mMovieIdData = null;

                JSONObject moviesJson = new JSONObject(jsonResponse);
                JSONArray moviesList = moviesJson.getJSONArray("results");

                mPosterData = new String[moviesList.length()];
                mMovieIdData = new String[moviesList.length()];

                for (int i = 0; i < moviesList.length(); i++) {
                    JSONObject movieObject = moviesList.getJSONObject(i);

                    mPosterData[i] = movieObject.getString("poster_path");
                    mMovieIdData[i] = movieObject.getString("id");
                }

                return mPosterData;

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String[] moviesData) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
            if (moviesData != null) {
                showMoviesDataView();

                mMoviesAdapter.setMoviesData(moviesData);
            } else {
                showErrorMessage();
            }
        }

    }

    public class FetchMovieDetailTask extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mLoadingIndicator.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(String... strings) {

            String movieId = strings[0];
            URL movieDetailRequestUrl = NetworkUtils.buildUrl(movieId);

            try {
                String jsonResponse = NetworkUtils
                        .getResponseFromHttpUrl(movieDetailRequestUrl);

                JSONObject movieDetailJson = new JSONObject(jsonResponse);

                Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                intent.putExtra("DETAIL_JSON", movieDetailJson.toString());
                startActivity(intent);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mLoadingIndicator.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();

        inflater.inflate(R.menu.movies_selection, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_sort) {

            View menuItemView = findViewById(R.id.action_sort);
            PopupMenu popup = new PopupMenu(this, menuItemView);

            MenuInflater inflater = popup.getMenuInflater();
            // Inflate the menu from xml
           inflater.inflate(R.menu.sort_options, popup.getMenu());
            popup.show();

            // Setup menu item selection
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    mMoviesAdapter.setMoviesData(null);

                    switch (item.getItemId()) {
                        case R.id.sort_option_popularity:
                            loadMoviesData(POPULARITY_SORT_OPTION);
                            return true;
                        case R.id.sort_option_top_rated:
                            loadMoviesData(TOP_RATED_SORT_OPTION);
                            return true;
                        default:
                            return false;
                    }
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }
}
