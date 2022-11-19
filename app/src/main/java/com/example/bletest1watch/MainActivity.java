package com.example.bletest1watch;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {
    // Debugging
    private static final String TAG = "BluetoothMain";

    // * UI * //
    public Button stopBtn;
    public int saveN;

    // * Time * //
    private boolean thread_state;

    // * Bleutooth * //
    private Controller controller;
    public BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터


    // * Message code *
    public static final int REQUEST_ENABLE_BT = 1; // 블루투스 활성화 요청 메시지
    public static final int DISCOVERY_REQUEST = 2; // 기기가 검색될 수 있도록 활성화 요청 메시지
    public static final int PERMISSIONS_REQUEST = 1; // 권한 요청

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3; // watch는 필요없음. 오직 read만 함.
    public static final int MESSAGE_DEVICE_OBJECT = 4;
    public static final int MESSAGE_TOAST = 5;

    public static final String DEVICE_OBJECT = "device_name";
    public static final String TOAST = "toast";


    private PowerManager.WakeLock wakeLock;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // 화면 켜두기
        Log.d(TAG,"app opened");
        doCheckPermission();
        initBLE();
        init();
        SharedObjects.deviceID = bluetoothAdapter.getName();
        PowerManager pm= (PowerManager) getSystemService(Context.POWER_SERVICE);
        //backgroundService 초기화
        String packageName= getPackageName();
        SharedObjects.backgroundService = new Intent(getApplicationContext(), WatchBackgroundService.class);
        SharedObjects.backgroundService.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        SharedObjects.backgroundService.setData(Uri.parse("package:" + packageName));
        if (pm.isIgnoringBatteryOptimizations(packageName) ){

        } else {    // 메모리 최적화가 되어 있다면, 풀기 위해 설정 화면 띄움.
            //bluetoothService 초기화, 실행
            Intent bluetoothService = new Intent(getApplicationContext(), WatchBluetoothService.class);
            bluetoothService.setData(Uri.parse("package:" + packageName));
            startForegroundService(bluetoothService);
        }

        stopBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view) {
                if(SharedObjects.isBackgroundRunning){
                    stopBtn.setText("NOT MEASURING");
                    stopService(SharedObjects.backgroundService);
                }
            }
        });
    }

    public void doCheckPermission() {
        // 권한 X
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.BODY_SENSORS, Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST);

        }
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            SharedObjects.isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "bletest1watch::wakelock");
        wakeLock.acquire();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode){
            case PERMISSIONS_REQUEST :
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }
                else {

                }
                break;
        }
    }

    public void init() { // initialize & get sensor
        // for UI
        stopBtn = (Button) findViewById(R.id.stopBtn);
        saveN = 0;

        // for FTP
        SetFTPPreference pref = new SetFTPPreference(getApplicationContext());
        pref.getPreferences();

        thread_state = false;
        Log.d(TAG, "Time thread : timeT.start");
    }

    private Handler handler = new Handler(new Handler.Callback() {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case Controller.STATE_CONNECTED:
                            break;
                        case Controller.STATE_CONNECTING:
                            break;
                        case Controller.STATE_LISTEN:
                        case Controller.STATE_NONE:
                            break;
                    }
                    break;
                case MESSAGE_READ: // 메시지 Read는 watch 만
                    Log.d(TAG,"MESSAGE_READ");

                    byte[] readBuf = (byte[]) msg.obj;
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    String[] msg_split = readMessage.split("-");

                    Log.d("watch","Command : "+msg_split[0]);
                    Toast.makeText(getApplicationContext(), "Command : "+ msg_split[0], Toast.LENGTH_SHORT).show();

                    if (msg_split[0].equals("start") && (!SharedObjects.isMeasuring)){
                        if (!SharedObjects.isConnected){ // 연결된 상태가 아닐때
                            Toast.makeText(getApplicationContext(), "Not connected.\n"+"Not started", Toast.LENGTH_SHORT).show();
                        }
                        else if (SharedObjects.isConnected){ // 연결된 상태일때 & 측정중이 아닐때
                            // 서비스 시작 (측정 시작)
                            startForegroundService(SharedObjects.backgroundService);
                            SharedObjects.conState = Boolean.FALSE;
                            SharedObjects.disconState = Boolean.FALSE;
                            SharedObjects.uploadState = Boolean.FALSE;
                            stopBtn.setText("FORCESTOP");

                            // Thread 초기화
                            /*if (mFtpConnectThread != null) {
                                mFtpConnectThread = null;
                            }
                            if (mFtpDisconnectThread != null) {
                                mFtpDisconnectThread = null;
                            }
                            if (mFtpUploadThread != null) {
                                mFtpUploadThread = null;
                            }*/
                        }
                    }
                    else if (msg_split[0].equals("stop") && (SharedObjects.isMeasuring)){
                        if (!SharedObjects.isConnected){ // 연결된 상태가 아닐때
                            Toast.makeText(getApplicationContext(), "Not connected.\n"+"No stop", Toast.LENGTH_SHORT).show();
                        }
                        else if (SharedObjects.isMeasuring){ // 연결된 상태일때 & 측정중일때
                            // 서비스 종료 (데이터 저장)
                            stopService(SharedObjects.backgroundService);;
                        }
                    }
                    else if (msg_split[0].equals("level")){
                        SharedObjects.level = Integer.parseInt(msg_split[1]);
                        Log.d(TAG, String.valueOf(SharedObjects.level));
                        //levelBtn.setText("Level : " + level);
                    }
                    else if (msg_split[0].equals("sendInfo")){
                        SetFTPPreference pref = new SetFTPPreference(getApplicationContext());

                        String ip = msg_split[1];
                        int port = Integer.parseInt(msg_split[2]);
                        String userId = msg_split[3];
                        String userPw = msg_split[4];
                        pref.setPreferences(ip, userPw, userId, port);
                    }
                    break;
                case MESSAGE_DEVICE_OBJECT: // ~기기와 연결되었다.
                    SharedObjects.isConnected = true;
                    BluetoothDevice connectingDevice = msg.getData().getParcelable(DEVICE_OBJECT);
                    Toast.makeText(getApplicationContext(), "Connected to " + connectingDevice.getName(),
                            Toast.LENGTH_SHORT).show();
                    SharedObjects.connectedDevice = connectingDevice.getName();
                    break;
                case MESSAGE_TOAST: // connection lost
                    SharedObjects.isConnected = false;
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    });

    public void initBLE() {
        // BluetoothAdapter : 기기 자체 블루투스 송수신 장치. 이 객체를 이용해 상호작용 가능.
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // BL 지원하는지 확인
        if (bluetoothAdapter != null) { // BL 지원하는 경우
            // 현재 블루투스가 활성화되어있는지 확인.
            if (!bluetoothAdapter.isEnabled()) { // False -> 현재 블루투스 비활성화되어있음.
                // 블루투스 활성화 요청
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else { // True -> 현재 블루투스 활성화되어있음.
                SharedObjects.controller = new Controller(this, handler);
            }
        }
    }

    // 블루투스 활성화 요청에 대한 액션
    public void onActivityResult ( int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    controller = new Controller(this, handler);
                } else {
                    Toast.makeText(this, "Bluetooth still disabled, turn off application!", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            case DISCOVERY_REQUEST:
                Toast.makeText(this, "It can be scanned for 5 minutes.", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (SharedObjects.controller != null)
            SharedObjects.controller.stop();
        wakeLock.release();
    }

}