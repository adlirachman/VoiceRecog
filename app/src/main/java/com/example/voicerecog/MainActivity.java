package com.example.voicerecog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

import AlizeSpkRec.*;


public class MainActivity extends AppCompatActivity {
    SimpleSpkDetSystem alizeSystem;
    AudioRecord audioRecord = null;
    boolean isRecording = false;
    int buffSize = 0;
    int buffSize2 = 0;
    int blockSize = 256;
    short[] shortBuffer = new short[1024];
    short[] shortBuffer2 = new short[1024];
    byte[] byteBuffer;
    int sampleRate = 44100;


    public EditText mNama;
    public TextView mNama2;
    public Button mStartButton,mStartButton2;
    public Button mStopButton,mStopButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            config();
            loadBackgroundmodel();
            Log.d("Alize Status", "Feature Count: "+String.valueOf(alizeSystem.featureCount()));
            Log.d("Alize Status", "Speaker Count: "+String.valueOf(alizeSystem.speakerCount()));
            Log.d("Alize Status", "isUBMLoaded  : "+String.valueOf(alizeSystem.isUBMLoaded()));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (AlizeException e) {
            e.printStackTrace();
        }
        mNama = (EditText) findViewById(R.id.nama);
        mStartButton = (Button) findViewById(R.id.start_record);
        mStopButton = (Button) findViewById(R.id.stop_record);
        mNama2 = (TextView) findViewById(R.id.result);
        mStartButton2 = (Button) findViewById(R.id.start_record2);
        mStopButton2 = (Button) findViewById(R.id.stop_record2);

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestRecordAudioPermission();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buffSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT );
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffSize);
                audioRecord.startRecording();

                isRecording=true;
                audioRecord.read(shortBuffer,0,shortBuffer.length);
                Toast.makeText(MainActivity.this,"Recording start",Toast.LENGTH_SHORT).show();
            }
        });

        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioRecord.stop();
                isRecording = false;
                try {
                    trainSpeakerModel(shortBuffer);
                } catch (AlizeException e) {
                    e.printStackTrace();
                }
                Toast.makeText(MainActivity.this,"Recording stopped",Toast.LENGTH_SHORT).show();
                try {
                    alizeSystem.resetAudio();
                    alizeSystem.resetFeatures();
                    Log.d("Alize","system reset done");
                } catch (AlizeException e) {
                    e.printStackTrace();
                }
            }
        });

        mStartButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                buffSize2 = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT );
                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffSize2);
                audioRecord.startRecording();

                isRecording=true;
                audioRecord.read(shortBuffer2,0,shortBuffer2.length);
                Toast.makeText(MainActivity.this,"Recording start",Toast.LENGTH_SHORT).show();
            }
        });

        mStopButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                audioRecord.stop();
                isRecording = false;
                Toast.makeText(MainActivity.this,"Recording stopped",Toast.LENGTH_SHORT).show();
                String nama = mNama.getText().toString();
                short[] audio = shortBuffer2;
                try {
                    alizeSystem.addAudio(audio);
                    SimpleSpkDetSystem.SpkRecResult verificationResult = alizeSystem.verifySpeaker(nama);
                    boolean match = verificationResult.match;
//                    Toast.makeText(MainActivity.this,String.valueOf(match),Toast.LENGTH_SHORT).show();
                    mNama2.setText(String.valueOf(match));
                } catch (AlizeException e) {
                    e.printStackTrace();
                }
            }
        });


    }

    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;

            // If the user previously denied this permission then show a message explaining why
            // this permission is needed
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }

    public void config() throws IOException, AlizeException {
        InputStream configAsset = getApplicationContext().getAssets().open("AlizeConfigurationExample.cfg");
        alizeSystem = new SimpleSpkDetSystem(configAsset,getApplicationContext().getFilesDir().getPath());
        configAsset.close();
    }

    public void loadBackgroundmodel() throws IOException, AlizeException {
        InputStream backgroundModelAsset = getApplicationContext().getAssets().open("gmm/world.gmm");
        alizeSystem.loadBackgroundModel(backgroundModelAsset);
        backgroundModelAsset.close();
    }

    public void trainSpeakerModel(short[] input) throws AlizeException {
        if(input != null){
            String nama = mNama.getText().toString();
            alizeSystem.addAudio(input);
            alizeSystem.createSpeakerModel(nama);
            alizeSystem.saveSpeakerModel(nama,"test_"+nama);
            Toast.makeText(MainActivity.this,"Train Speaker berhasil!",Toast.LENGTH_SHORT).show();
            Log.d("System Status", "speaker : "+String.valueOf(alizeSystem.speakerCount()));
        }
    }

}
