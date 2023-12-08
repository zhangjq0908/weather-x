package de.beowulf.wetter.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.beowulf.wetter.GlobalFunctions
import de.beowulf.wetter.R
import de.beowulf.wetter.adapter.Amount
import de.beowulf.wetter.adapter.DataPoint
import de.beowulf.wetter.databinding.FragmentGraphBinding
import java.text.SimpleDateFormat
import java.util.*

class GraphHourFragment : Fragment() {

    private lateinit var binding: FragmentGraphBinding

    private val hour = arrayOfNulls<String>(16)
    private val temp = arrayOfNulls<Int>(16)
    private val precipitation = arrayOfNulls<Int>(16)
    private val amount = arrayOfNulls<Double>(16)
    private val gf = GlobalFunctions()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGraphBinding.inflate(layoutInflater)
        val view: View = binding.root

        gf.initializeContext(requireContext())

        val tempTitle: String = getString(R.string.temperature) + gf.unitTemp()
        binding.TempTitle.text = tempTitle

        binding.graphView.setData(temp())
        binding.graphView2.setData(precipitation(), thirdDataSet = rainAmount())

        return view
    }

    private fun temp(): List<DataPoint> {
        val jsonObj = gf.result()

        for (i: Int in 0..3) {
            hour[i * 4] = SimpleDateFormat(gf.getTime(), Locale.ROOT).format(
                Date(
                    jsonObj.getJSONArray("hourly").getJSONObject(i * 4).getLong("dt") * 1000
                )
            )
        }
        for (i: Int in 0..15) {
            temp[i] = gf.graphTemp(jsonObj.getJSONArray("hourly").getJSONObject(i).getDouble("temp"))
        }
        return (0..15).map {
            DataPoint(it, temp[it]!!, hour[it])
        }
    }

    private fun precipitation(): List<DataPoint> {
        val jsonObj = gf.result()

        for (i: Int in 0..15) {
            precipitation[i] =
                (jsonObj.getJSONArray("hourly").getJSONObject(i).getDouble("pop") * 100).toInt()
        }
        return (0..15).map {
            DataPoint(it, precipitation[it]!!, hour[it])
        }
    }

    private fun rainAmount(): Amount {
        val jsonObj = gf.result()

        for (i: Int in 0..15) {
            val hourly = jsonObj.getJSONArray("hourly").getJSONObject(i)
            when {
                hourly.has("rain") -> {
                    amount[i] = gf.graphRain(hourly.getJSONObject("rain").getDouble("1h"))
                }
                hourly.has("snow") -> {
                    amount[i] = gf.graphRain(hourly.getJSONObject("snow").getDouble("1h"))
                }
                else -> {
                    amount[i] = gf.graphRain(0.0)
                }
            }
        }
        var factor: Int = 100.div(amount.maxByOrNull { it?:0.0 }?:0.0).toInt() / 10 * 10
        if (factor > 200 || factor == 0)
            factor = 1
        //set title with correct factor
        val rainTitle: String = getString(R.string.rain) + " (${gf.unitRain()} | %)"
        binding.RainTitle.text = rainTitle
        for (i: Int in 0..15)
            amount[i] = amount[i]?.times(factor)
        return Amount(
            (0..15).map {
                DataPoint(it, amount[it]!!.toInt(), hour[it])
            },
            factor,
            gf.unitRain()
        )
    }
}