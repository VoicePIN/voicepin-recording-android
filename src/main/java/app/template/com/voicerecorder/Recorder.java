package app.template.com.voicerecorder;

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
 * Class that recording and saving data in file.
 * File parameters:
 * extension .wav
 * Records sample rate 8000:
 * Records channel: 1
 * Records encoding: PCM 16 bit
 */
public class Recorder {
    private static final int RECORDER_BPP = 16;
    private static final String FILE_NAME_EXTENSION = ".wav";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLE_RATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord recorder = null;

    private Thread recordingThread = null;

    private int bufferSize = 0;
    private boolean isRecording = false;
    private String lastRecordFilePath = "";

    private String filename;
    private double amplitude;
    private String date_pattern_format;
    private String recordsFolder;
    private String filePrefix;
    private String configFileName;

    public Recorder() {
        init(new ConfigurationRecorder().getConfiguration());
    }

    public Recorder(ConfigurationRecorder configurationRecorder) {
        init(configurationRecorder);
    }

    private void init(ConfigurationRecorder configurationRecorder) {
        this.recordsFolder = configurationRecorder.getRecordsFolder();
        this.date_pattern_format = configurationRecorder.getFileDatePattern();
        this.filePrefix = configurationRecorder.getFilePrefix();
        this.configFileName = configurationRecorder.fileName;

        bufferSize = AudioRecord.getMinBufferSize(
                RECORDER_SAMPLE_RATE
                , RECORDER_CHANNELS
                , RECORDER_AUDIO_ENCODING);

        initRecorder();
    }

    private void initRecorder() {
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION
                , RECORDER_SAMPLE_RATE
                , RECORDER_CHANNELS
                , RECORDER_AUDIO_ENCODING
                , bufferSize);
    }

    /**
     * Start recording only if recorder is not recording at the moment.
     */
    public void startRecording() {
        if (!isRecording) {

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
     * Stop recording and saving data to file if Recorder is recording.
     *
     * @return Return full record file path that is finished recording. If records is not finished recording or startRecording never called then returning empty string.
     */
    public String stopRecording() {
        if (isRecording) {
            isRecording = false;
            String filePath = getFileToOverwrite().getAbsolutePath();
            lastRecordFilePath = filePath;
            if (null != recorder) {
                recorder.stop();
                recorder.release();

                initRecorder();
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
     * Returns full record file path that
     *
     * @return Return last full record file path which was recorded.
     */
    public String getLastRecordFilePath() {
        return lastRecordFilePath;
    }

    /**
     * Returns state of recorder. If recorder is actal recording return true.
     *
     * @return boolean flag. If recorder actual recording then returns true else return false
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Return amplitude that represents intensity voice. Range value <0,30>
     *
     * @return returns double value of intensity voice.
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

        filename = getTempFilename();
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
     * ConfigurationRecorder class use for congure records params like file prefix name,
     * records file folders where records are saving, file name date format.
     */
    public static class ConfigurationRecorder {
        private String recordsFolder;
        private String fileDatePattern;
        private String filePrefix;
        private String fileName;

        /**
         * Set records file folder that will contain all records.
         *
         * @return object builder
         */
        public ConfigurationRecorder setRecordsFolder(String recordsFolder) {
            this.recordsFolder = recordsFolder;
            return this;
        }

        /**
         * Set name date pattern. This pattern is middle part of file name.
         *
         * @param fileDatePattern Date pattern format same in {@link java.text.DateFormat}
         * @return {@link Recorder.ConfigurationRecorder}
         */
        public ConfigurationRecorder setFileDatePattern(String fileDatePattern) {
            this.fileDatePattern = fileDatePattern;
            return this;
        }

        /**
         * Setup file name prefix.
         *
         * @return object builder
         */
        public ConfigurationRecorder setFilePrefix(String filePrefix) {
            this.filePrefix = filePrefix;
            return this;
        }

        /**
         * Returns configuration for {@link Recorder} object.
         *
         * @return {@link Recorder.ConfigurationRecorder}
         */
        public ConfigurationRecorder getConfiguration() {
            return this;
        }


        /**
         * Return records file folder that will contain all records.
         *
         * @return Return {@link String} that is path contain records
         */
        public String getRecordsFolder() {
            return TextUtils.isEmpty(recordsFolder) ? "" : recordsFolder;
        }

        /**
         * Return date pattern format same in {@link java.text.DateFormat}
         * which will be used for build record name.
         *
         * @return Return {@link String} that is middle part of record name.
         */
        public String getFileDatePattern() {
            return TextUtils.isEmpty(fileDatePattern) ? "" : fileDatePattern;
        }

        /**
         * Returns prefix String that will be used in building records name.
         *
         * @return Return {@link String} that is first part of record name.
         */
        public String getFilePrefix() {
            return TextUtils.isEmpty(filePrefix) ? "" : filePrefix;
        }

        public ConfigurationRecorder setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }
    }
}


