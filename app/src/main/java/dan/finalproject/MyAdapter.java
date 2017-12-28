package manda094.finalproject;


import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


// this class is a prototype of the Custom Adapter for our Custom ListViews in this project
// it is now used for managing the Custom ListView of Anomalies' List and Machines' List
public class MyAdapter extends ArrayAdapter<Item> {

    // the current context
    private final Context context;
    // the ArrayList used to accommodate the contents of the list view
    private final ArrayList<Item> itemsArrayList;

	// constructor:
    public MyAdapter(Context context, ArrayList<Item> itemsArrayList) {

        super(context, R.layout.row, itemsArrayList);

        this.context = context;
        this.itemsArrayList = itemsArrayList;
    }

	// inflate/ initialize the view of the adapter 
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // current row
        View row = convertView;

        // 1. Create inflater
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // 2. Get rowView from inflater
        View rowView = inflater.inflate(R.layout.row, parent, false);

        // 3. Get the two text view from the rowView
        TextView labelView = (TextView) rowView.findViewById(R.id.label);
        TextView valueView = (TextView) rowView.findViewById(R.id.value);

        // 4. Set the text for textView
        labelView.setText(itemsArrayList.get(position).getTitle());
        valueView.setText(itemsArrayList.get(position).getDescription());


        // 5. return rowView
        return rowView;
    }


}