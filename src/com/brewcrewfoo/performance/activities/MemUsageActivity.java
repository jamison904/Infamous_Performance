package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.ListView;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Prop;
import com.brewcrewfoo.performance.util.PropAdapter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by h0rn3t on 10.02.2014.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
public class MemUsageActivity extends Activity implements  ActivityThemeChangeInterface,Constants {
    private boolean mIsLightTheme;
    SharedPreferences mPreferences;
    private ListView packList;
    private List<Prop> props = new ArrayList<Prop>();
    private PropAdapter adapter;
    private CurThread mCurThread;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.mem_usage);
        packList = (ListView) findViewById(R.id.memlist);
        readFile(MEM_INFO_PATH);
        //readFile(mCPUInfo, CPU_INFO_PATH);
        adapter = new PropAdapter(MemUsageActivity.this, R.layout.mem_item, props);
        packList.setAdapter(adapter);
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

    protected class CurThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(1200);
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
            readFile(MEM_INFO_PATH);
            adapter.notifyDataSetChanged();
        }
    };
    public void readFile(String fName) {
        props.clear();
        FileReader fr = null;
        try {
            fr = new FileReader(fName);
            BufferedReader br = new BufferedReader(fr);
            String line = br.readLine();
            while (null != line) {
                line=line.replace("\\s+","");
                props.add(new Prop(line.split(":")[0],line.split(":")[1]));
                line = br.readLine();
            }
        }
        catch (IOException ex) {
        }
        finally {
            if (null != fr) {
                try {
                    fr.close();
                }
                catch (IOException e) {
                }
            }
        }
    }
}
