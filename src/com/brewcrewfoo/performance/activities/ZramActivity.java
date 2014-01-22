package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 05.10.2013.
 */
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;
import java.text.NumberFormat;

import static java.lang.Integer.parseInt;

public class ZramActivity extends Activity implements Constants, SeekBar.OnSeekBarChangeListener, ActivityThemeChangeInterface {
    SharedPreferences mPreferences;
    private boolean mIsLightTheme;
    final Context context = this;
    private CurThread mCurThread;
    private TextView t1,t2,t3,t4,t5,tval1;
    private SeekBar mdisksize;
    private int ncpus=Helpers.getNumOfCpus();
    private int curcpu=0;
    private int curdisk=0;
    private float maxdisk = (Helpers.getTotMem() / 1024);
    private Button start_btn;
    private NumberFormat nf;
    private ProgressDialog progressDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.zram_settings);

        nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(2);

        curdisk=mPreferences.getInt(PREF_ZRAM,Math.round(maxdisk*18/100));

        mdisksize = (SeekBar) findViewById(R.id.val1);
        mdisksize.setOnSeekBarChangeListener(this);
        mdisksize.setMax(100);
        final int percent=Math.round(curdisk * 100 / maxdisk);
        mdisksize.setProgress(percent);
        tval1=(TextView)findViewById(R.id.tval1);
        tval1.setText(getString(R.string.zram_disk_size,Helpers.ReadableByteCount(curdisk*1024*1024))+" ("+String.valueOf(percent)+"%)");

        t1=(TextView)findViewById(R.id.t1);
        t2=(TextView)findViewById(R.id.t2);
        t3=(TextView)findViewById(R.id.t3);
        t4=(TextView)findViewById(R.id.t4);
        t5=(TextView)findViewById(R.id.t5);

        set_values();

        start_btn=(Button) findViewById(R.id.apply);
        start_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (is_zram_on()) {
                    new StopZramOperation().execute();
                }
                else {
                    new StartZramOperation().execute();
                }
            }
        });
        LinearLayout prev=(LinearLayout) findViewById(R.id.preview);

        prev.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                curcpu=mod(curcpu+1,ncpus);
                Toast.makeText(context, "CPU "+(curcpu+1), Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        if (is_zram_on()) {
            start_btn.setText(getString(R.string.mt_stop));
            mdisksize.setEnabled(false);
        }
        else {
            start_btn.setText(getString(R.string.mt_start));
            mdisksize.setEnabled(true);
        }
        if (mCurThread == null) {
            mCurThread = new CurThread();
            mCurThread.start();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            final int mb=Math.round(progress*maxdisk/100);
            tval1.setText(getString(R.string.zram_disk_size,Helpers.ReadableByteCount(mb*1024*1024))+" ("+String.valueOf(progress)+"%)");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        //setDiskSize(seekBar.getProgress());
        if(seekBar.getProgress()==0){
            seekBar.setProgress(18);
        }
        curdisk=Math.round(seekBar.getProgress()*maxdisk/100);
        tval1.setText(getString(R.string.zram_disk_size,Helpers.ReadableByteCount(curdisk*1024*1024))+" ("+String.valueOf(seekBar.getProgress())+"%)");

        mPreferences.edit().putInt(PREF_ZRAM,curdisk).apply();
        //curdisk=seekBar.getProgress();
        Intent returnIntent = new Intent();
        returnIntent.putExtra("result",2);
        setResult(RESULT_OK,returnIntent);
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
            //final String v=(String) msg.obj;
            set_values();
        }
    };
    public boolean is_zram_on(){
        CMDProcessor.CommandResult cr=new CMDProcessor().sh.runWaitFor("busybox echo `busybox cat /proc/swaps | busybox grep zram`");
        return (cr.success() && !cr.stdout.equals(""));
    }

    private int getDiskSize() {
        return parseInt(Helpers.readOneLine(ZRAM_SIZE_PATH.replace("zram0","zram"+curcpu)));
    }

    private int getCompressedDataSize(){
        return parseInt(Helpers.readOneLine(ZRAM_COMPR_PATH.replace("zram0","zram"+curcpu)));
    }

    private int getOriginalDataSize(){
        return parseInt(Helpers.readOneLine(ZRAM_ORIG_PATH.replace("zram0","zram"+curcpu)));
    }

    public float getCompressionRatio() {
        if(getOriginalDataSize()==0) return 0;
        return (float) ((float)getCompressedDataSize() / (float)getOriginalDataSize());
    }

    public float getUsedRatio() {
        if(getDiskSize()==0) return 0;
        return (float) ((float)getOriginalDataSize() / (float)getDiskSize());
    }

    public void setDiskSize(long v){
        v=(long)(v/ncpus);
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ncpus; i++) {
            sb.append("busybox echo ").append(String.valueOf(v * 1024 * 1024)).append(" > ").append(ZRAM_SIZE_PATH.replace("zram0", "zram" + i));
        }
        Helpers.shExec(sb,context,true);
    }


    private class StopZramOperation extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            final StringBuilder sb = new StringBuilder();
            sb.append("zramstop ").append(ncpus).append(";\n");
            Helpers.shExec(sb, context, true);
            return "";
        }
        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (is_zram_on()) {
                start_btn.setText(getString(R.string.mt_stop));
                mdisksize.setEnabled(false);
                mPreferences.edit().putBoolean(ZRAM_ON,true).apply();
            }
            else {
                start_btn.setText(getString(R.string.mt_start));
                mdisksize.setEnabled(true);
                mPreferences.edit().putBoolean(ZRAM_ON,false).apply();
            }
        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ZramActivity.this, null, getString(R.string.wait));

        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    private class StartZramOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            long v=(long)(curdisk/ncpus)*1024*1024;
            final StringBuilder sb = new StringBuilder();
            sb.append("zramstart \"").append(ncpus).append("\" \"").append(v).append("\";\n");
            Helpers.shExec(sb,context,true);
            return "";
        }
        @Override
        protected void onPostExecute(String result) {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            if (is_zram_on()) {
                start_btn.setText(getString(R.string.mt_stop));
                mdisksize.setEnabled(false);
                mPreferences.edit().putBoolean(ZRAM_ON,true).apply();
                if (mCurThread == null) {
                    mCurThread = new CurThread();
                    mCurThread.start();
                }
            }
            else {
                start_btn.setText(getString(R.string.mt_start));
                mdisksize.setEnabled(true);
                mPreferences.edit().putBoolean(ZRAM_ON,false).apply();
            }

        }
        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ZramActivity.this, null, getString(R.string.wait));
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    private int mod(int x, int y){
        int result = x % y;
        return result < 0? result + y : result;
    }
    public void set_values(){
        t1.setText(Helpers.ReadableByteCount(0));
        t2.setText("0");
        t3.setText(Helpers.ReadableByteCount(0));
        t4.setText("0");
        t5.setText(Helpers.ReadableByteCount(0));
        Boolean ist1 = false;
        Boolean ist3 = false;
        Boolean ist5 = false;
        if(new File(ZRAM_COMPR_PATH.replace("zram0","zram"+curcpu)).exists()){
            t1.setText(Helpers.ReadableByteCount(parseInt(Helpers.readOneLine(ZRAM_COMPR_PATH.replace("zram0","zram"+curcpu)))));
            ist1 =true;
        }
        if(new File(ZRAM_ORIG_PATH.replace("zram0","zram"+curcpu)).exists()){
            t3.setText(Helpers.ReadableByteCount(parseInt(Helpers.readOneLine(ZRAM_ORIG_PATH.replace("zram0","zram"+curcpu)))));
            ist3 =true;
        }
        if(new File(ZRAM_MEMTOT_PATH.replace("zram0","zram"+curcpu)).exists()){
            t5.setText(Helpers.ReadableByteCount(parseInt(Helpers.readOneLine(ZRAM_MEMTOT_PATH.replace("zram0","zram"+curcpu)))));
            ist5 =true;
        }
        if(ist1 && ist3 && ist5){
            t2.setText(nf.format(getCompressionRatio()));
            t4.setText(nf.format(getUsedRatio()));
        }

    }
}
