package paszkiewicz.mymediaplayer;

import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.SeekBar;

/**
 * Static functions.
 */

abstract class Util {
	/**
	 * Convenience class for binding and casting view types.
	 */
	static class ViewBinder {
		View root;

		ViewBinder(View root) {
			this.root = root;
		}

		@SuppressWarnings("unchecked")
		<T extends View> T bind(@IdRes int viewId, @Nullable View.OnClickListener
				onClickListener) {
			T view = (T) root.findViewById(viewId);
			view.setOnClickListener(onClickListener);
			return view;
		}

		@SuppressWarnings("unchecked")
		<T extends View> T bind(@IdRes int viewId) {
			return (T) root.findViewById(viewId);
		}
	}

	/**
	 * Convenience class for overriding 1 method from interface.
	 */
	abstract static class SeekBarChangeListenerAdapter implements SeekBar.OnSeekBarChangeListener {
		@Override
		public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {

		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {

		}
	}
}
