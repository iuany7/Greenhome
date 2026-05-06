package cacom.example.smarthome;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<HistoryRecord> recordList;
    private TextView totalRecords, avgTemp, avgHumi;
    private Button btnClear;
    private ImageView btnBack;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        sharedPreferences = getSharedPreferences("SensorData", MODE_PRIVATE);
        
        initialization();
        loadHistoryData();
    }

    private void initialization() {
        btnBack = findViewById(R.id.btn_back);
        btnClear = findViewById(R.id.btn_clear);
        totalRecords = findViewById(R.id.total_records);
        avgTemp = findViewById(R.id.avg_temp);
        avgHumi = findViewById(R.id.avg_humi);
        recyclerView = findViewById(R.id.recyclerView_history);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recordList = new ArrayList<>();
        adapter = new HistoryAdapter(recordList);
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
        
        btnClear.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定要清空所有历史记录吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    clearAllData();
                    Toast.makeText(this, "已清空所有记录", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
        });
    }

    private void loadHistoryData() {
        String dataJson = sharedPreferences.getString("history_data", "[]");
        
        try {
            JSONArray jsonArray = new JSONArray(dataJson);
            recordList.clear();
            
            // 反向遍历，最新的在前面
            for (int i = jsonArray.length() - 1; i >= 0; i--) {
                JSONObject json = jsonArray.getJSONObject(i);
                HistoryRecord record = new HistoryRecord();
                record.setTime(json.getString("time"));
                record.setTemp(json.getString("temp"));
                record.setHumi(json.getString("humi"));
                record.setLight(json.getString("light"));
                record.setSoil(json.getString("soil"));
                record.setCo2(json.getString("co2"));
                recordList.add(record);
            }
            
            adapter.notifyDataSetChanged();
            calculateStatistics(jsonArray);
            
        } catch (JSONException e) {
            e.printStackTrace();
            Toast.makeText(this, "加载历史记录失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateStatistics(JSONArray jsonArray) {
        if (jsonArray.length() == 0) {
            totalRecords.setText("0");
            avgTemp.setText("--°C");
            avgHumi.setText("--%RH");
            return;
        }

        double totalTemp = 0, totalHumi = 0;
        int count = 0;

        try {
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = jsonArray.getJSONObject(i);
                String tempStr = json.getString("temp").replace("°C", "").trim();
                String humiStr = json.getString("humi").replace("%RH", "").trim();
                
                try {
                    totalTemp += Double.parseDouble(tempStr);
                    totalHumi += Double.parseDouble(humiStr);
                    count++;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        totalRecords.setText(String.valueOf(jsonArray.length()));
        
        if (count > 0) {
            double avgTempVal = totalTemp / count;
            double avgHumiVal = totalHumi / count;
            avgTemp.setText(String.format(Locale.getDefault(), "%.1f°C", avgTempVal));
            avgHumi.setText(String.format(Locale.getDefault(), "%.1f%%RH", avgHumiVal));
        }
    }

    private void clearAllData() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("history_data", "[]");
        editor.apply();
        
        recordList.clear();
        adapter.notifyDataSetChanged();
        totalRecords.setText("0");
        avgTemp.setText("--°C");
        avgHumi.setText("--%RH");
    }

    // 历史记录数据模型
    public static class HistoryRecord {
        private String time;
        private String temp;
        private String humi;
        private String light;
        private String soil;
        private String co2;

        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
        
        public String getTemp() { return temp; }
        public void setTemp(String temp) { this.temp = temp; }
        
        public String getHumi() { return humi; }
        public void setHumi(String humi) { this.humi = humi; }
        
        public String getLight() { return light; }
        public void setLight(String light) { this.light = light; }
        
        public String getSoil() { return soil; }
        public void setSoil(String soil) { this.soil = soil; }
        
        public String getCo2() { return co2; }
        public void setCo2(String co2) { this.co2 = co2; }
    }

    // RecyclerView适配器
    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        private List<HistoryRecord> mData;

        public HistoryAdapter(List<HistoryRecord> data) {
            this.mData = data;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_record, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            HistoryRecord record = mData.get(position);
            holder.tvTime.setText(record.getTime());
            holder.tvTemp.setText(record.getTemp());
            holder.tvHumi.setText(record.getHumi());
            holder.tvLight.setText(record.getLight());
            holder.tvSoil.setText(record.getSoil());
            holder.tvCo2.setText(record.getCo2());
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTime, tvTemp, tvHumi, tvLight, tvSoil, tvCo2;

            ViewHolder(View itemView) {
                super(itemView);
                tvTime = itemView.findViewById(R.id.tv_time);
                tvTemp = itemView.findViewById(R.id.tv_temp);
                tvHumi = itemView.findViewById(R.id.tv_humi);
                tvLight = itemView.findViewById(R.id.tv_light);
                tvSoil = itemView.findViewById(R.id.tv_soil);
                tvCo2 = itemView.findViewById(R.id.tv_co2);
            }
        }
    }

    // 公开方法：供MainActivity调用以保存新记录
    public static void saveRecord(android.content.Context context, String temp, String humi, 
                                   String light, String soil, String co2) {
        SharedPreferences prefs = context.getSharedPreferences("SensorData", android.content.Context.MODE_PRIVATE);
        
        try {
            String dataJson = prefs.getString("history_data", "[]");
            JSONArray jsonArray = new JSONArray(dataJson);
            
            // 限制最多保存50条记录
            if (jsonArray.length() >= 50) {
                jsonArray = removeOldestRecord(jsonArray);
            }
            
            JSONObject newRecord = new JSONObject();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            newRecord.put("time", sdf.format(new Date()));
            newRecord.put("temp", temp);
            newRecord.put("humi", humi);
            newRecord.put("light", light);
            newRecord.put("soil", soil);
            newRecord.put("co2", co2);
            
            jsonArray.put(newRecord);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("history_data", jsonArray.toString());
            editor.apply();
            
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private static JSONArray removeOldestRecord(JSONArray jsonArray) throws JSONException {
        JSONArray newArray = new JSONArray();
        // 保留最新的49条
        for (int i = 1; i < jsonArray.length(); i++) {
            newArray.put(jsonArray.getJSONObject(i));
        }
        return newArray;
    }
}
