package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 01.11.2013.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SysctlEditor extends Activity implements Constants, AdapterView.OnItemClickListener, ActivityThemeChangeInterface {
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
    private final String dn= Environment.getExternalStorageDirectory().getAbsolutePath()+"/PerformanceControl/sysctl";

    private final String syspath="/system/etc/";
    private String sob=SYSCTL_SOB;
    private String[] tcp={};


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();
        setTheme();
        setContentView(R.layout.prop_view);


        new CMDProcessor().sh.runWaitFor("busybox mkdir -p "+dn );
        if(new File(syspath+"sysctl.conf").exists()){
            new CMDProcessor().sh.runWaitFor("busybox cp /system/etc/sysctl.conf"+" "+dn+"/sysctl.conf" );
        }
        else{
            new CMDProcessor().sh.runWaitFor("busybox echo \"# created by PerformanceControl\n\" > "+dn+"/sysctl.conf" );
        }
        Helpers.get_assetsScript("utils",context,"","");
        new CMDProcessor().sh.runWaitFor("busybox chmod 750 "+getFilesDir()+"/utils" );

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
                sb.append("mount -o rw,remount /system;\n");
                sb.append("busybox cp ").append(dn).append("/").append("sysctl.conf").append(" /system/etc/").append("sysctl.conf").append(";\n");
                sb.append("busybox chmod 644 ").append("/system/etc/").append("sysctl.conf").append(";\n");
                sb.append("mount -o ro,remount /system;\n");
                sb.append("busybox sysctl -p ").append("/system/etc/").append("sysctl.conf").append(";\n");
                Helpers.shExec(sb,context,true);
            }
        });
        final Switch setOnBoot = (Switch) findViewById(R.id.applyAtBoot);
        setOnBoot.setChecked(mPreferences.getBoolean(sob, false));
        setOnBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPreferences.edit().putBoolean(sob, isChecked).apply();
            }
        });
        tools.setVisibility(View.GONE);
        search.setVisibility(View.GONE);

        new GetPropOperation().execute();

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.sysctl_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.search_prop:
                if(search.isShown()){
                    search.setVisibility(RelativeLayout.GONE);
                    filterText.setText("");
                }
                else{
                    search.setVisibility(RelativeLayout.VISIBLE);
                }
                break;
            case R.id.reset:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.mt_reset))
                        .setMessage(getString(R.string.reset_msg))
                        .setNegativeButton(getString(R.string.cancel),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .setPositiveButton(getString(R.string.yes),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if(new File(syspath+"sysctl.conf").exists()){
                                            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                                            Date now = new Date();
                                            String nf = formatter.format(now)+"_" + "sysctl.conf";
                                            new CMDProcessor().sh.runWaitFor("busybox cp /system/etc/sysctl.conf"+" "+dn+"/"+nf );
                                            final StringBuilder sb = new StringBuilder();
                                            sb.append("mount -o rw,remount /system;\n");
                                            sb.append("busybox echo \"# created by PerformanceControl\n\" >").append(" /system/etc/").append("sysctl.conf").append(";\n");
                                            sb.append("mount -o ro,remount /system;\n");
                                            Helpers.shExec(sb,context,true);
                                        }
                                        new CMDProcessor().sh.runWaitFor("busybox echo \"# created by PerformanceControl\n\" > "+dn+"/sysctl.conf" );
                                    }
                }).create().show();
                break;
        }
        return true;
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
            CMDProcessor.CommandResult cr=null;
            cr=new CMDProcessor().sh.runWaitFor("busybox echo `sysctl -n net.ipv4.tcp_available_congestion_control`");
            if(cr.success()){
                tcp=cr.stdout.split(" ");
            }
            cr=new CMDProcessor().sh.runWaitFor("busybox echo `busybox find /proc/sys/* -type f -perm -644 | grep -v \"vm.\"`");
            if(cr.success()){
                return cr.stdout;
            }
            else{
                Log.d(TAG, "sysctl error: " + cr.stderr);
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
                    adapter = new PropAdapter(SysctlEditor.this, R.layout.prop_item, props);
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
        final View editDialog = factory.inflate(R.layout.build_prop_dialog, null);
        final EditText tv = (EditText) editDialog.findViewById(R.id.vprop);
        final EditText tn = (EditText) editDialog.findViewById(R.id.nprop);
        final TextView tt = (TextView) editDialog.findViewById(R.id.text1);
        final Spinner sp = (Spinner) editDialog.findViewById(R.id.spinner);
        final LinearLayout lpresets = (LinearLayout) editDialog.findViewById(R.id.lpresets);
        ArrayAdapter<CharSequence> vAdapter = new ArrayAdapter<CharSequence>(context, android.R.layout.simple_spinner_item);
        vAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vAdapter.clear();

        if(pp!=null){
            final String v=pp.getVal();
            final String n=pp.getName();
            lpresets.setVisibility(LinearLayout.GONE);
            if(n.contains("net.ipv4.tcp_congestion_control")){
                if(tcp.length>0){
                    vAdapter.add(v);
                    for (String vtcp : tcp) {
                        if(!vtcp.equals(v)) vAdapter.add(vtcp);
                    }
                    lpresets.setVisibility(LinearLayout.VISIBLE);
                    sp.setAdapter(vAdapter);
                }
            }
            if(v.equals("0")){
                vAdapter.add("0");
                vAdapter.add("1");
                lpresets.setVisibility(LinearLayout.VISIBLE);
                sp.setAdapter(vAdapter);
            }
            else if(v.equals("1")){
                vAdapter.add("1");
                vAdapter.add("0");
                lpresets.setVisibility(LinearLayout.VISIBLE);
                sp.setAdapter(vAdapter);
            }
            tv.setText(pp.getVal());
            tn.setText(pp.getName());
            tn.setVisibility(EditText.GONE);
            tt.setText(pp.getName());
            titlu=getString(R.string.edit_prop_title);
        }
        else{//add
            titlu=getString(R.string.add_prop_title);
            lpresets.setVisibility(LinearLayout.GONE);
            tt.setText(getString(R.string.prop_name));
            tn.setVisibility(EditText.VISIBLE);
        }
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                tv.setText(sp.getSelectedItem().toString().trim());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
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
                            if (tv.getText().toString() != null){
                                pp.setVal(tv.getText().toString().trim());
                                new CMDProcessor().sh.runWaitFor(getFilesDir()+"/utils -setprop \""+pp.getName()+"="+pp.getVal()+"\" "+dn+"/sysctl.conf");
                            }
                        }
                        else {
                            if (tv.getText().toString() != null && tn.getText().toString() != null && tn.getText().toString().trim().length() > 0){
                                props.add(new Prop(tn.getText().toString().trim(),tv.getText().toString().trim()));
                                new CMDProcessor().sh.runWaitFor(getFilesDir()+"/utils -setprop \""+tn.getText().toString().trim()+"="+tv.getText().toString().trim()+"\" "+dn+"/sysctl.conf");
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
                final String pn=aP.trim().replace("/",".").substring(10, aP.length());
                props.add(new Prop(pn,pv));
            }
        }
    }

}