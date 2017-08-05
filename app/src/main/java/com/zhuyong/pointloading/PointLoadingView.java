package com.zhuyong.pointloading;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

/**
 * 贝塞尔曲线控件
 * Created by zhuyong on 2017/7/18.
 * 实现思想：
 * 1.我们把整个过程分为两个部分：第一部分是小球从最高点下降到最低点，第二部分是从最低点再弹跳到最高点，如此反复；
 * 2.第一个部分：我只要判断当球体接触到绳子的瞬间使小球和绳子接触着一起下降到最低点即可。
 * 3.第二个部分：小球正常弹起，在球线分离之前，绳子和小球都是一块上升，球线分离之后绳子开始回弹再回弹~
 * 备注：这里面最难处理的就是曲线的状态，我们考虑用二阶贝塞尔曲线来做，只要动态改变曲线的控制点坐标即可，
 * 而这个控制点的坐标可以根据曲线的中心点坐标（就是绳子的中点）套用贝塞尔曲线公式求得，所以我们只要根据
 * 小球的圆心坐标来求出绳子中点坐标，进而求出控制点来绘制贝塞尔曲线即可。
 */

public class PointLoadingView extends View {
    private Paint paintCircle;//绘制弹跳小球
    private Paint paintPoint;//绘制端点两个空心小圆
    private Paint paintPointWhite;//绘制两个实心小圆用于填充端点空白处

    private int mPaintWidth = 10;//线的宽度
    private Point start;//左端点坐标
    private Point end;//右端点坐标
    private float mY;//弹跳小球圆心坐标
    private ValueAnimator mAnimatorDown;//下降动画
    private ValueAnimator mAnimatorUp;//上升动画
    private int TIME = 1500;//动画执行时长（小球从最低点弹到最高点所用时间，也是从最高点降落到最低点所用时间）
    private float mBerSaiErY;//贝塞尔曲线的控制点的Y坐标
    private int mViewWidth;//view宽度
    private int mViewHeight;//view高度
    private int mJumpHeight = 400;//小球跳起的最大高度，默认400dp，如果满足不了要求再根据view高度进行极端
    private int mDownPx = 100;//绳子下降的距离，默认100px，如果高度不允许再进行计算压缩高度
    private int mPointRadius = 40;//小球半径，40个像素

    public PointLoadingView(Context context) {
        this(context, null);
    }

