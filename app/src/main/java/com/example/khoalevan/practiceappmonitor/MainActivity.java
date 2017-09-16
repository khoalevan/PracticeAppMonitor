package com.example.khoalevan.practiceappmonitor;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewManager;
import android.view.ViewStub;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;


public class MainActivity extends Activity{

    private SharedPreferences mPrefs;
    private int intervalRead, intervalUpdate, intervalWidth, orientation, statusBarHeight, graphicMode, processesMode, settingsHeight, navigationBarHeight;
    private boolean cpuTotal, cpuAM, memUsed, memAvailable, memFree, cached, threshold, canvasLocked;
    private int animDuration = 200;
    private float sD;
    private Resources res;
    private boolean orientationChanged;
    private ToggleButton mBHide;
    private Button mBMemory;
    private Thread mThread;
    private ViewGraphic mVG;
    private TextView mTVCPUTotalP, mTVCPUAMP, mTVMemoryAM;
    private LinearLayout mLTopBar;
    private FrameLayout mLGraphicSurface, mLSettings;
    private Handler mHandlerVG = new Handler(), mHandler = new Handler();
    private ServiceReader mSR;
    private LinearLayout mLWelcome, mLMenu;
    private PopupWindow mPWMenu;
    private DecimalFormat mFormatPercent = new DecimalFormat("##0.0");
    private Runnable drawRunnable = new Runnable() {
        @SuppressWarnings("unchecked")
        @SuppressLint("NewApi")
        @Override
        public void run() {
            mHandler.postDelayed(this, intervalUpdate);
            if (mSR != null) {
                mHandlerVG.post(drawRunnableGraphic);
                setTextLabelCPU(null, mTVCPUTotalP, mSR.getCPUTotalP());
                if (processesMode == C.processesModeShowCPU) {
                    setTextLabelCPU(null, mTVCPUAMP, mSR.getCPUAMP());
                }
            }
        }
    },drawRunnableGraphic = new Runnable() {
        @Override
        public void run() {
            mThread = new Thread() {
                public void run() {
                    Canvas canvas = null;
                    if (!canvasLocked) {
                        canvas = mVG.lockCanvas();
                        if (canvas != null) {
                            canvasLocked = true;
                            mVG.onDrawCustomised(canvas, mThread);
                            System.out.println("------------------");
                            try {
                                mVG.unlockCanvasAndPost(canvas);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            canvasLocked = false;
                        }
                    }
                }
            };
            mThread.start();
        }
    };

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @SuppressLint("NewApi")
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mSR = ((ServiceReader.ServiceReaderDataBinder) service).getService();

            mVG.setService(mSR);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private BroadcastReceiver receiverSetIconRecord = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }, receiverDeadProcess = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    }, receiverFinish = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    @SuppressWarnings("deprecation")
    @SuppressLint({"InlinedApi", "NewApi", "InflateParams"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, ServiceReader.class));
        setContentView(R.layout.activity_main);

        mHandlerVG.post(drawRunnableGraphic);

        mPrefs = getSharedPreferences(getString(R.string.app_name) + C.prefs, MODE_PRIVATE);
        intervalRead = mPrefs.getInt(C.intervalRead, C.defaultIntervalUpdate);
        intervalUpdate = mPrefs.getInt(C.intervalUpdate, C.defaultIntervalUpdate);
        intervalWidth = mPrefs.getInt(C.intervalWidth, C.defaultIntervalWidth);

        cpuTotal = mPrefs.getBoolean(C.cpuTotal, true);
        cpuAM = mPrefs.getBoolean(C.cpuAM, true);

        memUsed = mPrefs.getBoolean(C.memUsed, true);
        memAvailable = mPrefs.getBoolean(C.memAvailable, true);
        memFree = mPrefs.getBoolean(C.memFree, false);
        cached = mPrefs.getBoolean(C.cached, false);
        threshold = mPrefs.getBoolean(C.threshold, true);

        res = getResources();
        sD = res.getDisplayMetrics().density;
        orientation = res.getConfiguration().orientation;
        statusBarHeight = res.getDimensionPixelSize(res.getIdentifier(C.sbh, C.dimen, C.android));

        final SeekBar mSBWidth = (SeekBar) findViewById(R.id.SBIntervalWidth);
        if (savedInstanceState != null && !savedInstanceState.isEmpty() && savedInstanceState.getInt(C.orientation) != orientation) orientationChanged = true;

        mVG = (ViewGraphic) findViewById(R.id.ANGraphic);
        graphicMode = mPrefs.getInt(C.graphicMode, C.graphicModeShowMemory);
        mVG.setGraphicMode(graphicMode);
        mBHide = (ToggleButton) findViewById(R.id.BHideMemory);
        mBHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                graphicMode = graphicMode == C.graphicModeShowMemory ? C.graphicModeHideMemory : C.graphicModeShowMemory;
                mPrefs.edit().putInt(C.graphicMode, graphicMode).commit();
                mVG.setGraphicMode(graphicMode);
                mBHide.setChecked(graphicMode == C.graphicModeShowMemory ? false : true);
                mHandlerVG.post(drawRunnableGraphic);
            }
        });
        mBHide.setChecked(graphicMode == C.graphicModeShowMemory ? false : true);

        processesMode = mPrefs.getInt(C.processesMode, C.processesModeShowCPU);
        mVG.setProcessesMode(processesMode);
        mBMemory = (Button) findViewById(R.id.BMemory);
        mBMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                processesMode = processesMode == C.processesModeShowCPU ? C.processesModeShowMemory : C.processesModeShowCPU;
                mPrefs.edit().putInt(C.processesMode, processesMode);
            }
        });

        mBMemory.setText(processesMode == 0 ? getString(R.string.w_main_memory) : getString(R.string.p_cpuusage));

        mLTopBar = (LinearLayout) findViewById(R.id.LTopBar);
        mLGraphicSurface = (FrameLayout) findViewById(R.id.LGraphicButton);

        if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            float sSW = res.getConfiguration().smallestScreenWidthDp;

            if (!ViewConfiguration.get(this).hasPermanentMenuKey() && !KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME) &&
                    (res.getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT || sSW > 560)) {
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

            };

            int paddingTop = mLTopBar.getPaddingTop();
            int paddingBottom = mLTopBar.getPaddingBottom();
            int paddingLeft = mLTopBar.getPaddingLeft();
            int paddingRight = mLTopBar.getPaddingRight();
            mLTopBar.setPadding(paddingLeft, paddingTop + statusBarHeight, paddingRight, paddingBottom);
        }

        if (mPrefs.getBoolean(C.welcome, true)) {
            mPrefs.edit().putLong(C.welcomeDate, Calendar.getInstance(TimeZone.getTimeZone(C.europeLondon)).getTimeInMillis()).commit();
            ViewStub v = (ViewStub) findViewById(R.id.VSWelcome);
            if (v != null) {
                mLWelcome = (LinearLayout) v.inflate();
                int bottomMargin = 0;
                if (Build.VERSION.SDK_INT >= 19) bottomMargin = navigationBarHeight;

                ((FrameLayout.LayoutParams) mLWelcome.getLayoutParams()).setMargins(0, 0, 0, (int)(35*sD) + bottomMargin);

                (mLWelcome.findViewById(R.id.BHint)).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPrefs.edit().putBoolean(C.welcome, false).commit();
                        mLWelcome.animate().setDuration(animDuration).setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                ((ViewManager) mLWelcome.getParent()).removeView(mLWelcome);
                                mLWelcome = null;
                            }
                        }).setStartDelay(0).alpha(0).translationYBy(-15*sD);
                    }
                });

                int aniDur = animDuration;
                int delayDUr = 500;
                if (orientationChanged) {
                    aniDur = 0;
                    delayDUr = 0;
                }
                mLWelcome.animate().setStartDelay(delayDUr).setDuration(aniDur).alpha(1).translationYBy(15*sD);
            }
        }

        mLMenu = (LinearLayout) getLayoutInflater().inflate(R.layout.layer_menu, null);
        mLMenu.setFocusableInTouchMode(true);

        mPWMenu = new PopupWindow(mLMenu, (int)(260*sD), WindowManager.LayoutParams.WRAP_CONTENT, true);


        /*Total cpu usage*/
        mTVCPUTotalP = (TextView) findViewById(R.id.TVCPUTotalP);
        mTVCPUAMP = (TextView) findViewById(R.id.TVCPUAMP);
        mTVMemoryAM = (TextView) findViewById(R.id.TVMemoryAM);

        mLSettings = (FrameLayout) findViewById(R.id.LSettings);
        mLSettings.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mLSettings.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                settingsHeight = mLSettings.getHeight();
                mLSettings.getLayoutParams().height = 0;
//				mIVSettingsBG.getLayoutParams().height = settingsHeight;
            }
        });
    }

    private void setTextLabelCPU(TextView absolute, TextView percent, List<Float> value, @SuppressWarnings("unchecked") List<Integer>... valueInteger) {
        if (valueInteger.length == 1) {
            percent.setText("100");
//            mTVMemoryAM.setVisibility(View.VISIBLE);
//            mTVMemoryAM.setText("200");
        } else if (!value.isEmpty()) {
            percent.setText(mFormatPercent.format(value.get(0)) + C.percent);
//            mTVMemoryAM.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean bindStatus = bindService(new Intent(this, ServiceReader.class), mServiceConnection, 0);
        registerReceiver(receiverSetIconRecord, new IntentFilter(C.actionSetIconRecord));
        registerReceiver(receiverDeadProcess, new IntentFilter(C.actionDeadProcess));
        registerReceiver(receiverFinish, new IntentFilter(C.actionFinishActivity));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.removeCallbacks(drawRunnable);
        mHandler.post(drawRunnable);
    }
}
