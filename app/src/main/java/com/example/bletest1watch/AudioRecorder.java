package com.example.bletest1watch;

import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class AudioRecorder {

    private File fileName;
    private MediaRecorder recorder;

    AudioRecorder(File fileName){
        this.fileName = fileName;
    }

    public void setFilename(File fileName){
        this.fileName = fileName;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void startRecord(){
        Log.d("audio", fileName.toString());
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC); // 어디에서 음성 데이터를 받을 것인지
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 압축 형식 설정
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        try {
            FileWriter fw = new FileWriter(fileName, true);
            PrintWriter out = new PrintWriter(fw);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recorder.setOutputFile(fileName);
        try {
            recorder.prepare();
            recorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecord(){
        if(recorder!=null){
            recorder.stop();
            recorder.release();
        }
    }
}
