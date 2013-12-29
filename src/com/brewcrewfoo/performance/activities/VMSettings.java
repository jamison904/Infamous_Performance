package com.brewcrewfoo.performance.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;
import com.brewcrewfoo.performance.util.ActivityThemeChangeInterface;
import com.brewcrewfoo.performance.util.CMDProcessor;
import com.brewcrewfoo.performance.util.Constants;
import com.brewcrewfoo.performance.util.Helpers;
import com.brewcrewfoo.performance.util.Prop;
import com.brewcrewfoo.performance.util.PropAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by h0rn3t on 29.12.2013.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
public class VMSettings extends Activity implements Constants, AdapterView.OnItemClickListener, ActivityThemeChangeInterface {
    private boolean mIsLightTheme;
    SharedPreferences mPreferences;
    private final Context context=this;
    Resources res;
    private ListView packList;
    private LinearLayout linlaHeaderProgress;
    private LinearLayout nofiles;
    private RelativeLayout tools,search;
    private PropAdapter adapter=null;
    private EditText filterText = null;
    private List<Prop> props = new ArrayList<Prop>();

    private Boolean isdyn=false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();
        setTheme();
        setContentView(R.layout.prop_view);


        if (new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists()){
            isdyn=Helpers.readOneLine(DYNAMIC_DIRTY_WRITEBACK_PATH).equals("1");
        }

        packList = (ListView) findViewById(R.id.applist);
        packList.setOnItemClickListener(this);
        linlaHeaderProgress = (LinearLayout) findViewById(R.id.linlaHeaderProgress);
        nofiles = (LinearLayout) findViewById(R.id.nofiles);
        tools = (RelativeLayout) findViewById(R.id.tools);
        search = (RelativeLayout) findViewById(R.id.search);
        filterText = (EditText) findViewById(R.id.filtru);
        filterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(adapter!=null)
                    adapter.getFilter().filter(filterText.getText().toString());
            }
        });
        Button applyBtn = (Button) findViewById(R.id.applyBtn);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                final StringBuilder sb = new StringBuilder();

                final String s=mPreferences.getString(PREF_VM,"");

                if(!s.equals("")){
                    String p[]=s.split(";");
                    for (String aP : p) {
                        if(!aP.equals("")&& aP!=null){
                            final String pn[]=aP.split(":");
                            sb.append("busybox echo ").append(pn[1]).append(" > ").append(VM_PATH).append(pn[0]).append(";\n");
                        }
                    }
                    Helpers.shExec(sb,context,true);
                }

            }
        });
        final Switch setOnBoot = (Switch) findViewById(R.id.applyAtBoot);
        setOnBoot.setChecked(mPreferences.getBoolean(VM_SOB, false));
        setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPreferences.edit().putBoolean(VM_SOB, isChecked).apply();
            }
        });
        tools.setVisibility(View.GONE);
        search.setVisibility(View.GONE);
        isdyn= (new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists());

        new GetPropOperation().execute();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    @Override
    public void onBackPressed(){
        if(search.isShown()){
            search.setVisibility(RelativeLayout.GONE);
            filterText.setText("");
        }
        else{
            finish();
        }
    }

    private class GetPropOperation extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            CMDProcessor.CommandResult cr=new CMDProcessor().sh.runWaitFor("busybox echo `busybox find "+VM_PATH+"* -type f -perm -644`");
            if(cr.success()){
                return cr.stdout;
            }
            else{
                Log.d(TAG, "VMSettings error: " + cr.stderr);
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if((result==null)||(result.length()<=0)) {
                finish();
            }
            else{
                load_prop(result);
                Collections.sort(props);
                linlaHeaderProgress.setVisibility(View.GONE);
                if(props.isEmpty()){
                    nofiles.setVisibility(View.VISIBLE);
                }
                else{
                    nofiles.setVisibility(View.GONE);
                    tools.setVisibility(View.VISIBLE);
                    adapter = new PropAdapter(VMSettings.this, R.layout.prop_item, props);
                    packList.setAdapter(adapter);
                }
            }
        }
        @Override
        protected void onPreExecute() {
            linlaHeaderProgress.setVisibility(View.VISIBLE);
            nofiles.setVisibility(View.GONE);
            tools.setVisibility(View.GONE);
        }
        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long row) {
        final Prop p = adapter.getItem(position);

        editPropDialog(p);
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
        super.onResume();
    }
    private void editPropDialog(Prop p) {
        final Prop pp = p;
        String titlu="";

        LayoutInflater factory = LayoutInflater.from(this);
        final View editDialog = factory.inflate(R.layout.prop_edit_dialog, null);
        final EditText tv = (EditText) editDialog.findViewById(R.id.vprop);
        final TextView tn = (TextView) editDialog.findViewById(R.id.nprop);

        if(pp!=null){
            tv.setText(pp.getVal());
            tn.setText(pp.getName());
            titlu=getString(R.string.edit_prop_title);
        }
        else{//add
            titlu=getString(R.string.add_prop_title);
        }
        final String vcur=tv.getText().toString();
        new AlertDialog.Builder(this)
                .setTitle(titlu)
                .setView(editDialog)
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                .setPositiveButton(getString(R.string.ps_volt_save), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (pp!=null) {
                            if ((tv.getText().toString() != null) && !vcur.equals(tv.getText().toString())){
                                pp.setVal(tv.getText().toString().trim());
                                set_pref(tn.getText().toString().trim(),tv.getText().toString().trim());
                            }
                        }
                        else {
                            if (tv.getText().toString() != null && tn.getText().toString() != null && tn.getText().toString().trim().length() > 0){
                                props.add(new Prop(tn.getText().toString().trim(),tv.getText().toString().trim()));
                                set_pref(tn.getText().toString().trim(),tv.getText().toString().trim());
                            }
                        }
                        Collections.sort(props);
                        adapter.notifyDataSetChanged();
                    }
                }).create().show();
    }

    public void load_prop(String s){
        props.clear();
        String p[]=s.split(" ");
        for (String aP : p) {
            if(aP.trim().length()>0 && aP!=null){
                final String pv=Helpers.readOneLine(aP).trim();
                final String pn=aP.substring(aP.lastIndexOf("/") + 1, aP.length());
                if(testprop(pn)){
                    props.add(new Prop(pn,pv));
                }
            }
        }
    }
    public boolean testprop(String s){
            if(isdyn && (s.contains("dirty_writeback_active_centisecs")||s.contains("dynamic_dirty_writeback")|| s.contains("dirty_writeback_suspend_centisecs"))){
                return false;
            }
            else{
                return true;
            }
    }
    public void set_pref(String n, String v){
        final String s=mPreferences.getString(PREF_VM,"");
        final StringBuilder sb = new StringBuilder();
        if(!s.equals("")){
            String p[]=s.split(";");
            for (String aP : p) {
                if(!aP.equals("") && aP!=null){
                    final String pn[]=aP.split(":");
                    if(!pn[0].equals(n)){
                        sb.append(pn[0]+':'+pn[1]+';');
                    }
                }
            }
        }
        sb.append(n + ':' + v + ';');

        mPreferences.edit().putString(PREF_VM, sb.toString()).commit();
    }
}
