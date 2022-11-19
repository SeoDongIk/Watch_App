package com.example.bletest1watch;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class SetFTPPreference {
    private Context context;
    private SharedPreferences sharedPreferences;
    private String TAG = "FTP preference";

    SetFTPPreference(Context context){
        this.context = context;
        sharedPreferences = context.getSharedPreferences("FTPinfo", Context.MODE_PRIVATE);
    }

    public void getPreferences(){ //설정 가져오기
        SharedObjects.ip = sharedPreferences.getString("ip","");
        SharedObjects.userPw = sharedPreferences.getString("userPw","");
        SharedObjects.userId = sharedPreferences.getString("userId","");
        SharedObjects.port = sharedPreferences.getInt("port",0);
        Log.d(TAG,"ip: "+ SharedObjects.ip);
        Log.d(TAG,"port: "+ SharedObjects.port);
        Log.d(TAG,"userId: "+ SharedObjects.userId);
        Log.d(TAG,"userPw: "+ SharedObjects.userPw);
    }

    public void setPreferences(String ip, String userPw, String userId, int port){ //설정 저장하기
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("ip", ip);
        editor.putString("userPw", userPw);
        editor.putString("userId", userId);
        editor.putInt("port", port);
        editor.commit();

        getPreferences();
    }
}
