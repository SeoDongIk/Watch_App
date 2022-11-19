package com.example.bletest1watch;

import static com.example.bletest1watch.SharedObjects.ip;
import static com.example.bletest1watch.SharedObjects.userId;
import static com.example.bletest1watch.SharedObjects.userPw;
import static com.example.bletest1watch.SharedObjects.port;

import android.util.Log;

public class FTPClass {
    private ConnectFTP ConnectFTP;
    private final String TAG = "FTP";
    public FtpConnectThread mFtpConnectThread;
    public FtpUploadThread mFtpUploadThread;
    public FtpDisconnectThread mFtpDisconnectThread;

    FTPClass(String filenames){
        ConnectFTP = new ConnectFTP();
        mFtpConnectThread = new FtpConnectThread();
        mFtpDisconnectThread = new FtpDisconnectThread();
        mFtpUploadThread = new FtpUploadThread(filenames);
    }

    public class FtpConnectThread extends Thread {
        public void run() {
            boolean status = false;
            status = ConnectFTP.ftpConnect(ip, userId, userPw, port);
            if (status == true) {
                SharedObjects.conState = Boolean.TRUE;
                Log.d(TAG, "Connection Success : "+ SharedObjects.conState);
            } else {
                SharedObjects.conState = Boolean.FALSE;
                Log.d(TAG, "Connection failed");
            }
        }
    }

    public class FtpDisconnectThread extends Thread {
        public void run() {
            boolean result = ConnectFTP.ftpDisConnect();
            if (result == true) {
                SharedObjects.disconState = Boolean.TRUE;
                Log.d(TAG, "Disconnection Success : "+ SharedObjects.disconState);
            } else {
                SharedObjects.disconState = Boolean.FALSE;
                Log.d(TAG, "Discconection failed");
            }
        }
    }

    public class FtpUploadThread extends Thread {
        private String mName;

        public FtpUploadThread(String name) {
            this.mName = name;
        }

        public void run() {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            boolean result = ConnectFTP.ftpUploadFile(mName);
            if (result == true) {
                SharedObjects.uploadState = Boolean.TRUE;
                Log.d(TAG, "File Upload Success : "+ SharedObjects.uploadState);
            } else {
                SharedObjects.uploadState = Boolean.TRUE;
                Log.d(TAG, "File Upload failed");
            }
        }
    }
}
