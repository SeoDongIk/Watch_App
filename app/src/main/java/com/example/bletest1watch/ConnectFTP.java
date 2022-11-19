package com.example.bletest1watch;

import android.os.Environment;
import android.util.Log;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;

public class ConnectFTP {
    private final String TAG = "Connect FTP";
    private String desFilePath = "test_file_1";
    public FTPClient mFTPClient = null;

    // Constructor
    public ConnectFTP(){
        mFTPClient = new FTPClient();
    }

    // Connect to FTP server
    public boolean ftpConnect(String host, String userName, String pwd, int port) {
        boolean result = false;
        try {
            mFTPClient.connect(host,port);
//            mFTPClient.connect(host,port); // host와 port를 이용해 FTP server에 connect
            Log.d(TAG, ".connect");

            // Reply 상태 체크
            // mFTPClient.getReplyCode() : 응답이 정상적인지 확인하기 위해 응답 받아옴
            // FTPReply.isPositiveCompletion() 서버와의 응답이 정상인지 확인(정상 - true, 비정상 - false)
            if(FTPReply.isPositiveCompletion(mFTPClient.getReplyCode())){
                result = mFTPClient.login(userName, pwd); // FTP server에 로그인
                mFTPClient.enterLocalPassiveMode();
                // active mode : client가 서버에 데이터 전송 요청 -> 서버가 client에 접속해 파일을 올려줌
                // -> active mode일때는 client가 정해준 특정 port로 서버가 데이터를 전송해줌.
                // --> client에 방화벽이 있다면 서버가 접근할 수 없음.
                // passive mode : client가 server로부터 데이터를 내려받음
                mFTPClient.setFileType(FTP.ASCII_FILE_TYPE); // 파일 타입 - 바이너리
                result = true;
            }

        } catch (Exception e){
            Log.d(TAG, "Couldn't connect to host");
            Log.d(TAG,"ip: "+host);
            Log.d(TAG,"port: "+port);
            Log.d(TAG,"userId: "+userName);
            Log.d(TAG,"userPw: "+pwd);
//            e.printStackTrace();
        }

        return result;
    }

    public boolean ftpDisConnect(){
        boolean result = false;
        try {
            mFTPClient.logout();
            mFTPClient.disconnect();
            result = true;
        } catch (Exception e){
            Log.d(TAG,"Failed to disconnect with server.");
        }
        return result; // disconnect 성공시 return true
    }

    public String ftpGetDirectory(){
        String directory = null;
        try {
            directory = mFTPClient.printWorkingDirectory();
        } catch (Exception e) {
            Log.d(TAG,"Couldn't get current directory");
        }
        return directory;
    }

    public boolean ftpChangeDirectory(String directory){
        try {
            mFTPClient.changeWorkingDirectory(directory);
            return true;
        } catch(Exception e){
            Log.d(TAG, "Couldn't change the directory");
        }
        return false;
    }

    public boolean ftpUploadFile(String fileName){
        boolean result = false;
        try {
            File filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS+"/"+fileName);
            mFTPClient.enterLocalPassiveMode();
            FileInputStream file = new FileInputStream(filePath);
            Log.d(TAG, String.valueOf(filePath));
            mFTPClient.changeWorkingDirectory("Child_test/"+ SharedObjects.connectedDevice + "/");
            Log.d(TAG,"Current directory : "+ ftpGetDirectory());

            mFTPClient.setFileType(FTP.BINARY_FILE_TYPE);
            mFTPClient.setFileTransferMode(FTP.BINARY_FILE_TYPE);
            result = mFTPClient.storeFile(fileName, file);

//            if(ftpChangeDirectory(desFilePath)){
////                fileName = '"' + fileName + '"';
//                result = mFTPClient.storeFile(fileName, file);
//            }

            file.close();
        } catch(Exception e){
            Log.d(TAG, "Couldn't upload the file.");
//            e.printStackTrace();
        }
        return result;
    }
}
