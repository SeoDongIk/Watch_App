package com.example.bletest1watch;

import static com.example.bletest1watch.SharedObjects.controller;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class WatchBluetoothService extends Service {
    // * FTP * //
    private ConnectFTP ConnectFTP;
    private FTPClass.FtpConnectThread mFtpConnectThread;
    private FTPClass.FtpDisconnectThread mFtpDisconnectThread;
    private FTPClass.FtpUploadThread mFtpUploadThread;
    private String ip, userId, userPw;
    private int port;
    private Boolean conState, disconState, uploadState;

    //
    private static final String TAG = "WatchBluetoothService";


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

    public WatchBluetoothService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onStart(Intent intent, int startId){
        if (controller != null) {
            if (controller.getState() == Controller.STATE_NONE) {
                // controller를 아직 시작하지 않았다고 판단 -> start
                controller.start();
                foregroundNotification();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "bluetooth stopped");
        stopForeground(true);
    }

    void foregroundNotification() { // foreground 실행 후 신호 전달 (안하면 앱 강제종료 됨)
        NotificationCompat.Builder builder;

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "bluetooth_service_channel";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Bluetooth Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                    .createNotificationChannel(channel);

            builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        } else {
            builder = new NotificationCompat.Builder(this);
        }

        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("블루투스서비스")
                .setContentIntent(pendingIntent);

        startForeground(1, builder.build());
    }
}