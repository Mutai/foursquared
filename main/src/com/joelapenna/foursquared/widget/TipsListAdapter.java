/**
 * Copyright 2010 Mark Wyszomierski
 */

package com.joelapenna.foursquared.widget;

import com.joelapenna.foursquare.Foursquare;
import com.joelapenna.foursquare.types.Group;
import com.joelapenna.foursquare.types.Tip;
import com.joelapenna.foursquared.FoursquaredSettings;
import com.joelapenna.foursquared.R;
import com.joelapenna.foursquared.util.RemoteResourceManager;
import com.joelapenna.foursquared.util.StringFormatters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * @date August 31, 2010
 * @author Mark Wyszomierski (markww@gmail.com), foursquare.
 */
public class TipsListAdapter extends BaseGroupAdapter<Tip> 
    implements ObservableAdapter {

    private static final String TAG = "";
    private static final boolean DEBUG = FoursquaredSettings.DEBUG;

    private LayoutInflater mInflater;
    private int mLayoutToInflate;
    private RemoteResourceManager mRrm;
    private RemoteResourceManagerObserver mResourcesObserver;
    private Handler mHandler = new Handler();
    private int mLoadedPhotoIndex;
    private boolean mDisplayTipVenueTitles;

    
    public TipsListAdapter(Context context, RemoteResourceManager rrm) {
        super(context);
        mInflater = LayoutInflater.from(context);
        mLayoutToInflate = R.layout.tip_list_item;
        mRrm = rrm;
        mResourcesObserver = new RemoteResourceManagerObserver();
        mLoadedPhotoIndex = 0;
        mDisplayTipVenueTitles = true;

        mRrm.addObserver(mResourcesObserver);
    }
    
    public void removeObserver() {
        mHandler.removeCallbacks(mRunnableLoadPhotos);
        mRrm.deleteObserver(mResourcesObserver);
    }

    public TipsListAdapter(Context context, int layoutResource) {
        super(context);
        mInflater = LayoutInflater.from(context);
        mLayoutToInflate = layoutResource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid unnecessary
        // calls to findViewById() on each row.
        ViewHolder holder;

        // When convertView is not null, we can reuse it directly, there is no
        // need to re-inflate it. We only inflate a new View when the
        // convertView supplied by ListView is null.
        if (convertView == null) {
            convertView = mInflater.inflate(mLayoutToInflate, null);

            // Creates a ViewHolder and store references to the two children
            // views we want to bind data to.
            holder = new ViewHolder();
            holder.photo = (ImageView) convertView.findViewById(R.id.ivAuthor);
            holder.title = (TextView) convertView.findViewById(R.id.tvTitle);
            holder.body = (TextView) convertView.findViewById(R.id.tvBody);
            holder.dateAndAuthor = (TextView) convertView.findViewById(R.id.tvDateAndAuthor);
            holder.friendCountTodoImg = (ImageView) convertView.findViewById(R.id.ivFriendCountAsTodo);
            holder.friendCountTodo = (TextView) convertView.findViewById(R.id.tvFriendCountAsTodo);
            holder.friendCountCompletedImg = (ImageView) convertView.findViewById(R.id.ivFriendCountCompleted);
            holder.friendCountCompleted = (TextView) convertView.findViewById(R.id.tvFriendCountCompleted);
            holder.completed = (ImageView) convertView.findViewById(R.id.ivTipCompleted);

            convertView.setTag(holder);
        } else {
            // Get the ViewHolder back to get fast access to the TextView
            // and the ImageView.
            holder = (ViewHolder) convertView.getTag();
        }

        Tip tip = (Tip) getItem(position);
        Uri photoUri = Uri.parse(tip.getUser().getPhoto());
        try {
            Bitmap bitmap = BitmapFactory.decodeStream(mRrm.getInputStream(photoUri));
            holder.photo.setImageBitmap(bitmap);
        } catch (IOException e) {
            if (Foursquare.MALE.equals(tip.getUser().getGender())) {
                holder.photo.setImageResource(R.drawable.blank_boy);
            } else {
                holder.photo.setImageResource(R.drawable.blank_girl);
            }
        }

        if (mDisplayTipVenueTitles && tip.getVenue() != null) {
            holder.title.setText("@" + tip.getVenue().getName());
            holder.title.setVisibility(View.VISIBLE);
        } else {
            holder.title.setVisibility(View.GONE);
        }
        
        holder.body.setText(tip.getText());
        holder.dateAndAuthor.setText(tip.getCreated());
        if (tip.getUser() != null) {
            holder.dateAndAuthor.setText(
                    holder.dateAndAuthor.getText() + 
                    " via " + StringFormatters.getUserFullName(tip.getUser()));
        }
        
        if (tip.getStats().getTodoCount() > 0) {
            holder.friendCountTodoImg.setVisibility(View.VISIBLE);
            holder.friendCountTodo.setVisibility(View.VISIBLE);
            holder.friendCountTodo.setText(String.valueOf(tip.getStats().getTodoCount()));
        } else {
            holder.friendCountTodoImg.setVisibility(View.GONE);
            holder.friendCountTodo.setVisibility(View.GONE);
        }
        
        if (tip.getStats().getDoneCount() > 0) {
            holder.friendCountCompletedImg.setVisibility(View.VISIBLE);
            holder.friendCountCompleted.setVisibility(View.VISIBLE);
            holder.friendCountCompleted.setText(String.valueOf(tip.getStats().getDoneCount()));
        } else {
            holder.friendCountCompletedImg.setVisibility(View.GONE);
            holder.friendCountCompleted.setVisibility(View.GONE);
        }
        
        if (tip.getStatus() != null) {
            holder.completed.setVisibility(View.VISIBLE);
            if (tip.getStatus().equals("done")) {
                holder.completed.setImageResource(R.drawable.crown_small);
                
            } else if (tip.getStatus().equals("todo")) {
                holder.completed.setImageResource(R.drawable.facebook_icon);
                
            }
        } else {
            holder.completed.setVisibility(View.GONE);
        }
        
        return convertView;
    }

    public void removeItem(int position) throws IndexOutOfBoundsException {
        group.remove(position);
        notifyDataSetInvalidated();
    }
    
    @Override
    public void setGroup(Group<Tip> g) {
        super.setGroup(g);
        mLoadedPhotoIndex = 0;
        
        mHandler.postDelayed(mRunnableLoadPhotos, 10L);
    }
    
    public void setDisplayTipVenueTitles(boolean displayTipVenueTitles) {
        mDisplayTipVenueTitles = displayTipVenueTitles;
    }

    private class RemoteResourceManagerObserver implements Observer {
        @Override
        public void update(Observable observable, Object data) {
            if (DEBUG) Log.d(TAG, "Fetcher got: " + data);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    notifyDataSetChanged();
                }
            });
        }
    }
    
    private Runnable mRunnableLoadPhotos = new Runnable() {
        @Override
        public void run() {
            if (mLoadedPhotoIndex < getCount()) {
                Tip tip = (Tip)getItem(mLoadedPhotoIndex++);
                if (tip.getUser() != null) {
                    Uri photoUri = Uri.parse(tip.getUser().getPhoto());
                    if (!mRrm.exists(photoUri)) {
                        mRrm.request(photoUri); 
                    }
                    mHandler.postDelayed(mRunnableLoadPhotos, 200L);
                }
            }
        }
    };

    static class ViewHolder {
        ImageView photo;
        TextView title;
        TextView body;
        TextView dateAndAuthor;
        ImageView friendCountTodoImg;
        TextView friendCountTodo;
        ImageView friendCountCompletedImg;
        TextView friendCountCompleted;
        ImageView completed;
    }
}