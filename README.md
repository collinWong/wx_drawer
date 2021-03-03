![1563442819594.gif](https://upload-images.jianshu.io/upload_images/5223818-9c2c23f816c650af.gif?imageMogr2/auto-orient/strip)

### 设计思路
1.自定义个组件类似RelativeLayout 
2.可以内部放子View，然后就是滑动主体在前，小程序View在后
3.重写dispatchTouchEvent 控制这两个子View的位置
4.加上临界点回弹动画
5.手势判断（惯性效果）


#### 1.继承RelativeLayout
如果要从新写一个GroupView组件需要measure → layout → draw 很多细节要处理也不一定处理的好。所以直接用系统提供的RelativeLayout
```java
        public class MoreHeadLayout extends RelativeLayout {
        ...
}
```

#### 2.内部子View 结构
就两个，如下图
![image.png](https://upload-images.jianshu.io/upload_images/5223818-bf0fc1a6f6038790.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
```java
    private View mHeadView;
    private NewNestedScrollView mBodyView;
   void init(){
        mHeadView = findViewById(R.id.head_layout);//view为head
        mBodyView = findViewById(R.id.body_layout);
    }
```
xml中的代码
![image.png](https://upload-images.jianshu.io/upload_images/5223818-3c5c92e31e53d823.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 3.重写dispatchTouchEvent
这一步主要是做控制手指滑动跟随，就是bodyView跟随你的手指滑动
先写一个方法，就是控制bodyView纵坐标的位置
```java
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

```
再计算滑动距离，然后调用setMarginTop 方法

```java
 @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int y = (int) ev.getRawY();
        int offY = lastY - y;
        lastY = y;
        switch (ev.getAction()) {
             ...
            case MotionEvent.ACTION_MOVE:
              if (isBodyTop && headViewVisible()) {
                    setMarginTop((int) (offY * damp));
                    return true;
                }

                if (isBodyTop && offY < 0) {
                    setMarginTop((int) (offY * damp));
                    return true;
                }
                break;
              ...
    }
```
这样bodyView 就可以跟随手指动了，若需要一些阻尼效果可以添加系数damp，就是bodyView移动的距离等于手指滑动的距离乘以系数damp。

#### 4.临界点与回弹
先写一个动画方法，该动画方法就是bodyView 从开始的高度移动到结束的高度。

```java

private ValueAnimator mAnim;
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
```
然后设置一个触发的临界点
```java
    private float closeOrOpen = 0.4f;//动画关闭还是打开的点（相对于头部高度的比例）

```
触发时机例如手指抬起事件
```java
            case MotionEvent.ACTION_UP:
            ...
             if (mBodyView.getTop() < mHeadView.getHeight() * closeOrOpen) {
                        startAnimator(mBodyView.getTop(), 0);//收起动画
                    } else {
                        startAnimator(mBodyView.getTop(), mHeadView.getMeasuredHeight());//打开动画

                    }
```
#### 5.手势判断（惯性效果）
添加手势判断会有更好的使用体验效果，方法如下
```java
    private GestureDetector mGestureDetector;

    mGestureDetector = new GestureDetector(getContext(), new MoreGestureListener());

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
```

在dispatchTouchEvent 方法里每个事件下面把事件传进去
```java
                mGestureDetector.onTouchEvent(ev);//为什么不放在最前面，因为会比关闭/打开动画先触发

```

#### 其他
大概的雏形就是这样可以扩展其他功能如，三个小点或者震动、headView跟随bodyView 移动等
附上用例地址
[https://github.com/collinWong/wx_drawer](https://github.com/collinWong/wx_drawer)
