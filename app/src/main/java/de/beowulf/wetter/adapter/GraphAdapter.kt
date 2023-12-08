package de.beowulf.wetter.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import de.beowulf.wetter.R
import java.text.DecimalFormat
import kotlin.math.abs


class TempGraphAdapter(context: Context, attributeSet: AttributeSet) : View(context, attributeSet) {

    private val dataSet = mutableListOf<DataPoint>()
    private val dataSet2 = mutableListOf<DataPoint>()
    private val dataSet3 = mutableListOf<DataPoint>()
    private var factor = 1
    private var xMin = 0
    private var xMax = 0f
    private var yMin = 0
    private var yNull = 0
    private var yMax = 0
    private var realY = 0
    private val bound = Rect()
    private val typedValue = TypedValue()
    private val dm = resources.displayMetrics
    private val radius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 5f, dm)
    private var rainUnit = ""
    private val df = DecimalFormat("#.#")

    private val dataPointPaint = Paint().apply {
        context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        color = typedValue.data
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 3.5f, dm)
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val dataPointFillPaint = Paint().apply {
        context.theme.resolveAttribute(R.attr.frontColor, typedValue, true)
        color = typedValue.data
        isAntiAlias = true
    }

    private val dataPointLinePaint = Paint().apply {
        context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        color = typedValue.data
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 4.5f, dm)
        isAntiAlias = true
    }

    private val paintDataGrid = Paint().apply {
        context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        color = typedValue.data
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13.5f, dm)
        isAntiAlias = true
    }

    private val paintYAxis = Paint().apply {
        context.theme.resolveAttribute(R.attr.frontColor, typedValue, true)
        color = typedValue.data
        typeface = Typeface.DEFAULT_BOLD
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13.5f, dm)
        strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 2f, dm)
        isAntiAlias = true
    }

    private val textPaintX = Paint().apply {
        context.theme.resolveAttribute(R.attr.frontColor, typedValue, true)
        color = typedValue.data
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 13.5f, dm)
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // draw axis
        if (dataSet.size == 8) {
            canvas.drawLine(0.toRealX() / 3, 0f, 0.toRealX() / 3, height.toFloat(), paintYAxis)
        } else {
            canvas.drawLine(0.toRealX() / 2, 0f, 0.toRealX() / 2, height.toFloat(), paintYAxis)
        }
        canvas.drawLine(0f, yNull.toRealY(), width.toFloat(), yNull.toRealY(), paintYAxis)

        //get text height
        paintYAxis.getTextBounds("0", 0, 1, bound)
        val textHeight: Float = bound.height().toFloat() / 2
        //get scale
        var add: Int = (yMax.toFloat() / 12f).toInt()
        if (add == 0 || yMax / add > 12)
            add++
        //write the scaled points
        var i = 0
        while (i <= yMax) {
            if (i != yNull && (i.toRealY() + (height / 12) - textHeight) + yMax.toRealY() > 0) {
                canvas.drawText(df.format(realY.toFloat().div(factor)), 1.5f, i.toRealY() + textHeight, paintYAxis)
                //horizontal grid lines
                if (dataSet.size == 8) {
                    canvas.drawLine(0.toRealX() / 3, i.toRealY(), width.toFloat(), i.toRealY(), paintDataGrid)
                } else {
                    canvas.drawLine(0.toRealX() / 2, i.toRealY(), width.toFloat(), i.toRealY(), paintDataGrid)
                }
            }
            realY += add
            i += add
        }
        //restore realY
        realY -= i

        dataSet.forEachIndexed { index, currentDataPoint ->
            val realX = currentDataPoint.xVal.toRealX()
            val realY = (currentDataPoint.yVal + yNull).toRealY()
            val xValue = currentDataPoint.xValue

            //vertical grid lines
            if (index % 2 == 0) {
                canvas.drawLine(realX, 0f, realX, 0.toRealY(), paintDataGrid)
            }

            //draw line to next point
            if (index < dataSet.size - 1) {
                val nextDataPoint = dataSet[index + 1]
                val endX = nextDataPoint.xVal.toRealX()
                val endY = (nextDataPoint.yVal + yNull).toRealY()
                canvas.drawLine(realX, realY, endX, endY, dataPointLinePaint)
            }

            //draw points
            canvas.drawCircle(realX, realY, radius, dataPointFillPaint)
            canvas.drawCircle(realX, realY, radius, dataPointPaint)

            //set date/hour
            if (xValue != null) {
                canvas.drawText(xValue, realX, height.toFloat() - 5f, textPaintX)
            }
            if (index == 0 && dataSet2 != emptyList<DataPoint>()) {
                canvas.drawText(
                    context.getString(R.string.Min),
                    realX - ((realX - 0.toRealX() / 3) / 2),
                    realY,
                    textPaintX
                )
            } else if (index == 0 && dataSet3 != emptyList<DataPoint>()) {
                val l = if (realY == 0.toRealY()) {
                    7.toRealY()
                } else {
                    realY
                }
                canvas.drawText(
                    "%",
                    realX - ((realX - 0.toRealX() / 3) / 2),
                    l,
                    paintDataGrid
                )
            }
        }

        dataSet2.forEachIndexed { index, currentDataPoint ->
            val realX = currentDataPoint.xVal.toRealX()
            val realY = (currentDataPoint.yVal + yNull).toRealY()

            //draw line to next point
            if (index < dataSet2.size - 1) {
                val nextDataPoint = dataSet2[index + 1]
                val endX = nextDataPoint.xVal.toRealX()
                val endY = (nextDataPoint.yVal + yNull).toRealY()
                canvas.drawLine(realX, realY, endX, endY, dataPointLinePaint)
            }

            //draw points
            canvas.drawCircle(realX, realY, radius, dataPointFillPaint)
            canvas.drawCircle(realX, realY, radius, dataPointPaint)
            if (index == 0) {
                canvas.drawText(
                    context.getString(R.string.Max),
                    realX - ((realX - 0.toRealX() / 3) / 2),
                    realY,
                    textPaintX
                )
            }
        }

        dataSet3.forEachIndexed { index, currentDataPoint ->
            val realX = currentDataPoint.xVal.toRealX()
            val realY = (currentDataPoint.yVal + yNull).toRealY()

            //draw line to next point
            if (index < dataSet3.size - 1) {
                val nextDataPoint = dataSet3[index + 1]
                val endX = nextDataPoint.xVal.toRealX()
                val endY = (nextDataPoint.yVal + yNull).toRealY()
                canvas.drawLine(realX, realY, endX, endY, paintYAxis)
            }

            //draw points
            canvas.drawCircle(realX, realY, radius, paintYAxis)
            if (index == 0) {
                canvas.drawText(
                    rainUnit,
                    realX - ((realX - 0.toRealX() / 3) / 2),
                    realY,
                    textPaintX
                )
            }
        }

        canvas.save()
    }

    fun setData(newDataSet: List<DataPoint>, secondDataSet: List<DataPoint>? = null, thirdDataSet: Amount? = null) {
        xMin = newDataSet.minByOrNull { it.xVal }?.xVal ?: 0
        xMax = newDataSet.maxByOrNull { it.xVal }?.xVal?.toFloat() ?: 0f
        yMin = newDataSet.minByOrNull { it.yVal }?.yVal ?: 0
        yMax = newDataSet.maxByOrNull { it.yVal }?.yVal ?: 0
        realY = newDataSet.minByOrNull { it.yVal }?.yVal ?: 0
        if (secondDataSet != null)
            yMax = secondDataSet.maxByOrNull { it.yVal }?.yVal ?: 0

        if (yMin < 0) {
            yMax -= yMin
            yNull = yMin * -1
            if (yMax < yNull)
                yMax = yNull
            yMin = 0
        } else if (yMin > 0) {
            realY = 0
            //When value is very high (e.g. Kelvin usage), don't set Minimum to 0
            if (yMin > 50 && thirdDataSet == null) {
                yMin -= 10
                yMax -= yMin
                realY = yMin
                yNull = -yMin
                yMin = 0
            }
        }

        if (thirdDataSet != null) {
            yMax = 120
        } else {
            yMax += abs(yMax / 4)
            if (yMax / 4 == 0)
                yMax++
        }
        xMax += 4f
        dataSet.clear()
        dataSet.addAll(newDataSet)
        dataSet2.clear()
        if (secondDataSet != null)
            dataSet2.addAll(secondDataSet)
        dataSet3.clear()
        if (thirdDataSet != null) {
            dataSet3.addAll(thirdDataSet.dataPoint)
            factor = thirdDataSet.factor
            rainUnit = thirdDataSet.unit
        }
        invalidate()
    }

    private fun Int.toRealX() = (toFloat() + 3f) / xMax * width
    private fun Int.toRealY() = height - (toFloat() / yMax * height + (height / 12))
}

data class DataPoint(
    val xVal: Int,
    val yVal: Int,
    val xValue: String?
)

data class Amount(
    val dataPoint: List<DataPoint>,
    val factor: Int,
    val unit: String
)