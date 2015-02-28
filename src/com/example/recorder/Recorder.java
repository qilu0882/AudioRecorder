package com.example.recorder;  
  
import android.app.Activity;  
import android.media.AudioFormat;  
import android.media.AudioManager;  
import android.media.AudioRecord;  
import android.media.AudioTrack;  
import android.media.MediaRecorder;  
import android.os.Bundle;  
import android.view.View;  
import android.widget.Button;  
import android.widget.SeekBar;  
import android.widget.Toast;  
import android.util.Log;

import java.io.File;
import java.io.BufferedInputStream;  
import java.io.BufferedOutputStream;  
import java.io.DataInputStream;  
import java.io.DataOutputStream;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;

import java.text.SimpleDateFormat;  
import java.util.Date;  
import java.util.Random;
  
public class Recorder extends Activity {  
    static final String TAG = "AudioRecorder";

    /** Called when the activity is first created. */  
    Button btnRecord, btnPlay, btnStop, btnExit;  
    SeekBar skbVolume;	//调节音量
    boolean isRecording = false;//是否正在录音的标记  
    static final int frequency = 16000;		// 16KHz
    static final int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;  
    static final int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;  
    int recBufSize,playBufSize;  
    AudioRecord audioRecord;  
    AudioTrack audioTrack;  

    File pcmFile;

    /** 
     * 生成随机文件名：当前年月日时分秒+五位随机数 
     *  
     * @return 
     */  
    public static String getRandomFileName() {  
	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");  
	String str = simpleDateFormat.format(new Date());

	return str;
  
//	Random random = new Random();  
//	int rannum = (int) (random.nextDouble() * (99999 - 10000 + 1)) + 10000;// 获取5位随机数  
//	return str + '_' + rannum;// 当前时间
    }
  
    @Override  
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);  
        setContentView(R.layout.main);  
        setTitle("Audio Recorder"); 

        // -----------------------------------------  
        recBufSize = AudioRecord.getMinBufferSize(frequency,  
                channelConfiguration, audioEncoding);  
  
        playBufSize=AudioTrack.getMinBufferSize(frequency,  
                channelConfiguration, audioEncoding);  
        // -----------------------------------------  
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, frequency,  
                channelConfiguration, audioEncoding, recBufSize);  
  
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, frequency,  
                channelConfiguration, audioEncoding,  
                playBufSize, AudioTrack.MODE_STREAM);  
        //------------------------------------------  
        btnRecord = (Button) this.findViewById(R.id.btnRecord);  
        btnRecord.setOnClickListener(new ClickEvent());  

        btnPlay = (Button) this.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(new ClickEvent());

        btnStop = (Button) this.findViewById(R.id.btnStop);  
        btnStop.setOnClickListener(new ClickEvent());  

        btnExit = (Button) this.findViewById(R.id.btnExit);  
        btnExit.setOnClickListener(new ClickEvent());  
        //------------------------------------------  

        skbVolume = (SeekBar)this.findViewById(R.id.skbVolume);  
        skbVolume.setMax(100);//音量调节的极限  
        skbVolume.setProgress(70);//设置seekbar的位置值  

        audioTrack.setStereoVolume(0.7f, 0.7f);//设置当前音量大小  

        skbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {  
              
            @Override  
            public void onStopTrackingTouch(SeekBar seekBar) {  
                float vol=(float)(seekBar.getProgress())/(float)(seekBar.getMax());  
                audioTrack.setStereoVolume(vol, vol);//设置音量  
            }  
              
            @Override  
            public void onStartTrackingTouch(SeekBar seekBar) {  
                // TODO Auto-generated method stub  
            }  
              
            @Override  
            public void onProgressChanged(SeekBar seekBar, int progress,  
                    boolean fromUser) {  
                // TODO Auto-generated method stub  
            }  
        });  
    }  
  
    @Override  
    protected void onDestroy() {  
        super.onDestroy();  
        android.os.Process.killProcess(android.os.Process.myPid());  
    }  
  
    class ClickEvent implements View.OnClickListener {  
  
        @Override  
        public void onClick(View v) {  
            if (v == btnRecord) {
                if (isRecording) {
			Log.e(TAG, "is already recording !");
			return;
		}
                isRecording = true;
                new RecordThread().start();
            } else if (v == btnPlay) {  
                isRecording = false;  
                new PlayThread().start();
            } else if (v == btnStop) {  
                isRecording = false;  
            } else if (v == btnExit) {  
                isRecording = false;  
                Recorder.this.finish();  
            }  
        }  
    }
  
    class RecordThread extends Thread {
        public void run() {
            try {
		String pcmFileName = "/storage/sdcard0/" + getRandomFileName() + ".pcm";
		pcmFile = new File(pcmFileName);
		Log.d(TAG, "pcmFileName: " + pcmFileName);
		 
                byte[] buffer = new byte[recBufSize];  
		BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(pcmFile), recBufSize);

                audioRecord.startRecording();//开始录制  
                  
                while (isRecording) {  
                    //从MIC保存数据到缓冲区  
                    int bufferReadResult = audioRecord.read(buffer, 0,  
                            recBufSize);  
  
                    byte[] tmpBuf = new byte[bufferReadResult];  
                    System.arraycopy(buffer, 0, tmpBuf, 0, bufferReadResult);  
                    //写入数据  
		    bos.write(tmpBuf, 0, tmpBuf.length);
                }
		bos.flush();
		bos.close();

                audioRecord.stop();  
            } catch (Throwable t) {  
                Toast.makeText(Recorder.this, t.getMessage(), 1000);  
            }  
        }  
    };  

    class PlayThread extends Thread {
        public void run() {
            try {  
                byte[] buffer = new byte[playBufSize];  
		BufferedInputStream bis = new BufferedInputStream(new FileInputStream(pcmFile), playBufSize);

                audioTrack.play();//开始播放  
                  
		int readSize = -1;
                while (bis != null && (readSize = bis.read(buffer)) != -1) {
                    //写入数据即播放  
                    audioTrack.write(buffer, 0, readSize);  
                }
                audioTrack.stop();  

		bis.close();
            } catch (Throwable t) {
                Toast.makeText(Recorder.this, t.getMessage(), 1000);
            }
        }
    };  

}
