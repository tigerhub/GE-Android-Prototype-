package manda094.finalproject;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * Created by DADTS on 16/12/6.
 */

public class CheckBoxAdapter {
    // Define our own adapter for the custom listView for contact
    public static class MyAdapter extends BaseAdapter {
        public static HashMap<Integer, Boolean> isSelected; // Used to store checkbox status
        private LayoutInflater inflater = null;
        private List<HashMap<String, Object>> list = null;  // Used to record information of custom listView
        private String itemString = null;                   // Used to record value of the textView in the listView

        // Constructor of the adapter and obtain the LayoutInflater from the given context
        public MyAdapter(Context context, List<HashMap<String, Object>> list) {
            this.list = list;
            inflater = LayoutInflater.from(context);
            init();
        }

        // Initialize the adapter, unchecked every checkbox
        public void init() {
            isSelected = new HashMap<>();
            for (int i = 0; i < list.size(); i++) {
                isSelected.put(i, false);
            }
        }

        // Implement abstract methods of parent class

        // Get the number of items represented by this Adapter
        @Override
        public int getCount() {
            return list.size();
        }

        // Get the item at the specified position
        @Override
        public Object getItem(int arg0) {
            return list.get(arg0);
        }

        // Get the id of the item at the specified position
        @Override
        public long getItemId(int arg0) {
            return 0;
        }

        // Get a View at the specified position
        @Override
        public View getView(int position, View view, ViewGroup arg2) {
            // Define a new ViewHolder to hold elements in a row
            ViewHolder holder = new ViewHolder();

            // Inflate the layout of a row
            if (view == null) {
                view = inflater.inflate(R.layout.listviewitem, null);
            }

            // Define the textView and checkbox in a row
            holder.tv = (TextView) view.findViewById(R.id.item_tv);
            holder.cb = (CheckBox) view.findViewById(R.id.item_cb);

            // Give the view a tag
            view.setTag(holder);

            // Get the information for a row
            HashMap<String, Object> map = list.get(position);

            // Set the text of textView
            if (map != null) {
                itemString = (String) map.get("item_tv");
                holder.tv.setText(itemString);
            }

            // Set the checkbox
            holder.cb.setChecked(isSelected.get(position));
            return view;
        }

    }

    // Create a structure to hold elements in custom listView
    public static class ViewHolder {
        public TextView tv = null;
        public CheckBox cb = null;
    }
}
