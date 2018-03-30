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
import com.nerisa.thesis.R;
import com.nerisa.thesis.model.Post;
import com.nerisa.thesis.model.Warning;

import java.util.Date;
import java.util.List;

/**
 * Created by nerisa on 3/13/18.
 */

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyViewHolder> {

        private List<Post> postList;

        public class MyViewHolder extends RecyclerView.ViewHolder {

            public TextView desc, date;

            public MyViewHolder(View view) {
                super(view);
                desc = (TextView) view.findViewById(R.id.desc);
                date = (TextView) view.findViewById(R.id.date);
            }
        }


        public PostAdapter(List<Post> postList) {
            this.postList = postList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.post_list, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Post post = postList.get(position);
            holder.desc.setText(post.getDesc());
            holder.date.setText(new Date(post.getDate()).toString());
        }

        @Override
        public int getItemCount() {
            return postList.size();
        }
}
