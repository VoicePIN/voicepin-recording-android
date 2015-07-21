package app.template.com.voicerecorder;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Mateusz M. on 2015-04-26.
 */
public class Recorder {
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "/'/'";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLE_RATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    public static File file;
    private Context context;
    public String finalSoundPath = null;
    Handler handler;

    public static String filename;
    private double amplitude;

    public Recorder(Context context) {
        this.context = context;
    }

    public File getFileToOverwrite() {
        File tempPicFile = null;
        String ext_storage_state = Environment.getExternalStorageState();
        File mediaStorage = new File(Environment.getExternalStorageDirectory() + "/VOICEPIN/SOUNDS");

        if (ext_storage_state.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {

            if (!mediaStorage.exists()) {
                mediaStorage.mkdirs();
            }

            String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
                    .format(new Date());

            tempPicFile = new File(mediaStorage.getPath() + File.separator
                    + "audio_" + timeStamp + AUDIO_RECORDER_FILE_EXT_WAV);

            file = tempPicFile;
        } else {
            Toast.makeText(context, "NO SDCARD MOUNTED", Toast.LENGTH_LONG).show();
        }

        return tempPicFile;
    }

    public String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();

        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    public void startRecording() {
        if (!isRecording) {
            handler = new Handler();

            bufferSize = AudioRecord.getMinBufferSize(
                    RECORDER_SAMPLE_RATE
                    , RECORDER_CHANNELS
                    , RECORDER_AUDIO_ENCODING);

            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC
                    , RECORDER_SAMPLE_RATE
                    , RECORDER_CHANNELS
                    , RECORDER_AUDIO_ENCODING
                    , bufferSize);

            recorder.startRecording();
            isRecording = true;

            recordingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    writeDataToFile();
                }
            }, "AudioRecorder Thread");

            recordingThread.start();
        }
    }

    private void writeDataToFile() {
        final byte data[] = new byte[bufferSize];

        filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read,sum;

        if (null != os) {
            while (isRecording) {
                sum = 0;
                read = recorder.read(data, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    for (byte b : data) {
                        sum += b * b;
                    }
                    amplitude = Math.sqrt(sum / read);
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stopRecording() {
        String filePath = getFileToOverwrite().getAbsolutePath();
        finalSoundPath = filePath;
        if (null != recorder) {
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(), filePath);
        deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        long longSampleRate = RECORDER_SAMPLE_RATE;
        int channels = 1;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLE_RATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();//TODO: handle error
        } catch (IOException e) {
            e.printStackTrace();//TODO: handle error
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (RECORDER_BPP * RECORDER_SAMPLE_RATE / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    public String getFinalRecordFilePath() {
        return finalSoundPath;
    }

    public boolean isRecording() {
        return isRecording;
    }

    public double getAmplitude() {
        return amplitude;
    }
}


