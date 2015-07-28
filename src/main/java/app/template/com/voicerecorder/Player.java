package app.template.com.voicerecorder;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Annotation;

/**
 * Created by Mateusz M. on 2015-04-28.
 */

/**
 * Class this is singleton which play file from storage or url.
 * Player have stat preparing,prepared, playing.
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
     * @return return singleton instance of Player
     * */
    public static Player getInstance() {
        if (instance == null) {
            instance = new Player();
        }

        return instance;
    }

    /**
     * Start preparing plauer for play. After call this method player is preparing.
     * If player is prepared then start playing.
     * If player is prepared and method is called again with this same parameter then player starts playing immediately form beginning.
     *
     * @return Flag that indicate the Player need to preparing.
     */
    public boolean play(String path) {
        if (this.sourceUrl.equals(path)) {
            if (mediaPlayer == null) {
                setDataSource(path);
            } else if (isPrepared) {
                if (isPlaying()) {
                    mediaPlayer.seekTo(0);
                } else {
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

    /**
     * Stop playing if playing is started before and not finished.
     *
     * @return flag that indicate the process is dane.
     */
    public boolean stop() {
        if (isPrepared && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Set listener that is called when player is ready to play.
     */
    public void setListener(MediaPlayer.OnPreparedListener listener) {
        this.listener = listener;
    }

    /**
     * Release record object. Method this should be called when object is not need more.
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
     * Set listener which is called when the end of a media source has been reached during playback.
     */
    public void setEndListener(MediaPlayer.OnCompletionListener onCompletionListener) {
        endListener = onCompletionListener;
        mediaPlayer.setOnCompletionListener(onCompletionListener);
    }

    /**
     * Stop playing media source. After call play the player will play data source immediately from the beggining
     * */
    public void pause() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.pause();
        } else {
            Log.e(Player.class.getSimpleName(), "Couldn't pause");
        }
    }

    /**
     * Return flag state which indicate that recorder is now playing data source.
     * @return  boolean flag indicate recorder state is now playing
     * */
    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }

    /**
     * Return flag state which indicate that recorder is now prepared for start playing data source.
     * @return  boolean flag indicate recorder state is prepared for playing
     * */
    public boolean isPrepared(String sourceUrl) {
        return isPrepared && this.sourceUrl.equals(sourceUrl);
    }

    /**
     * Seeks to 0 time position.
     * */
    public void seekToStart() {
        if (mediaPlayer != null && isPrepared) {
            mediaPlayer.seekTo(0);
        } else {
            Log.e(Player.class.getSimpleName(), "Couldn't seek to start");
        }
    }
}
