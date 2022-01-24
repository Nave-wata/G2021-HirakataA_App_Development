package com.example.g2021_hirakataa_app_development;

import android.Manifest;
import android.annotation.SuppressLint;
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

import org.jtransforms.fft.DoubleFFT_1D;
import java.util.ArrayList;

public class MainActivity extends Activity implements View.OnClickListener {
    AudioRecord audioRec = null;
    AudioTrack player = null;
    Button btn = null;

    private static final int PERMISSION_RECORD_AUDIO = 1;
    final static int SAMPLING_RATE = 44100;
    boolean bIsRecording = false;
    int bufSize = 1024;
    int seekBarMax = 20;
    int seekBarProgress = 10;
    int cnt = 0;
    DoubleFFT_1D fft = new DoubleFFT_1D(bufSize);

    int[] Base_Hz = {0, 2, 4, 6, 14, 26, 50, 96, 188, 374, 746, 1024};
    int[] Base_Hz_sub = {0, 1, 2, 3, 7, 13, 25, 48, 94, 187, 373, 511};
    double[] vol_ary = {0.5, 0.526, 0.555, 0.588, 0.625, 0.666, 0.714, 0.769, 0.833, 0.909,
                        1.0,
                        1.1, 1.2,   1.3,   1.4,   1.5,   1.6,   1.7,   1.8,   1.9,   2.0};
    double[] vol = new double[10];

    /** Called when the activity is first created. */

    @SuppressLint("SetTextI18n")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        for (int i = 0; i < 10; i++) {
            vol[i] = vol_ary[10];
        }

        btn = findViewById(R.id.button_id);
        btn.setOnClickListener(this);

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
        ArrayList<SeekBar> seekbars = new ArrayList<SeekBar>();

        seekbars.add(findViewById(R.id.seekBar_1));
        seekbars.add(findViewById(R.id.seekBar_2));
        seekbars.add(findViewById(R.id.seekBar_3));
        seekbars.add(findViewById(R.id.seekBar_4));
        seekbars.add(findViewById(R.id.seekBar_5));
        seekbars.add(findViewById(R.id.seekBar_6));
        seekbars.add(findViewById(R.id.seekBar_7));
        seekbars.add(findViewById(R.id.seekBar_8));
        seekbars.add(findViewById(R.id.seekBar_9));
        seekbars.add(findViewById(R.id.seekBar_10));

        cnt = 0;
        for (SeekBar sb : seekbars) {
            sb.setMax(seekBarMax);
            sb.setProgress(seekBarProgress);
            sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // つまみが変更された時に処理が実行される
                    vol[cnt] = vol_ary[progress];
                    Log.v("SeekBar1", "progress" + vol[0]);
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
            cnt += 1;
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
        int i, j;

        for (i = 0; i < bufSize; i++) {
            buf[i] = (double)inputBuffer[i];
        }

        // フーリエ変換
        fft.realForward(buf);

        for (i = 0; i < 10; i++) {
            for (j = Base_Hz[i] + 1; j <= Base_Hz[i + 1]; j++) {
                buf[j] *= vol[i];
            }
        }

        // 逆フーリエ変換
        fft.realInverse(buf, true);

        for (i = 0; i < bufSize; i++) {
            outputBuffer[i] = (byte)buf[i];
        }

        return outputBuffer;
    }
}