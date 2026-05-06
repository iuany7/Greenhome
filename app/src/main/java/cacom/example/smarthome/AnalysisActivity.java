package cacom.example.smarthome;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class AnalysisActivity extends AppCompatActivity {

    private ImageView btnBack;
    private Button btnAnalyze;
    private TextView analysisTemp, analysisHumi, analysisLight, analysisSoil, analysisCo2;
    private LinearLayout cardResult, cardCharts, layoutChartsContainer;
    private TextView riskLevel, envAssessment, suggestions;

    // Current sensor data
    private double currentTemp = 0, currentHumi = 0, currentLight = 0;
    private double currentSoil = 0, currentCo2 = 0;
    private boolean hasData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analysis);

        initialization();
        loadCurrentData();
    }

    private void initialization() {
        btnBack = findViewById(R.id.btn_back);
        btnAnalyze = findViewById(R.id.btn_analyze);
        analysisTemp = findViewById(R.id.analysis_temp);
        analysisHumi = findViewById(R.id.analysis_humi);
        analysisLight = findViewById(R.id.analysis_light);
        analysisSoil = findViewById(R.id.analysis_soil);
        analysisCo2 = findViewById(R.id.analysis_co2);
        cardResult = findViewById(R.id.card_result);
        cardCharts = findViewById(R.id.card_charts);
        layoutChartsContainer = findViewById(R.id.layout_charts_container);
        riskLevel = findViewById(R.id.risk_level);
        envAssessment = findViewById(R.id.env_assessment);
        suggestions = findViewById(R.id.suggestions);

        btnBack.setOnClickListener(v -> finish());

        btnAnalyze.setOnClickListener(v -> {
            if (!hasData) {
                Toast.makeText(this, "No sensor data available for analysis", Toast.LENGTH_SHORT).show();
                return;
            }
            performAnalysis();
        });
    }

    private void loadCurrentData() {
        // Read latest sensor data from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("CurrentSensorData", MODE_PRIVATE);
        
        String tempStr = prefs.getString("temp", "0");
        String humiStr = prefs.getString("humi", "0");
        String lightStr = prefs.getString("light", "0");
        String soilStr = prefs.getString("soil", "0");
        String co2Str = prefs.getString("co2", "0");

        try {
            currentTemp = Double.parseDouble(tempStr.replace("°C", "").trim());
            currentHumi = Double.parseDouble(humiStr.replace("%RH", "").trim());
            currentLight = Double.parseDouble(lightStr.replace("Lux", "").trim());
            currentSoil = Double.parseDouble(soilStr.replace("%RH", "").trim());
            currentCo2 = Double.parseDouble(co2Str.replace("PPM", "").trim());
            
            hasData = (currentTemp > 0 || currentHumi > 0);
            
            // Display current data
            analysisTemp.setText(String.format("%.1f°C", currentTemp));
            analysisHumi.setText(String.format("%.1f%%RH", currentHumi));
            analysisLight.setText(String.format("%.0fLux", currentLight));
            analysisSoil.setText(String.format("%.1f%%RH", currentSoil));
            analysisCo2.setText(String.format("%.0fPPM", currentCo2));
            
        } catch (NumberFormatException e) {
            e.printStackTrace();
            hasData = false;
        }
    }

    /**
     * Perform AI Analysis - Based on agricultural expert knowledge base
     */
    private void performAnalysis() {
        StringBuilder assessment = new StringBuilder();
        StringBuilder suggestion = new StringBuilder();
        int riskScore = 0; // Risk score: 0-100

        // 1. Temperature Analysis
        assessment.append("[Temperature Analysis]\n");
        if (currentTemp > 35) {
            assessment.append("⚠️ Temperature too high (").append(currentTemp).append("°C), exceeds optimal range\n");
            suggestion.append("• HIGH TEMP ALERT: Activate ventilation and cooling systems immediately\n");
            suggestion.append("• Turn on fans and evaporative cooling\n");
            riskScore += 20;
        } else if (currentTemp > 30) {
            assessment.append("⚡ Temperature slightly high (").append(currentTemp).append("°C), approaching critical level\n");
            suggestion.append("• Increase ventilation to prevent heat stress\n");
            riskScore += 10;
        } else if (currentTemp >= 20) {
            assessment.append("✅ Optimal temperature (").append(currentTemp).append("°C), ideal for growth\n");
        } else if (currentTemp >= 10) {
            assessment.append("⚡ Temperature slightly low (").append(currentTemp).append("°C), may slow growth\n");
            suggestion.append("• Reduce ventilation time, maintain warmth\n");
            riskScore += 10;
        } else {
            assessment.append("⚠️ Temperature too low (").append(currentTemp).append("°C), risk of cold damage\n");
            suggestion.append("• LOW TEMP ALERT: Close vents, activate heating systems\n");
            riskScore += 20;
        }

        // 2. Humidity Analysis
        assessment.append("\n[Humidity Analysis]\n");
        if (currentHumi > 85) {
            assessment.append("⚠️ Humidity too high (").append(currentHumi).append("%RH), promotes disease\n");
            suggestion.append("• HIGH HUMIDITY ALERT: Increase ventilation, prevent gray mold and downy mildew\n");
            suggestion.append("• Reduce irrigation frequency\n");
            riskScore += 25;
        } else if (currentHumi > 70) {
            assessment.append("⚡ Humidity slightly high (").append(currentHumi).append("%RH), needs ventilation\n");
            suggestion.append("• Increase ventilation time to reduce humidity\n");
            riskScore += 15;
        } else if (currentHumi >= 50) {
            assessment.append("✅ Optimal humidity (").append(currentHumi).append("%RH), good conditions\n");
        } else if (currentHumi >= 40) {
            assessment.append("⚡ Humidity slightly low (").append(currentHumi).append("%RH), plants may lack water\n");
            suggestion.append("• Increase irrigation or activate humidifiers\n");
            riskScore += 10;
        } else {
            assessment.append("⚠️ Humidity too low (").append(currentHumi).append("%RH), severely affects growth\n");
            suggestion.append("• DRYNESS ALERT: Activate humidifiers immediately, increase irrigation\n");
            suggestion.append("• Risk of spider mites, take preventive measures\n");
            riskScore += 20;
        }

        // 3. Light Analysis
        assessment.append("\n[Light Analysis]\n");
        if (currentLight > 50000) {
            assessment.append("⚠️ Light too intense (").append(currentLight).append("Lux), may cause photoinhibition\n");
            suggestion.append("• STRONG LIGHT ALERT: Use shade nets to prevent leaf burn\n");
            riskScore += 15;
        } else if (currentLight > 30000) {
            assessment.append("✅ Sufficient light (").append(currentLight).append("Lux), good for photosynthesis\n");
        } else if (currentLight >= 10000) {
            assessment.append("⚡ Moderate light (").append(currentLight).append("Lux), basically meets needs\n");
        } else if (currentLight >= 5000) {
            assessment.append("⚡ Insufficient light (").append(currentLight).append("Lux), affects photosynthesis\n");
            suggestion.append("• Add supplemental lighting, extend light duration\n");
            riskScore += 10;
        } else {
            assessment.append("⚠️ Severely insufficient light (").append(currentLight).append("Lux)\n");
            suggestion.append("• WEAK LIGHT ALERT: Must activate artificial lighting\n");
            riskScore += 15;
        }

        // 4. Soil Moisture Analysis
        assessment.append("\n[Soil Moisture Analysis]\n");
        if (currentSoil > 80) {
            assessment.append("⚠️ Soil too wet (").append(currentSoil).append("%RH), root oxygen deficiency\n");
            suggestion.append("• Stop irrigation, improve drainage, prevent root rot\n");
            riskScore += 20;
        } else if (currentSoil > 60) {
            assessment.append("✅ Good soil moisture (").append(currentSoil).append("%RH)\n");
        } else if (currentSoil >= 40) {
            assessment.append("⚡ Soil slightly dry (").append(currentSoil).append("%RH), needs irrigation\n");
            suggestion.append("• Activate water pump for irrigation\n");
            riskScore += 10;
        } else {
            assessment.append("⚠️ Soil drought (").append(currentSoil).append("%RH), severely affects crops\n");
            suggestion.append("• DROUGHT ALERT: Irrigate immediately, check irrigation system\n");
            riskScore += 20;
        }

        // 5. CO2 Concentration Analysis
        assessment.append("\n[CO2 Analysis]\n");
        if (currentCo2 > 1500) {
            assessment.append("⚠️ CO2 too high (").append(currentCo2).append("PPM)\n");
            suggestion.append("• Enhance ventilation\n");
            riskScore += 10;
        } else if (currentCo2 >= 800) {
            assessment.append("✅ Sufficient CO2 (").append(currentCo2).append("PPM), good for photosynthesis\n");
        } else if (currentCo2 >= 400) {
            assessment.append("⚡ Normal CO2 level (").append(currentCo2).append("PPM)\n");
        } else {
            assessment.append("⚡ Low CO2 (").append(currentCo2).append("PPM)\n");
            suggestion.append("• Consider adding CO2 fertilizer\n");
            riskScore += 5;
        }

        // 6. Pest & Disease Comprehensive Risk Assessment
        assessment.append("\n[Pest & Disease Risk Assessment]\n");
        
        // High temp & humidity - Gray mold, Downy mildew
        if (currentTemp >= 18 && currentTemp <= 23 && currentHumi > 85) {
            assessment.append("🔴 Gray Mold HIGH RISK: Temp ").append(currentTemp).append("°C, Humidity ").append(currentHumi).append("%RH\n");
            suggestion.append("• [Gray Mold Prevention] Reduce humidity immediately, spray carbendazim\n");
            riskScore += 20;
        }
        
        // Warm & humid - Powdery mildew
        if (currentTemp >= 15 && currentTemp <= 25 && currentHumi > 70) {
            assessment.append("🔴 Powdery Mildew HIGH RISK: Ideal conditions for pathogen\n");
            suggestion.append("• [Powdery Mildew Prevention] Increase ventilation, spray triadimefon\n");
            riskScore += 20;
        }
        
        // Hot & dry - Spider mites
        if (currentTemp >= 25 && currentHumi < 50) {
            assessment.append("🔴 Spider Mites HIGH RISK: Hot and dry environment\n");
            suggestion.append("• [Spider Mites Prevention] Increase humidity, spray abamectin\n");
            riskScore += 15;
        }
        
        // Warm & dry - Aphids
        if (currentTemp >= 20 && currentTemp <= 25 && currentHumi < 50) {
            assessment.append("🔴 Aphids HIGH RISK: Favorable for reproduction\n");
            suggestion.append("• [Aphids Prevention] Hang yellow sticky traps, spray imidacloprid\n");
            riskScore += 15;
        }
        
        // Soil too wet - Root rot
        if (currentSoil > 80) {
            assessment.append("🔴 Root Rot Risk: Excessive soil moisture\n");
            suggestion.append("• [Root Rot Prevention] Improve drainage, apply hymexazol\n");
            riskScore += 15;
        }

        // Determine risk level
        String riskText;
        int riskColor;
        if (riskScore >= 60) {
            riskText = "🔴 HIGH RISK - Take immediate action";
            riskColor = getColor(android.R.color.holo_red_dark);
        } else if (riskScore >= 40) {
            riskText = "🟠 MODERATE-HIGH RISK - Attention needed";
            riskColor = getColor(android.R.color.holo_orange_dark);
        } else if (riskScore >= 20) {
            riskText = "🟡 LOW-MODERATE RISK - Stay vigilant";
            riskColor = getColor(android.R.color.holo_orange_light);
        } else {
            riskText = "🟢 LOW RISK - Good conditions";
            riskColor = getColor(android.R.color.holo_green_dark);
        }

        // Display results
        cardResult.setVisibility(View.VISIBLE);
        riskLevel.setText(riskText);
        riskLevel.setTextColor(riskColor);
        envAssessment.setText(assessment.toString());
        suggestions.setText(suggestion.toString());

        // Scroll to result area
        cardResult.post(() -> cardResult.scrollTo(0, 0));
        
        Toast.makeText(this, "Analysis complete", Toast.LENGTH_SHORT).show();
        
        // Load line charts
        loadCharts();
    }

    /**
     * Load historical data line charts
     */
    private void loadCharts() {
        // Get historical data from SharedPreferences
        android.content.SharedPreferences prefs = getSharedPreferences("SensorData", MODE_PRIVATE);
        String dataJson = prefs.getString("history_data", "[]");
        
        try {
            JSONArray jsonArray = new JSONArray(dataJson);
            
            if (jsonArray.length() < 2) {
                // Insufficient data, don't show charts
                cardCharts.setVisibility(View.GONE);
                return;
            }
            
            cardCharts.setVisibility(View.VISIBLE);
            layoutChartsContainer.removeAllViews();
            
            // Create 5 charts
            createChart(jsonArray, "Temperature Trend (°C)", "temp", "#38c47f");
            createChart(jsonArray, "Humidity Trend (%RH)", "humi", "#2196F3");
            createChart(jsonArray, "Light Trend (Lux)", "light", "#FF9800");
            createChart(jsonArray, "Soil Moisture Trend (%RH)", "soil", "#9C27B0");
            createChart(jsonArray, "CO2 Trend (PPM)", "co2", "#F44336");
            
        } catch (JSONException e) {
            e.printStackTrace();
            cardCharts.setVisibility(View.GONE);
        }
    }

    /**
     * Create a single line chart
     */
    private void createChart(JSONArray dataArray, String title, String key, String color) {
        try {
            // Load chart layout
            View chartView = LayoutInflater.from(this).inflate(R.layout.item_chart, layoutChartsContainer, false);
            TextView tvTitle = chartView.findViewById(R.id.tv_chart_title);
            LineChart lineChart = chartView.findViewById(R.id.line_chart);
            
            tvTitle.setText(title);
            
            // Parse data
            List<Entry> entries = new ArrayList<>();
            List<String> labels = new ArrayList<>();
            
            // Display up to 10 recent data points
            int startIdx = Math.max(0, dataArray.length() - 10);
            
            for (int i = startIdx; i < dataArray.length(); i++) {
                JSONObject json = dataArray.getJSONObject(i);
                String valueStr = json.getString(key);
                
                // Remove unit
                double value = Double.parseDouble(valueStr.replaceAll("[^0-9.]", ""));
                
                entries.add(new Entry(i - startIdx, (float) value));
                
                // Extract time label
                String time = json.optString("time", "");
                if (time.length() > 5) {
                    labels.add(time.substring(time.length() - 5)); // Only show hour:minute
                } else {
                    labels.add(String.valueOf(i - startIdx + 1));
                }
            }
            
            // Set data series
            LineDataSet dataSet = new LineDataSet(entries, title);
            dataSet.setColor(android.graphics.Color.parseColor(color));
            dataSet.setCircleColor(android.graphics.Color.parseColor(color));
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawCircleHole(true);
            dataSet.setCircleHoleColor(android.graphics.Color.WHITE);
            dataSet.setCircleHoleRadius(2f);
            dataSet.setDrawValues(false);
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            dataSet.setCubicIntensity(0.2f);
            
            LineData lineData = new LineData(dataSet);
            lineChart.setData(lineData);
            
            // Set chart style
            Description description = new Description();
            description.setText("");
            lineChart.setDescription(description);
            
            lineChart.setDrawGridBackground(false);
            lineChart.setDrawBorders(false);
            lineChart.setScaleEnabled(true);
            lineChart.setPinchZoom(true);
            lineChart.setDragEnabled(true);
            
            // X-axis settings
            XAxis xAxis = lineChart.getXAxis();
            xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
            xAxis.setDrawGridLines(false);
            xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
            xAxis.setLabelCount(Math.min(labels.size(), 5));
            xAxis.setTextSize(10f);
            
            // Y-axis settings
            YAxis leftAxis = lineChart.getAxisLeft();
            leftAxis.setDrawGridLines(true);
            leftAxis.setGridColor(android.graphics.Color.parseColor("#E0E0E0"));
            leftAxis.setTextSize(11f);
            
            lineChart.getAxisRight().setEnabled(false);
            
            // Legend settings
            lineChart.getLegend().setEnabled(false);
            
            lineChart.animateX(1000);
            lineChart.invalidate();
            
            layoutChartsContainer.addView(chartView);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
