package com.bidtorrent.biddingservice.debug;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.ContextualizedBidResponse;
import com.bidtorrent.biddingservice.R;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ListViewAdapter extends ArrayAdapter<ContextualizedBidResponse> {

    private final int resource;
    // Declare Variables
    Context context;
    LayoutInflater inflater;
    List<ContextualizedBidResponse> bidresponseslist;
    private SparseBooleanArray mSelectedItemsIds;

    public ListViewAdapter(Context context, int resourceId,
                           List<ContextualizedBidResponse> bidresponseslist) {
        super(context, resourceId, bidresponseslist);
        mSelectedItemsIds = new SparseBooleanArray();
        this.context = context;
        this.bidresponseslist = bidresponseslist;
        inflater = LayoutInflater.from(context);
        this.resource = resourceId;
    }

    private class ViewHolder {
        TextView bidderuri;
        TextView price;
    }

    public View getView(int position, View view, ViewGroup parent) {
        final ViewHolder holder;
        if (view == null) {
            holder = new ViewHolder();
            view = inflater.inflate(resource, null);
            // Locate the TextViews in listview_item.xml
            holder.bidderuri = (TextView) view.findViewById(R.id.bidderuri);
            holder.price = (TextView) view.findViewById(R.id.price);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        // Capture position and set to the TextViews
        long bidderId = bidresponseslist.get(position).getBidderConfiguration().id;
        holder.bidderuri.setText("BidderId: " + bidderId);

        if (bidresponseslist.get(position) != null)
            holder.price.setText(NumberFormat.getCurrencyInstance(new Locale("en", "US")).format(bidresponseslist.get(position).getBidResponse().getPrice()));
        else
            holder.price.setText("TIMEOUT");

        if (position == 0) {
            holder.bidderuri.setTypeface(Typeface.DEFAULT_BOLD);
            holder.bidderuri.setTextColor(Color.rgb(40, 138, 32));
            holder.price.setTypeface(Typeface.DEFAULT_BOLD);
            holder.price.setTextColor(Color.rgb(40, 138, 32));
        } else {
            holder.bidderuri.setTypeface(Typeface.DEFAULT);
            holder.bidderuri.setTextColor(Color.BLACK);
            holder.price.setTypeface(Typeface.DEFAULT);
            holder.price.setTextColor(Color.BLACK);
        }

        return view;
    }
}
