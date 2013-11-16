/*
 * Performance Control - An Android CPU Control application Copyright (C) 2012
 * James Roberts
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.brewcrewfoo.performance.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.GovSetActivity;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class CPUSettings extends Fragment implements SeekBar.OnSeekBarChangeListener, Constants {

    private SeekBar mMaxSlider;
    private SeekBar mMinSlider;
    private Spinner mGovernor;
    private Spinner mIo;
    private TextView mCurFreq;
    private TextView mMaxSpeedText;
    private TextView mMinSpeedText;
    private String[] mAvailableFrequencies;
    private String mMaxFreqSetting;
    private String mMinFreqSetting;
    private CurCPUThread mCurCPUThread;
    SharedPreferences mPreferences;
    private boolean mIsTegra3 = false;
    private boolean mIsDynFreq = false;
    private static final int NEW_MENU_ID=Menu.FIRST+1;
    private Context context;
    private final String supported[]={"ondemand","ondemandplus","lulzactive","lulzactiveW","interactive","hyper","conservative"};
    private int curcpu=0;
    private boolean curon=true;
    private int nCpus;
    private TextView mCurCpu;
    private String[] mAvailableGovernors;
    private Resources res;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getActivity();
        res=getResources();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        nCpus=Helpers.getNumOfCpus();
        if(savedInstanceState!=null) {
            curcpu=savedInstanceState.getInt("curcpu");
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpu_settings, root, false);

        LinearLayout mpdlayout=(LinearLayout) view.findViewById(R.id.mpd);

        Switch mpdsw = (Switch) view.findViewById(R.id.mpd_switch);
        if(!Helpers.binExist("mpdecision").equals(NOT_FOUND)){
            mpdlayout.setVisibility(LinearLayout.VISIBLE);
            Boolean mpdon = Helpers.moduleActive("mpdecision");
            mpdsw.setChecked(mpdon);
            mPreferences.edit().putBoolean("mpdecision",mpdon).apply();

            mpdsw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton v, boolean checked) {
                    if (checked) {
                        new CMDProcessor().su.runWaitFor("stop mpdecision");
                        mPreferences.edit().putBoolean("mpdecision",false).apply();
                    }
                    else{
                        new CMDProcessor().su.runWaitFor("start mpdecision");
                        mPreferences.edit().putBoolean("mpdecision",true).apply();
                    }

                }
            });
        }

        mCurCpu = (TextView) view.findViewById(R.id.curcpu);
        mCurFreq = (TextView) view.findViewById(R.id.current_speed);
        mCurFreq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(curcpu>=(nCpus-1)) curcpu=0;
                else curcpu++;
                getCPUval(curcpu);
            }
        });

        mCurFreq.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                if(new File(CPU_ON_PATH.replace("cpu0","cpu"+curcpu)).exists() && curcpu>0){
                    final StringBuilder sb = new StringBuilder();
                    sb.append("busybox chmod 644 ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu));
                    if(curon){
                        sb.append("busybox echo \"0\" > ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu));
                    }
                    else{
                        sb.append("busybox echo \"1\" > ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu));
                    }
                    sb.append("busybox chmod 444 ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu));
                    Helpers.shExec(sb,context,true);

                }
                getCPUval(curcpu);
                return true;
            }
        });

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();
        mIsDynFreq = new File(DYN_MAX_FREQ_PATH).exists() && new File(DYN_MIN_FREQ_PATH).exists();
        mAvailableFrequencies = new String[0];
        mAvailableGovernors = Helpers.readOneLine(GOVERNORS_LIST_PATH).split(" ");

        String availableFrequenciesLine = Helpers.readOneLine(STEPS_PATH);
        if (availableFrequenciesLine != null) {
            mAvailableFrequencies = availableFrequenciesLine.split(" ");
            Arrays.sort(mAvailableFrequencies, new Comparator<String>() {
                @Override
                public int compare(String object1, String object2) {
                    return Integer.valueOf(object1).compareTo(Integer.valueOf(object2));
                }
            });
        }


        int mFrequenciesNum = mAvailableFrequencies.length - 1;

        String[] mAvailableIo = Helpers.getAvailableIOSchedulers();
        String mCurrentIo = Helpers.getIOScheduler();

        mMaxSlider = (SeekBar) view.findViewById(R.id.max_slider);
        mMaxSlider.setMax(mFrequenciesNum);
        mMaxSlider.setOnSeekBarChangeListener(this);
        mMaxSpeedText = (TextView) view.findViewById(R.id.max_speed_text);

        mMinSlider = (SeekBar) view.findViewById(R.id.min_slider);
        mMinSlider.setMax(mFrequenciesNum);
        mMinSlider.setOnSeekBarChangeListener(this);
        mMinSpeedText = (TextView) view.findViewById(R.id.min_speed_text);



        mGovernor = (Spinner) view.findViewById(R.id.pref_governor);
        ArrayAdapter<CharSequence> governorAdapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String mAvailableGovernor : mAvailableGovernors) {
            governorAdapter.add(mAvailableGovernor);
        }
        mGovernor.setAdapter(governorAdapter);
        mGovernor.post(new Runnable() {
            public void run() {
                mGovernor.setOnItemSelectedListener(new GovListener());
            }
        });

        mIo = (Spinner) view.findViewById(R.id.pref_io);
        ArrayAdapter<CharSequence> ioAdapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item);
        ioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String aMAvailableIo : mAvailableIo) {
            ioAdapter.add(aMAvailableIo);
        }
        mIo.setAdapter(ioAdapter);
        mIo.setSelection(Arrays.asList(mAvailableIo).indexOf(mCurrentIo));
        mIo.post(new Runnable() {
            public void run() {
                mIo.setOnItemSelectedListener(new IOListener());
            }
        });

        Switch mSetOnBoot = (Switch) view.findViewById(R.id.cpu_sob);
        mSetOnBoot.setChecked(mPreferences.getBoolean(CPU_SOB, false));
        mSetOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton v, boolean checked) {
                final SharedPreferences.Editor editor = mPreferences.edit();
                editor.putBoolean(CPU_SOB, checked);
                if (checked) {
                    for (int i = 0; i < nCpus; i++){
                        editor.putString(PREF_MIN_CPU+i, getMinSpeed(i));
                        editor.putString(PREF_MAX_CPU+i, getMaxSpeed(i));

                    }
                    editor.putString(PREF_GOV, Helpers.readOneLine(GOVERNOR_PATH));
                    editor.putString(PREF_IO, Helpers.getIOScheduler());
                }
                editor.commit();
            }
        });

        getCPUval(curcpu);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_settings_menu, menu);
        Helpers.addItems2Menu(menu,NEW_MENU_ID,getString(R.string.menu_tab),(ViewPager) getView().getParent());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Helpers.removeCurItem(item,NEW_MENU_ID,(ViewPager) getView().getParent());
        switch(item.getItemId()){
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
            case R.id.gov_settings:
                for(byte i=0;i<supported.length;i++){
                    if(supported[i].equals(Helpers.readOneLine(GOVERNOR_PATH))){
                        intent = new Intent(context, GovSetActivity.class);
                        intent.putExtra("cpu",Integer.toString(curcpu));
                        startActivity(intent);
                        break;
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
            if (seekBar.getId() == R.id.max_slider) {
                setMaxSpeed(seekBar, progress);
            }
            else if (seekBar.getId() == R.id.min_slider) {
                setMinSpeed(seekBar, progress);
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        // we have a break now, write the values..
        final StringBuilder sb = new StringBuilder();
        if (seekBar.getId() == R.id.max_slider){
            sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ").append(MAX_FREQ_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
            if (mIsDynFreq) {
                sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ").append(DYN_MAX_FREQ_PATH).append(";\n");
            }
            if (mIsTegra3) {
                sb.append("busybox echo ").append(mMaxFreqSetting).append(" > ").append(TEGRA_MAX_FREQ_PATH).append(";\n");
            }
        }
        else if(seekBar.getId() == R.id.min_slider){
            sb.append("busybox echo ").append(mMinFreqSetting).append(" > ").append(MIN_FREQ_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
            if (mIsDynFreq) {
                sb.append("busybox echo ").append(mMinFreqSetting).append(" > ").append(DYN_MIN_FREQ_PATH).append(";\n");
            }
        }

        Helpers.shExec(sb,context,true);
    }

    public class GovListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            final StringBuilder sb = new StringBuilder();
            String selected = parent.getItemAtPosition(pos).toString();
            for (int i = 0; i < nCpus; i++){
                sb.append("busybox echo ").append(selected).append(" > ").append(GOVERNOR_PATH.replace("cpu0", "cpu" + i)).append(";\n");
            }
            updateSharedPrefs(PREF_GOV, selected);
            // reset gov settings
            mPreferences.edit().remove(GOV_SETTINGS).remove(GOV_NAME).apply();
            Helpers.shExec(sb,context,true);
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }

    public class IOListener implements OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int pos,long id) {
            String selected = parent.getItemAtPosition(pos).toString();
			final StringBuilder sb = new StringBuilder();
			for(byte i=0; i<2; i++){
                if (new File(IO_SCHEDULER_PATH.replace("mmcblk0","mmcblk"+i)).exists())
				    sb.append("busybox echo ").append(selected).append(" > ").append(IO_SCHEDULER_PATH.replace("mmcblk0","mmcblk"+i)).append(";\n");
			}
			Helpers.shExec(sb,context,true);
            updateSharedPrefs(PREF_IO, selected);
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putInt("curcpu",curcpu);
    }
    @Override
    public void onResume() {
        if (mCurCPUThread == null) {
            mCurCPUThread = new CurCPUThread();
            mCurCPUThread.start();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        Helpers.updateAppWidget(context);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (mCurCPUThread != null) {
            if (mCurCPUThread.isAlive()) {
                mCurCPUThread.interrupt();
                try {
                    mCurCPUThread.join();
                }
                catch (InterruptedException e) {
                }
            }
        }
        super.onDestroy();
    }

    public void setMaxSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int minSliderProgress = mMinSlider.getProgress();
        if (progress <= minSliderProgress) {
            mMinSlider.setProgress(progress);
            mMinSpeedText.setText(Helpers.toMHz(current));
            mMinFreqSetting = current;
        }
        mMaxSpeedText.setText(Helpers.toMHz(current));
        mMaxFreqSetting = current;
        updateSharedPrefs(PREF_MAX_CPU+curcpu, current);
    }

    public void setMinSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeedText.setText(Helpers.toMHz(current));
            mMaxFreqSetting = current;
        }
        mMinSpeedText.setText(Helpers.toMHz(current));
        mMinFreqSetting = current;
        updateSharedPrefs(PREF_MIN_CPU+curcpu, current);
    }

    public String getMaxSpeed(int i){
        if (mIsTegra3) {
            String curTegraMaxSpeed = Helpers.readOneLine(TEGRA_MAX_FREQ_PATH);
            int curTegraMax = 0;
            try {
                curTegraMax = Integer.parseInt(curTegraMaxSpeed);
                if (curTegraMax > 0) {
                    return Integer.toString(curTegraMax);
                }
            }
            catch (NumberFormatException ex) {
                return "0";
            }
        }
        else{
            if(new File(DYN_MAX_FREQ_PATH).exists()){
                return Helpers.readOneLine(DYN_MAX_FREQ_PATH);
            }
            else{
                return Helpers.readOneLine(MAX_FREQ_PATH.replace("cpu0","cpu"+i));
            }
        }
        return "0";
    }
    public String getMinSpeed(int i){
        String mCurMinSpeed;
        if(new File(DYN_MIN_FREQ_PATH).exists()){
            mCurMinSpeed = Helpers.readOneLine(DYN_MIN_FREQ_PATH);
        }
        else{
            mCurMinSpeed = Helpers.readOneLine(MIN_FREQ_PATH.replace("cpu0","cpu"+i));
        }
        return mCurMinSpeed;
    }

    public void getCPUval(int i){
        final String mCurMaxSpeed=getMaxSpeed(i);
        mMaxSpeedText.setText(Helpers.toMHz(mCurMaxSpeed));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mCurMaxSpeed));
        mMaxFreqSetting=mCurMaxSpeed;
        mMaxSlider.setEnabled(true);

        final String mCurMinSpeed=getMinSpeed(i);
        mMinSpeedText.setText(Helpers.toMHz(mCurMinSpeed));
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mCurMinSpeed));
        mMinSlider.setEnabled(true);
        mMinFreqSetting=mCurMinSpeed;

        String mCurrentGovernor = Helpers.readOneLine(GOVERNOR_PATH);
        mGovernor.setSelection(Arrays.asList(mAvailableGovernors).indexOf(mCurrentGovernor));

        mCurCpu.setText(Integer.toString(i+1));
        mCurCpu.setTextColor(res.getColor(R.color.pc_blue));
        curon=true;

        if((new File(CPU_ON_PATH.replace("cpu0","cpu"+i)).exists())){
            if(Helpers.readOneLine(CPU_ON_PATH.replace("cpu0","cpu"+i)).equals("0")){
                mCurCpu.setTextColor(res.getColor(R.color.pc_red));
                curon=false;
                mMaxSlider.setEnabled(false);
                mMinSlider.setEnabled(false);
            }
        }
        mPreferences.edit().putBoolean("cpuon"+curcpu,curon).apply();
    }

    protected class CurCPUThread extends Thread {
        private boolean mInterrupt = false;

        public void interrupt() {
            mInterrupt = true;
        }

        @Override
        public void run() {
            try {
                while (!mInterrupt) {
                    sleep(500);
                    String curFreq="0";
                    if(new File(CUR_CPU_PATH.replace("cpu0","cpu"+curcpu)).exists()){
                        curFreq = Helpers.readOneLine(CUR_CPU_PATH.replace("cpu0","cpu"+curcpu));
                    }
                    mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0,curFreq));
                }
            }
            catch (InterruptedException e) {
                //return;
            }
        }
    }


    protected Handler mCurCPUHandler = new Handler() {
        public void handleMessage(Message msg) {
        mCurFreq.setText(Helpers.toMHz((String) msg.obj));
        }
    };

    private void updateSharedPrefs(String var, String value) {
        final SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(var, value).commit();
    }

}

