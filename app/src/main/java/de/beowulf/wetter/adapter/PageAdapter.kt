package de.beowulf.wetter.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import de.beowulf.wetter.fragments.*

class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {

    override fun getItemCount(): Int {
            return 5
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> GraphDayFragment()
            1 -> DayFragment()
            3 -> HourFragment()
            4 -> GraphHourFragment()
            else -> MainFragment()
        }
    }
}