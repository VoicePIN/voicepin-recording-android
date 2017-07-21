package com.voicepin.android.audio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Plays file from storage or URL.
 */
public class Player {
    private static Player instance;
    private MediaPlayer mediaPlayer;
    private MediaPlayer.OnPreparedListener listener;

    private boolean isPrepared;
    private String sourceUrl = "";
    private MediaPlayer.OnCompletionListener endListener;

    private Player() {
    }

    /**
     * @return singleton instance
     * */
    public static Player getInstance() {
        if (instance == null) {
            instance = new Player();
        }

        return instance;
    }

    /**
     * Starts playback.
     *
     * @param path the path of the file, or the http/rtsp URL of the stream you want to play
     */
    public void play(String path) {
        if (this.sourceUrl.equals(path)) {
            if (mediaPlayer == null) {
                prepareAndStart(path);
            } else if (isPrepared) {
                if (isPlaying()) {
                    mediaPlayer.seekTo(0);
                } else {
                    mediaPlayer.start();
                }
            } else {
                if (endListener != null) {
                    endListener.onCompletion(mediaPlayer);
                }
                prepareAndStart(path);
            }
        } else {
            if (endListener != null) {
                endListener.onCompletion(mediaPlayer);
            }
            release();
            prepareAndStart(path);
        }
    }

    private void prepareAndStart(String url) {
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

    /**
     * Stops playback.
     */
    public void stop() {
        if (isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }

    /**
     * Sets listener that is called when player is ready to play.
     */
    public void setListener(MediaPlayer.OnPreparedListener listener) {
        this.listener = listener;
    }

    /**
     * Release player. Should be called when player is not needed anymore.
     */
    public void release() {
        isPrepared = false;
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    /**
     * Set listener which is called when the end of the data source has been reached.
     */
    public void setEndListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        endListener = onCompletionListener;
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    /**
     * Pause playback.
     */
    public void pause() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.pause();
        } else {
            Log.e(Player.class.getSimpleName(), "Couldn't pause");
        }
    }

    /**
     * @return flag indicating that the data source is being played
     */
    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    /**
     * @return flag indicating that recorder is prepared for playing
     */
    public boolean isPrepared(String sourceUrl) {
        return isPrepared && this.sourceUrl.equals(sourceUrl);
    }

    /**
     * Seeks to 0 time position.
     */
    public void seekToStart() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(0);
        } else {
            Log.e(Player.class.getSimpleName(), "Couldn't seek to start");
        }
    }
}
