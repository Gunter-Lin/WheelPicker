package com.gunter.lib;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Gunter on 2016/2/19.
 */
public class WheelPicker extends View {

    private final int DIR_UP = 1;
    private final int DIR_DOWN = -1;
    private final int VELOCITY_UNIT = 5;
    private List<String> mDatas;
    private Rect[] mDataRects;
    private VelocityTracker mVelocityTracker;
    private Item mCurrentItem;
    private float mHalfRange;//半个区间大小，该区间用于确定item的位置是否属于mCurrentScale
    private float mTextSize, mTextScale;
    private int mBigTextColor, mSmallTextColor;
    private float mVerticalSpacing;
    private float mCenterX, mCenterY, mStartY, mEndY;
    private boolean isLoop;
    private Paint mPaint;
    private float mGroupHeight;
    private boolean isOverRange;
    private ValueAnimator mScrollAnim;
    private float mStartOffset;
    private float mStartPosition;
    private OnChangedListener mListener;

    public WheelPicker(Context context) {
        this(context, null);
    }

    public WheelPicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        mDatas = new ArrayList<>();
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.WheelPicker);
        mTextSize = ta.getDimension(R.styleable.WheelPicker_textSize, getSp(context, 18));
        mTextScale = ta.getFloat(R.styleable.WheelPicker_textScale, 1.8f);
        mBigTextColor = ta.getColor(R.styleable.WheelPicker_bigTextColor, getResources().getColor(R.color.white));
        mSmallTextColor = ta.getColor(R.styleable.WheelPicker_smallTextColor, getResources().getColor(R.color.white));
        mVerticalSpacing = ta.getDimension(R.styleable.WheelPicker_verticalSpacing, getDip(context, 40));
        ta.recycle();
    }

    //初始化item文本占用的空间大小
    private void initText() {
        mDataRects = new Rect[mDatas.size()];
        for (int i = 0; i < mDatas.size(); i++) {
            mDataRects[i] = new Rect();
        }
        Paint textPaint = new Paint();
        textPaint.setTextSize(mTextSize);

        mGroupHeight = 0;
        //获取每个item文本对应的Rect，以及一组item的总高度mGroupHeight
        for (int i = 0; i < mDataRects.length; i++) {
            textPaint.getTextBounds(mDatas.get(i), 0, mDatas.get(i).length(), mDataRects[i]);
            if (mCurrentItem.getPosition() == i) {
                mGroupHeight += mDataRects[i].height() * mTextScale + mVerticalSpacing;
            }
            mGroupHeight += mDataRects[i].height() + mVerticalSpacing;
        }

//        Log.e("Gunter", "mGroupHeight：" + mGroupHeight);
//        Log.e("Gunter", "getMeasuredHeight()：" + getMeasuredHeight());
//        Log.e("Gunter", "getHeight()：" + getHeight());

        //如果一组item的高度大于整个控件的高度，则循环，否则不循环
        if (mGroupHeight > getMeasuredHeight())
            isLoop = true;
        else
            isLoop = false;

    }

    private float getDip(Context context, int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    private float getSp(Context context, int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value, context.getResources().getDisplayMetrics());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = getMeasuredHeight();

        mCenterX = getMeasuredWidth() / 2;
        mCenterY = measuredHeight / 2;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDatas.size() == 0)
            return;
        int pos = mCurrentItem.getPosition();
        float scale = mCurrentItem.getScale();
