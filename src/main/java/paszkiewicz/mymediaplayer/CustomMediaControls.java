package paszkiewicz.mymediaplayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Custom overlay media controls for media player.
 */

public class CustomMediaControls extends RelativeLayout {
	final static int FADE_IN = 200;
	final static int FADE_OUT = 350;
	final static int TIMEOUT = 3000;
	final static int LOOP_TIMER = 100;

	final static float STICKY_PROGRESS_ALPHA = 0.7f;
	final static int STICKY_PROGRESS_MINIMUM_VIDEO_DURATION = 5000;

	//views
	FrameLayout controlsContainer;
	ImageView playPauseButton;
	ImageView audioButton;
	ImageView closeButton;
	ImageView menuButton;

	TextView currentTimeText;
	TextView totalTimeText;
	SeekBar seekBar;

	ProgressBar stickyProgressBar;

	/**
	 * True if progressbar should show on videos longer than
	 * {@link #STICKY_PROGRESS_MINIMUM_VIDEO_DURATION}
	 */
	private boolean isShowStickyProgressBar;
	/**
	 * True if progressbar was sucesfully validated against video duration
	 */
	private boolean isShowStickyProgressBarValid;

	private ExtendededMediaPlayerControls mediaPlayer;
	private MediaControlsCallback callback;

	private boolean isControlsShown = false;

	private int visibleTimeLeft = 0;
	private Handler handler;
	private Runnable loopRunnable = new Runnable() {
		@Override
		public void run() {
			controllerLoop();
		}
	};

