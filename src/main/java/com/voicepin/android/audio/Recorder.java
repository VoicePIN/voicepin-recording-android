package com.voicepin.android.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

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

/**
 * Records microphone input and saves data in a file.
 * Audio file parameters:
 *
 * <ul>
 *     <li>Extension: {@value FILE_NAME_EXTENSION}</li>
 *     <li>Sample rate (Hz): {@value RECORDER_SAMPLE_RATE}</li>
 *     <li>Channels: {@value RECORDER_CHANNELS}</li>
 *     <li>Encoding: PCM 16 bit</li>
 * </ul>
 */
public class Recorder {
    private static final int RECORDER_BPP = 16;
    private static final String FILE_NAME_EXTENSION = ".wav";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLE_RATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;

    private Thread recordingThread = null;

    private int bufferSize = 0;
    private boolean isRecording = false;
    private String lastRecordFilePath = "";

    private double amplitude;
    private String date_pattern_format;
    private String recordsFolder;
    private String filePrefix;
    private String configFileName;

    public Recorder() {
        init(new ConfigurationRecorder());
    }

    public Recorder(ConfigurationRecorder configurationRecorder) {
        init(configurationRecorder);
    }

    private void init(ConfigurationRecorder configurationRecorder) {
        this.recordsFolder = configurationRecorder.directory;
        this.date_pattern_format = configurationRecorder.datePattern;
        this.filePrefix = configurationRecorder.prefix;
        this.configFileName = configurationRecorder.fileName;
    }

    /**
     * Starts recording.
     */
    public void startRecording() {
        if (!isRecording) {

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

    /**
     * Stops recording.
     *
     * @return file path to recording if it was started, null otherwise
     */
    public String stopRecording() {
        if (isRecording) {
            String filePath = getFileToOverwrite().getAbsolutePath();
            lastRecordFilePath = filePath;
            isRecording = false;
            if (null != recorder) {

                recorder.stop();
                recorder.release();

                recorder = null;
                recordingThread = null;
            }

            copyWaveFile(getTempFilename(), filePath);
            deleteTempFile();
            return lastRecordFilePath;
        } else {
            return "";
        }
    }

    /**
     * @return file path to last recording
     */
    public String getLastRecordFilePath() {
        return lastRecordFilePath;
    }

    /**
     * @return true when currently recording, false otherwise
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * @return amplitude that represents current intensity of voice (value range: <0,30>)
     */
    public double getAmplitude() {
        return amplitude;
    }

    private File getFileToOverwrite() {
        File tempPicFile = null;
        String ext_storage_state = Environment.getExternalStorageState();
        File mediaStorage = new File(Environment.getExternalStorageDirectory() + recordsFolder);

        if (ext_storage_state.equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {

            if (!mediaStorage.exists()) {
                mediaStorage.mkdirs();
            }
            String timeStamp;
            if (!TextUtils.isEmpty(date_pattern_format)) {
                timeStamp = new SimpleDateFormat(date_pattern_format)
                        .format(new Date());
            }else {
                timeStamp = configFileName;
            }

            tempPicFile = new File(mediaStorage.getPath() + File.separator
                    + filePrefix + timeStamp + FILE_NAME_EXTENSION);
        } else {
            Log.e("Recorder", "No mounted storage");

        }

        return tempPicFile;
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();

        File file = new File(filepath, "/'/'");
        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void writeDataToFile() {
        final byte data[] = new byte[bufferSize];

        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read, sum;

        if (null != os) {
            while (isRecording) {
                sum = 0;
                read = recorder.read(data, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    for (byte b : data) {
                        sum += b * b;
                    }
                    if (read != 0) {
                        amplitude = Math.sqrt(sum / read);
                    }

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

            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';
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
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
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
        header[32] = (byte) (RECORDER_BPP * RECORDER_SAMPLE_RATE / 8);
        header[33] = 0;
        header[34] = RECORDER_BPP;
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

    /**
     * Configures recording file parameters.
     */
    public static class ConfigurationRecorder {
        private String directory;
        private String datePattern;
        private String prefix;
        private String fileName;

        /**
         * @param recordsFolder recording directory
         */
        public ConfigurationRecorder setDirectory(String recordsFolder) {
            this.directory = recordsFolder;
            return this;
        }

        /**
         * @param fileDatePattern filename date format ({@link java.text.DateFormat})
         */
        public ConfigurationRecorder setDatePattern(String fileDatePattern) {
            this.datePattern = fileDatePattern;
            return this;
        }

        /**
         * @param filePrefix filename prefix
         */
        public ConfigurationRecorder setPrefix(String filePrefix) {
            this.prefix = filePrefix;
            return this;
        }

        /**
         * @param fileName alternative file identifier if date pattern is not defined
         */
        public ConfigurationRecorder setAlternativeId(String fileName) {
            this.fileName = fileName;
            return this;
        }

    }
}


