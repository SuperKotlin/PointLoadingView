package com.zhuyong.pointloading;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private PointLoadingView mPointLoadingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPointLoadingView = (PointLoadingView) findViewById(R.id.BeisaierTwoView);
        mPointLoadingView.post(new Runnable() {
            @Override
            public void run() {
                mPointLoadingView.Startdown();
            }
        });
    }
}
