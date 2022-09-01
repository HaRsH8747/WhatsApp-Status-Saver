package com.example.whatsapp_status_saver.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsapp_status_saver.ImageDetailActivity
import com.example.whatsapp_status_saver.R
import com.example.whatsapp_status_saver.Utils
import com.example.whatsapp_status_saver.VideoDetailActivity
import com.example.whatsapp_status_saver.databinding.PhotoItemBinding
import com.example.whatsapp_status_saver.databinding.VideoItemBinding
import com.example.whatsapp_status_saver.model.IVModel

class ImageAdapter(
    val context: Context,
    private var filesList: MutableList<IVModel>,
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var PHOTO_ITEM = 1
    private var VIDEO_ITEM = 0

    inner class PhotoViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var photoItemBinding: PhotoItemBinding

        init {
            photoItemBinding = PhotoItemBinding.bind(itemView)
        }
    }

    inner class VideoViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        var videoItemBinding: VideoItemBinding

        init {
            videoItemBinding = VideoItemBinding.bind(itemView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        var view: View? = null
        return if (viewType == PHOTO_ITEM) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.photo_item, parent, false)
            PhotoViewHolder(view)
        } else {
            view = LayoutInflater.from(parent.context).inflate(R.layout.video_item, parent, false)
            VideoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = filesList[position]
        if (holder.itemViewType == PHOTO_ITEM){
            Glide.with(context).load(file.uri).into((holder as PhotoViewHolder).photoItemBinding.ivPhoto)
            holder.photoItemBinding.cvWallpaper.setOnClickListener {
                val path: String = filesList[position].path
                val destpath = Environment.getExternalStorageDirectory().absolutePath + Utils.SAVE_FOLDER_NAME
                val intent = Intent(context, ImageDetailActivity::class.java)
                intent.putExtra("DEST_PATH", destpath)
                intent.putExtra("FILE", path)
                intent.putExtra("FILENAME", file.fileName)
                intent.putExtra("URI", file.uri.toString())
                context.startActivity(intent)
            }
        }else{
            Glide.with(context).load(file.uri).into((holder as VideoViewHolder).videoItemBinding.ivVideo)
            holder.videoItemBinding.cvWallpaper.setOnClickListener {
                val path: String = filesList[position].path
                val destpath = Environment.getExternalStorageDirectory().absolutePath + Utils.SAVE_FOLDER_NAME
                val intent = Intent(context, VideoDetailActivity::class.java)
                intent.putExtra("DEST_PATH_VIDEO", destpath)
                intent.putExtra("FILE_VIDEO", path)
                intent.putExtra("FILENAME_VIDEO", file.fileName)
                intent.putExtra("URI_VIDEO", file.uri.toString())
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return filesList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (filesList[position].uri.toString().endsWith(".mp4")) {
            VIDEO_ITEM
        } else {
            PHOTO_ITEM
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: MutableList<IVModel>){
        filesList = mutableListOf()
        filesList.clear()
        filesList.addAll(newList)
//        filesList.distinct()
        notifyDataSetChanged()
    }
}