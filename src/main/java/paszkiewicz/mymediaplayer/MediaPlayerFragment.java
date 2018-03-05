package paszkiewicz.mymediaplayer;

import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Surface;

import java.io.IOException;

public class MediaPlayerFragment extends CustomMediaPlayerFragment implements MediaPlayer
        .OnPreparedListener{
    private MyMediaPlayer mediaPlayer;

    @Override
    public void release() {
        super.release();
        releaseMediaPlayer();
    }


    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                mediaPlayer.stop();
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    void doStartVideo() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            return;
        }

        mediaPlayer = new MyMediaPlayer();
        if (isSurfaceCreated)
            mediaPlayer.setSurface(new Surface(textureView.getSurfaceTexture()));
        try {
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            if (playedFile != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.setDataSource(playedFile.getAbsolutePath());
            } else
                mediaPlayer.setDataSource(playedUrl);
            mediaPlayer.setOnPreparedListener(this);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            callback.onMediaPlayerError("Error loading video file");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mediaPlayer != null) {
            mediaPlayer.saveState(outState);
            isVideoPausedOnPause = !mediaPlayer.isPlaying();
            mediaPlayer.pause();
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        if (mediaPlayer != null) {
            if (!isVideoPausedOnPause)
                mediaPlayer.start();
        }
        super.onResume();
    }


    @Override
    public void onPrepared(MediaPlayer mp) {
        adjustVerticalFit(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
        textureView.setBackground(null);
        mediaPlayer.setScreenOnWhilePlaying(true);
        if (!mediaPlayer.startWithRestoreState(savedMediaPlayerState, getArguments().getBoolean
                (ARG_PAUSE_ON_AUDIO))) {
            //stub for loading thumbnail
        }
        mediaControls.setupWithMediaPlayer(this.mediaPlayer, (CustomMediaControls
                .MediaControlsCallback) getActivity());
        callback.onMediaPlayerReady();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        isSurfaceCreated = true;
        if (isVideoStarted)
            if (mediaPlayer != null) {
                adjustVerticalFit(mediaPlayer.getVideoWidth(), mediaPlayer.getVideoHeight());
                mediaPlayer.setSurface(new Surface(surfaceTexture));
            } else
                doStartVideo();
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
}
