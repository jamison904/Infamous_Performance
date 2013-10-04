package com.brewcrewfoo.performance.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;

import com.brewcrewfoo.performance.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by h0rn3t on 22.09.2013.
 */
public class PropAdapter extends ArrayAdapter<Prop> {
    private Context c;
    private int id;
    private List<Prop> props;
    private Filter filter;

    public PropAdapter(Context context, int textViewResourceId,List<Prop> objects) {
        super(context, textViewResourceId, objects);
        c = context;
        id = textViewResourceId;
        props = objects;
    }
    public Prop getItem(int i){
        return props.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;

        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }

        final Prop p = props.get(position);
        if (p != null) {
            TextView pp = (TextView) v.findViewById(R.id.prop);
            TextView pv = (TextView) v.findViewById(R.id.pval);
            if(pp!=null){
                pp.setText(p.getName());
            }
            if(pv!=null){
                pv.setText(p.getVal());
            }

        }
        return v;
    }

    @Override
    public Filter getFilter(){
        if(filter == null)
            filter = new AppFilter(props);
        return filter;
    }

    private class AppFilter extends Filter {

        private List<com.brewcrewfoo.performance.util.Prop> sourceObjects;

        public AppFilter(List<com.brewcrewfoo.performance.util.Prop> props) {
            sourceObjects = new ArrayList<com.brewcrewfoo.performance.util.Prop>();
            synchronized (this) {
                sourceObjects.addAll(props);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence chars) {
            String filterSeq = chars.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (filterSeq != null && filterSeq.length() > 0) {
                List<com.brewcrewfoo.performance.util.Prop> filter = new ArrayList<com.brewcrewfoo.performance.util.Prop>();
                for (int i=0;i<props.size();i++) {
                    com.brewcrewfoo.performance.util.Prop o=props.get(i);
                    if (o.getName().toLowerCase().contains(filterSeq))
                        filter.add(o);
                }
                result.count = filter.size();
                result.values = filter;
            } else {
                synchronized (this) {
                    result.values = sourceObjects;
                    result.count = sourceObjects.size();
                }
            }
            return result;
        }

        @Override
        protected void publishResults(CharSequence constraint,FilterResults results) {
            List<com.brewcrewfoo.performance.util.Prop> filtered = (List<com.brewcrewfoo.performance.util.Prop>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = filtered.size(); i < l; i++)
                add((com.brewcrewfoo.performance.util.Prop) filtered.get(i));
            notifyDataSetInvalidated();
        }
    }
}
