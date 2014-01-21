package com.brewcrewfoo.performance.fragments;

/**
 * Created by h0rn3t on 02.01.2014.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.activities.PCSettings;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;

import java.io.File;

public class CPUAdvanced extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener,Constants {

    SharedPreferences mPreferences;
    private CheckBoxPreference mMpdecision,mIntelliplug,mEcomode;
    private ListPreference mSOmax,mSOmin;
    private String pso="";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mPreferences.registerOnSharedPreferenceChangeListener(this);
        addPreferencesFromResource(R.layout.cpu_advanced);

        pso=getString(R.string.ps_so_minmax);

        mSOmax = (ListPreference) findPreference("pref_so_max");
        mSOmin = (ListPreference) findPreference("pref_so_min");

        mMpdecision = (CheckBoxPreference) findPreference("pref_mpdecision");
        mIntelliplug = (CheckBoxPreference) findPreference("pref_intelliplug");
        mEcomode = (CheckBoxPreference) findPreference("pref_ecomode");

        if (!new File(SO_MAX_FREQ).exists() || !new File(SO_MIN_FREQ).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("so_min_max");
            getPreferenceScreen().removePreference(hideCat);
        }
        else{
            final String availableFrequencies = Helpers.readOneLine(STEPS_PATH);
            if (availableFrequencies != null) {
                CharSequence[] entries = availableFrequencies.split(" ");
                mSOmax.setEntries(entries);
                mSOmax.setEntryValues(entries);
                mSOmin.setEntries(entries);
                mSOmin.setEntryValues(entries);
                final String readsomax=Helpers.readOneLine(SO_MAX_FREQ);
                final String readsomin=Helpers.readOneLine(SO_MIN_FREQ);
                mSOmax.setValue(readsomax);
                mSOmin.setValue(readsomin);
                mSOmax.setSummary(pso+ readsomax+" Hz");
                mSOmin.setSummary(pso+ readsomin+" Hz");
            }
        }

        if (Helpers.binExist("mpdecision").equals(NOT_FOUND)){
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("mpdecision");
            getPreferenceScreen().removePreference(hideCat);
        }
        else{
            Boolean mpdon = Helpers.moduleActive("mpdecision");
            mMpdecision.setChecked(mpdon);
            mPreferences.edit().putBoolean("mpdecision",mpdon).apply();
        }
        if (!new File(INTELLI_PLUG).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("intelliplug");
            getPreferenceScreen().removePreference(hideCat);
        }
        else{
            mIntelliplug.setChecked(Helpers.readOneLine(INTELLI_PLUG).equals("1"));
        }
        if (!new File(ECO_MODE).exists()) {
            PreferenceCategory hideCat = (PreferenceCategory) findPreference("ecomode");
            getPreferenceScreen().removePreference(hideCat);
        }
        else{
            mEcomode.setChecked(Helpers.readOneLine(ECO_MODE).equals("1"));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.tablist:
                Helpers.getTabList(getString(R.string.menu_tab),(ViewPager) getView().getParent(),getActivity());
                break;
            case R.id.app_settings:
                Intent intent = new Intent(getActivity(), PCSettings.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference==mMpdecision) {
            if(mMpdecision.isChecked()){
                new CMDProcessor().su.runWaitFor("start mpdecision");
            }
            else{
                new CMDProcessor().su.runWaitFor("stop mpdecision");
            }
            return true;
        }
        else if(preference==mIntelliplug) {
            if (Helpers.readOneLine(INTELLI_PLUG).equals("0")){
                new CMDProcessor().su.runWaitFor("busybox echo 1 > " + INTELLI_PLUG);
            }
            else{
                new CMDProcessor().su.runWaitFor("busybox echo 0 > " + INTELLI_PLUG);
            }
            return true;
        }
        else if(preference==mEcomode) {
            if (Helpers.readOneLine(ECO_MODE).equals("0")){
                new CMDProcessor().su.runWaitFor("busybox echo 1 > " + ECO_MODE);
            }
            else{
                new CMDProcessor().su.runWaitFor("busybox echo 0 > " + ECO_MODE);
            }
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onSharedPreferenceChanged(final SharedPreferences sharedPreferences, String key) {
        if (key.equals("pref_so_max")) {
            final String values = mSOmax.getValue();
            if (!values.equals(Helpers.readOneLine(SO_MAX_FREQ))){
                new CMDProcessor().su.runWaitFor("busybox echo "+values+" > " + SO_MAX_FREQ);
            }
            mSOmax.setSummary(pso + values + " Hz");
        }
        else if (key.equals("pref_so_min")) {
            final String values = mSOmin.getValue();
            if (!values.equals(Helpers.readOneLine(SO_MIN_FREQ))){
                new CMDProcessor().su.runWaitFor("busybox echo "+values+" > " + SO_MIN_FREQ);
            }
            mSOmin.setSummary(pso + values + " Hz");
        }

    }


}

