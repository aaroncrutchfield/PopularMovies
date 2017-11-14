package com.example.android.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by AaronC on 7/26/2017.
 */

public class PosterAdapter extends RecyclerView.Adapter<PosterAdapter.MoviePosterAdapterViewHolder>{

    private static final String TAG = PosterAdapter.class.getSimpleName();

    private static final String MOVIE = "movie";

    private Context context;
    private ArrayList<Movie> movieArrayList = new ArrayList<>();
    private ArrayList<byte[]> favoritesPosterArray = new ArrayList<>();

    private Boolean viewingFavorites;

    PosterAdapter(Context context){
        this.context = context;
    }

    public class MoviePosterAdapterViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        final ImageView moviePoster;
        final TextView tvTitle;

        MoviePosterAdapterViewHolder(View itemView) {
            super(itemView);
            moviePoster = itemView.findViewById(R.id.iv_movie_poster);
            tvTitle = itemView.findViewById(R.id.tv_movie_title);

            itemView.setOnClickListener(this);
        }

        /**
         * Stores the Movie Object at the current adapter position into an intent and passes it to
         * the DetailsActivity
         * @param view The moviePoster ImageView that was clicked
         */
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(view.getContext(), DetailsActivity.class);
            int position = getAdapterPosition();

            intent.putExtra(MOVIE, movieArrayList.get(position));
            view.getContext().startActivity(intent);
        }
    }

    /**
     * Inflates the layout of the Movie list_item then returns a ViewHolder containing that layout
     * @param parent the parent ViewGroup
     * @param viewType identifies the type of view
     * @return ViewHolder containing the inflated layout
     */
    @Override
    public MoviePosterAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int movieListItemId = R.layout.movie_list_item;
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        View view = layoutInflater.inflate(movieListItemId, parent, false);

        return new MoviePosterAdapterViewHolder(view);
    }

    /**
     * Loads the poster image and binds it to the ImageView in the ViewHolder
     * @param holder the ViewHolder
     * @param position the position of the ViewHolder to be bound
     */
    @Override
    public void onBindViewHolder(MoviePosterAdapterViewHolder holder, int position) {
        Movie currentMovie = movieArrayList.get(position);
        String currentMoviePoster = currentMovie.getPosterLocationUriString();
        String movieTitle = movieArrayList.get(position).getTitle();

        //Load image from the internet if this isn't a favorite movie
        if (!viewingFavorites) {
            Picasso.with(context)
                    .load(currentMoviePoster)
                    .fit()
                    .into(holder.moviePoster);
        } else {
            //Load the image from the byteArray and convert to a bitmap
            byte[] imageBytes = favoritesPosterArray.get(position);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            holder.moviePoster.setImageBitmap(bitmap);
        }
        holder.tvTitle.setText(movieTitle);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        if (null == movieArrayList) return 0;
        return movieArrayList.size();
    }

    /**
     * Clears previous data from the ArrayList of Movies, adds new Movies, then notifies the Adapter
     * @param moviesArrayList arrayList containing all of the movies
     * @param sortBySelectionString String to inform if these are favorite movies
     */
    void setMoviesArrayList(ArrayList<Movie> moviesArrayList, String sortBySelectionString) {
        this.movieArrayList.clear();
        this.movieArrayList.addAll(moviesArrayList);

        //Set viewingFavorites for use later
        viewingFavorites = sortBySelectionString.equals("favorites");

        notifyDataSetChanged();
    }

    /**
     * Sets an arrayList containing byteArray representation of the favorite movie posters
     * @param posterBytes byteArray representation of the poster
     */
    void setFavoritesPosterArray(ArrayList<byte[]> posterBytes){
        this.favoritesPosterArray.clear();
        this.favoritesPosterArray.addAll(posterBytes);
    }
}