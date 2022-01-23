package com.example.g2021_hirakataa_app_development;

import android.Manifest;
import android.annotation.SuppressLint;
import org.jtransforms.fft.DoubleFFT_1D;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener {
    AudioRecord audioRec = null;
    AudioTrack player = null;
    Button btn = null;
    TextView mText;

    private static final int PERMISSION_RECORD_AUDIO = 1;
    final static int SAMPLING_RATE = 44100;
    boolean bIsRecording = false;
    int bufSize = 1024;
    int seekBarMax = 20;
    int seekBarProgress = 9;
    DoubleFFT_1D fft = new DoubleFFT_1D(bufSize);

    double[] vol_ary = {0.5, 0.526, 0.555, 0.588, 0.625, 0.666, 0.714, 0.769, 0.833, 0.909,
            1.0,
            1.1, 1.2,   1.3,   1.4,   1.5,   1.6,   1.7,   1.8,   1.9,   2.0};
    String[] vol_str = {"-10", "-9", "-8", "-7", "-6", "-5", "-4", "-3", "-2", "-1",
            "0",
            "1",  "2",  "3",  "4",  "5",  "6",  "7",  "8",  "9", "10"};
    double vol = vol_ary[0];

    /** Called when the activity is first created. */

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = findViewById(R.id.button_id);
        btn.setText(R.string.stopping_label);
        btn.setOnClickListener(this);

        mText = findViewById(R.id.textView);
        mText.setText(R.string.stopping_label);

        // AudioRecordの作成
        audioRec = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize);

        // AudioTrackの作成
        player = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLING_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufSize,
                AudioTrack.MODE_STREAM
        );

        // SeekBarのインスタンスを取得
        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setMax(seekBarMax);
        seekBar.setProgress(seekBarProgress);
        // SeekBarのつまみの変更を検知する
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // つまみが変更された時に処理が実行される
                vol = vol_ary[progress];
                Log.v("SeekBar", "progress" + vol);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // ユーザーがタップ開始した時に処理が実行される
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // ユーザーがタップ終了した時に処理が実行される
            }
        });

        checkRecordable();
    }

    @SuppressLint("ObsoleteSdkInt")
    public void checkRecordable(){
        if(!SpeechRecognizer.isRecognitionAvailable(getApplicationContext())) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 23) {
            if(ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.RECORD_AUDIO
                        },
                        PERMISSION_RECORD_AUDIO);
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btn) {
            if (bIsRecording) {
                btn.setText(R.string.stopping_label);
                bIsRecording = false;
            } else {
                // 録音開始
                Log.v("AudioRecord", "startRecording");
                player.play();
                audioRec.startRecording();
                bIsRecording = true;
                // 録音スレッド
                new Thread(() -> {
                    byte[] inputBuffer = new byte[bufSize];
                    byte[] outputBuffer;
                    while (bIsRecording) {
                        // 録音データ読み込み
                        audioRec.read(inputBuffer, 0, bufSize);
                        outputBuffer = Amplification(inputBuffer);
                        player.write(outputBuffer, 0, bufSize);
                        // Log.v("AudioRecord", "read " + bufSize + " bytes");
                    }
                    // 録音停止
                    Log.v("AudioRecord", "stop");
                    player.stop();
                    audioRec.stop();
                }).start();
                btn.setText(R.string.running_label);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioRec.release();
    }

    protected byte[] Amplification(byte[] inputBuffer) {
        double[] buf = new double[bufSize];
        byte[] outputBuffer = new byte[bufSize];
        int i;

        for (i = 0; i < bufSize; i++) {
            buf[i] = (double)inputBuffer[i];
        }

        // フーリエ変換
        fft.realForward(buf);

        for (i = 0; i < bufSize; i++) {
            buf[i] = buf[i] * vol;
        }

        // 逆フーリエ変換
        fft.realInverse(buf, true);

        for (i = 0; i < bufSize; i++) {
            outputBuffer[i] = (byte)buf[i];
        }

        return outputBuffer;
    }
}