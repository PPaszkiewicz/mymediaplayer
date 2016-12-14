package paszkiewicz.mymediaplayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.widget.ImageView;

/**
 * Hold AnimatedVectorDrawables outside controls since devices below api 21 will crash while trying
 * to import them.
 */
@TargetApi(21)
class AnimatedVectorButtons {
	AnimatedVectorDrawable playPauseVector;
	AnimatedVectorDrawable pausePlayVector;
	AnimatedVectorDrawable volumeMuteVector;
	AnimatedVectorDrawable muteVolumeVector;

	AnimatedVectorButtons(Context c) {
		playPauseVector = (AnimatedVectorDrawable) c.getDrawable(R.drawable
				.ic_playpause_animated);
		pausePlayVector = (AnimatedVectorDrawable) c.getDrawable(R.drawable
				.ic_pauseplay_animated);
		muteVolumeVector = (AnimatedVectorDrawable) c.getDrawable(R.drawable
				.ic_mutevolume_animated);
		volumeMuteVector = (AnimatedVectorDrawable) c.getDrawable(R.drawable
				.ic_volumemute_animated);
	}

	/**
	 * Run animated vector drawable's transformation of AnimatedVectorDrawables. <br>If ImageView
	 * has no drawable, animation will be paused instead of ran.
	 *
	 * @param view        ImageView that will display the image
	 * @param trueVector  played if <code>condition</code> is true
	 * @param falseVector played if <code>condition</code> is false
	 * @param condition   boolean for selecting animated vector
	 */
	@TargetApi(21)
	void toggleButtonAnimatedVectorDrawable(ImageView view, AnimatedVectorDrawable
			trueVector, AnimatedVectorDrawable falseVector, boolean condition) {
		AnimatedVectorDrawable targetVector = condition ? trueVector : falseVector;
		boolean doStart = !(view.getDrawable() == null);

		view.setImageDrawable(targetVector);

		if (doStart)
			targetVector.start();
		else
			targetVector.stop();
	}
}
