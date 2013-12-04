package com.brewcrewfoo.performance.activities;
/**
 * Created by h0rn3t on 11.09.2013.
 */
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class KSMActivity extends Activity implements Constants, SeekBar.OnSeekBarChangeListener, ActivityThemeChangeInterface {
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    final Context context = this;
    private TextView t1,t2,t3,t4,t5;
    private CurThread mCurThread;
    private Boolean ist1=false;
    private Boolean ist2=false;
    private Boolean ist3=false;
    private Boolean ist4=false;
    private Boolean ist5=false;
    private EditText edit1;
    private EditText edit2;
    private SeekBar mPage2Scan;
    private SeekBar mSleep;
    final private int maxPages2Scan=2048;
    final private int maxSleep=5000;
    private int ksm=0;
    private String ksmpath=KSM_RUN_PATH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.ksm_settings);
        t1=(TextView)findViewById(R.id.t2);
        t2=(TextView)findViewById(R.id.t4);
        t3=(TextView)findViewById(R.id.t6);
        t4=(TextView)findViewById(R.id.t8);
        t5=(TextView)findViewById(R.id.t10);
        if(new File(UKSM_RUN_PATH).exists()){
            ksm=1;
            ksmpath=UKSM_RUN_PATH;
        }
        if (new File(KSM_PAGESSHARED_PATH[ksm]).exists()) {
            t1.setText(Helpers.readOneLine(KSM_PAGESSHARED_PATH[ksm]));
            ist1=true;
        }
        else{
            LinearLayout relativeLayout = (LinearLayout) findViewById(R.id.relativeLayout1);
            relativeLayout.setVisibility(LinearLayout.GONE);
        }

        if (new File(KSM_PAGESSHARED_PATH[ksm]).exists()) {
            t2.setText(Helpers.readOneLine(KSM_PAGESUNSHERED_PATH[ksm]));
            ist2=true;
        }
        else{
            LinearLayout relativeLayout = (LinearLayout) findViewById(R.id.relativeLayout2);
            relativeLayout.setVisibility(LinearLayout.GONE);
        }
        if (new File(KSM_PAGESSHARING_PATH[ksm]).exists()) {
            t3.setText(Helpers.readOneLine(KSM_PAGESSHARING_PATH[ksm]));
            ist3=true;
        }
        else{
            LinearLayout relativeLayout = (LinearLayout) findViewById(R.id.relativeLayout3);
            relativeLayout.setVisibility(LinearLayout.GONE);
        }
        if (new File(KSM_PAGESVOLATILE_PATH[ksm]).exists()) {
            t4.setText(Helpers.readOneLine(KSM_PAGESVOLATILE_PATH[ksm]));
            ist4=true;
        }
        else{
            LinearLayout relativeLayout = (LinearLayout) findViewById(R.id.relativeLayout4);
            relativeLayout.setVisibility(LinearLayout.GONE);
        }
        if (new File(KSM_FULLSCANS_PATH[ksm]).exists()) {
            t5.setText(Helpers.readOneLine(KSM_FULLSCANS_PATH[ksm]));
            ist5=true;
        }
        else{
            LinearLayout relativeLayout = (LinearLayout) findViewById(R.id.relativeLayout5);
            relativeLayout.setVisibility(LinearLayout.GONE);
        }

        final int v1=Integer.parseInt(Helpers.readOneLine(KSM_PAGESTOSCAN_PATH[ksm]));
        final int v2=Integer.parseInt(Helpers.readOneLine(KSM_SLEEP_PATH[ksm]));

        TextView tval1 = (TextView) findViewById(R.id.tval1);
        tval1.setText(getString(R.string.ksm_pagtoscan));

        TextView tval2 = (TextView) findViewById(R.id.tval2);
        tval2.setText(getString(R.string.ksm_sleep));

        mPage2Scan = (SeekBar) findViewById(R.id.val1);
        edit1=(EditText) findViewById(R.id.edit1);
        mPage2Scan.setOnSeekBarChangeListener(this);
        mPage2Scan.setMax(maxPages2Scan);
        mPage2Scan.setProgress(v1);
        edit1.setText(String.valueOf(v1));
        edit1.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) { }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
                final String text = edit1.getText().toString();
                try {
                    int value = Integer.parseInt(text);
                    if(value>maxPages2Scan){
                        value=maxPages2Scan;
                        edit1.setText(String.valueOf(value));
                    }
                    mPage2Scan.setProgress(value);
                }
                catch (NumberFormatException nfe) {
                    return;
                }
            }
        });

        mSleep = (SeekBar) findViewById(R.id.val2);
        edit2=(EditText) findViewById(R.id.edit2);
        mSleep.setOnSeekBarChangeListener(this);
        mSleep.setMax(maxSleep);
        mSleep.setProgress(v2);
        edit2.setText(String.valueOf(v2));
        edit2.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}

            @Override
            public void onTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {
                final String text = edit2.getText().toString();
                try {
                    int value = Integer.parseInt(text);
                    if(value>maxSleep){
                        value=maxSleep;
                        edit2.setText(String.valueOf(value));
                    }
                    mSleep.setProgress(value);

                }
                catch (NumberFormatException nfe) {
                    return;
                }
            }
        });
        ((Button) findViewById(R.id.apply)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                int i1,i2;
                try {
                    i1 = Integer.parseInt(edit1.getText().toString());
                    i2 = Integer.parseInt(edit2.getText().toString());
                }
                catch (NumberFormatException e) {
                    return;
                }

                new CMDProcessor().su.runWaitFor("busybox echo " + i1 + " > " + KSM_PAGESTOSCAN_PATH[ksm]);
                new CMDProcessor().su.runWaitFor("busybox echo " + i2 + " > " + KSM_SLEEP_PATH[ksm]);
                mPreferences.edit()
                        .putString("pref_ksm_pagetoscan", String.valueOf(i1))
                        .putString("pref_ksm_sleep", String.valueOf(i2))
                        .commit();
                Intent returnIntent = new Intent();
                returnIntent.putExtra("result",1);
                setResult(RESULT_OK,returnIntent);
                finish();
            }
        });
        ((Button) findViewById(R.id.rst)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Runnable runnable = new Runnable() {
                    public void run() {
                        final String vlast=Helpers.readOneLine(ksmpath);
                        final StringBuilder sb = new StringBuilder();
                        sb.append("busybox echo 0 > ").append(ksmpath).append(";\n").append("sleep 0.5;\n");
                        sb.append("busybox echo 2 > ").append(ksmpath).append(";\n").append("sleep 0.5;\n");
                        sb.append("busybox echo ").append(vlast).append(" > ").append(ksmpath).append(";\n");
                        Helpers.shExec(sb,context,true);

                    }
                };
                new Thread(runnable).start();
            }
        });
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.val1) {
                edit1.setText(String.valueOf(progress));
            }
            else if (seekBar.getId() == R.id.val2) {
                edit2.setText(String.valueOf(progress));
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onResume() {
        if (mCurThread == null) {
            mCurThread = new CurThread();
            mCurThread.start();
        }
        super.onResume();
    }
    @Override
    public void onDestroy() {
        if (mCurThread != null) {
            if (mCurThread.isAlive()) {
                mCurThread.interrupt();
                try {
                    mCurThread.join();
                }
                catch (InterruptedException e) {
                }
            }
        }
        super.onDestroy();
    }
    @Override
    public boolean isThemeChanged() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        return is_light_theme != mIsLightTheme;
    }

    @Override
    public void setTheme() {
        final boolean is_light_theme = mPreferences.getBoolean(PREF_USE_LIGHT_THEME, false);
        mIsLightTheme = is_light_theme;
        setTheme(is_light_theme ? R.style.Theme_Light : R.style.Theme_Dark);
    }

    protected class CurThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(800);
                    mCurHandler.sendMessage(mCurHandler.obtainMessage(0,null));
                }
            }
            catch (InterruptedException e) {
                //return;
            }
        }
    }
    protected Handler mCurHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (ist1) t1.setText(Helpers.readOneLine(KSM_PAGESSHARED_PATH[ksm]));
            if (ist2) t2.setText(Helpers.readOneLine(KSM_PAGESUNSHERED_PATH[ksm]));
            if (ist3) t3.setText(Helpers.readOneLine(KSM_PAGESSHARING_PATH[ksm]));
            if (ist4) t4.setText(Helpers.readOneLine(KSM_PAGESVOLATILE_PATH[ksm]));
            if (ist5) t5.setText(Helpers.readOneLine(KSM_FULLSCANS_PATH[ksm]));
        }
    };


}
