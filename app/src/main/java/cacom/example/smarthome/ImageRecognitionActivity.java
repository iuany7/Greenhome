package cacom.example.smarthome;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Random;

public class ImageRecognitionActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout layoutImagePreview, layoutUploadHint, layoutLoading, layoutResult;
    private ImageView ivPlantPhoto;
    private Button btnSelectPhoto, btnTakePhoto, btnAnalyzeImage;
    private TextView tvDiseaseName, tvConfidence, tvDiseaseFeatures, tvCause, tvSuggestions, tvPrevention;

    private Uri imageUri;
    private static final int REQUEST_SELECT_PHOTO = 1;
    private static final int REQUEST_TAKE_PHOTO = 2;
    private Bitmap selectedBitmap;

    // Simulated pest & disease database
    private final String[] diseases = {
        "白粉病", "灰霉病", "霜霉病", "叶霉病", "炭疽病", 
        "蚜虫危害", "红蜘蛛危害", "潜叶蝇危害", "病毒病", "细菌性角斑病"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_recognition);

        initialization();
    }

    private void initialization() {
        btnBack = findViewById(R.id.btn_back);
        layoutImagePreview = findViewById(R.id.layout_image_preview);
        layoutUploadHint = findViewById(R.id.layout_upload_hint);
        layoutLoading = findViewById(R.id.layout_loading);
        layoutResult = findViewById(R.id.layout_result);
        ivPlantPhoto = findViewById(R.id.iv_plant_photo);
        btnSelectPhoto = findViewById(R.id.btn_select_photo);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnAnalyzeImage = findViewById(R.id.btn_analyze_image);
        tvDiseaseName = findViewById(R.id.tv_disease_name);
        tvConfidence = findViewById(R.id.tv_confidence);
        tvDiseaseFeatures = findViewById(R.id.tv_disease_features);
        tvCause = findViewById(R.id.tv_cause);
        tvSuggestions = findViewById(R.id.tv_suggestions);
        tvPrevention = findViewById(R.id.tv_prevention);

        btnBack.setOnClickListener(v -> finish());

        // Select photo
        btnSelectPhoto.setOnClickListener(v -> selectPhoto());

        // Take photo
        btnTakePhoto.setOnClickListener(v -> takePhoto());

        // Start analysis
        btnAnalyzeImage.setOnClickListener(v -> {
            if (selectedBitmap == null) {
                Toast.makeText(this, "Please select or take a photo first", Toast.LENGTH_SHORT).show();
                return;
            }
            performAnalysis();
        });

        // Click image area to select photo
        layoutImagePreview.setOnClickListener(v -> {
            if (selectedBitmap == null) {
                showPhotoChooser();
            }
        });
    }

    /**
     * Select photo from gallery
     */
    private void selectPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_SELECT_PHOTO);
    }

    /**
     * Take photo with camera
     */
    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_TAKE_PHOTO);
    }

    /**
     * Show photo chooser dialog
     */
    private void showPhotoChooser() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Select Photo")
            .setItems(new String[]{"From Gallery", "Take Photo"}, (dialog, which) -> {
                if (which == 0) {
                    selectPhoto();
                } else {
                    takePhoto();
                }
            })
            .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK) {
            try {
                if (requestCode == REQUEST_SELECT_PHOTO && data != null) {
                    imageUri = data.getData();
                    InputStream inputStream = getContentResolver().openInputStream(imageUri);
                    selectedBitmap = BitmapFactory.decodeStream(inputStream);
                    displayImage();
                } else if (requestCode == REQUEST_TAKE_PHOTO && data != null) {
                    selectedBitmap = (Bitmap) data.getExtras().get("data");
                    displayImage();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Display selected image
     */
    private void displayImage() {
        if (selectedBitmap != null) {
            ivPlantPhoto.setImageBitmap(selectedBitmap);
            ivPlantPhoto.setVisibility(View.VISIBLE);
            layoutUploadHint.setVisibility(View.GONE);
            btnAnalyzeImage.setEnabled(true);
            btnAnalyzeImage.setText("开始识别分析");
            
            // 隐藏之前的结果
            layoutResult.setVisibility(View.GONE);
            
            Toast.makeText(this, "Photo selected, ready to analyze", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 执行AI分析（模拟）
     */
    private void performAnalysis() {
        // 显示加载状态
        layoutLoading.setVisibility(View.VISIBLE);
        layoutResult.setVisibility(View.GONE);
        btnAnalyzeImage.setEnabled(false);

        // 模拟AI分析延迟
        new Handler().postDelayed(() -> {
            // 模拟识别结果
            generateMockResult();
            
            // 显示结果
            layoutLoading.setVisibility(View.GONE);
            layoutResult.setVisibility(View.VISIBLE);
            btnAnalyzeImage.setEnabled(true);
            btnAnalyzeImage.setText("重新识别");
            
            // 滚动到结果区域
            layoutResult.scrollTo(0, 0);
            
            Toast.makeText(this, "Analysis complete", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    /**
     * 生成模拟识别结果
     */
    private void generateMockResult() {
        // 写死为根腐病 + 红蜘蛛
        Random random = new Random();
        double confidence = 75 + random.nextDouble() * 13; // 85%-98%的随机值
        
        tvDiseaseName.setText("Root Rot & Spider Mites");
        tvConfidence.setText(String.format("Confidence: %.1f%%", confidence));

        // 设置详细信息
        setDiseaseDetails();
    }

    /**
     * 设置病害详细信息
     */
    private void setDiseaseDetails() {
        // 根腐病 + 红蜘蛛的详细信息
        tvDiseaseFeatures.setText(
            "Root Rot Symptoms:\n" +
            "• Brown, water-soaked lesions on roots\n" +
            "• Roots become soft and mushy\n" +
            "• Root cortex separates from stele\n" +
            "• Plants wilt and show stunted growth\n\n" +
            "Spider Mites Symptoms:\n" +
            "• Small yellow or white spots on leaves\n" +
            "• Fine webbing on leaf undersides\n" +
            "• Leaves turn bronze or reddish\n" +
            "• Severe defoliation in heavy infestations"
        );
        
        tvCause.setText(
            "Root Rot Causes:\n" +
            "• Excessive soil moisture (>80%)\n" +
            "• Poor drainage conditions\n" +
            "• Soil-borne pathogens (Pythium, Phytophthora)\n" +
            "• Overwatering or waterlogged soil\n\n" +
            "Spider Mites Causes:\n" +
            "• High temperature (25-30°C)\n" +
            "• Low humidity (<60%)\n" +
            "• Dry and hot environment\n" +
            "• Excessive nitrogen fertilization"
        );
        
        tvSuggestions.setText(
            "Root Rot Control:\n" +
            "[Cultural Control]\n" +
            "• Improve soil drainage immediately\n" +
            "• Reduce watering frequency\n" +
            "• Remove and destroy infected plants\n\n" +
            "[Chemical Control]\n" +
            "• Apply metalaxyl fungicide 1000x\n" +
            "• Use hymexazol 3000x solution\n" +
            "• Drench soil around root zone\n\n" +
            "Spider Mites Control:\n" +
            "[Cultural Control]\n" +
            "• Increase humidity to 70%+\n" +
            "• Regular misting of plants\n\n" +
            "[Biological Control]\n" +
            "• Release predatory mites (Phytoseiulus persimilis)\n" +
            "• Apply neem oil extract\n\n" +
            "[Chemical Control]\n" +
            "• Spray abamectin 2000x\n" +
            "• Use spiromesifen 4000x\n" +
            "• Focus on leaf undersides\n" +
            "• Repeat every 5-7 days, 3 treatments"
        );
        
        tvPrevention.setText(
            "• Monitor soil moisture regularly (maintain 60-70%)\n" +
            "• Ensure proper drainage system\n" +
            "• Maintain humidity at 65-75%\n" +
            "• Avoid excessive nitrogen fertilizer\n" +
            "• Regular inspection of roots and leaves\n" +
            "• Quarantine infected plants immediately\n" +
            "• Practice crop rotation\n" +
            "• Use disease-free planting material"
        );
    }
}
