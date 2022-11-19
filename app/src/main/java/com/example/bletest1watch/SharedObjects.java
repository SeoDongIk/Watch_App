package com.example.bletest1watch;

import android.content.Intent;

public class SharedObjects { //여러 함수나 클래스에서 읽는 객체들 (쓰기는 단방향)
    //측정Intent
    public static Intent backgroundService = null;

    //측정용
    public static int level = 0;
    public static String deviceID = "";
    public static Controller controller = null;
    public static boolean isMeasuring = false;
    public static boolean isConnected = false;
    public static boolean isBackgroundRunning = false;
    public static boolean isWhiteListing = false;

    //FTP
    public static String ip, userId, userPw;
    public static String fileName;
    public static int port;
    static Boolean conState, disconState, uploadState;

    //기기이름
    public static String connectedDevice;
}
