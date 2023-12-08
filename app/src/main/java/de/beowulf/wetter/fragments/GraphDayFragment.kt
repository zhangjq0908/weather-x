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

class GraphDayFragment : Fragment() {

    private lateinit var binding: FragmentGraphBinding

    private val day = arrayOfNulls<String>(8)
    private val temp = arrayOfNulls<Int>(8)
    private val precipitation = arrayOfNulls<Int>(8)
    private val amount = arrayOfNulls<Double>(8)
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

        binding.graphView.setData(temp("min"), temp("max"))
        binding.graphView2.setData(precipitation(), thirdDataSet = rainAmount())

        return view
    }

    private fun temp(type: String): List<DataPoint> {
        val jsonObj = gf.result()

        for (i: Int in 0..3) {
            day[i * 2] = SimpleDateFormat(getString(R.string.daydate), Locale.getDefault()).format(
                Date(
                    jsonObj.getJSONArray("daily").getJSONObject(i * 2).getLong("dt") * 1000
                )
            )
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }
        for (i: Int in 0..7) {
            temp[i] = (gf.graphTemp(jsonObj.getJSONArray("daily").getJSONObject(i).getJSONObject("temp").getDouble(type)))
        }
        return (0..7).map {
            DataPoint(it, temp[it]!!, day[it])
        }
    }

    private fun precipitation(): List<DataPoint> {
        val jsonObj = gf.result()

        for (i: Int in 0..7) {
            precipitation[i] =
                (jsonObj.getJSONArray("daily").getJSONObject(i).getDouble("pop") * 100).toInt()
        }
        return (0..7).map {
            DataPoint(it, precipitation[it]!!, day[it])
        }
    }

    private fun rainAmount(): Amount {
        val jsonObj = gf.result()

        for (i: Int in 0..7) {
            val daily = jsonObj.getJSONArray("daily").getJSONObject(i)
            when {
                daily.has("rain") -> {
                    amount[i] = gf.graphRain(daily.getDouble("rain"))
                }
                daily.has("snow") -> {
                    amount[i] = gf.graphRain(daily.getDouble("snow"))
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
        for (i: Int in 0..7)
            amount[i] = amount[i]?.times(factor)
        return Amount(
            (0..7).map {
                DataPoint(it, amount[it]!!.toInt(), day[it])
            },
            factor,
            gf.unitRain()
        )
    }
}