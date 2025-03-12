package com.example.musicandpicture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicandpicture.database.AppDatabase;
import com.example.musicandpicture.database.ImageNoteEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageFullscreenActivity extends AppCompatActivity {

    private static final String TAG = "ImageFullscreenActivity";
    private ViewPager2 fullscreenViewPager;
    private Toolbar fullscreenToolbar;
    private ImageButton flipImageButton;
    private CardView filterControlsCard;
    private CardView imageBacksideCard;
    private EditText imageNotesEditText;
    private Button saveNotesButton;

    private Button filterNoneButton;
    private Button filterRockButton;
    private Button filterPopButton;
    private Button filterJazzButton;
    private Button filterClassicalButton;
    private Button filterElectronicButton;
    private Button saveImageButton;
    private Button shareImageButton;

    private List<MediaItem> imageList;
    private FullscreenImageAdapter adapter;
    private int initialPosition;
    private boolean isBacksideShowing = false;
    private String currentFilterName = "none";

    // Database operations executor
    private final Executor executor = Executors.newSingleThreadExecutor();

    // ColorMatrix for different filters
    private ColorMatrix currentFilterMatrix = new ColorMatrix();
    private static final ColorMatrix ROCK_FILTER = createRockFilter();
    private static final ColorMatrix POP_FILTER = createPopFilter();
    private static final ColorMatrix JAZZ_FILTER = createJazzFilter();
    private static final ColorMatrix CLASSICAL_FILTER = createClassicalFilter();
    private static final ColorMatrix ELECTRONIC_FILTER = createElectronicFilter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_fullscreen);

        // Get the initial position from intent
        initialPosition = getIntent().getIntExtra("position", 0);

        // Use global image list from MainActivity
        imageList = new ArrayList<>(MainActivity.globalImageList);

        initViews();
        setupToolbar();
        setupViewPager();
        setupFilterButtons();
        setupSaveShareButtons();
        setupFlipFunctionality();

        // Load any existing notes for the current image
        loadImageNotes();
    }

    private void initViews() {
        fullscreenViewPager = findViewById(R.id.fullscreenViewPager);
        fullscreenToolbar = findViewById(R.id.fullscreenToolbar);
        flipImageButton = findViewById(R.id.flipImageButton);
        filterControlsCard = findViewById(R.id.filterControlsCard);
        imageBacksideCard = findViewById(R.id.imageBacksideCard);
        imageNotesEditText = findViewById(R.id.imageNotesEditText);
        saveNotesButton = findViewById(R.id.saveNotesButton);

        filterNoneButton = findViewById(R.id.filterNoneButton);
        filterRockButton = findViewById(R.id.filterRockButton);
        filterPopButton = findViewById(R.id.filterPopButton);
        filterJazzButton = findViewById(R.id.filterJazzButton);
        filterClassicalButton = findViewById(R.id.filterClassicalButton);
        filterElectronicButton = findViewById(R.id.filterElectronicButton);
        saveImageButton = findViewById(R.id.saveImageButton);
        shareImageButton = findViewById(R.id.shareImageButton);
    }

    private void setupToolbar() {
        setSupportActionBar(fullscreenToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
    }

    private void setupViewPager() {
        adapter = new FullscreenImageAdapter(this, imageList);
        fullscreenViewPager.setAdapter(adapter);
        fullscreenViewPager.setCurrentItem(initialPosition, false);

        // Add page change listener to update UI when swiping
        fullscreenViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // If showing backside, flip back to front when changing images
                if (isBacksideShowing) {
                    flipCard();
                }

                // Reset filter when changing images
                resetFilter();

                // Load notes for the new image
                loadImageNotes();
            }
        });
    }

    private void resetFilter() {
        currentFilterMatrix.reset();
        currentFilterName = "none";
        adapter.setFilter(currentFilterMatrix);

        // Reset active button states
        updateFilterButtonStates();
    }

    private void setupFilterButtons() {
        // Reset filter (Normal view)
        filterNoneButton.setOnClickListener(v -> {
            currentFilterMatrix.reset();
            currentFilterName = "none";
            applyFilter();
            updateFilterButtonStates();
        });

        // Rock filter
        filterRockButton.setOnClickListener(v -> {
            currentFilterMatrix = ROCK_FILTER;
            currentFilterName = "rock";
            applyFilter();
            updateFilterButtonStates();
        });

        // Pop filter
        filterPopButton.setOnClickListener(v -> {
            currentFilterMatrix = POP_FILTER;
            currentFilterName = "pop";
            applyFilter();
            updateFilterButtonStates();
        });

        // Jazz filter
        filterJazzButton.setOnClickListener(v -> {
            currentFilterMatrix = JAZZ_FILTER;
            currentFilterName = "jazz";
            applyFilter();
            updateFilterButtonStates();
        });

        // Classical filter
        filterClassicalButton.setOnClickListener(v -> {
            currentFilterMatrix = CLASSICAL_FILTER;
            currentFilterName = "classical";
            applyFilter();
            updateFilterButtonStates();
        });

        // Electronic filter
        filterElectronicButton.setOnClickListener(v -> {
            currentFilterMatrix = ELECTRONIC_FILTER;
            currentFilterName = "electronic";
            applyFilter();
            updateFilterButtonStates();
        });

        // Set initial button states
        updateFilterButtonStates();
    }

    private void updateFilterButtonStates() {
        // Reset all buttons to default state
        filterNoneButton.setAlpha(0.7f);
        filterRockButton.setAlpha(0.7f);
        filterPopButton.setAlpha(0.7f);
        filterJazzButton.setAlpha(0.7f);
        filterClassicalButton.setAlpha(0.7f);
        filterElectronicButton.setAlpha(0.7f);

        // Highlight the active filter button
        switch (currentFilterName) {
            case "none":
                filterNoneButton.setAlpha(1.0f);
                break;
            case "rock":
                filterRockButton.setAlpha(1.0f);
                break;
            case "pop":
                filterPopButton.setAlpha(1.0f);
                break;
            case "jazz":
                filterJazzButton.setAlpha(1.0f);
                break;
            case "classical":
                filterClassicalButton.setAlpha(1.0f);
                break;
            case "electronic":
                filterElectronicButton.setAlpha(1.0f);
                break;
        }
    }

    private void applyFilter() {
        adapter.setFilter(currentFilterMatrix);
        saveCurrentFilter();
    }

    private void saveCurrentFilter() {
        // Save the current filter to database
        int currentPosition = fullscreenViewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < imageList.size()) {
            final MediaItem currentItem = imageList.get(currentPosition);
            final String imageUri = currentItem.getUri().toString();

            executor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                    // Check if we already have a record for this image
                    ImageNoteEntity entity = db.imageNoteDao().getImageNoteByUri(imageUri);

                    if (entity == null) {
                        // Create new entity
                        entity = new ImageNoteEntity(imageUri);
                        entity.setAppliedFilter(currentFilterName);
                        db.imageNoteDao().insert(entity);
                    } else {
                        // Update existing entity
                        db.imageNoteDao().updateFilter(imageUri, currentFilterName);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving filter to database", e);
                }
            });
        }
    }

    private void setupSaveShareButtons() {
        // Save image with current filter
        saveImageButton.setOnClickListener(v -> {
            Bitmap filteredBitmap = adapter.getCurrentFilteredBitmap();
            if (filteredBitmap != null) {
                saveImageToGallery(filteredBitmap);
            } else {
                Toast.makeText(this, R.string.error_saving_image, Toast.LENGTH_SHORT).show();
            }
        });

        // Share image with current filter
        shareImageButton.setOnClickListener(v -> {
            Bitmap filteredBitmap = adapter.getCurrentFilteredBitmap();
            if (filteredBitmap != null) {
                shareImage(filteredBitmap);
            } else {
                Toast.makeText(this, R.string.error_sharing_image, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFlipFunctionality() {
        // Flip card button
        flipImageButton.setOnClickListener(v -> flipCard());

        // Save notes button
        saveNotesButton.setOnClickListener(v -> {
            String notes = imageNotesEditText.getText().toString();
            saveImageNotes(notes);
            Toast.makeText(this, R.string.notes_saved, Toast.LENGTH_SHORT).show();

            // Flip back to front side after saving
            flipCard();
        });
    }

    private void loadImageNotes() {
        int currentPosition = fullscreenViewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < imageList.size()) {
            final MediaItem currentItem = imageList.get(currentPosition);
            final String imageUri = currentItem.getUri().toString();

            executor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                    final ImageNoteEntity entity = db.imageNoteDao().getImageNoteByUri(imageUri);

                    runOnUiThread(() -> {
                        if (entity != null) {
                            // Load notes
                            if (entity.getNotes() != null) {
                                imageNotesEditText.setText(entity.getNotes());
                            } else {
                                imageNotesEditText.setText("");
                            }

                            // Apply saved filter
                            if (entity.getAppliedFilter() != null) {
                                applyFilterByName(entity.getAppliedFilter());
                            } else {
                                resetFilter();
                            }
                        } else {
                            // No saved data for this image
                            imageNotesEditText.setText("");
                            resetFilter();
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error loading image notes from database", e);
                }
            });
        }
    }

    private void applyFilterByName(String filterName) {
        currentFilterName = filterName;

        switch (filterName) {
            case "rock":
                currentFilterMatrix = ROCK_FILTER;
                break;
            case "pop":
                currentFilterMatrix = POP_FILTER;
                break;
            case "jazz":
                currentFilterMatrix = JAZZ_FILTER;
                break;
            case "classical":
                currentFilterMatrix = CLASSICAL_FILTER;
                break;
            case "electronic":
                currentFilterMatrix = ELECTRONIC_FILTER;
                break;
            case "none":
            default:
                currentFilterMatrix = new ColorMatrix();
                break;
        }

        adapter.setFilter(currentFilterMatrix);
        updateFilterButtonStates();
    }

    private void saveImageNotes(String notes) {
        int currentPosition = fullscreenViewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < imageList.size()) {
            final MediaItem currentItem = imageList.get(currentPosition);
            final String imageUri = currentItem.getUri().toString();
            final String noteText = notes;

            executor.execute(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(getApplicationContext());

                    // Check if we already have a record for this image
                    ImageNoteEntity entity = db.imageNoteDao().getImageNoteByUri(imageUri);

                    if (entity == null) {
                        // Create new entity
                        entity = new ImageNoteEntity(imageUri);
                        entity.setNotes(noteText);
                        entity.setAppliedFilter(currentFilterName);
                        db.imageNoteDao().insert(entity);
                    } else {
                        // Update existing entity
                        db.imageNoteDao().updateNotes(imageUri, noteText);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving notes to database", e);
                    runOnUiThread(() -> {
                        Toast.makeText(ImageFullscreenActivity.this,
                                "Failed to save notes: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void flipCard() {
        // Disable flip button during animation
        flipImageButton.setEnabled(false);

        // Prepare animation
        final View visibleView = isBacksideShowing ? imageBacksideCard : fullscreenViewPager;
        final View invisibleView = isBacksideShowing ? fullscreenViewPager : imageBacksideCard;

        // Show the view that will become visible
        invisibleView.setVisibility(View.VISIBLE);
        invisibleView.setAlpha(0f);

        // Set filter controls visibility
        filterControlsCard.setVisibility(isBacksideShowing ? View.VISIBLE : View.GONE);

        // Create rotation and fade animations
        ObjectAnimator rotation = ObjectAnimator.ofFloat(visibleView, "rotationY", 0f, 90f);
        rotation.setDuration(300);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        rotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                visibleView.setVisibility(View.GONE);

                ObjectAnimator rotationIn = ObjectAnimator.ofFloat(invisibleView, "rotationY", -90f, 0f);
                rotationIn.setDuration(300);
                rotationIn.setInterpolator(new AccelerateDecelerateInterpolator());

                invisibleView.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // Re-enable flip button after animation
                                flipImageButton.setEnabled(true);
                            }
                        })
                        .start();

                rotationIn.start();
            }
        });

        rotation.start();

        // Update state
        isBacksideShowing = !isBacksideShowing;

        // Update UI based on flip state
        if (isBacksideShowing) {
            // Change flip button icon to indicate "flip back"
            flipImageButton.setImageResource(R.drawable.ic_flip_back);
        } else {
            // Change flip button icon to indicate "flip to back"
            flipImageButton.setImageResource(R.drawable.ic_flip);
        }
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";

        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ using MediaStore
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "Musicandpicture");

                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    outputStream = resolver.openOutputStream(imageUri);
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                        outputStream.close();
                        Toast.makeText(this, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                // Below Android 10
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Musicandpicture");

                if (!imagesDir.exists()) {
                    boolean dirCreated = imagesDir.mkdirs();
                    if (!dirCreated) {
                        Toast.makeText(this, "Failed to create directory", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                File imageFile = new File(imagesDir, fileName);
                outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                outputStream.close();

                // Notify gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFile));
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving image to gallery", e);
            Toast.makeText(this, R.string.error_saving_image, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(Bitmap bitmap) {
        try {
            // Save to cache directory first
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream);
            stream.close();

            // Get content URI through FileProvider
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.example.musicandpicture.fileprovider", imageFile);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image_via)));
        } catch (IOException e) {
            Log.e(TAG, "Error sharing image", e);
            Toast.makeText(this, R.string.error_sharing_image, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Create different music style filters using ColorMatrix

    // Rock filter - high contrast, slight red tint, prominent blacks
    private static ColorMatrix createRockFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.3f); // Increase saturation
        float[] colorMatrix = {
                1.2f, 0, 0, 0, 10,     // Red channel
                0, 0.9f, 0, 0, -10,    // Green channel
                0, 0, 0.9f, 0, -10,    // Blue channel
                0, 0, 0, 1.1f, 0       // Alpha channel
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // Pop filter - bright, vibrant, colorful
    private static ColorMatrix createPopFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.5f); // Higher saturation
        float[] colorMatrix = {
                1.1f, 0, 0, 0, 20,     // Red channel
                0, 1.1f, 0, 0, 20,     // Green channel
                0, 0, 1.1f, 0, 20,     // Blue channel
                0, 0, 0, 1, 0          // Alpha channel
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // Jazz filter - warm, vintage, lower saturation
    private static ColorMatrix createJazzFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.8f); // Lower saturation
        float[] colorMatrix = {
                1.1f, 0.1f, 0.1f, 0, 10,     // Red channel
                0.1f, 1.0f, 0.1f, 0, 0,      // Green channel
                0.1f, 0.1f, 0.9f, 0, -10,    // Blue channel
                0, 0, 0, 1, 0                // Alpha channel
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // Classical filter - elegant, soft, subtle vintage
    private static ColorMatrix createClassicalFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.7f); // Lower saturation
        float[] colorMatrix = {
                0.9f, 0, 0, 0, 10,    // Red channel
                0, 0.9f, 0, 0, 10,    // Green channel
                0, 0, 1.0f, 0, 10,    // Blue channel
                0, 0, 0, 1, 0         // Alpha channel
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // Electronic filter - bright, high contrast, tech-like
    private static ColorMatrix createElectronicFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.2f); // Increase saturation
        float[] colorMatrix = {
                0.8f, 0, 0.2f, 0, 0,      // Red channel
                0.2f, 0.8f, 0, 0, 0,      // Green channel
                0, 0.2f, 1.1f, 0, 20,     // Enhanced blue channel
                0, 0, 0, 1, 0             // Alpha channel
        };
        matrix.set(colorMatrix);
        return matrix;
    }
}