package paszkiewicz.mymediaplayer;


import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

/**
 * Media player fragment.<br>Use {@link #playFile(File)} to automatically fit size and start playing
 * videos.
 */

public abstract class CustomMediaPlayerFragment extends Fragment implements TextureView.SurfaceTextureListener {
	public final static String TAG = "paszkiewicz.mymediaplayer.CustomMediaPlayerFragment:TAG";
	public final static String ARG_PAUSE_ON_AUDIO = "paszkiewicz.mymediaplayer" +
			".CustomMediaPlayerFragment:ARG_PAUSE_ON_AUDIO";
	public final static String ARG_STICKY_PROGRESS = "paszkiewicz.mymediaplayer" +
			".CustomMediaPlayerFragment:ARG_STICKY_PROGRESS";
	public final static String ARG_CONTROLS_TINT = "paszkiewicz.mymediaplayer" +
			".CustomMediaPlayerFragment:ARG_PROGRESS_COLOR";

	TextureView textureView;
	CustomMediaControls mediaControls;

	Callback callback;
	File playedFile;
	String playedUrl;
	boolean isSurfaceCreated;
	boolean isVideoStarted;
	boolean isVideoPausedOnPause;

	Bundle savedMediaPlayerState;

	public CustomMediaPlayerFragment() {
	}

	public static CustomMediaPlayerFragment newInstance(boolean pauseOnAudio) {
		return createInstance(pauseOnAudio, false, 0);
	}

	public static CustomMediaPlayerFragment newInstance(boolean pauseOnAudio, boolean
			showStickyProgressBar, int controlsTint) {
		return createInstance(pauseOnAudio, showStickyProgressBar, controlsTint);
	}

	private static CustomMediaPlayerFragment createInstance(boolean pauseOnAudio, boolean
			showStickyProgressBar, int controlsTint) {
		Bundle args = new Bundle();
		args.putBoolean(ARG_PAUSE_ON_AUDIO, pauseOnAudio);
		args.putBoolean(ARG_STICKY_PROGRESS, showStickyProgressBar);
		args.putInt(ARG_CONTROLS_TINT, controlsTint);
		CustomMediaPlayerFragment fragment = new MediaPlayerFragment();	//todo: fix and use exo
		fragment.setArguments(args);
		return fragment;
	}

	Bundle getSavedMediaPlayerState(@Nullable Bundle savedInstanceState) {
		return MyMediaPlayer.getSavedState(savedInstanceState);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		View root = inflater.inflate(R.layout.mymediaplayer_mediaplayer, container, false);
		isSurfaceCreated = false;
		textureView = (TextureView) root.findViewById(R.id.mymediaplayer_surface);
		mediaControls = (CustomMediaControls) root.findViewById(R.id.mymediaplayer_controls);

		mediaControls.setShowStickyProgressBar(getArguments().getBoolean(ARG_STICKY_PROGRESS));
		mediaControls.setProgressBarsColor(getArguments().getInt(ARG_CONTROLS_TINT));
		root.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				callback.onMediaPlayerClicked();
				mediaControls.toggleVisibility();
			}
		});

		savedMediaPlayerState = getSavedMediaPlayerState(savedInstanceState);

		textureView.setVisibility(View.GONE);
		textureView.setSurfaceTextureListener(this);

		if (isVideoStarted) {
			textureView.setVisibility(View.VISIBLE);
		}
		return root;
	}

	public void playFile(File videoFile) {
		this.playedFile = videoFile;
		startPlayingVideo();
	}

	public void streamVideo(String url) {
		this.playedUrl = url;
		startPlayingVideo();
	}

	private void startPlayingVideo() {
		isVideoStarted = true;
		if (!isSurfaceCreated) {
			if (textureView == null) //view is not inflated yet...
				return;
			textureView.setVisibility(View.VISIBLE);
		} else
			doStartVideo();
	}

	abstract void doStartVideo();

	/**
	 * Detach the fragment after this call since TextureView will be unusable.<br>To play another
	 * video you need to attach it again.
	 */
	public void release() {
		mediaControls.release();
		isVideoStarted = false;
		isSurfaceCreated = false;
		playedFile = null;
		playedUrl = null;

		textureView.setVisibility(View.GONE);
		savedMediaPlayerState = null;
	}

	void adjustVerticalFit(int width, int height) {
		View container = (View) getView().getParent();
		float containerHeight = container.getMeasuredHeight();
		float containerWidth = container.getMeasuredWidth();

		float screenRatio = containerWidth / containerHeight;
		float videoRatio = (float) width / height;

		FrameLayout.LayoutParams layoutParams;
		if (videoRatio < screenRatio) {
			int desiredWidth = (int) (containerHeight * videoRatio);
			layoutParams = new FrameLayout.LayoutParams(
					desiredWidth,
					(int) containerHeight);
		} else {
			int desiredHeight = (int) (containerWidth / videoRatio);
			layoutParams = new FrameLayout.LayoutParams(
					(int) containerWidth,
					desiredHeight);
		}
		textureView.setLayoutParams(layoutParams);
		mediaControls.setLayoutParams(layoutParams);
	}

	@Override
	public void onDestroy() {
		release();
		super.onDestroy();
	}

	@Override
	public void onAttach(Activity activity) {
		callback = (Callback) activity;
		super.onAttach(activity);
	}

	@Override
	public void onAttach(Context context) {
		callback = (Callback) getActivity();
		super.onAttach(context);
	}

	public File getPlayedFile() {
		return playedFile;
	}

	public CustomMediaControls getMediaControls() {
		return mediaControls;
	}



	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
		isSurfaceCreated = false;
		return false;
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

	}

	public interface Callback {
		void onMediaPlayerError(String message);

		void onMediaPlayerClicked();

		void onMediaPlayerReady();
	}
}
