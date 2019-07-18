package wong.colin.wx_drawer.morehead;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.NestedScrollView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.ArrayList;

/**
 * Created by huangchuangliang on 2018/9/5.
 */
public class NewNestedScrollView extends NestedScrollView {

    private ArrayList<OnScrollListener> mLisenterList = new ArrayList<>();


    public NewNestedScrollView(@NonNull Context context) {
        super(context);
    }

    public NewNestedScrollView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void addOnScrollListener(OnScrollListener listener) {
        if(listener != null && !mLisenterList.contains(listener)){
            mLisenterList.add(listener);
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        super.onScrollChanged(l, t, oldl, oldt);

        for(OnScrollListener listener:mLisenterList) {
            if (listener != null) {
                listener.onScroll(t);
            }
        }
    }

    public interface OnScrollListener{
        void onScroll(int scrollY);

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }
}
