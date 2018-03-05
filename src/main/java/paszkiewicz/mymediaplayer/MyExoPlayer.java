package paszkiewicz.mymediaplayer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.TextureView;

import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.FileDataSourceFactory;

import static paszkiewicz.mymediaplayer.MyMediaPlayer.*;

/** Doesn't work well. */
public class MyExoPlayer implements CustomMediaControls.ExtendededMediaPlayerControls {
    final SimpleExoPlayer player;
    private boolean hasAudio = false;

    public MyExoPlayer(Context context){
        player = ExoPlayerFactory.newSimpleInstance(context, new DefaultTrackSelector());
    }

    void setAudioStreamType(int type){
        AudioAttributes aa = new AudioAttributes.Builder()
                .setContentType(type)
                .build();
        player.setAudioAttributes(aa);
    }

    void setLooping(boolean looping){
        if(looping)
            player.setRepeatMode(Player.REPEAT_MODE_ONE);
        else
            player.setRepeatMode(Player.REPEAT_MODE_OFF);
    }

    void setDataSource(Uri filePath){
        MediaSource m = new ExtractorMediaSource.Factory(new FileDataSourceFactory())
                .createMediaSource(filePath);
        player.prepare(m);
    }

    int getVideoWidth(){
        Format vf = player.getVideoFormat();
        return vf.width;
    }

    int getVideoHeight(){
        Format vf = player.getVideoFormat();
        return vf.height;
    }

    /**
     * Start media player with restored players state. Call this in onPrepared().
     *
     * @param b bundle retrieved from {@link MyMediaPlayer#getSavedState(Bundle)}
     * @return true if video started playing, false if not
     */
    public boolean startWithRestoreState(@Nullable Bundle b, boolean pauseOnAudio) {
        checkHasAudio();

        if (b != null) {
            seekTo(b.getInt(SAVE_POSITION, 0));
            setVolume(b.getFloat(SAVE_VOLUME, 1f));
            if (!b.getBoolean(SAVE_PAUSEDSTATE, false))
                start();
            else
                return false;
        } else {
            //no restore state - setup
            setVolume(1f);
            seekTo(0);

            if (!hasAudio)
                start();
            else if (!pauseOnAudio)
                start();
            else
                return false;
        }
        return true;
    }

    private void checkHasAudio() {
        Format af = player.getVideoFormat();
        hasAudio = (af.channelCount == Format.NO_VALUE);
//        MediaPlayer.TrackInfo[] trackInfos = getTrackInfo();
//        for (MediaPlayer.TrackInfo trackInfo : trackInfos) {
//            if (trackInfo.getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
//                hasAudio = true;
//                break;
//            }
//        }
    }

    /**
     * Save players state in given bundle
     *
     * @param outState bundle to inflate
     * @return outState with player state
     */
    public Bundle saveState(Bundle outState) {
        Bundle b = new Bundle();
        b.putInt(SAVE_POSITION, getCurrentPosition());
        b.putBoolean(SAVE_PAUSEDSTATE, !isPlaying());
        b.putFloat(SAVE_VOLUME, getVolume());
        outState.putBundle(SAVE_PLAYERSTATE, b);
        return outState;
    }

    public float getVolume() {
        return player.getVolume();
    }

    @Override
    public void toggleMuted() {
        setVolume((player.getVolume() + 1f) % 2);
    }

    @Override
    public boolean hasAudio() {
        return false;
    }

    @Override
    public boolean isMuted() {
        return false;
    }

    public void setVolume(float volume) {
        player.setVolume(volume);
    }

    @Nullable
    public Player.VideoComponent getVideoComponent() {
        return player.getVideoComponent();
    }

    @Nullable
    public Player.TextComponent getTextComponent() {
        return player.getTextComponent();
    }

    public void addListener(Player.EventListener listener) {
        player.addListener(listener);
    }

    public void removeListener(Player.EventListener listener) {
        player.removeListener(listener);
    }

    public int getPlaybackState() {
        return player.getPlaybackState();
    }

    public void setPlayWhenReady(boolean playWhenReady) {
        player.setPlayWhenReady(playWhenReady);
    }

    public boolean getPlayWhenReady() {
        return player.getPlayWhenReady();
    }

    public void setRepeatMode(int repeatMode) {
        player.setRepeatMode(repeatMode);
    }

    public int getRepeatMode() {
        return player.getRepeatMode();
    }

    public void setShuffleModeEnabled(boolean shuffleModeEnabled) {
        player.setShuffleModeEnabled(shuffleModeEnabled);
    }

    public boolean getShuffleModeEnabled() {
        return player.getShuffleModeEnabled();
    }

    public boolean isLoading() {
        return player.isLoading();
    }

    public void seekToDefaultPosition() {
        player.seekToDefaultPosition();
    }

    public void seekToDefaultPosition(int windowIndex) {
        player.seekToDefaultPosition(windowIndex);
    }

    public void seekTo(long positionMs) {
        player.seekTo(positionMs);
    }

    public void seekTo(int windowIndex, long positionMs) {
        player.seekTo(windowIndex, positionMs);
    }

    public void setPlaybackParameters(@Nullable PlaybackParameters playbackParameters) {
        player.setPlaybackParameters(playbackParameters);
    }

    public PlaybackParameters getPlaybackParameters() {
        return player.getPlaybackParameters();
    }

    public void stop() {
        player.stop();
    }

    public void stop(boolean reset) {
        player.stop(reset);
    }

    public void release() {
        player.release();
    }

    public int getRendererCount() {
        return player.getRendererCount();
    }

    public int getRendererType(int index) {
        return player.getRendererType(index);
    }

    public TrackGroupArray getCurrentTrackGroups() {
        return player.getCurrentTrackGroups();
    }

    public TrackSelectionArray getCurrentTrackSelections() {
        return player.getCurrentTrackSelections();
    }

    @Nullable
    public Object getCurrentManifest() {
        return player.getCurrentManifest();
    }

    public Timeline getCurrentTimeline() {
        return player.getCurrentTimeline();
    }

    public int getCurrentPeriodIndex() {
        return player.getCurrentPeriodIndex();
    }

    public int getCurrentWindowIndex() {
        return player.getCurrentWindowIndex();
    }

    public int getNextWindowIndex() {
        return player.getNextWindowIndex();
    }

    public int getPreviousWindowIndex() {
        return player.getPreviousWindowIndex();
    }

    @Override
    public void start() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public int getDuration() {
        return (int) player.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) player.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {
        player.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return player.getPlayWhenReady();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public long getBufferedPosition() {
        return player.getBufferedPosition();
    }

    public int getBufferedPercentage() {
        return player.getBufferedPercentage();
    }

    public boolean isCurrentWindowDynamic() {
        return player.isCurrentWindowDynamic();
    }

    public boolean isCurrentWindowSeekable() {
        return player.isCurrentWindowSeekable();
    }

    public boolean isPlayingAd() {
        return player.isPlayingAd();
    }

    public int getCurrentAdGroupIndex() {
        return player.getCurrentAdGroupIndex();
    }

    public int getCurrentAdIndexInAdGroup() {
        return player.getCurrentAdIndexInAdGroup();
    }

    public long getContentPosition() {
        return player.getContentPosition();
    }
}
