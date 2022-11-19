package com.example.bletest1watch;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class WatchBackgroundService extends Service implements SensorEventListener {
    private static final String TAG = "Background";
    private String fileName;
    private File path;
    private SensorManager manager;
    private Sensor mHeartRate, mGyro, mAccel, mStep;
    private ArrayList<String> sensorData, copyData;

    private int fileVer = 1;
    private int saveN = 0;
    private boolean thread_state = true;

    private TimeThread timeT;
    private Context context = null;
    private PowerManager.WakeLock wakeLock;
    private AudioRecorder audioRecorder = null;

    private class TimeThread extends Thread {
        private int timeValue = 0;

        public void run() {
            while (true) {
                try {
                    timeValue++;
                    Thread.sleep(1000);   // 1000ms, 즉 1초 단위로 작업스레드 실행
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.i(TAG, "Time thread interrupted");
                }
                if ((timeValue == 600) && thread_state) { // time = 10m(600)
                    Log.d(TAG, "Time thread : 10m");
                    timeValue = 0;
                    if (sensorData.size() != 0) {
                        saveData();

                        saveN++;
                        Log.d(TAG, "Time thread : save N -" + saveN);
                    }

                }
            }
        }

        public void re_start(){ // thread start -> 다시 측정 start 버튼 눌렀을때
            fileVer = 0;
            timeValue = 0;
            thread_state = true;
            Log.d(TAG, "Time thread : timeT.re_start("+timeValue+","+thread_state+")");
        }

        public void cancel(){ // thread stop -> 측정 stop 버튼 눌렀을때
            Log.d(TAG, "Time thread : timeT.cancel");
            thread_state = false;
        }
    }

    // 센서 측정 중지
    private class stop_sensor extends Thread {
        public void run() {
            timeT.cancel();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i(TAG, "Main thread sleep interrupted");
            }
            saveData();
            unregister();
            audioRecorder.stopRecord();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override //서비스 시작 시
    public void onStart(Intent intent, int startId){
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "bletest1watch::wakelock");
        wakeLock.acquire();

        foregroundNotification();

        setFileName();

        Log.d(TAG,"measure start");
        SharedObjects.isMeasuring = true;
        init();
        timeT = new TimeThread();
        timeT.start();
        start_sensor();
        audioRecorder.startRecord();
        SharedObjects.isBackgroundRunning = true;
    }

    @Override
    public void onDestroy(){ //서비스 종료 시
        wakeLock.release();
        Log.d(TAG,"measure stop");
        SharedObjects.isMeasuring = false;
        stop_sensor mstopsensor = new stop_sensor();
        try {
            mstopsensor.start(); //측정, 녹음 종료
            mstopsensor.join(); //저장까지 기다림
            SharedObjects.isBackgroundRunning = false;
            //doFTP(SharedObjects.fileName+".mp4"); //음성파일 전송
            //doFTP(SharedObjects.fileName+".txt"); //텍스트파일 전송
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"Service Destroyed");
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void doFTP(String uploadFileName){ // 파일 보내기
        FTPClass ftp = new FTPClass(uploadFileName);
        FTPClass.FtpConnectThread mFtpConnectThread;
        FTPClass.FtpDisconnectThread mFtpDisconnectThread;
        FTPClass.FtpUploadThread mFtpUploadThread;
        SharedObjects.conState = Boolean.FALSE;
        SharedObjects.disconState = Boolean.FALSE;
        SharedObjects.uploadState = Boolean.FALSE;

        try {
            // FTP 서버에 연결
            mFtpConnectThread = ftp.mFtpConnectThread;
            mFtpConnectThread.start();
            mFtpConnectThread.join(); // 얘가 끝날때까지 main이 기다려 줌.
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 서버에 연결을 성공했다면 -> 업로드
        if(SharedObjects.conState == Boolean.TRUE){

            mFtpUploadThread = ftp.mFtpUploadThread;
            mFtpUploadThread.start();

            // 업로드에 성공했다면 -> 연결 끊기
            if (SharedObjects.uploadState == Boolean.TRUE){
                mFtpDisconnectThread = ftp.mFtpDisconnectThread;
                mFtpDisconnectThread.start();
            }
        }
    }

    void foregroundNotification() { // foreground 실행 후 신호 전달 (안하면 앱 강제종료 됨)
        NotificationCompat.Builder builder;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "measuring_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Measuring Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("측정")
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());
    }

    public void init(){
        // for sensor
        manager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mHeartRate = manager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mGyro = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mAccel = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mStep = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        // for writing data
        sensorData = new ArrayList<String>();
        copyData = new ArrayList<String>();
        audioRecorder = new AudioRecorder(new File(path,fileName+".mp4"));
    }

    public void saveData(){ // 파일 기기 내에 저장
        Log.d(TAG, "Time thread : saveData()");
        try {
            // Save total Data
            File f = new File(path,fileName+".txt");
            Log.d(TAG, "Time thread : saveData - fileName : "+ f);
            FileWriter fw = new FileWriter(f, true); // 기존 파일에 append
            Log.d(TAG, "Time thread : saveData() - fileWriter fw");
            PrintWriter out = new PrintWriter(fw);

            // Deep copy (동시 수정을 피하기 위한)
            copyData.addAll(sensorData);
            sensorData.clear();
            Log.d(TAG, "Time thread : saveData - copyData : "+copyData.size());
            out.println(copyData);
            out.close();

            copyData.clear();
            Log.d(TAG, "Time thread : saveData end");

        } catch(IOException e) {
            Log.e(TAG, "save data failed", e);
        }
    }

    public void setFileName(){
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        } else {
            Toast.makeText(getApplicationContext(), "외부 메모리 읽기 쓰기 불가능",Toast.LENGTH_SHORT).show();
        }
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd_H_mm_ss");
        String time = mFormat.format(date);
        Log.d(TAG,"Device name : "+ SharedObjects.deviceID);
        fileName = SharedObjects.deviceID + "-" + time;
        SharedObjects.fileName = fileName;
    }

    public void register() { // register listener
        manager.registerListener(this, mHeartRate, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mAccel, SensorManager.SENSOR_DELAY_GAME);
        manager.registerListener(this, mStep, SensorManager.SENSOR_DELAY_GAME);
    }

    public void unregister() { // unregister listener
        manager.unregisterListener(this);
        sensorData.clear();
    }

    // Sensor work //
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }
    public final void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        int tag = SharedObjects.level;

        //Date date = new Date(System.currentTimeMillis());
        //SimpleDateFormat mFormat = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss_SSS");
        long date = System.currentTimeMillis();
        String time = Long.toString(date);

        if (sensor.getType() == Sensor.TYPE_HEART_RATE) {
            sensorData.add(tag+"+"+"HR+"+time+"+"+event.values[0]);
        }

        if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensorData.add(tag+"+"+"GX+"+time+"+"+event.values[0]);
            sensorData.add(tag+"+"+"GY+"+time+"+"+event.values[1]);
            sensorData.add(tag+"+"+"GZ+"+time+"+"+event.values[2]);
        }

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensorData.add(tag+"+"+"AX+"+time+"+"+event.values[0]);
            sensorData.add(tag+"+"+"AY+"+time+"+"+event.values[1]);
            sensorData.add(tag+"+"+"AZ+"+time+"+"+event.values[2]);
        }

        if (sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            sensorData.add(tag+"+"+"SC+"+time+"+"+event.values[0]);
        }
    }

    public void start_sensor(){
        saveN = 0;
        register();
        setFileName();
        timeT.re_start();
    }
}