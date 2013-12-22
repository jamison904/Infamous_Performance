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
import com.brewcrewfoo.performance.activities.MainActivity;
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
    private CurCPUThread mCurCPUThread;
    SharedPreferences mPreferences;
    private boolean mIsTegra3 = false;
    private boolean mIsDynFreq = false;
    private static final int NEW_MENU_ID=Menu.FIRST+1;
    private Context context;
    private final String supported[]={"ondemand","ondemandplus","lulzactive","lulzactiveW","interactive","hyper","conservative"};

    private int nCpus=0;
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
            MainActivity.curcpu=savedInstanceState.getInt("curcpu");
            MainActivity.mMaxFreqSetting=savedInstanceState.getString("maxfreq");
            MainActivity.mMinFreqSetting=savedInstanceState.getString("minfreq");
            MainActivity.mCurGovernor=savedInstanceState.getString("governor");
            MainActivity.mCurIO=savedInstanceState.getString("io");
            MainActivity.mCPUon=savedInstanceState.getString("cpuon");
        }
        else{
            getCPUval(MainActivity.curcpu);
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpu_settings, root, false);

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();
        mIsDynFreq = new File(DYN_MAX_FREQ_PATH).exists() && new File(DYN_MIN_FREQ_PATH).exists();

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
                if(nCpus==1) return;
                if(MainActivity.curcpu>=(nCpus-1)) MainActivity.curcpu=0;
                else  MainActivity.curcpu++;
                getCPUval(MainActivity.curcpu);
                setCPUval(MainActivity.curcpu);
            }
        });

        mCurFreq.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                if(new File(CPU_ON_PATH.replace("cpu0","cpu"+MainActivity.curcpu)).exists() && MainActivity.curcpu>0){
                    final StringBuilder sb = new StringBuilder();
                    sb.append("busybox chmod 644 ").append(CPU_ON_PATH.replace("cpu0", "cpu" + MainActivity.curcpu)).append(";\n");
                    if(MainActivity.mCPUon.equals("1")){
                        sb.append("busybox echo \"0\" > ").append(CPU_ON_PATH.replace("cpu0", "cpu" + MainActivity.curcpu)).append(";\n");
                        MainActivity.mCPUon="0";
                    }
                    else{
                        sb.append("busybox echo \"1\" > ").append(CPU_ON_PATH.replace("cpu0", "cpu" + MainActivity.curcpu)).append(";\n");
                        MainActivity.mCPUon="1";
                    }
                    sb.append("busybox chmod 444 ").append(CPU_ON_PATH.replace("cpu0", "cpu" + MainActivity.curcpu)).append(";\n");
                    Helpers.shExec(sb,context,true);

                    setCPUval(MainActivity.curcpu);
                }

                return true;
            }
        });


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
        mGovernor.setSelection(Arrays.asList(mAvailableGovernors).indexOf(MainActivity.mCurGovernor));
        mGovernor.post(new Runnable() {
            public void run() {
                mGovernor.setOnItemSelectedListener(new GovListener());
            }
        });


        String[] mAvailableIo = Helpers.getAvailableIOSchedulers();
        mIo = (Spinner) view.findViewById(R.id.pref_io);

        ArrayAdapter<CharSequence> ioAdapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item);
        ioAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String aMAvailableIo : mAvailableIo) {
            ioAdapter.add(aMAvailableIo);
        }
        mIo.setAdapter(ioAdapter);
        mIo.setSelection(Arrays.asList(mAvailableIo).indexOf(MainActivity.mCurIO));
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
                        final String r=Helpers.readCPU(context,i);
                        if(v!=null){
                            editor.putString(PREF_MIN_CPU+i, r.split(":")[0]);
                            editor.putString(PREF_MAX_CPU+i, r.split(":")[1]);
                            editor.putString(PREF_GOV, r.split(":")[2]);
                            editor.putString(PREF_IO, r.split(":")[3]);
                            editor.putString("cpuon" + i, r.split(":")[4]);
                        }
                    }
                }
                editor.commit();
            }
        });

        setCPUval(MainActivity.curcpu);

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
                for (String aSupported : supported) {
                    if (aSupported.equals(MainActivity.mCurGovernor)) {
                        intent = new Intent(context, GovSetActivity.class);
                        intent.putExtra("cpu", Integer.toString(MainActivity.curcpu));
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
        final StringBuilder sb = new StringBuilder();
        if (seekBar.getId() == R.id.max_slider){
            sb.append("busybox echo ").append(MainActivity.mMaxFreqSetting).append(" > ").append(MAX_FREQ_PATH.replace("cpu0", "cpu" + MainActivity.curcpu)).append(";\n");
            if (mIsDynFreq) {
                sb.append("busybox echo ").append(MainActivity.mMaxFreqSetting).append(" > ").append(DYN_MAX_FREQ_PATH).append(";\n");
            }
            if (mIsTegra3) {
                sb.append("busybox echo ").append(MainActivity.mMaxFreqSetting).append(" > ").append(TEGRA_MAX_FREQ_PATH).append(";\n");
            }
        }
        else if(seekBar.getId() == R.id.min_slider){
            sb.append("busybox echo ").append(MainActivity.mMinFreqSetting).append(" > ").append(MIN_FREQ_PATH.replace("cpu0", "cpu" + MainActivity.curcpu)).append(";\n");
            if (mIsDynFreq) {
                sb.append("busybox echo ").append(MainActivity.mMinFreqSetting).append(" > ").append(DYN_MIN_FREQ_PATH).append(";\n");
            }
        }
        Helpers.shExec(sb,context,true);
        Helpers.updateAppWidget(context);
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
            MainActivity.mCurGovernor=selected;
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
            MainActivity.mCurIO=selected;
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putInt("curcpu",MainActivity.curcpu);
        saveState.putString("maxfreq",MainActivity.mMaxFreqSetting);
        saveState.putString("minfreq",MainActivity.mMinFreqSetting);
        saveState.putString("governor",MainActivity.mCurGovernor);
        saveState.putString("io",MainActivity.mCurIO);
        saveState.putString("cpuon",MainActivity.mCPUon);
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
        //Helpers.updateAppWidget(context);
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
            MainActivity.mMinFreqSetting = current;
        }
        mMaxSpeedText.setText(Helpers.toMHz(current));
        MainActivity.mMaxFreqSetting = current;
        updateSharedPrefs(PREF_MAX_CPU+MainActivity.curcpu, current);
    }

    public void setMinSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeedText.setText(Helpers.toMHz(current));
            MainActivity.mMaxFreqSetting = current;
        }
        mMinSpeedText.setText(Helpers.toMHz(current));
        MainActivity.mMinFreqSetting = current;
        updateSharedPrefs(PREF_MIN_CPU+MainActivity.curcpu, current);
    }



    public void setCPUval(int i){
        mMaxSpeedText.setText(Helpers.toMHz(MainActivity.mMaxFreqSetting));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(MainActivity.mMaxFreqSetting));

        mMinSpeedText.setText(Helpers.toMHz(MainActivity.mMinFreqSetting));
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(MainActivity.mMinFreqSetting));

        mCurCpu.setText(Integer.toString(i+1));
        mCurCpu.setTextColor(res.getColor(R.color.pc_blue));

        if(mIsDynFreq && MainActivity.curcpu>0){
            mMaxSlider.setEnabled(false);
            mMinSlider.setEnabled(false);
        }
        else{
            mMaxSlider.setEnabled(true);
            mMinSlider.setEnabled(true);
        }
        if(MainActivity.mCPUon.equals("0")){
             mCurCpu.setTextColor(res.getColor(R.color.pc_red));
             mMaxSlider.setEnabled(false);
             mMinSlider.setEnabled(false);
        }
        mPreferences.edit().putString("cpuon" + MainActivity.curcpu, MainActivity.mCPUon).apply();
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
                    if(new File(CUR_CPU_PATH.replace("cpu0","cpu"+MainActivity.curcpu)).exists()){
                        curFreq = Helpers.readOneLine(CUR_CPU_PATH.replace("cpu0","cpu"+MainActivity.curcpu));
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
        Helpers.updateAppWidget(context);
    }

    private void getCPUval(int i){
        final String v=Helpers.readCPU(context,i);
        if(v!=null){
            MainActivity.mMinFreqSetting=v.split(":")[0];
            MainActivity.mMaxFreqSetting=v.split(":")[1];
            MainActivity.mCurGovernor=v.split(":")[2];
            MainActivity.mCurIO=v.split(":")[3];
            MainActivity.mCPUon=v.split(":")[4];
        }
    }
}