	public CustomMediaControls(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflate(context, R.layout.mymediaplayer_controls, this);
		Util.ViewBinder b = new Util.ViewBinder(this);
		setVisibility(GONE);
		controlsContainer = b.bind(R.id.mymediaplayer_control_container);
		playPauseButton = b.bind(R.id.mymediaplayer_playpause, new OnClickListener() {
			@Override
			public void onClick(View view) {
				onPlayPausePressed();
			}
		});
		audioButton = b.bind(R.id.mymediaplayer_audio, new OnClickListener() {
			@Override
			public void onClick(View view) {
				onAudioButtonPressed();
			}
		});
		closeButton = b.bind(R.id.mymediaplayer_close, new OnClickListener() {
			@Override
			public void onClick(View view) {
				resetFadeOutTimer();
				callback.onMediaControlsClose();
			}
		});
		menuButton = b.bind(R.id.mymediaplayer_menu, new OnClickListener() {
			@Override
			public void onClick(View view) {
				resetFadeOutTimer();
				callback.onMediaControlsMenu();
			}
		});

		currentTimeText = b.bind(R.id.mymediaplayer_text_current);
		totalTimeText = b.bind(R.id.mymediaplayer_text_total);
		seekBar = b.bind(R.id.mymediaplayer_seekbar);
		stickyProgressBar = b.bind(R.id.mymediaplayer_sticky_progress);

		seekBar.setOnSeekBarChangeListener(new Util.SeekBarChangeListenerAdapter() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
				if (b) {
					mediaPlayer.seekTo(i);
					updateProgress();
				}
			}
		});
	}

	/**
	 * setup media player to observe, mediaplayer must already be prepared
	 *
	 * @param mediaPlayer mediaPlayer
	 * @param callback    callback for buttons
	 */
	public void setupWithMediaPlayer(ExtendededMediaPlayerControls mediaPlayer,
									 MediaControlsCallback
											 callback) {
		this.mediaPlayer = mediaPlayer;
		this.callback = callback;
		setVisibility(VISIBLE);

		totalTimeText.setText(formatMilisecondsToTime(mediaPlayer.getDuration()));
		int duration = mediaPlayer.getDuration();
		seekBar.setMax(duration);
		stickyProgressBar.setMax(duration);

		isShowStickyProgressBarValid = isShowStickyProgressBar && duration >
				STICKY_PROGRESS_MINIMUM_VIDEO_DURATION;

		invalidatePlayPauseButton();
		invalidateAudioButton();
		// prevent animation on first fadeIn()
		playPauseButton.jumpDrawablesToCurrentState();
		audioButton.jumpDrawablesToCurrentState();

		if (!mediaPlayer.isPlaying())
			fadeIn();
		else if (isShowStickyProgressBarValid) {
			stickyProgressBar.setVisibility(VISIBLE);
			stickyProgressBar.setAlpha(STICKY_PROGRESS_ALPHA);
		}
		updateProgress();
	}

	/**
	 * Show or hide the view, when shown stays visible until {@link #TIMEOUT}, then auto-hides
	 */
	public void toggleVisibility() {
		if (!isControlsShown) {
			fadeIn();
		} else {
			fadeOut();
		}
	}

	/**
	 * When true we get extra progressbar while controls are hidden.
	 *
	 * @param showStickyProgressBar set to true to show it, false to hide
	 */
	public void setShowStickyProgressBar(boolean showStickyProgressBar) {
		isShowStickyProgressBar = showStickyProgressBar;
		if (!showStickyProgressBar)
			return;

		if (mediaPlayer != null)
			isShowStickyProgressBarValid = mediaPlayer.getDuration() >
					STICKY_PROGRESS_MINIMUM_VIDEO_DURATION;

		if (visibleTimeLeft <= 0 && isShowStickyProgressBarValid) {
			stickyProgressBar.setAlpha(STICKY_PROGRESS_ALPHA);
			stickyProgressBar.setVisibility(VISIBLE);
		} else {
			stickyProgressBar.setVisibility(GONE);
		}
	}

	/**
	 * Fade in the controls and fade out sticky progress.
	 */
	private void fadeIn() {
		visibleTimeLeft = TIMEOUT + FADE_IN;
		isControlsShown = true;
		controlsContainer.setVisibility(VISIBLE);
		controlsContainer.setAlpha(0);
		controlsContainer.animate().setListener(null).alpha(1).setDuration(FADE_IN);
		if (isShowStickyProgressBarValid) {
			stickyProgressBar.animate().alpha(0f).setDuration(FADE_IN)
					.setListener(new
										 AnimatorListenerAdapter() {
											 @Override
											 public void onAnimationEnd(Animator animation) {
												 stickyProgressBar.setVisibility(GONE);
											 }
										 });
		}
	}

	/**
	 * Fade out controls and fade in sticky progress.
	 */
	private void fadeOut() {
		visibleTimeLeft = 0;
		isControlsShown = false;
		controlsContainer.animate().alpha(0).setDuration(FADE_OUT)
				.setListener(new
									 AnimatorListenerAdapter() {
										 @Override
										 public void
										 onAnimationEnd(Animator animation) {
											 controlsContainer.setVisibility(GONE);
										 }
									 });
		if (isShowStickyProgressBarValid) {
			stickyProgressBar.setVisibility(VISIBLE);
			stickyProgressBar.setAlpha(0);
			stickyProgressBar.animate().setListener(null).alpha(STICKY_PROGRESS_ALPHA)
					.setDuration(FADE_OUT);
		}
	}

	private void resetFadeOutTimer() {
		visibleTimeLeft = TIMEOUT;
	}

	/**
	 * Loop performed by handler, reduces fadeOutTimer and updates the progress.
	 */
	private void controllerLoop() {
		handler.postDelayed(loopRunnable, LOOP_TIMER);
		updateProgress();

		//if any button is pressed we delay hiding the view...
		if (seekBar.isPressed() || playPauseButton.isPressed() || audioButton.isPressed() ||
				closeButton.isPressed() || menuButton.isPressed())
			resetFadeOutTimer();

		//only code needed to hide views below
		if (visibleTimeLeft <= 0)
			return;

		visibleTimeLeft -= LOOP_TIMER;

		if (visibleTimeLeft <= 0) {
			visibleTimeLeft = 0;
			if (mediaPlayer != null && mediaPlayer.isPlaying() && isControlsShown)
				fadeOut();
		}
	}

	/**
	 * Update any elements that depend on current mediaplayer progress.
	 */
	private void updateProgress() {
		//update progress text
		if (mediaPlayer != null) {
			int pos = mediaPlayer.getCurrentPosition();
			currentTimeText.setText(formatMilisecondsToTime(pos));
			if (!seekBar.isPressed())
				seekBar.setProgress(pos);
			if (isShowStickyProgressBarValid)
				stickyProgressBar.setProgress(pos);
		}
	}

	private void onPlayPausePressed() {
		if (mediaPlayer == null)
			return;

		if (mediaPlayer.isPlaying()) {
			mediaPlayer.pause();
		} else {
			mediaPlayer.start();
		}
		invalidatePlayPauseButton();
		resetFadeOutTimer();
	}

	private void invalidatePlayPauseButton() {
		if (mediaPlayer == null)
			return;
		playPauseButton.setSelected(mediaPlayer.isPlaying());
	}

	private void onAudioButtonPressed() {
		if (mediaPlayer == null)
			return;
		if (!mediaPlayer.hasAudio())
			throw new IllegalStateException("CustomMediaControls - Video has no audio, audio " +
					"button " +
					"should not be clickable");

		mediaPlayer.toggleMuted();
		invalidateAudioButton();
		resetFadeOutTimer();
	}

	private void invalidateAudioButton() {
		audioButton.setClickable(mediaPlayer.hasAudio());
		if (!mediaPlayer.hasAudio()) {
			audioButton.setVisibility(GONE);
			return;
		}
		audioButton.setVisibility(VISIBLE);
		audioButton.setSelected(mediaPlayer.isMuted());
	}

	@Override
	public void setLayoutParams(ViewGroup.LayoutParams params) {
		params.height = calculateProperHeight(params.height);
		super.setLayoutParams(params);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		handler.removeCallbacks(loopRunnable);
		handler = null;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (handler == null)
			handler = new Handler();
		controllerLoop();
	}

	/**
	 * Release and clean up any used resources.<br> Controls can still be reactivated after calling
	 * {@link #setupWithMediaPlayer(ExtendededMediaPlayerControls, MediaControlsCallback)}
	 */
	public void release() {
		controlsContainer.setVisibility(GONE);
		if (isShowStickyProgressBar) {
			stickyProgressBar.setProgress(0);
			stickyProgressBar.setAlpha(STICKY_PROGRESS_ALPHA);
			stickyProgressBar.setVisibility(VISIBLE);
		}
		isShowStickyProgressBarValid = false;
		mediaPlayer = null;
		callback = null;
		setVisibility(GONE);
	}

	/**
	 * Calculate height to use for media controls.
	 *
	 * @param height height to test
	 * @return height if its large enough, otherwise minimum
	 */
	public int calculateProperHeight(int height) {
		int minHeight = getResources().getDimensionPixelSize(R.dimen.mediacontrol_minimum_height);
		return Math.max(minHeight, height);
	}

	/**
	 * Tint progress and seekbar
	 *
	 * @param color color to be used for tint, if 0 then no tinting occurs
	 */
	public void setProgressBarsColor(int color) {
		if (color == 0)
			return;

		seekBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		seekBar.getThumb().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
		stickyProgressBar.getProgressDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
	}

	private String formatMilisecondsToTime(int milis) {
		long minutes = TimeUnit.MILLISECONDS.toMinutes(milis);
		milis -= TimeUnit.MINUTES.toMillis(minutes);
		long seconds = TimeUnit.MILLISECONDS.toSeconds(milis);
		return String.format(Locale.US, "%02d:%02d", minutes, seconds);
	}

	public interface MediaControlsCallback {
		void onMediaControlsClose();

		void onMediaControlsMenu();
	}

	public interface ExtendededMediaPlayerControls extends MediaController.MediaPlayerControl {
		void toggleMuted();

		boolean hasAudio();

		boolean isMuted();
	}
}