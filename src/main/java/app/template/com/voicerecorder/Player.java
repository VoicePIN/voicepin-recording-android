package app.template.com.voicerecorder;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Created by Mateusz M. on 2015-04-28.
 */
public class Player {
    private static Player instance;
    private MediaPlayer mediaPlayer;
    MediaPlayer.OnPreparedListener listener;

    private boolean isPrepared;
    private String sourceUrl = "";
    private MediaPlayer.OnCompletionListener endListener;

    private Player() {
    }

    public static Player getInstance() {

        if (instance == null) {
            instance = new Player();
        }
        return instance;
    }

    private void setDataSource(String url) {
        this.sourceUrl = url;
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(url);

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    isPrepared = true;
                    mediaPlayer.start();

                    if (listener != null) {
                        listener.onPrepared(mediaPlayer);
                    }

                }
            });

            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
            isPrepared = false;
        }
    }

    public boolean play(String path) {
        if (this.sourceUrl.equals(path)) {
            if (mediaPlayer == null) {
                setDataSource(path);
            } else if (isPrepared) {
                if (isPlaying()) {
                    mediaPlayer.seekTo(0);
                }else {
                    mediaPlayer.start();
                }
                return true;
            } else {
                if (endListener != null) {
                    endListener.onCompletion(mediaPlayer);
                }
                setDataSource(path);
            }
        } else {
            if (endListener != null) {
                endListener.onCompletion(mediaPlayer);
            }
            release();
            setDataSource(path);
        }
        return false;
    }

    public boolean stop() {
        release();
        return true;
    }

    public void setListener(MediaPlayer.OnPreparedListener listener) {
        this.listener = listener;
    }

    public void release() {
        isPrepared = false;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public void setEndListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        endListener = onCompletionListener;
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    public void pause() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.pause();
        } else {
            Log.e(Player.class.getSimpleName(), "Couldn't pause");
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    public boolean isPrepared(String sourceUrl) {
        return isPrepared && this.sourceUrl.equals(sourceUrl);
    }

    public void seekToStart() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(0);
        }else {
            Log.e(Player.class.getSimpleName(), "Couldn't seek to start");
        }
    }
}
