package com.example.util

object CalculatorEngine {

    data class CalculationResult(
        val totalCost: Double,
        val profit: Double,
        val grandTotal: Double,
        val perUnitCost: Double
    )

    fun calculateSummary(totalOperationalCosts: Double, designCharge: Double, profitPercent: Double, quantity: Double): CalculationResult {
        val totalCost = totalOperationalCosts + designCharge
        val profit = (profitPercent / 100) * totalCost
        val grandTotal = totalCost + profit
        val perUnitCost = if (quantity > 0) grandTotal / quantity else 0.0
        return CalculationResult(totalCost, profit, grandTotal, perUnitCost)
    }

    // Offset Engine
    fun calculatePaperCost(quantity: Double, extra: Double, yield: Double, rate: Double): Double {
        return ((quantity + extra) / yield) * rate
    }

    fun calculatePositiveCost(width: Double, height: Double, colors: Double, rate: Double, minCharge: Double): Double {
        val cost = width * height * colors * rate
        return maxOf(cost, minCharge)
    }

    fun calculatePlateCost(totalSheets: Double, yield: Double, colors: Double, rate: Double): Double {
        return (totalSheets / yield) * colors * rate
    }

    fun calculatePrintingCharge(sheets: Double, part: Double, color: Double, side: Double, impressionFactor: Double, rate: Double, minCharge: Double): Double {
        val totalImpressions = (sheets * part * color * side) / impressionFactor
        val cost = totalImpressions * rate
        return maxOf(cost, minCharge)
    }

    fun calculateLaminationCost(width: Double, height: Double, sheets: Double, sides: Double, rate: Double): Double {
        return width * height * sheets * sides * rate
    }

    fun calculateDieBlockCost(width: Double, height: Double, rate: Double): Double {
        return width * height * rate
    }

    fun calculateDieCuttingCost(quantity: Double, rate: Double): Double {
        return (quantity / 1000.0) * rate
    }

    fun calculateFoilStampingCost(width: Double, height: Double, rate: Double): Double {
        return width * height * rate
    }

    fun calculatePaperCuttingCost(sheets: Double, rimFactor: Double, rate: Double): Double {
        return (sheets * rimFactor) * rate
    }

    fun calculatePackagingCost(quantity: Double, unitsPerPacket: Double, packingRate: Double, packets: Double, deliveryRate: Double): Double {
        return (quantity / unitsPerPacket) * packingRate + (packets * deliveryRate)
    }

    fun calculateOverheadDesign(quantity: Double, overheadRate: Double, designCharge: Double): Double {
        return (quantity * overheadRate) + designCharge
    }

    // Large Format Digital
    fun calculateLargeFormatDigital(width: Double, height: Double, quantity: Double, baseRate: Double, finishingCharge: Double): Double {
        val totalSqFt = width * height * quantity
        return (totalSqFt * baseRate) + finishingCharge
    }

    // Branding
    fun calculateCommercialBranding(quantity: Double, baseRate: Double, setupCharge: Double): Double {
        return (quantity * baseRate) + setupCharge
    }
}
