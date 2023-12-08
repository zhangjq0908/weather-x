package de.beowulf.wetter.activities

import  android.os.Bundle
import android.util.TypedValue
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.adapter.PagerAdapter

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(GlobalFunctions().getTheme(this))
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!GlobalFunctions().gradient(this)) {
            val typedValue = TypedValue()
            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true)
            val root = findViewById<RelativeLayout>(R.id.Main)
            root.setBackgroundColor(typedValue.data)
        }

        val viewPager = findViewById<ViewPager2>(R.id.pager)
        val adapter = PagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(2, false)
        //cache all pages: needs a bit more ram, but looks smoother
        //and is less CPU intensive
        viewPager.offscreenPageLimit = 4
    }

    override fun onBackPressed() {
        val viewPager = findViewById<ViewPager2>(R.id.pager)
        if (viewPager.currentItem != 2) {
            if (viewPager.currentItem < 2)
                viewPager.currentItem++
            else
                viewPager.currentItem--
        } else {
            super.onBackPressed()
        }
    }
}