package com.example.musicandpicture;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ImageFullscreenActivity extends AppCompatActivity {

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

    // 滤镜矩阵
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

        // 获取从上一个活动传递过来的数据
        initialPosition = getIntent().getIntExtra("position", 0);

        // 使用主活动中的全局图片列表
        imageList = new ArrayList<>(MainActivity.globalImageList);

        initViews();
        setupToolbar();
        setupViewPager();
        setupFilterButtons();
        setupSaveShareButtons();
        setupFlipFunctionality();
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

        // 添加页面变更监听器，以便在滑动时更新UI
        fullscreenViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 如果在切换图片时正在显示背面，切换回正面
                if (isBacksideShowing) {
                    flipCard();
                }

                // 重置滤镜
                currentFilterMatrix.reset();
                adapter.setFilter(currentFilterMatrix);
            }
        });
    }

    private void setupFilterButtons() {
        filterNoneButton.setOnClickListener(v -> {
            currentFilterMatrix.reset();
            applyFilter();
        });

        filterRockButton.setOnClickListener(v -> {
            currentFilterMatrix = ROCK_FILTER;
            applyFilter();
        });

        filterPopButton.setOnClickListener(v -> {
            currentFilterMatrix = POP_FILTER;
            applyFilter();
        });

        filterJazzButton.setOnClickListener(v -> {
            currentFilterMatrix = JAZZ_FILTER;
            applyFilter();
        });

        filterClassicalButton.setOnClickListener(v -> {
            currentFilterMatrix = CLASSICAL_FILTER;
            applyFilter();
        });

        filterElectronicButton.setOnClickListener(v -> {
            currentFilterMatrix = ELECTRONIC_FILTER;
            applyFilter();
        });
    }

    private void applyFilter() {
        adapter.setFilter(currentFilterMatrix);
    }

    private void setupSaveShareButtons() {
        saveImageButton.setOnClickListener(v -> {
            Bitmap filteredBitmap = adapter.getCurrentFilteredBitmap();
            if (filteredBitmap != null) {
                saveImageToGallery(filteredBitmap);
            } else {
                Toast.makeText(this, R.string.error_saving_image, Toast.LENGTH_SHORT).show();
            }
        });

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
        flipImageButton.setOnClickListener(v -> flipCard());

        saveNotesButton.setOnClickListener(v -> {
            String notes = imageNotesEditText.getText().toString();
            // 在实际应用中，这里应该将笔记保存到数据库或与图片关联
            Toast.makeText(this, R.string.notes_saved, Toast.LENGTH_SHORT).show();
            flipCard(); // 保存后翻回正面
        });
    }

    private void flipCard() {
        // 禁用控件避免动画过程中的交互
        flipImageButton.setEnabled(false);

        // 准备动画
        final View visibleView = isBacksideShowing ? imageBacksideCard : fullscreenViewPager;
        final View invisibleView = isBacksideShowing ? fullscreenViewPager : imageBacksideCard;

        // 显示即将变为可见的视图
        invisibleView.setVisibility(View.VISIBLE);
        invisibleView.setAlpha(0f);

        // 设置过滤器控制卡片的可见性
        filterControlsCard.setVisibility(isBacksideShowing ? View.VISIBLE : View.GONE);

        // 创建旋转和淡入淡出动画
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
                                // 完成后重新启用翻转按钮
                                flipImageButton.setEnabled(true);
                            }
                        })
                        .start();

                rotationIn.start();
            }
        });

        rotation.start();

        // 更新状态
        isBacksideShowing = !isBacksideShowing;
    }

    private void saveImageToGallery(Bitmap bitmap) {
        String fileName = "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".jpg";

        try {
            OutputStream outputStream;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10及以上使用MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Musicandpicture");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    outputStream = getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();

                    Toast.makeText(this, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
                }
            } else {
                // Android 9及以下使用文件系统
                File imagesDir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "Musicandpicture");

                if (!imagesDir.exists()) {
                    imagesDir.mkdirs();
                }

                File imageFile = new File(imagesDir, fileName);
                outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                outputStream.close();

                // 通知图库更新
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(imageFile));
                sendBroadcast(mediaScanIntent);

                Toast.makeText(this, R.string.image_saved_to_gallery, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, R.string.error_saving_image, Toast.LENGTH_SHORT).show();
        }
    }

    private void shareImage(Bitmap bitmap) {
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "shared_image.jpg");

            FileOutputStream stream = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            stream.close();

            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this, "com.example.musicandpicture.fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_image_via)));
        } catch (Exception e) {
            e.printStackTrace();
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

    // 创建各种音乐风格的滤镜
    private static ColorMatrix createRockFilter() {
        ColorMatrix matrix = new ColorMatrix();
        // 高对比度，略微偏红，突出黑色
        matrix.setSaturation(1.3f); // 增加饱和度
        float[] colorMatrix = {
                1.2f, 0, 0, 0, 10,    // 红色通道
                0, 0.9f, 0, 0, -10,   // 绿色通道
                0, 0, 0.9f, 0, -10,   // 蓝色通道
                0, 0, 0, 1.1f, 0      // alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    private static ColorMatrix createPopFilter() {
        ColorMatrix matrix = new ColorMatrix();
        // 明亮、鲜艳、彩色
        matrix.setSaturation(1.5f); // 高饱和度
        float[] colorMatrix = {
                1.1f, 0, 0, 0, 20,    // 红色通道
                0, 1.1f, 0, 0, 20,    // 绿色通道
                0, 0, 1.1f, 0, 20,    // 蓝色通道
                0, 0, 0, 1, 0         // alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    private static ColorMatrix createJazzFilter() {
        ColorMatrix matrix = new ColorMatrix();
        // 温暖、复古、低饱和度
        matrix.setSaturation(0.8f); // 降低饱和度
        float[] colorMatrix = {
                1.1f, 0.1f, 0.1f, 0, 10,    // 红色通道
                0.1f, 1.0f, 0.1f, 0, 0,     // 绿色通道
                0.1f, 0.1f, 0.9f, 0, -10,   // 蓝色通道
                0, 0, 0, 1, 0               // alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    private static ColorMatrix createClassicalFilter() {
        ColorMatrix matrix = new ColorMatrix();
        // 高雅、柔和、稍带复古感
        matrix.setSaturation(0.7f); // 降低饱和度
        float[] colorMatrix = {
                0.9f, 0, 0, 0, 10,    // 红色通道
                0, 0.9f, 0, 0, 10,    // 绿色通道
                0, 0, 1.0f, 0, 10,    // 蓝色通道
                0, 0, 0, 1, 0         // alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }

    private static ColorMatrix createElectronicFilter() {
        ColorMatrix matrix = new ColorMatrix();
        // 明亮、高对比度、有科技感
        matrix.setSaturation(1.2f); // 增加饱和度
        float[] colorMatrix = {
                0.8f, 0, 0.2f, 0, 0,       // 红色通道
                0.2f, 0.8f, 0, 0, 0,       // 绿色通道
                0, 0.2f, 1.1f, 0, 20,      // 蓝色通道增强
                0, 0, 0, 1, 0              // alpha通道
        };
        matrix.set(colorMatrix);
        return matrix;
    }
}