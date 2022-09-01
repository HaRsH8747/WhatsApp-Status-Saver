package com.example.whatsapp_status_saver.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int {
        return 4
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                HomeFragment()
            }
            1 -> {
                ImageFragment()
            }
            2 -> {
                VideoFragment()
            }
            3 -> {
                FavoriteFragment()
            }
            else -> {
                HomeFragment()
            }
        }
    }
}