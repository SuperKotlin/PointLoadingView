package com.zhuyong.pointloading;

/**
 * Created by zhuyong on 2017/7/18.
 */

public class Point {

    // 设置两个变量用于记录坐标的位置
    private float x;
    private float y;

    // 构造方法用于设置坐标
    public Point(float x, float y) {
        this.x = x;
        this.y = y;
    }

    // get方法用于获取坐标
    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }
}
