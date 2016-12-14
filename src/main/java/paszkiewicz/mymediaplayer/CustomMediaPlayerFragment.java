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

public class CustomMediaPlayerFragment extends Fragment implements MediaPlayer
		.OnPreparedListener, TextureView.SurfaceTextureListener, MyMediaPlayer
		.OnFirstUnpauseListener {
	public final static String TAG = "paszkiewicz.mymediaplayer.CustomMediaPlayerFragment:TAG";
	public final static String ARG_PAUSE_ON_AUDIO = "paszkiewicz.mymediaplayer" +
			".CustomMediaPlayerFragment:ARG_PAUSE_ON_AUDIO";
	public final static String ARG_STICKY_PROGRESS = "paszkiewicz.mymediaplayer" +
			".CustomMediaPlayerFragment:ARG_STICKY_PROGRESS";
	public final static String ARG_CONTROLS_TINT = "paszkiewicz.mymediaplayer" +
			".CustomMediaPlayerFragment:ARG_PROGRESS_COLOR";

	private TextureView textureView;
	private ImageView videoThumbnail;
	private CustomMediaControls mediaControls;

	private Callback callback;
	private File playedFile;
	private String playedUrl;
	private boolean isSurfaceCreated;
	private boolean isVideoStarted;
	private boolean isVideoPausedOnPause;

	private MyMediaPlayer mediaPlayer;
	private Bundle savedMediaPlayerState;

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
		CustomMediaPlayerFragment fragment = new CustomMediaPlayerFragment();
		fragment.setArguments(args);
		return fragment;
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
			savedInstanceState) {
		View root = inflater.inflate(R.layout.mymediaplayer_mediaplayer, container, false);
		isSurfaceCreated = false;
		textureView = (TextureView) root.findViewById(R.id.mymediaplayer_surface);
		videoThumbnail = (ImageView) root.findViewById(R.id.mymediaplayer_video_thumbnail);
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

		savedMediaPlayerState = MyMediaPlayer.getSavedState(savedInstanceState);

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

	/**
	 * Detach the fragment after this call since TextureView will be unusable.<br>To play another
	 * video you need to attach it again.
	 */
	public void release() {
		mediaControls.release();
		releaseMediaPlayer();
		isVideoStarted = false;
		isSurfaceCreated = false;
		playedFile = null;
		playedUrl = null;
		videoThumbnail.setImageBitmap(null);

		textureView.setVisibility(View.GONE);
	}

	private void releaseMediaPlayer() {
		if (mediaPlayer != null) {
			if (mediaPlayer.isPlaying())
				mediaPlayer.stop();
			mediaPlayer.reset();
			mediaPlayer.release();
			mediaPlayer = null;
		}
		savedMediaPlayerState = null;
	}

	private void doStartVideo() {
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

	private void adjustVerticalFit(int width, int height) {
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
		videoThumbnail.setLayoutParams(layoutParams);
		mediaControls.setLayoutParams(layoutParams);
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
		callback.onMediaPlayerReady(mediaPlayer);
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

	public MediaPlayer getMediaPlayer() {
		return mediaPlayer;
	}

	public CustomMediaControls getMediaControls() {
		return mediaControls;
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

	@Override
	public void onFirstVideoUnpause(int milliseconds) {
		videoThumbnail.setImageBitmap(null);
	}

	public interface Callback {
		void onMediaPlayerError(String message);

		void onMediaPlayerClicked();

		void onMediaPlayerReady(MediaPlayer player);
	}
}
