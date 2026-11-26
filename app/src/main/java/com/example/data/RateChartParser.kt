package com.example.data

import kotlin.math.abs

/**
 * Rate list ke text (PDF se nikla hua ya paste kiya hua) ko FAT x SNF chart me badalta hai.
 * Format: pehli line par SNF columns (8.00 8.10 ...), phir har line: FAT, uske saare rate.
 */
object RateChartParser {

    data class Result(
        val rates: Map<String, Double>,
        val fatValues: List<Double>,
        val snfValues: List<Double>,
        val cellCount: Int
    )

    private val NUMBER = Regex("""\d+\.\d+|\d+""")

    private fun round1(v: Double): Double = Math.round(v * 10) / 10.0

    private fun isStepSequence(nums: List<Double>): Boolean {
        if (nums.size < 5) return false
        return nums.zipWithNext().all { (a, b) -> abs((b - a) - 0.1) < 0.011 }
    }

    fun parse(text: String): Result? {
        val lines: List<List<Double>> = text.lines().map { line ->
            NUMBER.findAll(line).mapNotNull { it.value.toDoubleOrNull() }.toList()
        }.filter { it.isNotEmpty() }

        var snfCols: List<Double> = RateChart.SNF_VALUES
        var headerIndex = -1
        for ((i, nums) in lines.withIndex()) {
            if (nums.all { it in 6.0..11.0 } && isStepSequence(nums)) {
                snfCols = nums.map { round1(it) }
                headerIndex = i
                break
            }
        }

        val rates = mutableMapOf<String, Double>()
        val fats = sortedSetOf<Double>()
        for ((i, nums) in lines.withIndex()) {
            if (i == headerIndex || nums.size < 2) continue
            val fat = round1(nums[0])
            if (fat !in 2.0..12.0) continue
            val rowRates = nums.drop(1)
            if (rowRates.size < snfCols.size) continue
            var added = 0
            snfCols.forEachIndexed { idx, snf ->
                val rate = rowRates[idx]
                if (rate in 1.0..500.0) {
                    rates[RateChart.key(fat, snf)] = DairyFormatters.roundTwoDecimals(rate)
                    added++
                }
            }
            if (added >= snfCols.size - 1) fats.add(fat)
        }

        // Kam se kam 5 FAT rows mili hon tabhi list maano
        if (fats.size < 5 || rates.size < snfCols.size * 5) return null
        return Result(
            rates = rates,
            fatValues = fats.toList(),
            snfValues = snfCols,
            cellCount = rates.size
        )
    }
}
