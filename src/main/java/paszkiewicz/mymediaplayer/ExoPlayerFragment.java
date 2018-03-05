package paszkiewicz.mymediaplayer;

import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.video.VideoListener;

import java.io.IOException;

public class ExoPlayerFragment extends CustomMediaPlayerFragment implements Player.EventListener {
    private MyExoPlayer exoPlayer;

    @Override
    void doStartVideo() {
        if(exoPlayer != null && exoPlayer.isPlaying())
            return;
        exoPlayer = new MyExoPlayer(getContext());
        if(isSurfaceCreated)
            exoPlayer.player.setVideoTextureView(textureView);

        exoPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        exoPlayer.setLooping(true);
        exoPlayer.setDataSource(Uri.parse(playedFile.getAbsolutePath()));
        exoPlayer.player.addListener(this);
    }


    @Override
    public void onResume() {
        if (exoPlayer != null) {
            if (!isVideoPausedOnPause)
                exoPlayer.start();
        }
        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (exoPlayer != null) {
            exoPlayer.saveState(outState);
            isVideoPausedOnPause = !exoPlayer.isPlaying();
            exoPlayer.pause();
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        isSurfaceCreated = true;
        if (isVideoStarted)
            if (exoPlayer != null) {
                adjustVerticalFit(exoPlayer.getVideoWidth(), exoPlayer.getVideoHeight());
                exoPlayer.player.setVideoSurface(new Surface(surfaceTexture));
            } else
                doStartVideo();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {
        if(!isLoading){
            adjustVerticalFit(exoPlayer.getVideoWidth(), exoPlayer.getVideoHeight());
            textureView.setBackground(null);
            //exoPlayer.player.setScreenOnWhilePlaying(true);
            if (!exoPlayer.startWithRestoreState(savedMediaPlayerState, getArguments().getBoolean
                    (ARG_PAUSE_ON_AUDIO))) {
                //stub for loading thumbnail
            }
            mediaControls.setupWithMediaPlayer(this.exoPlayer, (CustomMediaControls
                    .MediaControlsCallback) getActivity());
            callback.onMediaPlayerReady();
        }
    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {
        Log.e("ExoFragment", "onPlayerError: "+error.getMessage());
    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }
}
