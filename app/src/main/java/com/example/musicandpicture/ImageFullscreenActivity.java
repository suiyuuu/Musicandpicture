package com.example.musicandpicture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.example.musicandpicture.database.AppDatabase;
import com.example.musicandpicture.database.ImageNoteEntity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
    private FloatingActionButton fabEdit;
    private ImageView backgroundImageView;
    private ConstraintLayout mainContainer;

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
    private Bitmap blurredBackgroundBitmap;

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

        // 设置全屏和透明状态栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // 设置状态栏为透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.setStatusBarColor(Color.TRANSPARENT);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        setContentView(R.layout.activity_image_fullscreen_enhanced);

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
        customizeUI();

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
        fabEdit = findViewById(R.id.fabEdit);
        backgroundImageView = findViewById(R.id.backgroundImageView);
        mainContainer = findViewById(R.id.mainContainer);

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

                // Update blurred background
                updateBlurredBackground();
            }
        });

        // 设置初始化后的背景
        fullscreenViewPager.post(this::updateBlurredBackground);
    }

    private void updateBlurredBackground() {
        try {
            int currentPosition = fullscreenViewPager.getCurrentItem();
            if (currentPosition >= 0 && currentPosition < imageList.size() && adapter != null) {
                Bitmap originalBitmap = adapter.getCurrentBitmap();
                if (originalBitmap != null) {
                    // 创建模糊的背景图
                    blurredBackgroundBitmap = blurBitmap(originalBitmap, 25f);

                    // 应用暗色调滤镜
                    if (blurredBackgroundBitmap != null) {
                        Bitmap darkened = darkenBitmap(blurredBackgroundBitmap, 0.5f);
                        backgroundImageView.setImageBitmap(darkened);
                        backgroundImageView.setAlpha(isBacksideShowing ? 0.85f : 0.15f);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating blurred background", e);
        }
    }

    private Bitmap blurBitmap(Bitmap bitmap, float blurRadius) {
        try {
            // 创建一个较小的位图以提高性能
            Bitmap input = Bitmap.createScaledBitmap(bitmap,
                    bitmap.getWidth() / 2,
                    bitmap.getHeight() / 2,
                    true);

            // 创建输出位图
            Bitmap output = Bitmap.createBitmap(
                    input.getWidth(),
                    input.getHeight(),
                    Bitmap.Config.ARGB_8888);

            // 使用RenderScript进行模糊处理
            RenderScript rs = RenderScript.create(this);
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation inAlloc = Allocation.createFromBitmap(rs, input);
            Allocation outAlloc = Allocation.createFromBitmap(rs, output);

            script.setRadius(blurRadius);
            script.setInput(inAlloc);
            script.forEach(outAlloc);

            outAlloc.copyTo(output);

            // 清理资源
            rs.destroy();
            input.recycle();

            return output;
        } catch (Exception e) {
            Log.e(TAG, "Error blurring bitmap", e);
            return null;
        }
    }

    private Bitmap darkenBitmap(Bitmap bitmap, float darknessFactor) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.5f); // 降低饱和度

        // 降低亮度
        float[] colorTransform = {
                darknessFactor, 0, 0, 0, 0,
                0, darknessFactor, 0, 0, 0,
                0, 0, darknessFactor, 0, 0,
                0, 0, 0, 1, 0
        };

        ColorMatrix darkenMatrix = new ColorMatrix();
        darkenMatrix.set(colorTransform);

        // 组合两个滤镜
        colorMatrix.postConcat(darkenMatrix);

        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    private void resetFilter() {
        currentFilterMatrix.reset();
        currentFilterName = "none";
        adapter.setFilter(currentFilterMatrix);

        // Reset active button states
        updateFilterButtonStates();
    }

    private void setupFilterButtons() {
        // 美化滤镜按钮
        styleFilterButtons();

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

    private void styleFilterButtons() {
        // 设置按钮样式
        Button[] filterButtons = {
                filterNoneButton, filterRockButton, filterPopButton,
                filterJazzButton, filterClassicalButton, filterElectronicButton
        };

        for (Button button : filterButtons) {
            // 创建圆角背景
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16f);
            shape.setColor(Color.parseColor("#80000000")); // 半透明黑色
            shape.setStroke(2, Color.parseColor("#40FFFFFF")); // 细白色边框

            // 设置按钮背景
            button.setBackground(shape);
            button.setTextColor(Color.WHITE);
            button.setAllCaps(false); // 不自动大写
            button.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            button.setElevation(4f);
        }
    }

    private void updateFilterButtonStates() {
        // 重置所有按钮到默认状态
        Button[] filterButtons = {
                filterNoneButton, filterRockButton, filterPopButton,
                filterJazzButton, filterClassicalButton, filterElectronicButton
        };

        for (Button button : filterButtons) {
            button.setAlpha(0.7f);

            // 创建默认按钮背景
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(16f);
            shape.setColor(Color.parseColor("#80000000")); // 半透明黑色
            shape.setStroke(2, Color.parseColor("#40FFFFFF")); // 细白色边框

            button.setBackground(shape);
        }

        // 高亮当前选中的滤镜按钮
        Button activeButton = null;

        switch (currentFilterName) {
            case "none":
                activeButton = filterNoneButton;
                break;
            case "rock":
                activeButton = filterRockButton;
                break;
            case "pop":
                activeButton = filterPopButton;
                break;
            case "jazz":
                activeButton = filterJazzButton;
                break;
            case "classical":
                activeButton = filterClassicalButton;
                break;
            case "electronic":
                activeButton = filterElectronicButton;
                break;
        }

        if (activeButton != null) {
            // 创建激活状态按钮背景
            GradientDrawable activeShape = new GradientDrawable();
            activeShape.setShape(GradientDrawable.RECTANGLE);
            activeShape.setCornerRadius(16f);
            activeShape.setColor(Color.parseColor("#80FFFFFF")); // 半透明白色
            activeShape.setStroke(2, Color.WHITE); // 白色边框

            activeButton.setBackground(activeShape);
            activeButton.setAlpha(1.0f);
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
        // 美化保存和分享按钮
        styleActionButtons();

        // Save image with current filter
        saveImageButton.setOnClickListener(v -> {
            animateButtonPress(saveImageButton);
            Bitmap filteredBitmap = adapter.getCurrentFilteredBitmap();
            if (filteredBitmap != null) {
                saveImageToGallery(filteredBitmap);
            } else {
                Toast.makeText(this, R.string.error_saving_image, Toast.LENGTH_SHORT).show();
            }
        });

        // Share image with current filter
        shareImageButton.setOnClickListener(v -> {
            animateButtonPress(shareImageButton);
            Bitmap filteredBitmap = adapter.getCurrentFilteredBitmap();
            if (filteredBitmap != null) {
                shareImage(filteredBitmap);
            } else {
                Toast.makeText(this, R.string.error_sharing_image, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void styleActionButtons() {
        // 设置保存和分享按钮的样式
        GradientDrawable buttonShape = new GradientDrawable();
        buttonShape.setShape(GradientDrawable.RECTANGLE);
        buttonShape.setCornerRadius(24f);
        buttonShape.setColor(ContextCompat.getColor(this, R.color.colorAccent));

        saveImageButton.setBackground(buttonShape);
        saveImageButton.setTextColor(Color.WHITE);
        saveImageButton.setAllCaps(false);
        saveImageButton.setPadding(32, 16, 32, 16);
        saveImageButton.setElevation(4f);

        shareImageButton.setBackground(buttonShape);
        shareImageButton.setTextColor(Color.WHITE);
        shareImageButton.setAllCaps(false);
        shareImageButton.setPadding(32, 16, 32, 16);
        shareImageButton.setElevation(4f);
    }

    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.95f)
                .scaleY(0.95f)
                .setDuration(100)
                .withEndAction(() ->
                        view.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .start()
                )
                .start();
    }

    private void setupFlipFunctionality() {
        // 使用FAB代替工具栏按钮进行翻转
        fabEdit.setOnClickListener(v -> flipCard());

        // 原翻转按钮保留但隐藏，功能迁移到FAB
        flipImageButton.setVisibility(View.GONE);

        // Save notes button
        saveNotesButton.setOnClickListener(v -> {
            animateButtonPress(saveNotesButton);
            String notes = imageNotesEditText.getText().toString();
            saveImageNotes(notes);
            Toast.makeText(this, R.string.notes_saved, Toast.LENGTH_SHORT).show();

            // 翻转回正面
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
                            // 加载笔记
                            if (entity.getNotes() != null) {
                                imageNotesEditText.setText(entity.getNotes());
                            } else {
                                imageNotesEditText.setText("");
                            }

                            // 应用保存的滤镜
                            if (entity.getAppliedFilter() != null) {
                                applyFilterByName(entity.getAppliedFilter());
                            } else {
                                resetFilter();
                            }
                        } else {
                            // 此图片无保存数据
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

                    // 检查是否已有此图片的记录
                    ImageNoteEntity entity = db.imageNoteDao().getImageNoteByUri(imageUri);

                    if (entity == null) {
                        // 创建新实体
                        entity = new ImageNoteEntity(imageUri);
                        entity.setNotes(noteText);
                        entity.setAppliedFilter(currentFilterName);
                        db.imageNoteDao().insert(entity);
                    } else {
                        // 更新已有实体
                        db.imageNoteDao().updateNotes(imageUri, noteText);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error saving notes to database", e);
                    runOnUiThread(() -> {
                        Toast.makeText(ImageFullscreenActivity.this,
                                "保存笔记失败: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }
    }

    private void flipCard() {
        // 禁用翻转按钮，避免动画过程中交互
        fabEdit.setEnabled(false);

        // 准备动画
        final View visibleView = isBacksideShowing ? imageBacksideCard : fullscreenViewPager;
        final View invisibleView = isBacksideShowing ? fullscreenViewPager : imageBacksideCard;

        // 显示即将可见的视图
        invisibleView.setVisibility(View.VISIBLE);
        invisibleView.setAlpha(0f);

        // 设置滤镜控制卡片的可见性
        filterControlsCard.setVisibility(isBacksideShowing ? View.VISIBLE : View.GONE);

        // 更改背景模糊程度
        animateBackgroundForFlip(!isBacksideShowing);

        // 创建旋转和淡入淡出动画
        ObjectAnimator rotation = ObjectAnimator.ofFloat(visibleView, "rotationY", 0f, 90f);
        rotation.setDuration(400);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());

        rotation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                visibleView.setVisibility(View.GONE);

                ObjectAnimator rotationIn = ObjectAnimator.ofFloat(invisibleView, "rotationY", -90f, 0f);
                rotationIn.setDuration(400);
                rotationIn.setInterpolator(new DecelerateInterpolator());

                invisibleView.animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                // 动画结束后重新启用翻转按钮
                                fabEdit.setEnabled(true);

                                // 如果显示笔记面，自动聚焦到输入框
                                if (!isBacksideShowing) { // 现在要变成true
                                    imageNotesEditText.requestFocus();
                                }
                            }
                        })
                        .start();

                rotationIn.start();
            }
        });

        rotation.start();

        // 更新状态
        isBacksideShowing = !isBacksideShowing;

        // 根据翻转状态更新FAB图标
        updateFabIcon();
    }

    private void animateBackgroundForFlip(boolean toBackside) {
        float startAlpha = backgroundImageView.getAlpha();
        float endAlpha = toBackside ? 0.85f : 0.15f;

        ObjectAnimator alphaAnimation = ObjectAnimator.ofFloat(backgroundImageView, "alpha", startAlpha, endAlpha);
        alphaAnimation.setDuration(800);
        alphaAnimation.setInterpolator(new DecelerateInterpolator());
        alphaAnimation.start();
    }

    private void updateFabIcon() {
        if (isBacksideShowing) {
            // 显示返回图标
            fabEdit.setImageResource(R.drawable.ic_flip_back);
        } else {
            // 显示编辑图标
            fabEdit.setImageResource(R.drawable.ic_edit_notes);
        }
    }

    private void customizeUI() {
        // 设置卡片界面风格
        styleBacksideCard();

        // 设置滤镜控制面板风格
        styleFilterPanel();

        // 更新FAB图标
        updateFabIcon();
    }

    private void styleBacksideCard() {
        // 设置背景卡片圆角和边距
        imageBacksideCard.setRadius(24f);
        imageBacksideCard.setCardElevation(16f);

        // 设置EditText样式
        imageNotesEditText.setBackground(null); // 移除背景
        imageNotesEditText.setHintTextColor(Color.parseColor("#80FFFFFF")); // 半透明白色提示文字
        imageNotesEditText.setTextColor(Color.parseColor("#F0F0F0")); // 亮白色文字
        imageNotesEditText.setHint("写下你对这张照片的感受...");

        // 设置保存按钮样式
        GradientDrawable saveButtonShape = new GradientDrawable();
        saveButtonShape.setShape(GradientDrawable.RECTANGLE);
        saveButtonShape.setCornerRadius(24f);
        saveButtonShape.setColor(ContextCompat.getColor(this, R.color.colorAccent));

        saveNotesButton.setBackground(saveButtonShape);
        saveNotesButton.setTextColor(Color.WHITE);
        saveNotesButton.setText("保存笔记");
        saveNotesButton.setAllCaps(false);
        saveNotesButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    }

    private void styleFilterPanel() {
        // 设置滤镜控制面板样式
        filterControlsCard.setRadius(16f);
        filterControlsCard.setCardElevation(12f);
        filterControlsCard.setCardBackgroundColor(Color.parseColor("#80000000")); // 半透明黑色
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";

        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用MediaStore
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

                        // 显示美化的成功提示
                        showSuccessToast("图片已保存到相册");
                    }
                }
            } else {
                // Android 9及以下
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Musicandpicture");

                if (!imagesDir.exists()) {
                    boolean dirCreated = imagesDir.mkdirs();
                    if (!dirCreated) {
                        Toast.makeText(this, "无法创建目录", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                File imageFile = new File(imagesDir, fileName);
                outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                outputStream.close();

                // 通知图库更新
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFile));
                sendBroadcast(mediaScanIntent);

                // 显示美化的成功提示
                showSuccessToast("图片已保存到相册");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving image to gallery", e);
            Toast.makeText(this, R.string.error_saving_image, Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        View view = toast.getView();

        // 设置Toast背景和文字颜色
        if (view != null) {
            view.setBackgroundResource(R.drawable.success_toast_background);
            TextView text = view.findViewById(android.R.id.message);
            if (text != null) {
                text.setTextColor(Color.WHITE);
            }
        }

        toast.show();
    }

    private void shareImage(Bitmap bitmap) {
        try {
            // 保存到缓存目录
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream);
            stream.close();

            // 通过FileProvider获取内容URI
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.example.musicandpicture.fileprovider", imageFile);

            // 创建分享意图
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

    @Override
    public void onBackPressed() {
        // 如果正在显示背面，先翻转回正面
        if (isBacksideShowing) {
            flipCard();
        } else {
            super.onBackPressed();
        }
    }

    // 创建不同音乐风格的滤镜

    // 摇滚滤镜 - 高对比度，略微偏红，突出黑色
    private static ColorMatrix createRockFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.3f); // 增加饱和度
        float[] colorMatrix = {
                1.2f, 0, 0, 0, 10,     // 红色通道
                0, 0.9f, 0, 0, -10,    // 绿色通道
                0, 0, 0.9f, 0, -10,    // 蓝色通道
                0, 0, 0, 1.1f, 0       // Alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // 流行滤镜 - 明亮，鲜艳，多彩
    private static ColorMatrix createPopFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.5f); // 更高饱和度
        float[] colorMatrix = {
                1.1f, 0, 0, 0, 20,     // 红色通道
                0, 1.1f, 0, 0, 20,     // 绿色通道
                0, 0, 1.1f, 0, 20,     // 蓝色通道
                0, 0, 0, 1, 0          // Alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // 爵士滤镜 - 温暖，复古，降低饱和度
    private static ColorMatrix createJazzFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.8f); // 降低饱和度
        float[] colorMatrix = {
                1.1f, 0.1f, 0.1f, 0, 10,     // 红色通道
                0.1f, 1.0f, 0.1f, 0, 0,      // 绿色通道
                0.1f, 0.1f, 0.9f, 0, -10,    // 蓝色通道
                0, 0, 0, 1, 0                // Alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // 古典滤镜 - 优雅，柔和，带有复古感
    private static ColorMatrix createClassicalFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0.7f); // 降低饱和度
        float[] colorMatrix = {
                0.9f, 0, 0, 0, 10,    // 红色通道
                0, 0.9f, 0, 0, 10,    // 绿色通道
                0, 0, 1.0f, 0, 10,    // 蓝色通道
                0, 0, 0, 1, 0         // Alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    // 电子滤镜 - 明亮，高对比度，科技感
    private static ColorMatrix createElectronicFilter() {
        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(1.2f); // 增加饱和度
        float[] colorMatrix = {
                0.8f, 0, 0.2f, 0, 0,       // 红色通道
                0.2f, 0.8f, 0, 0, 0,       // 绿色通道
                0, 0.2f, 1.1f, 0, 20,      // 增强蓝色通道
                0, 0, 0, 1, 0              // Alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }
}