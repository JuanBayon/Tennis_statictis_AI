package com.uoc.tennis.AI

import com.uoc.tennis.MainActivity
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics
import org.apache.commons.math.stat.descriptive.moment.Kurtosis
import org.apache.commons.math.stat.descriptive.moment.Skewness
import org.apache.commons.math.stat.descriptive.moment.StandardDeviation
import org.apache.commons.math.stat.descriptive.rank.Median
import java.text.SimpleDateFormat
import java.time.Period
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.pow
import kotlin.streams.toList


class TransformSensorData(
    data: ArrayList<MainActivity.SensorData>,
    gender: String,
    age: String,
    laterality: String,
    backhand: String,
    hitType: String,
    playerType: String
) {

    private var list: ArrayList<MainActivity.SensorData> = data
    private var gender: String = gender
    private var age: String = age
    private var laterality: String = laterality
    private var backhand: String = backhand
    private var strokeType: String = hitType
    private var playerType: String = playerType

    private var resultMap: HashMap<String, Any> = HashMap()

    fun getStatisticalData(): ArrayList<HashMap<String, Any>> {
        val result = ArrayList<HashMap<String, Any>>()

        val acc_list: List<MainActivity.SensorData> = list.stream().filter{ a -> a.sensor == "Accelerometer" }.toList()
        val gyr_list: List<MainActivity.SensorData> = list.stream().filter{ a -> a.sensor == "Gyroscope" }.toList()

        val acc_parts = ceil((acc_list.size.toDouble() / 130)).toInt()
        val acc_list_parts = ArrayList<ArrayList<MainActivity.SensorData>>()
        var index = 0
        for (i in 1..acc_parts) {
            if (i != acc_parts) {
                acc_list_parts.add(list.subList(index, 130*i) as ArrayList<MainActivity.SensorData>)

            } else {
                acc_list_parts.add(list.subList(index, acc_parts) as ArrayList<MainActivity.SensorData>)
            }
            index = 130*i + 1
        }

        val gyr_parts = ceil((acc_list.size.toDouble() / 260)).toInt()
        val gyr_list_parts = ArrayList<ArrayList<MainActivity.SensorData>>()
        index = 0
        for (i in 1..gyr_parts) {
            if (i != gyr_parts) {
                gyr_list_parts.add(list.subList(index, 260*i) as ArrayList<MainActivity.SensorData>)
            } else {
                gyr_list_parts.add(list.subList(index, gyr_parts) as ArrayList<MainActivity.SensorData>)
            }
            index = 130*i + 1
        }

        val iterations = min(acc_list_parts.size, gyr_list_parts.size)
        for (i in iterations-1  downTo 0 step 1) {
            val resultMap = HashMap<String, Any>()
            val xs_acc = acc_list_parts.elementAt(i).stream().mapToDouble { a -> a.x}.toList()
            val ys_acc = acc_list_parts.elementAt(i).stream().mapToDouble { a -> a.y}.toList()
            val zs_acc = acc_list_parts.elementAt(i).stream().mapToDouble { a -> a.z}.toList()

            val xs_gyr = gyr_list_parts.elementAt(i).stream().mapToDouble { a -> a.x}.toList()
            val ys_gyr = gyr_list_parts.elementAt(i).stream().mapToDouble { a -> a.y}.toList()
            val zs_gyr = gyr_list_parts.elementAt(i).stream().mapToDouble { a -> a.z}.toList()

            resultMap["sessionID"] = list[0].sessionID
            val period_acc = Period.between(SimpleDateFormat("dd-MM-yyyy").parse(acc_list.elementAt(-1).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                SimpleDateFormat("dd-MM-yyyy").parse(acc_list.elementAt(0).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            resultMap["time_acc"] = period_acc.days * 3600 * 24

            val period_gyr = Period.between(SimpleDateFormat("dd-MM-yyyy").parse(gyr_list.elementAt(-1).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                SimpleDateFormat("dd-MM-yyyy").parse(gyr_list.elementAt(0).timestamp).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
            resultMap["time_gyr"] = period_gyr.days * 3600 * 24

            calculateStatistics(xs_acc, "acc", "x")
            calculateStatistics(ys_acc, "acc", "y")
            calculateStatistics(zs_acc, "acc", "z")

            calculateStatistics(xs_gyr, "gyr", "x")
            calculateStatistics(ys_gyr, "gyr", "y")
            calculateStatistics(zs_gyr, "gyr", "z")

            resultMap[MainActivity.AGE] = age
            resultMap[MainActivity.GENDER] = gender
            resultMap[MainActivity.STROKE_TYPE] = strokeType
            resultMap[MainActivity.PLAYER_TYPE] = playerType
            resultMap[MainActivity.LATERALITY] = laterality
            resultMap[MainActivity.BACKHAND] = backhand

            result.add(resultMap)
        }

        return result
    }

    private fun calculateStatistics(data: List<Double>, suffix: String, prefix: String) {

        val average = data.average()
        resultMap["${prefix}_mean_${suffix}"] = average
        val sd = StandardDeviation()
        resultMap["${prefix}_std_${suffix}"] = sd.evaluate(data.toDoubleArray())
        resultMap["${prefix}_min_${suffix}"] = data.min()
        resultMap["${prefix}_max_${suffix}"] = data.max()
        val median = Median()
        resultMap["${prefix}_median_${suffix}"] = median.evaluate(data.toDoubleArray())
        resultMap["${prefix}_mad_${suffix}"] = median.evaluate(data.map { x -> abs(x - median.evaluate(x)) }.toDoubleArray())
        val da = DescriptiveStatistics(data.toDoubleArray())
        resultMap["${prefix}_iqr_${suffix}"] = da.getPercentile(75.0) - da.getPercentile(25.0)
        resultMap["${prefix}_pcount_${suffix}"] = data.filter { x -> x > 0 }.sum()
        resultMap["${prefix}_ncount_${suffix}"] = data.filter { x -> x < 0 }.sum()
        resultMap["${prefix}_abvmean_${suffix}"] = data.filter { x -> x > average}.sum()
        // ELIMINAR CNTPEAKS DE PYTHON!!!
        val sk = Skewness()
        resultMap["${prefix}_skew_${suffix}"] = sk.evaluate(data.toDoubleArray())
        val krt = Kurtosis()
        resultMap["${prefix}_kurt_${suffix}"] = krt.evaluate(data.toDoubleArray())
        if (suffix == "acc") {
            resultMap["${prefix}_energy_${suffix}"] = data.map { x -> x.pow(2.0) }.sum() / 100
        }

    }


}