package wong.colin.wx_drawer.morehead;

import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import wong.colin.wx_drawer.R;

/**
 * 类似微信小程序下拉
 * Created by huangchuangliang on 2018/9/4.
 */
public class MoreHeadLayout extends RelativeLayout {
    private View mHeadView;
    private NewNestedScrollView mBodyView;
    private float closeOrOpen = 0.4f;//动画关闭还是打开的点（相对于头部高度的比例）
    private float damp = 0.5f;//阻尼系数（当在头部下拉时候，头部移动距离与手指移动距离的比例）
    private boolean isBodyTop = true;
    private ValueAnimator mAnim;
    private GestureDetector mGestureDetector;
    private boolean mIsHaveScrolled = false;//是否有滑动过（判断是否要把up事件给子View,不然会出现滑动后触发点击）
    private boolean isVibraed;

    public MoreHeadLayout(@NonNull Context context) {
        super(context);
        init();
    }


    public MoreHeadLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    private void init() {
        mGestureDetector = new GestureDetector(getContext(), new MoreGestureListener());
        post(new Runnable() {
            @Override
            public void run() {
                hideHead();
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mHeadView = findViewById(R.id.head_layout);//view为head
        mBodyView = findViewById(R.id.body_layout);
        mBodyView.addOnScrollListener(new NewNestedScrollView.OnScrollListener() {
            @Override
            public void onScroll(int scrollY) {
                isBodyTop = scrollY == 0;
            }
        });
    }

    int lastY;//上一个事件Y坐标

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int y = (int) ev.getRawY();
        int offY = lastY - y;
        lastY = y;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(mBodyView.getTop() < mHeadView.getHeight() * closeOrOpen) {
                    isVibraed = false;
                }
                cancleAnimator();
                mGestureDetector.onTouchEvent(ev);
                break;
            case MotionEvent.ACTION_MOVE:
                mGestureDetector.onTouchEvent(ev);
                //触发震动
                if (mBodyView.getTop() > mHeadView.getHeight() * closeOrOpen && !isVibraed) {
                    isVibraed = true;
                    Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                    vibrator.vibrate(100);  // 设置手机振动

                }

                if (isBodyTop && headViewVisible()) {
                    setMarginTop((int) (offY * damp));
                    return true;
                }

                if (isBodyTop && offY < 0) {
                    setMarginTop((int) (offY * damp));
                    return true;
                }


                break;
            case MotionEvent.ACTION_UP:
                //可见
                if (headViewVisible()) {
                    if (mBodyView.getTop() < mHeadView.getHeight() * closeOrOpen) {
                        startAnimator(mBodyView.getTop(), 0);//收起动画
                    } else {
                        startAnimator(mBodyView.getTop(), mHeadView.getMeasuredHeight());//打开动画

                    }

                }
                mGestureDetector.onTouchEvent(ev);//为什么不放在最前面，因为会比关闭/打开动画先触发
                if (mIsHaveScrolled && headViewVisible()) {
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return super.dispatchTouchEvent(ev);

    }


    private boolean headViewVisible() {
        return mBodyView.getTop() > 0;
    }

    private void setMarginTop(int offY) {
        RelativeLayout.LayoutParams paramsBody = (LayoutParams) mBodyView.getLayoutParams();
        int marginTop = paramsBody.topMargin - offY;
        if (marginTop < 0) {
            marginTop = 0;
        } else if (marginTop > mHeadView.getHeight()) {
            marginTop = mHeadView.getHeight();
        }
        paramsBody.topMargin = marginTop;
        mBodyView.setLayoutParams(paramsBody);
    }

    public void hideHead() {
        RelativeLayout.LayoutParams params = (LayoutParams) mBodyView.getLayoutParams();
        params.topMargin = 0;
        mBodyView.setLayoutParams(params);
    }

    @Override
    public boolean canScrollVertically(int direction) {

        if (mBodyView.getTop() == mHeadView.getMeasuredHeight() && isBodyTop) {
            return false;
        } else {
            return true;
        }
    }


    /**
     * 收起或打开动画
     *
     * @param start 开始高度
     * @param end   结束高度
     */
    public void startAnimator(int start, int end) {
        startAnimator(start, end, 100);
    }

    /**
     * 收起或打开动画
     *
     * @param start 开始高度
     * @param end   结束高度
     * @param time  动画时间
     */
    public void startAnimator(int start, int end, long time) {

        if (mAnim != null && mAnim.isRunning()) {
            mAnim.cancel();
        }
        mAnim = ValueAnimator.ofInt(start, end);
        mAnim.setDuration(time);
        mAnim.setTarget(mBodyView);
        mAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int currentValue = (Integer) animation.getAnimatedValue();
                RelativeLayout.LayoutParams params = (LayoutParams) mBodyView.getLayoutParams();
                params.topMargin = currentValue;
                mBodyView.setLayoutParams(params);
            }
        });
        mAnim.start();

    }

    public void cancleAnimator() {
        if (mAnim != null && mAnim.isRunning()) {
            mAnim.cancel();
        }
    }


    class MoreGestureListener implements GestureDetector.OnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            mIsHaveScrolled = false;
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            mIsHaveScrolled = true;
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //当头部显示并且向上fling时候关闭头部（惯性视觉）
            if (mBodyView.getTop() > 0 && velocityY < 0) {
                startAnimator(mBodyView.getTop(), 0);
            }
            return false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (changed) {
            hideHead();
        }
        super.onLayout(changed, l, t, r, b);

    }

    //收起
    public void resetHead() {
        if (mBodyView.getTop() > 0) {
            startAnimator(mBodyView.getTop(), 0);
        }
    }

    public void showPullTip(int height) {
        startAnimator(0, height, 300);
        //延迟1秒后收起
        postDelayed(new Runnable() {
            @Override
            public void run() {
                startAnimator(mBodyView.getTop(), 0, 300);
            }
        }, 1300);
    }
}
