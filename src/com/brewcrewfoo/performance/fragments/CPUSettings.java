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
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.GovSetActivity;
import com.brewcrewfoo.performance.activities.PCSettings;
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
    private Context context;
    private final String supported[]={"ondemand","ondemandplus","lulzactive","lulzactiveW","interactive","hyper","conservative","lionheart","adaptive","intellidemand"};

    private TextView mCurCpu;
    private Resources res;

    private final int nCpus=Helpers.getNumOfCpus();
    private String[] mCurGovernor=new String[nCpus];
    private String[] mCurIO=new String[nCpus];
    private String[] mMaxFreqSetting=new String[nCpus];
    private String[] mMinFreqSetting=new String[nCpus];
    private String[] mCPUon=new String[nCpus];
    private int curcpu=0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context=getActivity();
        res=getResources();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        if(savedInstanceState!=null) {
            curcpu=savedInstanceState.getInt("curcpu");
            mMaxFreqSetting[curcpu]=savedInstanceState.getString("maxfreq");
            mMinFreqSetting[curcpu]=savedInstanceState.getString("minfreq");
            mCurGovernor[curcpu]=savedInstanceState.getString("governor");
            mCurIO[curcpu]=savedInstanceState.getString("io");
            mCPUon[curcpu]=savedInstanceState.getString("cpuon");
        }
        else{
            getCPUval();
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup root, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cpu_settings, root, false);

        mIsTegra3 = new File(TEGRA_MAX_FREQ_PATH).exists();
        mIsDynFreq = new File(DYN_MAX_FREQ_PATH).exists() && new File(DYN_MIN_FREQ_PATH).exists();

        mCurCpu = (TextView) view.findViewById(R.id.curcpu);
        mCurFreq = (TextView) view.findViewById(R.id.current_speed);
        mCurFreq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(nCpus==1) return;
                if(curcpu>=(nCpus-1)) curcpu=0;
                else  curcpu++;
                getCPUval();
                setCPUval(curcpu);
            }
        });

        mCurFreq.setOnLongClickListener(new View.OnLongClickListener(){
            @Override
            public boolean onLongClick(View view) {
                if(new File(CPU_ON_PATH.replace("cpu0","cpu"+curcpu)).exists() && curcpu>0){
                    final StringBuilder sb = new StringBuilder();
                    sb.append("busybox chmod 644 ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
                    if(mCPUon[curcpu].equals("1")){
                        sb.append("busybox echo \"0\" > ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
                        mCPUon[curcpu]="0";
                    }
                    else{
                        sb.append("busybox echo \"1\" > ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
                        mCPUon[curcpu]="1";
                    }
                    sb.append("busybox chmod 444 ").append(CPU_ON_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
                    Helpers.shExec(sb,context,true);

                    setCPUval(curcpu);
                }

                return true;
            }
        });


        mAvailableFrequencies = new String[0];
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
        String[] mAvailableGovernors = Helpers.readOneLine(GOVERNORS_LIST_PATH).split(" ");
        ArrayAdapter<CharSequence> governorAdapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item);
        governorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        for (String mAvailableGovernor : mAvailableGovernors) {
            governorAdapter.add(mAvailableGovernor.trim());
        }
        mGovernor.setAdapter(governorAdapter);
        mGovernor.setSelection(Arrays.asList(mAvailableGovernors).indexOf(mCurGovernor[curcpu]));
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
        mIo.setSelection(Arrays.asList(mAvailableIo).indexOf(mCurIO[curcpu]));
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
                    final String r=Helpers.readCPU(context,nCpus);
                    if(r!=null){
                        for (int i = 0; i < nCpus; i++){
                            editor.putString(PREF_MIN_CPU+i, r.split(":")[i*5]);
                            editor.putString(PREF_MAX_CPU+i, r.split(":")[i*5+1]);
                            editor.putString(PREF_GOV, r.split(":")[i*5+2]);
                            editor.putString(PREF_IO, r.split(":")[i*5+3]);
                            editor.putString("cpuon" + i, r.split(":")[i*5+4]);
                        }
                    }
                }
                editor.commit();
            }
        });

        setCPUval(curcpu);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.cpu_settings_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.tablist:
                Helpers.getTabList(getString(R.string.menu_tab),(ViewPager) getView().getParent(),getActivity());
                break;
            case R.id.app_settings:
                Intent intent = new Intent(context, PCSettings.class);
                startActivity(intent);
                break;
            case R.id.gov_settings:
                for (String aSupported : supported) {
                    if (aSupported.equals(mCurGovernor[curcpu])) {
                        if(new File(GOV_SETTINGS_PATH+mCurGovernor[curcpu]).exists()){
                            intent = new Intent(context, GovSetActivity.class);
                            intent.putExtra("cpu", Integer.toString(curcpu));
                            startActivity(intent);
                        }
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
            switch (seekBar.getId()){
                case R.id.max_slider:
                    setMaxSpeed(seekBar, progress);
                    break;
                case R.id.min_slider:
                    setMinSpeed(seekBar, progress);
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        final StringBuilder sb = new StringBuilder();
        switch (seekBar.getId()){
            case R.id.max_slider:
                sb.append("busybox echo ").append(mMaxFreqSetting[curcpu]).append(" > ").append(MAX_FREQ_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
                if (mIsDynFreq) {
                    sb.append("busybox echo ").append(mMaxFreqSetting[curcpu]).append(" > ").append(DYN_MAX_FREQ_PATH).append(";\n");
                }
                if (mIsTegra3) {
                    sb.append("busybox echo ").append(mMaxFreqSetting[curcpu]).append(" > ").append(TEGRA_MAX_FREQ_PATH).append(";\n");
                }
                break;
            case R.id.min_slider:
                sb.append("busybox echo ").append(mMinFreqSetting[curcpu]).append(" > ").append(MIN_FREQ_PATH.replace("cpu0", "cpu" + curcpu)).append(";\n");
                if (mIsDynFreq) {
                    sb.append("busybox echo ").append(mMinFreqSetting[curcpu]).append(" > ").append(DYN_MIN_FREQ_PATH).append(";\n");
                }
                break;
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
            mCurGovernor[curcpu]=selected;
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
            mCurIO[curcpu]=selected;
        }
        public void onNothingSelected(AdapterView<?> parent) {
            // Do nothing.
        }
    }
    @Override
    public void onSaveInstanceState(Bundle saveState) {
        super.onSaveInstanceState(saveState);
        saveState.putInt("curcpu",curcpu);
        saveState.putString("maxfreq",mMaxFreqSetting[curcpu]);
        saveState.putString("minfreq",mMinFreqSetting[curcpu]);
        saveState.putString("governor",mCurGovernor[curcpu]);
        saveState.putString("io",mCurIO[curcpu]);
        saveState.putString("cpuon",mCPUon[curcpu]);
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
                    Log.d(TAG, "CPU thread error " + e);
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
            mMinFreqSetting[curcpu] = current;
        }
        mMaxSpeedText.setText(Helpers.toMHz(current));
        mMaxFreqSetting[curcpu] = current;
        updateSharedPrefs(PREF_MAX_CPU+curcpu, current);
    }

    public void setMinSpeed(SeekBar seekBar, int progress) {
        String current = "";
        current = mAvailableFrequencies[progress];
        int maxSliderProgress = mMaxSlider.getProgress();
        if (progress >= maxSliderProgress) {
            mMaxSlider.setProgress(progress);
            mMaxSpeedText.setText(Helpers.toMHz(current));
            mMaxFreqSetting[curcpu] = current;
        }
        mMinSpeedText.setText(Helpers.toMHz(current));
        mMinFreqSetting[curcpu] = current;
        updateSharedPrefs(PREF_MIN_CPU+curcpu, current);
    }



    public void setCPUval(int i){
        mMaxSpeedText.setText(Helpers.toMHz(mMaxFreqSetting[i]));
        mMaxSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mMaxFreqSetting[i]));

        mMinSpeedText.setText(Helpers.toMHz(mMinFreqSetting[i]));
        mMinSlider.setProgress(Arrays.asList(mAvailableFrequencies).indexOf(mMinFreqSetting[i]));

        mCurCpu.setText(Integer.toString(i+1));
        mCurCpu.setTextColor(res.getColor(R.color.pc_blue));

        if(mIsDynFreq && curcpu>0){
            mMaxSlider.setEnabled(false);
            mMinSlider.setEnabled(false);
        }
        else{
            mMaxSlider.setEnabled(true);
            mMinSlider.setEnabled(true);
        }
        if(mCPUon[curcpu].equals("0")){
             mCurCpu.setTextColor(res.getColor(R.color.pc_red));
             mMaxSlider.setEnabled(false);
             mMinSlider.setEnabled(false);
        }
        mPreferences.edit().putString("cpuon" + curcpu, mCPUon[curcpu]).apply();
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
                    if(new File(CUR_CPU_PATH.replace("cpu0","cpu"+curcpu)).exists()){
                        mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0,Helpers.readOneLine(CUR_CPU_PATH.replace("cpu0","cpu"+curcpu))));
                    }
                    else{
                        mCurCPUHandler.sendMessage(mCurCPUHandler.obtainMessage(0,"0"));
                    }
                }
            }
            catch (InterruptedException e) {
                Log.d(TAG, "CPU thread error "+e);
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

    private void getCPUval(){
        final String r=Helpers.readCPU(context,nCpus);
        if(r!=null){
            for (int i = 0; i < nCpus; i++){
                mMinFreqSetting[i]=r.split(":")[i*5];
                mMaxFreqSetting[i]=r.split(":")[i*5+1];
                mCurGovernor[i]=r.split(":")[i*5+2];
                mCurIO[i]=r.split(":")[i*5+3];
                mCPUon[i]=r.split(":")[i*5+4];
            }
        }
    }
}

