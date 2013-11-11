package com.brewcrewfoo.performance.activities;

/**
 * Created by h0rn3t on 01.11.2013.
 */
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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

    private String mod="sysctl";
    private final String syspath="/system/etc/";
    private String cmd="busybox echo `busybox find /proc/sys/* -type f -perm -644 | grep -v \"vm.\"`";
    private String sob=SYSCTL_SOB;
    private Boolean isdyn=false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        res = getResources();
        setTheme();
        setContentView(R.layout.prop_view);

        Intent i=getIntent();
        mod=i.getStringExtra("mod");
        if(mod.equals("vm")){
            cmd="busybox echo `busybox find /proc/sys/vm/* -type f -prune -perm -644`";
            sob=VM_SOB;
        }


        new CMDProcessor().sh.runWaitFor("busybox mkdir -p "+dn );
        if(new File(syspath+mod+".conf").exists()){
            new CMDProcessor().sh.runWaitFor("busybox cp /system/etc/"+mod+".conf"+" "+dn+"/"+mod+".conf" );
        }
        else{
            new CMDProcessor().sh.runWaitFor("busybox echo \"# created by PerformanceControl\n\" > "+dn+"/"+mod+".conf" );
        }
        Helpers.get_assetsScript("utils",context,"","");
        new CMDProcessor().su.runWaitFor("busybox chmod 750 "+getFilesDir()+"/utils" );

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
                sb.append("busybox mount -o remount,rw /system").append(";\n");
                sb.append("busybox cp ").append(dn).append("/").append(mod).append(".conf").append(" /system/etc/").append(mod).append(".conf").append(";\n");
                sb.append("busybox chmod 644 ").append("/system/etc/").append(mod).append(".conf").append(";\n");
                sb.append("busybox mount -o remount,ro /system").append(";\n");
                sb.append("busybox sysctl -p ").append("/system/etc/").append(mod).append(".conf").append(";\n");
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
        isdyn= (new File(DYNAMIC_DIRTY_WRITEBACK_PATH).exists());

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
                search.setVisibility(RelativeLayout.VISIBLE);
                break;
            case R.id.reset:
                if(new File(syspath+mod+".conf").exists()){
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss");
                    Date now = new Date();
                    String nf = formatter.format(now)+"_" + mod+".conf";
                    new CMDProcessor().sh.runWaitFor("busybox cp /system/etc/"+mod+".conf"+" "+dn+"/"+nf );
                    final StringBuilder sb = new StringBuilder();
                    sb.append("busybox mount -o remount,rw /system").append(";\n");
                    sb.append("busybox echo \"# created by PerformanceControl\n\" >").append(" /system/etc/").append(mod).append(".conf").append(";\n");
                    sb.append("busybox mount -o remount,ro /system").append(";\n");
                    Helpers.shExec(sb,context,true);
                }
                new CMDProcessor().sh.runWaitFor("busybox echo \"# created by PerformanceControl\n\" > "+dn+"/"+mod+".conf" );
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
            CMDProcessor.CommandResult cr=new CMDProcessor().sh.runWaitFor(cmd);
            if(cr.success()){
                return cr.stdout;
            }
            else{
                Log.d(TAG, mod+" error: " + cr.stderr);
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
                                new CMDProcessor().sh.runWaitFor(getFilesDir()+"/utils -setprop \""+pp.getName()+"="+pp.getVal()+"\" "+dn+"/"+mod+".conf");
                            }
                        }
                        else {
                            if (tv.getText().toString() != null && tn.getText().toString() != null && tn.getText().toString().trim().length() > 0){
                                props.add(new Prop(tn.getText().toString().trim(),tv.getText().toString().trim()));
                                new CMDProcessor().sh.runWaitFor(getFilesDir()+"/utils -setprop \""+tn.getText().toString().trim()+"="+tv.getText().toString().trim()+"\" "+dn+"/"+mod+".conf");
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
                if(testprop(pn)){
                        props.add(new Prop(pn,pv));
                }
            }
        }
    }
    public boolean testprop(String s){
        if(mod.equals("sysctl") || !isdyn){
            return true;
        }
        else{
            if(s.contains("dirty_writeback_active_centisecs")||s.contains("dynamic_dirty_writeback")|| s.contains("dirty_writeback_suspend_centisecs")){
                return false;
            }
            else{
                return true;
            }
        }
    }
}