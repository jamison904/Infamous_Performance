package com.brewcrewfoo.performance.util;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by h0rn3t on 10.02.2014.
 * http://forum.xda-developers.com/member.php?u=4674443
 */
public class PropUtil implements Constants {

    public static List<Prop> load_prop(String s){
        List<Prop> props = new ArrayList<Prop>();
        props.clear();
        if(s==null) return props;
        final String p[]=s.split("\n");
        for (String aP : p) {
            try{
                if(aP!=null && aP.contains("::") && !aP.contains("uevent") && !aP.contains("debug")){
                    String pn=aP.split("::")[0];
                    pn=pn.substring(pn.lastIndexOf("/") + 1, pn.length()).trim();
                    props.add(new Prop(pn,aP.split("::")[1].trim()));
                }
            }
            catch (Exception e){
            }
        }
        return props;
    }

    public static void set_pref(String n, String v,String pref,SharedPreferences mPreferences){
        final String s=mPreferences.getString(pref,"");
        final StringBuilder sb = new StringBuilder();
        if(!s.equals("")){
            String p[]=s.split(";");
            for (String aP : p) {
                if(aP!=null && aP.contains(":")){
                    final String pn[]=aP.split(":");
                    if(!pn[0].equals(n)) sb.append(pn[0]).append(':').append(pn[1]).append(';');
                }
            }
        }
        sb.append(n).append(':').append(v).append(';');
        mPreferences.edit().putString(pref, sb.toString()).commit();
    }
}
