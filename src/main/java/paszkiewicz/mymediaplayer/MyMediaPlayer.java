package paszkiewicz.mymediaplayer;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * MediaPlayer implementing extended interface for custom controls and methods for saving the
 * state.
 */

public class MyMediaPlayer extends MediaPlayer implements CustomMediaControls
		.ExtendededMediaPlayerControls {
	public final static String SAVE_PLAYERSTATE = "paszkiewicz.mymediaplayer.MyMediaPlayer:state";
	public final static String SAVE_POSITION = "SAVE_POSITION";
	public final static String SAVE_PAUSEDSTATE = "SAVE_PAUSEDSTATE";
	public final static String SAVE_VOLUME = "SAVE_VOLUME";

	private float volume;
	private boolean hasAudio = false;

	/**
	 * Get saved players state
	 *
	 * @param savedInstanceState bundle saved in fragment or activity's lifecycle
	 * @return saved state's bundle
	 */
	public static Bundle getSavedState(@Nullable Bundle savedInstanceState) {
		if (savedInstanceState == null)
			return null;
		return savedInstanceState.getBundle(SAVE_PLAYERSTATE);
	}

	@Override
	public void toggleMuted() {
		setVolume((volume + 1f) % 2);
	}

	public float getVolume() {
		return volume;
	}

	public void setVolume(float volume) {
		this.volume = volume;
		setVolume(volume, volume);
	}

	@Override
	public boolean isMuted() {
		return volume < 1f;
	}

	/**
	 * @return true if played file has at least 1 audio track
	 */
	@Override
	public boolean hasAudio() {
		return hasAudio;
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

	/**
	 * Start media player with restored players state. Call this in onPrepared().
	 *
	 * @param b bundle retrieved from {@link #getSavedState(Bundle)}
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
		TrackInfo[] trackInfos = getTrackInfo();
		for (TrackInfo trackInfo : trackInfos) {
			if (trackInfo.getTrackType() == TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
				hasAudio = true;
				break;
			}
		}
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
}
