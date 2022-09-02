package neo.whatsapp_status_saver.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import neo.whatsapp_status_saver.AppPref
import neo.whatsapp_status_saver.Utils
import neo.whatsapp_status_saver.adapters.FavouriteAdapter
import neo.whatsapp_status_saver.databinding.FragmentFavoriteBinding
import neo.whatsapp_status_saver.model.IVModel
import java.util.regex.Pattern

class FavoriteFragment : Fragment() {

    private lateinit var binding: FragmentFavoriteBinding
    private lateinit var appPref: AppPref
    private lateinit var adapter: FavouriteAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFavoriteBinding.inflate(layoutInflater)
        appPref = AppPref(requireContext())

        val currentFileList = appPref.getString(AppPref.FAVOURITE_ITEMS).toString()
        val pattern = Pattern.compile(":")
        val items = pattern.split(currentFileList)
        Utils.favouriteList.clear()
        for (file in Utils.filesList) {
            for (item in items) {
                if (item == file.fileName){
                    Utils.favouriteList.add(IVModel(file.path,file.fileName,file.uri,file.lastModified))
                }
            }
        }

        binding.rvFavourite.setHasFixedSize(true)
        val staggeredGridLayoutManager =
            StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
        binding.rvFavourite.layoutManager = staggeredGridLayoutManager
        adapter = FavouriteAdapter(requireContext(), Utils.favouriteList)
        binding.rvFavourite.adapter = adapter
        adapter.notifyDataSetChanged()

        binding.btnRate.setOnClickListener {
            val str = "android.intent.action.VIEW"
            val sb2 = StringBuilder()
            sb2.append("http://play.google.com/store/apps/details?id=")
            sb2.append(requireContext().packageName)
            requireContext().startActivity(Intent(str, Uri.parse(sb2.toString())))
        }

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if (Utils.favouriteList.isEmpty()){
            binding.tvEmptyFav.visibility = View.VISIBLE
        }else{
            binding.tvEmptyFav.visibility = View.INVISIBLE
        }
        adapter.notifyDataSetChanged()
    }

}