package paszkiewicz.mymediaplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Image view that displays state-list (selector) and level-list drawables with vector values.
 * <p>use app:vectorImageViewSrc to provide drawable resource.</p>
 * <p>Call {@link #releaseVectorImageData()} if replacing selector or level-list with other drawable</p>
 */
public class VectorImageView extends AppCompatImageView {
    private final static String ANDROID_NS = "http://schemas.android.com/apk/res/android";
    private final static String SELECTOR_TAG = "selector";
    private final static String LEVEL_LIST_TAG = "level-list";
    private final static String ITEM_TAG = "item";

    private static final String TAG = "VectorImageView";
    private final static String SAVE_VALIDATOR = TAG + ".save.Validator";
    private final static String SAVE_SUPER = TAG + ".save.Super";

    //actual compat implementation, or calls to super above 21
    private final ViewInvalidator mViewInvalidator;

    /**
     * This forces view to crash the app if invalid selector item attribute is provided. View might
     * look inconsistent if this is raised. Can be set through xml attribute or setter.
     */
    private boolean mIgnoreMissingAttributes = false;

    public VectorImageView(Context context) {
        this(context, null);
    }

    public VectorImageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VectorImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mViewInvalidator = new ViewInvalidator21();
        }else{
            mViewInvalidator = new ViewInvalidatorCompat();
        }
//        mViewInvalidator = new ViewInvalidatorCompat();
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VectorImageView);
            mIgnoreMissingAttributes = a.getBoolean(R.styleable.VectorImageView_vectorImageViewIgnoreMissingAttributes, false);
            int vectorsDrawableRes = a.getResourceId(R.styleable.VectorImageView_vectorImageViewSrc, 0);
            if (vectorsDrawableRes != 0) {
//                Log.d(TAG, "setVectorsDrawable: drawable resource is "
//                        + getResources().getResourceName(vectorsDrawableRes));
                setVectorsDrawable(vectorsDrawableRes);
            }
            a.recycle();
        }
    }

    /**
     * Even more compat version of {@link #setImageResource(int)} to use below lollipop.
     *
     * @param vectorsDrawable state-list or level-list drawable with vector items
     */
    public void setVectorsDrawable(@DrawableRes int vectorsDrawable) {
        mViewInvalidator.setVectorsCompat(vectorsDrawable);
    }

    /**
     * Set if view should crash if it can't parse all attributes. By default <code>false</code>. This
     * usually should not be called aside from testing.
     *
     * @param ignore true to ignore errors
     */
    public void setIgnoreMissingAttributes(boolean ignore) {
        mIgnoreMissingAttributes = ignore;
    }

    /**
     * Clean stored vector data, this is necessary only if selector or level-list was set and normal drawable is being put in here.
     * <p>This does nothing in Lollipop.</p>
     */
    public void releaseVectorImageData() {
        mViewInvalidator.releaseVectorImageData();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        mViewInvalidator.drawableStateChanged();
    }

    @Override
    public void setImageLevel(int level) {
        mViewInvalidator.setImageLevel(level);
    }

    /**
     * Get current image level. For compat implementation this is internal variable, natively this is
     * level of image drawable.
     */
    public int getImageLevel() {
        if (mViewInvalidator instanceof ViewInvalidatorCompat)
            return ((ViewInvalidatorCompat) mViewInvalidator).mImageLevel;
        else {
            Drawable d = getDrawable();
            if (d != null)
                return d.getLevel();
        }
        return 0;
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle b = new Bundle();
        b.putParcelable(SAVE_SUPER, super.onSaveInstanceState());
        mViewInvalidator.onSaveInstanceState(b);
        return b;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Bundle savedB = (Bundle) state;
        super.onRestoreInstanceState(savedB.getParcelable(SAVE_SUPER));
        mViewInvalidator.onRestoreInstanceState(savedB);
    }

    private abstract class ViewInvalidator {
        //calls for modifying data
        abstract void setVectorsCompat(int vectorsDrawable);

        abstract void releaseVectorImageData();

        abstract void drawableStateChanged();

        abstract void setImageLevel(int level);

        //optional save state
        abstract void onSaveInstanceState(Bundle b);

        abstract void onRestoreInstanceState(Bundle b);
    }

    //support calls
    private class ViewInvalidatorCompat extends ViewInvalidator {
        private int mImageLevel = 0;
        private int mCurrentImageResource = 0;
        private final ArrayList<LevelResource> mLevelResources = new ArrayList<>();
        private final ArrayList<StateResource> mStateResources = new ArrayList<>();

        void releaseVectorImageData() {
            mImageLevel = 0;
            mCurrentImageResource = 0;
            mLevelResources.clear();
            mStateResources.clear();
        }

        @SuppressLint("ResourceType")
        void setVectorsCompat(@DrawableRes int vectorsDrawable) {
            //manually parse XML of drawables
            XmlResourceParser xmp = getContext().getResources().getXml(vectorsDrawable);
            try{
                if (xmp.getEventType() == XmlPullParser.START_DOCUMENT) {
                    xmp.next();
                } else
                    throw new IOException("Invalid xml - no start document tag found");

                releaseVectorImageData();

                //determine if we get level list or selector
                while (xmp.getEventType() != XmlPullParser.END_DOCUMENT) {
                    if (LEVEL_LIST_TAG.equals(xmp.getName())) {
                        parseLevelList(xmp);
                        break;
                    } else if (SELECTOR_TAG.equals(xmp.getName())) {
                        parseStateList(xmp);
                        break;
                    } else if (xmp.getDepth() > 2) {
                        Log.e(TAG, "setVectorsDrawable: missed start tag " + xmp.getDepth());
                        break;
                    }
                    xmp.next();
                }

            } catch (XmlPullParserException | IOException e) {
                e.printStackTrace();
            }finally {
                xmp.close();
            }

            if (mLevelResources.isEmpty() && mStateResources.isEmpty()) {
                //invalid argument?
                Log.w(TAG, "setVectorsDrawable: Provided vector drawable is not a selector or level-list " + getContext().getResources().getResourceName(vectorsDrawable) + ", trying to use native call...");
                setImageResource(vectorsDrawable);
            }
            invalidate();
        }

        private void parseLevelList(XmlResourceParser xmp) throws XmlPullParserException, IOException {
            LevelResource.Builder builder;
            while (xmp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (ITEM_TAG.equals(xmp.getName())) {
                    builder = new LevelResource.Builder();

                    _attributeLoop:
                    for (int i = 0; i < xmp.getAttributeCount(); i++) {
                        if (ANDROID_NS.equals(xmp.getAttributeNamespace(i))) {
                            switch (xmp.getAttributeName(i)) {
                                case "id":
                                    break;
                                case "drawable":
                                    builder.setDrawable(xmp.getAttributeResourceValue(i, 0));
                                    break;
                                case "maxLevel":
                                    builder.setMaxLevel(xmp.getAttributeIntValue(i, 0));
                                    break;
                                case "minLevel":
                                    builder.setMinLevel(xmp.getAttributeIntValue(i, 0));
                                    break;
                                default:
                                    Log.e(TAG, "parseLevelList: unimplemented attribute: " + xmp.getAttributeName(i));
                                    break _attributeLoop;
                            }
                        }
                    }
                    if (builder.canBuild()) {
                        mLevelResources.add(builder.build());
                    }
                }
                xmp.next();
            }
        }

        private void parseStateList(XmlResourceParser xmp) throws XmlPullParserException, IOException {
            StateResource.Builder builder;
            while (xmp.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (ITEM_TAG.equals(xmp.getName())) {
                    builder = new StateResource.Builder();

                    _attributeLoop:
                    for (int i = 0; i < xmp.getAttributeCount(); i++) {
                        if (ANDROID_NS.equals(xmp.getAttributeNamespace(i))) {
                            switch (xmp.getAttributeName(i)) {
                                case "id":  //id is also valid but ignored
                                    break;
                                case "drawable":
                                    builder.setDrawable(xmp.getAttributeResourceValue(i, 0));
                                    break;
                                case "state_activated":
                                    builder.addFlag(State.ACTIVATED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_active":
                                    builder.addFlag(State.ACTIVE, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_checked":
                                    builder.addFlag(State.CHECKED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_enabled":
                                    builder.addFlag(State.ENABLED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_first":
                                    builder.addFlag(State.FIRST, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_focused":
                                    builder.addFlag(State.FOCUSED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_last":
                                    builder.addFlag(State.LAST, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_middle":
                                    builder.addFlag(State.MIDDLE, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_pressed":
                                    builder.addFlag(State.PRESSED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_selected":
                                    builder.addFlag(State.SELECTED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_single":
                                    builder.addFlag(State.SINGLE, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                case "state_window_focused":
                                    builder.addFlag(State.WINDOW_FOCUSED, xmp.getAttributeBooleanValue(i, true));
                                    break;
                                default:
                                    if (mIgnoreMissingAttributes) {
                                        Log.e(TAG, "parseStateList: unimplemented attribute: " + xmp.getAttributeName(i));
                                        break _attributeLoop;
                                    } else
                                        throw new IllegalArgumentException("unimplemented attribute: " + xmp.getAttributeName(i));
                            }
                        }
                    }
                    if (builder.canBuild()) {
                        mStateResources.add(builder.build());
                    }
                }
                xmp.next();
            }
        }

        @Override
        void drawableStateChanged() {
            int imgResource = 0;
            if (!mLevelResources.isEmpty()) {
                //find suitable level
                for (LevelResource lr : mLevelResources) {
                    if (lr.isValid(mImageLevel)) {
                        imgResource = lr.drawable;
                        break;
                    }
                }
            } else if (!mStateResources.isEmpty()) {
                //find suitable state
                for (StateResource sr : mStateResources) {
                    if (sr.isValid(VectorImageView.this)) {
                        imgResource = sr.drawable;
                        break;
                    }
                }
            }
            if (imgResource != mCurrentImageResource) {
                mCurrentImageResource = imgResource;
                //this triggers invalidate
                setImageResource(imgResource);
            }
        }

        @Override
        public void setImageLevel(int level) {
            if (!mLevelResources.isEmpty()) {
                mImageLevel = level;
                VectorImageView.this.drawableStateChanged();
            } else
                VectorImageView.super.setImageLevel(level);
        }

        @Override
        void onSaveInstanceState(Bundle b) {
            b.putInt(SAVE_VALIDATOR, mImageLevel);
        }

        @Override
        void onRestoreInstanceState(Bundle b) {
            mImageLevel = b.getInt(SAVE_VALIDATOR, 0);
        }
    }

    //native calls
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private class ViewInvalidator21 extends ViewInvalidator {
        @Override
        void setVectorsCompat(int vectorsDrawable) {
            setImageResource(vectorsDrawable);
        }

        @Override
        void releaseVectorImageData() {
            //nothing
        }

        @Override
        void setImageLevel(int level) {
            VectorImageView.super.setImageLevel(level);
        }

        @Override
        void drawableStateChanged() {
        }

        @Override
        void onSaveInstanceState(Bundle b) {
        }

        @Override
        void onRestoreInstanceState(Bundle b) {
        }
    }

    /**
     * Resource with min and max level.
     */
    private final static class LevelResource {
        final int minLevel;
        final int maxLevel;

        @DrawableRes
        final int drawable;

        /**
         * See if this level is ok for view
         *
         * @param level level to validate
         * @return true if level is valid
         */
        private boolean isValid(int level) {
            return level >= minLevel && level <= maxLevel;
        }

        private LevelResource(@DrawableRes int drawable, int minLevel, int maxLevel) {
            this.drawable = drawable;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
        }

        private static class Builder {
            int minLevel = 0;
            int maxLevel = 0;
            @DrawableRes
            int drawable = 0;

            private Builder setMinLevel(int minlevel) {
                this.minLevel = minlevel;
                return this;
            }

            private Builder setMaxLevel(int maxLevel) {
                this.maxLevel = maxLevel;
                return this;
            }

            private Builder setDrawable(int drawable) {
                this.drawable = drawable;
                return this;
            }

            /**
             * Building is only possible if drawable is set.
             */
            private boolean canBuild() {
                return drawable != 0;
            }

            private LevelResource build() {
                return new LevelResource(drawable, minLevel, maxLevel);
            }
        }
    }

    /**
     * Resource with state flags.
     */
    private final static class StateResource {
        @State.StateValue
        final int requiredTrueStates;
        @State.StateValue
        final int requiredFalseStates;
        @DrawableRes
        final int drawable;

        private StateResource(@DrawableRes int drawable) {
            this(drawable, 0, 0);
        }

        private StateResource(@DrawableRes int drawable,
                              @State.StateValue int requiredTrueStates,
                              @State.StateValue int requiredFalseStates) {
            this.drawable = drawable;
            this.requiredTrueStates = requiredTrueStates;
            this.requiredFalseStates = requiredFalseStates;
        }

        /**
         * See if this state is ok for view
         *
         * @param view view to check state of
         * @return true if state is valid, false if not
         */
        private boolean isValid(ImageView view) {
            //only states that are available for ImageView
            return internalCheck(State.ACTIVATED, view.isActivated())
                    && internalCheck(State.ENABLED, view.isEnabled())
                    && internalCheck(State.FOCUSED, view.isFocused())
                    && internalCheck(State.PRESSED, view.isPressed())
                    && internalCheck(State.SELECTED, view.isSelected());
        }

        /**
         * Compare state int with state of view.
         *
         * @return true if this state is valid
         */
        private boolean internalCheck(int flag, boolean viewState) {
            //common case - flag is not raised (ignored)
            if (((requiredTrueStates | requiredFalseStates) & flag) == 0)
                return true;

            //check if state aligns with required flags
            if (viewState)
                return (requiredTrueStates & flag) == flag;
            else
                return (requiredFalseStates & flag) == flag;
        }

        private static class Builder {
            @DrawableRes
            int drawable = 0;
            @State.StateValue
            int requiredTrueStates = 0;
            @State.StateValue
            int requiredFalseStates = 0;

            private Builder setDrawable(@DrawableRes int drawable) {
                this.drawable = drawable;
                return this;
            }

            /**
             * Add state flags to the builder
             *
             * @param flags      flags to add
             * @param isRequired if state is required as true or false
             * @return this builder
             */
            private Builder addFlag(@State.StateValue int flags, boolean isRequired) {
                if (isRequired)
                    requiredTrueStates |= flags;
                else
                    requiredFalseStates |= flags;
                return this;
            }

            /**
             * Building only possible if drawable is set.
             */
            private boolean canBuild() {
                return drawable != 0;
            }

            StateResource build() {
                return new StateResource(drawable, requiredTrueStates, requiredFalseStates);
            }
        }
    }

    /**
     * Most view states available for state list drawable.
     */
    private static final class State {
        final static int ACTIVATED = 1;
        final static int ACTIVE = 1 << 1;
        final static int CHECKED = 1 << 2;
        final static int ENABLED = 1 << 3;
        final static int FIRST = 1 << 4;
        final static int FOCUSED = 1 << 5;
        final static int LAST = 1 << 6;
        final static int MIDDLE = 1 << 7;
        final static int PRESSED = 1 << 8;
        final static int SELECTED = 1 << 9;
        final static int SINGLE = 1 << 10;
        final static int WINDOW_FOCUSED = 1 << 11;

        @IntDef(flag = true, value = {ACTIVATED, ACTIVE, CHECKED, ENABLED, FIRST, FOCUSED,
                LAST, MIDDLE, PRESSED, SELECTED, SINGLE, WINDOW_FOCUSED})
        @interface StateValue {
        }
    }
}
