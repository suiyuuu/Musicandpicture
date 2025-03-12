package com.example.musicandpicture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

public class FullscreenImageAdapter extends RecyclerView.Adapter<FullscreenImageAdapter.ImageViewHolder> {

    private static final String TAG = "FullscreenImageAdapter";
    private Context context;
    private List<MediaItem> imageList;
    private ColorMatrix filterMatrix = new ColorMatrix();
    private ColorMatrixColorFilter currentFilter;
    private Bitmap[] bitmapCache;

    public FullscreenImageAdapter(Context context, List<MediaItem> imageList) {
        this.context = context;
        this.imageList = imageList;
        this.currentFilter = new ColorMatrixColorFilter(filterMatrix);
        this.bitmapCache = new Bitmap[imageList.size()];
    }

    public void setFilter(ColorMatrix matrix) {
        this.filterMatrix = matrix;
        this.currentFilter = new ColorMatrixColorFilter(matrix);
        notifyDataSetChanged();
    }

    public Bitmap getCurrentFilteredBitmap() {
        int currentPosition = ((ImageFullscreenActivity) context).findViewById(R.id.fullscreenViewPager).getVerticalScrollbarPosition();

        if (currentPosition < 0 || currentPosition >= bitmapCache.length || bitmapCache[currentPosition] == null) {
            Log.e(TAG, "No bitmap available at position: " + currentPosition);
            return null;
        }

        // Create a new bitmap and apply the filter
        Bitmap originalBitmap = bitmapCache[currentPosition];
        Bitmap resultBitmap = Bitmap.createBitmap(
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(currentFilter);
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        return resultBitmap;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_fullscreen_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        MediaItem item = imageList.get(position);

        // Load the image using Glide
        Glide.with(context)
                .load(item.getUri())
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                Target<Drawable> target, boolean isFirstResource) {
                        Log.e(TAG, "Image load failed for position: " + position, e);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                                                   Target<Drawable> target, DataSource dataSource,
                                                   boolean isFirstResource) {
                        // When image is loaded, cache the bitmap for filter operations
                        if (resource instanceof BitmapDrawable) {
                            Bitmap loadedBitmap = ((BitmapDrawable) resource).getBitmap();
                            bitmapCache[position] = loadedBitmap;
                        }
                        return false;
                    }
                })
                .into(holder.imageView);

        // Apply current filter
        holder.imageView.setColorFilter(currentFilter);
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.fullscreenImageView);
        }
    }
}