    public PointLoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PointLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        //绘制两个空心的端点，和贝塞尔曲线
        paintPoint = new Paint();
        paintPoint.setColor(Color.WHITE);
        paintPoint.setStyle(Paint.Style.STROKE);
        paintPoint.setStrokeWidth(mPaintWidth);
        paintPoint.setAntiAlias(true);
        //绘制两个实心的小球用于填充两个端点的空白处，颜色和背景色相同，造成透明效果
        paintPointWhite = new Paint();
        paintPointWhite.setColor(ContextCompat.getColor(context, R.color.mPointViewBgColor));
        paintPointWhite.setStyle(Paint.Style.FILL);
        paintPointWhite.setStrokeWidth(mPaintWidth);
        paintPointWhite.setAntiAlias(true);
        //绘制弹跳小球的画笔，白色、填充
        paintCircle = new Paint();
        paintCircle.setColor(Color.WHITE);
        paintCircle.setStyle(Paint.Style.FILL);
        paintCircle.setStrokeWidth(mPaintWidth);
        paintCircle.setAntiAlias(true);

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Path path = new Path();
        //设置起点
        path.moveTo(start.getX(), mViewHeight / 2);
        path.quadTo(mViewWidth / 2, mBerSaiErY, end.getX(), end.getY());
        canvas.drawPath(path, paintPoint);
        //绘制两个端点
        canvas.drawCircle(start.getX(), start.getY(), 12, paintPoint);
        canvas.drawCircle(end.getX(), end.getY(), 12, paintPoint);
        //绘制两个实心的小球用于填充两个端点的空白处
        canvas.drawCircle(start.getX(), start.getY(), 12 - mPaintWidth / 2, paintPointWhite);
        canvas.drawCircle(end.getX(), end.getY(), 12 - mPaintWidth / 2, paintPointWhite);
        //绘制弹跳的小球
        canvas.drawCircle(mViewWidth / 2, mY, mPointRadius, paintCircle);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mY = mViewHeight / 2 - mJumpHeight;//小球圆心坐标（X坐标不变，Y坐标等于view高度的一半减去弹跳高度）
        mBerSaiErY = mViewHeight / 2;//贝塞尔曲线控制点坐标（X坐标不变，在最开始的时候，Y坐标等于view高度的一半）
        start = new Point(0 + 10 + mPaintWidth + getPaddingLeft(), mViewHeight / 2);//左端点
        end = new Point(mViewWidth - 10 - mPaintWidth - getPaddingRight(), mViewHeight / 2);//右端点
        //这里判断mJumpHeight能不能达到需要的高度，如果不行则根据view高度重新计算（跳起高度最大不能超过view高度的一半减去小球的半径）
        if (mJumpHeight > mViewHeight / 2 - mPointRadius / 2) {
            mJumpHeight = mViewHeight / 2 - mPointRadius;//为什么减去直径而不是半径，因为小球弹起的起点高度是从半径开始的
        }
        //为什么是3倍，因为这个过程中球上升的高度是100的3倍，就像我们默认是绳子下降100个像素，
        // 然后上升100个像素后球线分离，线再下降150个像素，线在上升50个像素到最后绳子静止，而这个过程中球上升的高度是300个像素
        if (mJumpHeight <= 3 * mDownPx) {//球上升的最大高度必须大于3倍的绳子下降最低距离，这里减去10个像素之差，否则绳子会失去弹性效果
            mDownPx = mJumpHeight / 3 - 10;
        }
        initAnimator();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mViewWidth = w;
        mViewHeight = h;
    }


    /**
     * 初始化动画
     */
    private void initAnimator() {
        //(下降过程部分)
        //功能：球从最高处降落到最低处
        //这个过程中需要考虑两个过程，一是球下降到接触绳子之前，二是接触绳子之后一直到下降到最低点然后结束。
        //mY的范围是从最高点下降到最低点（mViewHeight / 2 - mJumpHeight——>mViewHeight / 2 + mDownPx）
        //这里我们实时获取到mY(小球圆心Y坐标)，根据mY的值判断何时接触绳子，接触绳子之后开始给控制点mBerSaiErY赋值，重新绘制贝塞尔曲线。
        mAnimatorDown = ValueAnimator.ofFloat(mViewHeight / 2 - mJumpHeight, mViewHeight / 2 + mDownPx);
        mAnimatorDown.setDuration(TIME);
        mAnimatorDown.setInterpolator(new AccelerateInterpolator());//加速下降
        mAnimatorDown.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mY = (float) valueAnimator.getAnimatedValue();
                if (mY < mViewHeight / 2 - mPointRadius - mPaintWidth / 2) {//小球下降，没有接触绳子
                    mBerSaiErY = mViewHeight / 2;//在这个过程中绳子没有发生变化
                } else if (mY >= mViewHeight / 2 - mPointRadius - mPaintWidth / 2 && mY <= mViewHeight / 2 + mDownPx) {
                    //在这个过程中，绳子贴着小球一块下降到最低点。
                    mBerSaiErY = getControlPointF(start, end,
                            new Point(mViewWidth / 2, mY + mPointRadius + mPaintWidth / 2)).getY();
                }
                invalidate();//重新绘制

            }
        });
        mAnimatorDown.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                up();//当下降完成后开始上升
            }
        });
        //(弹起过程部分)
        //功能：球从最低处弹起到最高处
        //这个过程中需要考虑两个部分，第一部分是小球一直在上升，第二部分是绳子的回弹效果。
        //小球：小球的上升很好理解，直接从最低点到最高点减速上升
        //绳子：小球上升到距离水平面一定距离后（这里默认和绳子下降的最低距离相同），小球和绳子分离，小球继续上升，绳子开始回弹。
        //绳子的回弹过程，从最低点弹起——>水平面（绳子水平是的位置）——>上升到球线分离——>水平面（绳子水平是的位置）——>最低点一半的距离——>水平面（绳子水平是的位置）
        mAnimatorUp = ValueAnimator.ofFloat(mViewHeight / 2 + mDownPx, mViewHeight / 2 - mJumpHeight);
        mAnimatorUp.setInterpolator(new DecelerateInterpolator());//减速上升
        mAnimatorUp.setDuration(TIME);
        mAnimatorUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                mY = (float) valueAnimator.getAnimatedValue();
                if (mY >= mViewHeight / 2 - mDownPx - mPointRadius - mPaintWidth / 2
                        && mY <= mViewHeight / 2 + mDownPx) {//上升100个像素后球线分离
                    mBerSaiErY = getControlPointF(start, end,
                            new Point(mViewWidth / 2, mY + mPointRadius + mPaintWidth / 2)).getY();
                } else if (mY >= mViewHeight / 2 - mDownPx - (mDownPx + mDownPx / 2) - mPointRadius - mPaintWidth / 2
                        && mY < mViewHeight / 2 - mDownPx - mPointRadius - mPaintWidth / 2) {//线下降150个像素
                    //获取曲线上的中心点坐标
                    float mCenterY = (mViewHeight / 2 - mDownPx - mPointRadius - mPaintWidth / 2) * 2 - mY;
                    //根据中心点坐标获取曲线的控制点坐标Y
                    mBerSaiErY = getControlPointF(start, end, new Point(mViewWidth / 2, mCenterY + mPointRadius + mPaintWidth / 2)).getY();

                } else if (mY >= mViewHeight / 2 - mDownPx - (mDownPx + mDownPx / 2) - mDownPx / 2 - mPointRadius - mPaintWidth / 2
                        && mY < mViewHeight / 2 - mDownPx - (mDownPx + mDownPx / 2) - mPointRadius - mPaintWidth / 2) {//线上升50个像素
                    //获取曲线上的中心点坐标
                    float mCenterY = mY + 3 * mDownPx;
                    //根据中心点坐标获取曲线的控制点坐标Y
                    mBerSaiErY = getControlPointF(start, end, new Point(mViewWidth / 2, mCenterY + mPointRadius + mPaintWidth / 2)).getY();
                } else {//线静止
                    mBerSaiErY = mViewHeight / 2;
                }
                invalidate();//重新绘制

            }
        });
        mAnimatorUp.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Startdown();//当上升完成后开始下降
            }
        });

    }

    /**
     * 开启下降动画
     */
    public void Startdown() {
        mAnimatorDown.start();
    }

    /**
     * 开启上升动画
     */
    private void up() {
        mAnimatorUp.start();
    }

    /**
     * 根据 最高点，获取贝塞尔曲线的 控制点
     *
     * @param startPointF  开始点
     * @param endPointF    结束点
     * @param bezierPointF 最高点
     * @return 控制点
     */
    public static Point getControlPointF(Point startPointF, Point endPointF, Point bezierPointF) {
        //B(t)=(1-t)^2P0+2t(1-t)P1+t^2P2;
        Point controlPointF = new Point(0, 0);
        float tmp = 0.5F;
        float t = 0.5F;
        controlPointF.setX((bezierPointF.getX() - tmp * tmp * startPointF.getY() - t * t * endPointF.getY()) / (2 * t * tmp));
        controlPointF.setY((bezierPointF.getY() - tmp * tmp * startPointF.getY() - t * t * endPointF.getY()) / (2 * t * tmp));
        return controlPointF;
    }

}
