/*
 * Infamous Performance - An Android CPU Control application Copyright (C) 2014
 * Jamison904
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
 *
 */

package com.infamous.performance.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.*;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.infamous.performance.R;
import com.infamous.performance.activities.KSMActivity;
import com.infamous.performance.activities.MemUsageActivity;
import com.infamous.performance.activities.PCSettings;
import com.infamous.performance.activities.PackActivity;
import com.infamous.performance.activities.ZramActivity;
import com.infamous.performance.util.CMDProcessor;
import com.infamous.performance.util.Constants;
import com.infamous.performance.util.Helpers;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

public class AboutInfamous extends PreferenceFragment implements OnSharedPreferenceChangeListener
{

	@Override
	public void onSharedPreferenceChanged(SharedPreferences p1, String p2)
	{
		// TODO: Implement this method and add dev cards as well as links to infamous. This is only a hold
	}

    SharedPreferences mPreferences;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); } 
		}
