package com.nerisa.thesis.adapter;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.firebase.ui.storage.images.FirebaseImageLoader;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.nerisa.thesis.custodian.R;
import com.nerisa.thesis.model.Warning;

import java.util.List;

/**
 * Created by nerisa on 3/13/18.
 */

public class WarningAdapter extends RecyclerView.Adapter<WarningAdapter.MyViewHolder> {

        private List<Warning> warningList;
        private RequestManager glide;

        public class MyViewHolder extends RecyclerView.ViewHolder {

            public TextView desc, date;
            public ImageView warningImage;

            public MyViewHolder(View view) {
                super(view);
                desc = (TextView) view.findViewById(R.id.desc);
                date = (TextView) view.findViewById(R.id.date);
                warningImage = (ImageView) view.findViewById(R.id.warning_image);
            }
        }


        public WarningAdapter(RequestManager glide, List<Warning> warningList) {
            this.glide = glide;
            this.warningList = warningList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.warning_list, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Log.d("??????", "called");
            Warning warning = warningList.get(position);
            holder.desc.setText(warning.getDesc());
            holder.date.setText(warning.getDate().toString());
            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageReference = storage.getReferenceFromUrl(warning.getImage());
            glide.using(new FirebaseImageLoader())
                    .load(storageReference)
                    .into(holder.warningImage);
        }

        @Override
        public int getItemCount() {
            return warningList.size();
        }
}