//            Log.e("Gunter", "scale：" + scale);
//            Log.e("Gunter", "mDataRects[pos].width()：" + mDataRects[pos].width());
//            Log.e("Gunter", "mCurrentItem.getOffset()：" + mCurrentItem.getOffset());
//            Log.e("Gunter", "mDataRects[pos].height()：" + mDataRects[pos].height());
//            Log.e("Gunter", "mCenterX：" + mCenterX);
//            Log.e("Gunter", "mCenterY：" + mCenterY);
        mPaint.setColor(mBigTextColor);
        mPaint.setTextSize(mTextSize * scale);
        canvas.drawText(mDatas.get(pos), mCenterX - (mDataRects[pos].width() * scale) / 2,
                (mCenterY + mCurrentItem.getOffset()) + (mDataRects[pos].height() * scale) / 2, mPaint);

        //画中间item以下的item文本
        drawLowerText(canvas);

        //画中间item以上的item文本
        drawUpperText(canvas);
    }

    private void drawUpperText(Canvas canvas) {
        float bottomY;
        Item item = mCurrentItem;
        while (true) {
            item = item.getLastItem();
            int pos = item.getPosition();
            float scale = item.getScale();
            bottomY = (mCenterY + item.getOffset()) + (mDataRects[pos].height() * scale) / 2;
            Log.e("Gunter", "item.getOffset()" + item.getOffset());
            if (bottomY <= 0 || (!isLoop && pos == mDatas.size() - 1))
                break;
            mPaint.setTextSize(mTextSize * scale);
            canvas.drawText(mDatas.get(pos), mCenterX - (mDataRects[pos].width() * scale) / 2,
                    bottomY, mPaint);
        }

    }

    private void drawLowerText(Canvas canvas) {
        float topY;
        Item item = mCurrentItem;
        while (true) {
            item = item.getNextItem();
            int pos = item.getPosition();
            float scale = item.getScale();
            topY = (mCenterY + item.getOffset()) - (mDataRects[pos].height() * scale) / 2;
            Log.e("Gunter", "topY" + topY);
            if (topY >= getMeasuredHeight() || (!isLoop && pos == 0))
                break;
            mPaint.setTextSize(mTextSize * scale);
            canvas.drawText(mDatas.get(pos), mCenterX - (mDataRects[pos].width() * scale) / 2,
                    topY + (mDataRects[pos].height() * scale), mPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final VelocityTracker verTracker = getTracker(event);
        float moveLength;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mStartY = event.getY();
                //只有循环的时候才有scroll动画
                if (isLoop) {
                    if (mScrollAnim != null && mScrollAnim.isRunning()) {
                        mScrollAnim.cancel();
                    }
                } else {
                    mStartOffset = mCurrentItem.getOffset();//获取按下时的偏移量
                    mStartPosition = mCurrentItem.getPosition();//获取按下时的item
                }
                break;
            case MotionEvent.ACTION_MOVE:
                moveLength = event.getY() - mStartY;
                if (isLoop)
                    mCurrentItem.setOffset(moveLength);
                else drag(moveLength);
                invalidateView();
                break;
            case MotionEvent.ACTION_UP:
                moveLength = event.getY() - mStartY;
                if (isLoop) {
                    verTracker.computeCurrentVelocity(VELOCITY_UNIT);
                    float velY = verTracker.getYVelocity();
                    Log.e("Gunter3", "VelocityY:" + velY);
                    resetCurrentItem(event.getY() - mStartY);
                    if (Math.abs(velY) < VELOCITY_UNIT || !isLoop) {
                        resetOffset();
                    } else {
                        smoothScroll(velY);
                    }
                } else {
                    if (!isOverRange) {
                        resetCurrentItem(moveLength);
                    }
                    resetOffset();
                }
                break;
        }
        return true;
    }


    private void drag(float moveLength) {
        if (moveLength < 0) {
            //这个moveRange变量确定了：该item组可正常滑动的Y距离，注意是正常滑动
            float moveRange = ((mDatas.size() - 1 - mStartPosition) * mHalfRange * 2) + mHalfRange + mStartOffset;
            if (-moveLength >= moveRange) {//滑动距离超出moveRange按照以下处理
                mCurrentItem.setPosition(mDatas.size() - 1);
                mCurrentItem.setOffset((moveLength + moveRange) * 0.2f - mHalfRange);
                isOverRange = true;
            } else {//没有超出moveRange时仍然正常处理，即mCurrentItem.setOffset(moveLength);
                isOverRange = false;
                mCurrentItem.setOffset(moveLength);
            }
        } else {
            float moveRange = ((mStartPosition) * mHalfRange * 2) + mHalfRange - mStartOffset;
            if (moveLength >= moveRange) {
                mCurrentItem.setPosition(0);
                mCurrentItem.setOffset((moveLength - moveRange) * 0.2f + mHalfRange);
                isOverRange = true;
            } else {
                isOverRange = false;
                mCurrentItem.setOffset(moveLength);
            }
        }
    }

    //按照给定的Y轴上的速度velocityY进行平滑滚动，velocityY值的正负来确定方向
    private void smoothScroll(float velocityY) {
        final float offset = mCurrentItem.getOffset();
        int moveCount = (int) velocityY / VELOCITY_UNIT;
        float moveUnit = mHalfRange * 2;
        final int position = mCurrentItem.getPosition();
        mScrollAnim = ValueAnimator.ofObject(new TypeEvaluator<Float>() {
            @Override
            public Float evaluate(float fraction, Float startValue, Float endValue) {
                float newValue = startValue + fraction * (endValue - startValue);
                mCurrentItem.setOffset(offset);
                mCurrentItem.setPosition(position);
                resetCurrentItem(newValue);
                return newValue;
            }
        }, offset, moveUnit * moveCount).setDuration(100 * Math.abs(moveCount));
        mScrollAnim.setInterpolator(new DecelerateInterpolator());//减速
        mScrollAnim.start();
    }

    //对偏移量进行复位
    private void resetOffset() {
        if (mCurrentItem.getOffset() != 0) {
            ValueAnimator.ofObject(new TypeEvaluator<Float>() {
                @Override
                public Float evaluate(float fraction, Float startValue, Float endValue) {
                    float newValue = startValue + fraction * (endValue - startValue);
                    mCurrentItem.setOffset(newValue);
                    invalidateView();
                    return newValue;
                }
            }, mCurrentItem.getOffset(), 0f).setDuration(100).start();
        }
    }

    //重置当前mCurrentItem，前面经过mCurrentItem.setOffset有可能已经使mCurrentItem不被选中，也就是isSelected等于false
    //调用该函数可以重新使mCurrentItem指向被选中的item
    private void resetCurrentItem(float offset) {
        Log.e("Gunter1", "offset:" + offset);
        float unit = mHalfRange * 2;
        Log.e("Gunter1", "unit:" + unit);
        float arg;
        if (offset > 0) {
            arg = offset + mHalfRange;
            int position = mCurrentItem.getPosition() - (int) ((arg / unit) % mDatas.size());
            if (position < 0) {
                position += mDatas.size();
            }
            Log.e("Gunter1", "(int) ((arg / unit) % mDatas.size():" + (int) ((arg / unit) % mDatas.size()));
            Log.e("Gunter", "++++++position:" + position);
            Log.e("Gunter1", "arg % unit:" + arg % unit);
            mCurrentItem.setPosition(position);
            mCurrentItem.setOffset(arg % unit - mHalfRange);
        } else if (offset < 0) {
            arg = offset - mHalfRange;
            Log.e("Gunter2", "arg:" + arg);
            int position = mCurrentItem.getPosition() - (int) ((arg / unit) % mDatas.size());
            if (position > mDatas.size() - 1) {
                position -= mDatas.size();
            }
            mCurrentItem.setPosition(position);
            Log.e("Gunter", "-----position:" + position);
            mCurrentItem.setOffset(arg % unit + mHalfRange);
        }
        callback();
        invalidateView();
    }

    private void callback() {
        if (mListener != null)
            mListener.onChanged(mCurrentItem.getPosition());
    }

    //VelocityTracker用于确定手指滑动的速度，后面需要根据该速度确定滚动的距离
    private VelocityTracker getTracker(final MotionEvent event) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(event);
        return mVelocityTracker;
    }

    //获得当前选项
    public String getSelectedItem() {
        if (mDatas.size() != 0) {
            return mDatas.get(mCurrentItem.getPosition());
        }
        return null;
    }

    public void setOnChangedListener(OnChangedListener listener) {
        mListener = listener;
    }

    /**
     * 初始化，添加item
     *
     * @param position 指定默认的时候，哪个item被选中
     * @param datas    给定所有的items
     */
    public void initData(int position, String... datas) {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mDatas.clear();
        Collections.addAll(mDatas, datas);
        mCurrentItem = new Item(position, 0);
        initText();
        //半个区间的大小
        mHalfRange = (mVerticalSpacing + mDataRects[0].height() / 2 + mDataRects[0].height() / 2 * mTextScale) / 2;
        invalidateView();
        callback();
    }

    private void invalidateView() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            invalidate();
        } else {
            postInvalidate();
        }
    }

    public interface OnChangedListener {
        void onChanged(int currentPos);
    }

    private class Item {
        private int position;
        private float offset;

        private Item(int position, float offset) {
            this.position = position;
            this.offset = offset;
        }

        public int getPosition() {
            return position;
        }

        public void setPosition(int position) {
            this.position = position;
        }

        public float getOffset() {
            return offset;
        }

        public void setOffset(float offset) {
            this.offset = offset;
        }

        public float getScale() {
            if (offset == 0)
                return mTextScale;
            else if (Math.abs(offset) >= mHalfRange * 2)
                return 1.0f;
            else
                return (mHalfRange * 2 - Math.abs(offset)) / (mHalfRange * 2) * (mTextScale - 1.0f) + 1.0f;
        }

        public Item getNextItem() {
            float halfHeight = mDataRects[0].height() / 2;
            float pointTop = -(mHalfRange * 2 + mDataRects[0].height() + mVerticalSpacing);
            float pointMiddle = -(mHalfRange * 2);
            float nextScale = 1.0f;
            if (offset > pointTop && offset < pointMiddle) {
                nextScale = (mTextScale - 1.0f) * ((offset - pointTop) / (pointMiddle - pointTop)) + 1.0f;
            } else if (offset >= pointMiddle && offset < 0) {
                nextScale = mTextScale - getScale() + 1.0f;
            }
            return new Item(getNext(), offset + getScale() * halfHeight + mVerticalSpacing + nextScale * halfHeight);
        }

        public Item getLastItem() {
            float halfHeight = mDataRects[0].height() / 2;
            float pointBottom = mHalfRange * 2 + mDataRects[0].height() + mVerticalSpacing;
            float pointMiddle = mHalfRange * 2;
            float lastScale = 1.0f;
            if (offset < pointBottom && offset > pointMiddle) {
                lastScale = (mTextScale - 1.0f) * ((pointBottom - offset) / (pointBottom - pointMiddle)) + 1.0f;
            } else if (offset <= pointMiddle && offset > 0) {
                lastScale = mTextScale - getScale() + 1.0f;
            }
            return new Item(getLast(), offset - (getScale() * halfHeight + mVerticalSpacing + lastScale * halfHeight));
        }

        private int getNext() {
            if (position == mDatas.size() - 1)
                return 0;
            return position + 1;
        }

        private int getLast() {
            if (position == 0)
                return mDatas.size() - 1;
            return position - 1;
        }

        public boolean isSelected() {
            if (offset < 0)
                return offset > -mHalfRange;
            else
                return offset <= mHalfRange;
        }

    }

}
