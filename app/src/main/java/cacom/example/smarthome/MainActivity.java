package cacom.example.smarthome;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    TextView TempVal,HumiVal,LightVal,SoilVal,CO2Val,TempThreshold,SoilThreshold,HumiThreshold,LightThreshold,CO2Threshold,Switch1Name,Switch2Name,Switch3Name,Switch4Name,Switch5Name;
    Switch Mode,Threshold,Switch1,Switch2,Switch3,Switch4,Switch5;
    ImageView TempThresholdDown,TempThresholdAdd,SoilThresholdDown,SoilThresholdAdd,HumiThresholdDown,HumiThresholdAdd,LightThresholdDown,LightThresholdAdd,CO2ThresholdDown,CO2ThresholdAdd;
    ImageView btnHistory, btnAnalysis, btnImageRecognition, btnAIChat, btnDashboard;


    int math = (int) ((Math.random() * 9 + 1) * (10000));
    String mqttid="sadjk"+String.valueOf(math);
    private ScheduledExecutorService scheduler;
    private MqttClient client;
    private Handler handler;
    private String host = "tcp://47.109.89.8:1883";     // TCP协议
    private String userName = "root23";
    private String passWord = "root34";
    private String mqtt_id = mqttid;
    private String mqtt_sub_topic = "";
    private String mqtt_pub_topic = "";
    private Vibrator myVibrator;
    private SharedPreferences sharedPreferences;
    private AlertDialog loginDialog;

    Message msg = new Message();
    //
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences("UserData", MODE_PRIVATE);


        Control_initialization();
        showLoginDialog(() -> {
            // 对话框关闭后执行的核心逻辑
            initializeAfterLogin();
        });

        // 导航按钮点击事件
        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnAnalysis.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AnalysisActivity.class);
            startActivity(intent);
        });

        btnImageRecognition.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ImageRecognitionActivity.class);
            startActivity(intent);
        });

        btnAIChat.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AIChatActivity.class);
            startActivity(intent);
        });

        btnDashboard.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
            startActivity(intent);
        });


    }

    private void showLoginDialog(Runnable onSuccess) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_login, null);
        EditText etUsername = dialogView.findViewById(R.id.et_username);
        EditText etPassword = dialogView.findViewById(R.id.et_password);

        // 自动填充已保存数据
        String savedUsername = sharedPreferences.getString("username", "");
        String savedPassword = sharedPreferences.getString("password", "");
        etUsername.setText(savedUsername);
        etPassword.setText(savedPassword);

        // 构建不可取消的对话框
        loginDialog = new AlertDialog.Builder(this)
                .setTitle(savedUsername.isEmpty() ? "首次输入" : "确认主题")
                .setView(dialogView)
                .setPositiveButton("确定", null) // 先设为null
                .setNegativeButton("取消", (dialog, which) -> finish())
                .setCancelable(false) // 禁用返回键和外部点击
                .create();

        // 自定义确定按钮逻辑
        loginDialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = loginDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String username = etUsername.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                // 清除旧错误提示
                etUsername.setError(null);
                etPassword.setError(null);

                // 输入校验
                boolean hasError = false;
                if (username.isEmpty()) {
                    etUsername.setError("不能为空");
                    hasError = true;
                }
                if (password.isEmpty()) {
                    etPassword.setError("不能为空");
                    hasError = true;
                }

                // 校验通过后保存数据并执行后续逻辑
                if (!hasError) {
                    saveUserData(username, password);
                    mqtt_sub_topic=username;
                    mqtt_pub_topic=password;
                    loginDialog.dismiss();
                    onSuccess.run(); // 执行后续初始化
                }
            });
        });

        loginDialog.show();
    }

    private void saveUserData(String username, String password) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("username", username);
        editor.putString("password", password);
        editor.apply();
        showSavedData();
    }

    private void showSavedData() {
        String username = sharedPreferences.getString("username", "未保存");
        String password = sharedPreferences.getString("password", "未保存");

    }

    private void initializeAfterLogin() {
        // 这里放置需要在登录后执行的代码
        // 例如：跳转到主界面、加载网络数据等
        Mqtt_init();
        startReconnect();
        Listen_for_events();
        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case 1: //开机校验更新回传
                        break;
                    case 2:  // 反馈回传
                        break;
                    case 3:  //MQTT 收到消息回传   UTF8Buffer msg=new UTF8Buffer(object.toString());

                        System.out.println(msg.obj.toString());
                        //  parseJsonobj(msg.obj.toString());
                        parseJsonobj(msg.obj.toString());
                        // publishmessageplus(mqtt_pub_topic,"{\"data1\":28,\"data2\":83,\"data3\":9,\"data4\":9,\"data5\":9}\n");

                        // 显示MQTT数据
                        break;
                    case 30:  //连接失败
                        //parseJsonobj(mqtt_sub_topic);
                        Toast.makeText(MainActivity.this,"MQTT服务器连接失败" ,Toast.LENGTH_SHORT).show();
                        break;
                    case 31:   //连接成功
                        Toast.makeText(MainActivity.this,"MQTT服务器连接成功,等待硬件数据上报" ,Toast.LENGTH_SHORT).show();
                        try {
                            //parseJsonobj(mqtt_sub_topic);
                            client.subscribe(mqtt_sub_topic,0);
                            //client.publish(mqtt_pub_topic,1);
                        } catch (MqttException e) {

                            e.printStackTrace();
                        }
                        break;
                    default:
                        break;
                }
            }
        };

    }
    private void Mqtt_init()
    {
        try {
            //host为主机名，test为clientid即连接MQTT的客户端ID，一般以客户端唯一标识符表示，MemoryPersistence设置clientid的保存形式，默认为以内存保存
            client = new MqttClient(host, mqtt_id,

                    new MemoryPersistence());

            //MQTT的连接设置

            MqttConnectOptions options = new MqttConnectOptions();
            //设置是否清空session,这里如果设置为false表示服务器会保留客户端的连接记录，这里设置为true表示每次连接到服务器都以新的身份连接
            options.setCleanSession(false);
            //设置连接的用户名
            options.setUserName(userName);
            //设置连接的密码

            options.setPassword(passWord.toCharArray());
            // 设置超时时间 单位为秒
            options.setConnectionTimeout(10);
            // 设置会话心跳时间 单位为秒 服务器会每隔1.5*20秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
            options.setKeepAliveInterval(20);
            //设置回调

            client.setCallback(new MqttCallback() {
                @Override

                public void connectionLost(Throwable cause) {
                    //连接丢失后，一般在这里面进行重连
                    System.out.println("connectionLost----------");

                    //startReconnect();
                }
                @Override

                public void deliveryComplete(IMqttDeliveryToken token) {
                    //publish后会执行到这里
                    //publishmessageplus(mqtt_pub_topic,"nihao");

                    System.out.println("deliveryComplete---------"
                            + token.isComplete());

                }
                @Override

                public void messageArrived(String topicName, MqttMessage message)
                        throws Exception {

                    //subscribe后得到的消息会执行到这里面
                    System.out.println("messageArrived----------");
                    Message msg = new Message();

                    // parseJsonobj(mqtt_sub_topic);

                    msg.what = 3;   //收到消息标志位
//                    msg.obj = topicName + "---" +message.toString();
                    msg.obj = message.toString();
                    handler.sendMessage(msg);    // hander 回传

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // MQTT连接函数
    private void Mqtt_connect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!(client.isConnected()) )  //如果还未连接
                    {
                        MqttConnectOptions options = null;
                        client.connect(options);
                        Message msg = new Message();
                        msg.what = 31;
                        handler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    handler.sendMessage(msg);
                }
            }
        }).start();
    }

    // MQTT重新连接函数
    private void startReconnect() {

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (!client.isConnected()) {
                    Mqtt_connect();
                }
            }
        }, 0*1000, 10 * 1000, TimeUnit.MILLISECONDS);
    }

    // 订阅函数    (下发任务/命令)
    private void publishmessageplus(String topic,String message2)
    {
        if (client == null || !client.isConnected()) {
            return;
        }
        MqttMessage message = new MqttMessage();
        message.setPayload(message2.getBytes());
        try {
            client.publish(topic,message2.getBytes(),0,false);
            // client.publish(topic,message);
        } catch (MqttException e) {

            e.printStackTrace();
        }
    }
    private void parseJsonobj(String jsonobj){
        // 解析json

        try {

            JSONObject jsonObject = new JSONObject(jsonobj);
            String sensor1 = jsonObject.getString("sensor1");
            String sensor2 = jsonObject.getString("sensor2");
            String sensor3 = jsonObject.getString("sensor3");
            String sensor4 = jsonObject.getString("sensor4");//
            String sensor5 = jsonObject.getString("sensor5");
            String sensor6 = jsonObject.getString("sensor6");
            String sensor7 = jsonObject.getString("sensor7");
            String sensor8 = jsonObject.getString("sensor8");
            String sensor9 = jsonObject.getString("sensor9");
            String sensor10 = jsonObject.getString("sensor10");
            String sensor11 = jsonObject.getString("sensor11");
            String sensor12 = jsonObject.getString("sensor12");
            String sensor13 = jsonObject.getString("sensor13");
            String sensor14 = jsonObject.getString("sensor14");
            String sensor15 = jsonObject.getString("sensor15");
            
            // 更新UI显示
            TempVal.setText(sensor1+"°C");
            HumiVal.setText(sensor2+"%RH");
            LightVal.setText(sensor3+"Lux");
            SoilVal.setText(sensor4+"%RH");
            CO2Val.setText(sensor5+"PPM");
            TempThreshold.setText(sensor6);
            HumiThreshold.setText(sensor7);
            LightThreshold.setText(sensor8);
            SoilThreshold.setText(sensor9);
            CO2Threshold.setText(sensor10);

            // 保存当前传感器数据供分析页面使用
            saveCurrentSensorData(sensor1, sensor2, sensor3, sensor4, sensor5);
            
            // 保存历史记录
            HistoryActivity.saveRecord(MainActivity.this, 
                sensor1+"°C", sensor2+"%RH", sensor3+"Lux", 
                sensor4+"%RH", sensor5+"PPM");

            if (Integer.parseInt(sensor11)==1){

                Switch1Name.setTextColor(Color.GREEN);
            }else {

                Switch1Name.setTextColor(Color.RED);
            }

            if (Integer.parseInt(sensor12)==1){

                Switch2Name.setTextColor(Color.GREEN);
            }else {

                Switch2Name.setTextColor(Color.RED);
            }

            if (Integer.parseInt(sensor13)==1){

                Switch3Name.setTextColor(Color.GREEN);
            }else {

                Switch3Name.setTextColor(Color.RED);
            }

            if (Integer.parseInt(sensor14)==1){

                Switch4Name.setTextColor(Color.GREEN);
                myVibrator.cancel();
                myVibrator.vibrate(new long[]{100, 100, 100, 1000}, 0);
            }else {
                myVibrator.cancel();
                Switch4Name.setTextColor(Color.RED);
            }

            if (Integer.parseInt(sensor15)==1){
                Switch5Name.setTextColor(Color.GREEN);
            }else {
                Switch5Name.setTextColor(Color.RED);
            }
            //

        } catch (JSONException e) {

            e.printStackTrace();
        }
    }


    // 保存当前传感器数据供分析页面使用
    private void saveCurrentSensorData(String temp, String humi, String light, String soil, String co2) {
        SharedPreferences.Editor editor = getSharedPreferences("CurrentSensorData", MODE_PRIVATE).edit();
        editor.putString("temp", temp);
        editor.putString("humi", humi);
        editor.putString("light", light);
        editor.putString("soil", soil);
        editor.putString("co2", co2);
        editor.apply();
    }


    //给控件获取id
    private void Control_initialization(){
        myVibrator = (Vibrator) getSystemService(Service.VIBRATOR_SERVICE);
        TempVal=findViewById(R.id.TempVal);
        HumiVal=findViewById(R.id.HumiVal);
        LightVal=findViewById(R.id.LightVal);
        SoilVal=findViewById(R.id.SoilVal);
        CO2Val=findViewById(R.id.CO2Val);

        TempThreshold=findViewById(R.id.TempThreshold);
        HumiThreshold=findViewById(R.id.HumiThreshold);
        LightThreshold=findViewById(R.id.LightThreshold);
        SoilThreshold=findViewById(R.id.SoilThreshold);
        CO2Threshold=findViewById(R.id.CO2Threshold);

        Switch1=findViewById(R.id.Switch1);
        Switch2=findViewById(R.id.Switch2);
        Switch3=findViewById(R.id.Switch3);
        Switch4=findViewById(R.id.Switch4);
        Switch5=findViewById(R.id.Switch5);

        Switch1Name=findViewById(R.id.Switch1Name);
        Switch2Name=findViewById(R.id.Switch2Name);
        Switch3Name=findViewById(R.id.Switch3Name);
        Switch4Name=findViewById(R.id.Switch4Name);
        Switch5Name=findViewById(R.id.Switch5Name);

        Mode=findViewById(R.id.Mode);
        Threshold=findViewById(R.id.Threshold);

        TempThresholdAdd=findViewById(R.id.TempThresholdAdd);
        TempThresholdDown=findViewById(R.id.TempThresholdDown);
        HumiThresholdAdd=findViewById(R.id.HumiThresholdAdd);
        HumiThresholdDown=findViewById(R.id.HumiThresholdDown);
        LightThresholdAdd=findViewById(R.id.LightThresholdAdd);
        LightThresholdDown=findViewById(R.id.LightThresholdDown);
        SoilThresholdAdd=findViewById(R.id.SoilThresholdAdd);
        SoilThresholdDown=findViewById(R.id.SoilThresholdDown);
        CO2ThresholdAdd=findViewById(R.id.CO2ThresholdAdd);
        CO2ThresholdDown=findViewById(R.id.CO2ThresholdDown);

        btnHistory=findViewById(R.id.btn_history);
        btnAnalysis=findViewById(R.id.btn_analysis);
        btnImageRecognition=findViewById(R.id.btn_image_recognition);
        btnAIChat=findViewById(R.id.btn_ai_chat);

        btnDashboard=findViewById(R.id.btn_dashboard);

        Switch1.setVisibility(View.INVISIBLE);
        Switch2.setVisibility(View.INVISIBLE);
        Switch3.setVisibility(View.INVISIBLE);
        Switch4.setVisibility(View.INVISIBLE);
        Switch5.setVisibility(View.INVISIBLE);


    }
    //监听事件
    private void Listen_for_events(){

      Mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              if (isChecked){
                  Switch1.setVisibility(View.VISIBLE);
                  Switch2.setVisibility(View.VISIBLE);
                  Switch3.setVisibility(View.VISIBLE);
                  Switch4.setVisibility(View.VISIBLE);
                  Switch5.setVisibility(View.VISIBLE);
                  Switch1.setChecked(false);
                  Switch2.setChecked(false);
                  Switch3.setChecked(false);
                  Switch4.setChecked(false);
                  Switch5.setChecked(false);
                  publishmessageplus(mqtt_pub_topic,"Manual");
              }else {
                  Switch1.setVisibility(View.INVISIBLE);
                  Switch2.setVisibility(View.INVISIBLE);
                  Switch3.setVisibility(View.INVISIBLE);
                  Switch4.setVisibility(View.INVISIBLE);
                  Switch5.setVisibility(View.INVISIBLE);
                  publishmessageplus(mqtt_pub_topic,"Automatic");
              }
          }
      });

        Switch1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
              if (isChecked){
                  publishmessageplus(mqtt_pub_topic,"Switch1ON");
              }else {
                  publishmessageplus(mqtt_pub_topic,"Switch1OFF");
              }
          }
      });

        Switch2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    publishmessageplus(mqtt_pub_topic,"Switch2ON");
                }else {
                    publishmessageplus(mqtt_pub_topic,"Switch2OFF");
                }
            }
        });

        Switch3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    publishmessageplus(mqtt_pub_topic,"Switch3ON");
                }else {
                    publishmessageplus(mqtt_pub_topic,"Switch3OFF");
                }
            }
        });

        Switch4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    publishmessageplus(mqtt_pub_topic,"Switch4ON");
                }else {
                    publishmessageplus(mqtt_pub_topic,"Switch4OFF");
                }
            }
        });


        Switch5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    publishmessageplus(mqtt_pub_topic,"Switch5ON");
                }else {
                    publishmessageplus(mqtt_pub_topic,"Switch5OFF");
                }
            }
        });


        Threshold.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    publishmessageplus(mqtt_pub_topic,"ThresholdON");
                }else {
                    publishmessageplus(mqtt_pub_topic,"ThresholdOFF");
                }
            }
        });

        TempThresholdDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"TempThresholdDown");
            }
        });

        TempThresholdAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"TempThresholdAdd");
            }
        });



        HumiThresholdAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"HumiThresholdAdd");
            }
        });

        HumiThresholdDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"HumiThresholdDown");
            }
        });

        LightThresholdAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"LightThresholdAdd");
            }
        });

        LightThresholdDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"LightThresholdDown");
            }
        });

        SoilThresholdAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"SoilThresholdAdd");
            }
        });

        SoilThresholdDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"SoilThresholdDown");
            }
        });

        CO2ThresholdAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"CO2ThresholdAdd");
            }
        });

        CO2ThresholdDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                publishmessageplus(mqtt_pub_topic,"CO2ThresholdDown");
            }
        });

    }
}