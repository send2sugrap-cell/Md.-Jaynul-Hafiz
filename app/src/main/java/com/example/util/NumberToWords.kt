package com.example.util

object NumberToWords {

    private val tensNames = arrayOf(
        "", " Ten", " Twenty", " Thirty", " Forty", " Fifty",
        " Sixty", " Seventy", " Eighty", " Ninety"
    )

    private val numNames = arrayOf(
        "", " One", " Two", " Three", " Four", " Five", " Six", " Seven",
        " Eight", " Nine", " Ten", " Eleven", " Twelve", " Thirteen",
        " Fourteen", " Fifteen", " Sixteen", " Seventeen", " Eighteen", " Nineteen"
    )

    private val banglaNumNames = arrayOf(
        "", "এক", "দুই", "তিন", "চার", "পাঁচ", "ছয়", "সাত", "আট", "নয়", "দশ",
        "এগারো", "বারো", "তেরো", "চৌদ্দ", "পনেরো", "ষোলো", "সতেরো", "আঠারো", "উনিশ",
        "বিশ", "একুশ", "বাইশ", "তেইশ", "চব্বিশ", "পঁচিশ", "ছাব্বিশ", "সাতাশ", "আঠাশ", "ঊনত্রিশ",
        "ত্রিশ", "একত্রিশ", "বত্রিশ", "তেত্রিশ", "চৌত্রিশ", "পঁয়ত্রিশ", "ছত্রিশ", "সাঁইত্রিশ", "আটত্রিশ", "ঊনচল্লিশ",
        "চল্লিশ", "একচল্লিশ", "বিয়াল্লিশ", "তেতাল্লিশ", "চুয়াল্লিশ", "পঁয়তাল্লিশ", "ছেচল্লিশ", "সাতচল্লিশ", "আটচল্লিশ", "ঊনপঞ্চাশ",
        "পঞ্চাশ", "একান্ন", "বায়ান্ন", "তিপ্পান্ন", "চুয়ান্ন", "পঞ্চান্ন", "ছাপ্পান্ন", "সাতান্ন", "আটান্ন", "ঊনষাট",
        "ষাট", "একষট্টি", "বাষট্টি", "তেষট্টি", "চৌষট্টি", "পঁয়ষট্টি", "ছেষট্টি", "সাতষট্টি", "আটষট্টি", "ঊনসত্তর",
        "সত্তর", "একাতর", "বাহাত্তর", "তিয়াত্তর", "চুয়াত্তর", "পঁচাত্তর", "ছিয়াত্তর", "সাতাত্তর", "আটাত্তর", "ঊনআশি",
        "আশি", "একাশি", "বিরাশি", "তিরাশি", "চুরাশি", "পঁচাশি", "ছিয়াশি", "সাতাশি", "অষ্টআশি", "ঊননব্বই",
        "নব্বই", "একানব্বই", "বিরানব্বই", "তিরানব্বই", "চুরানব্বই", "পঁচানব্বই", "ছিয়ানব্বই", "সাতানব্বই", "আটানব্বই", "নিরানব্বই"
    )

    private fun convertEnglishLessThanOneThousand(number: Int): String {
        var soFar = ""
        var num = number

        if (num % 100 < 20) {
            soFar = numNames[num % 100]
            num /= 100
        } else {
            soFar = numNames[num % 10]
            num /= 10
            soFar = tensNames[num % 10] + soFar
            num /= 10
        }
        if (num == 0) return soFar
        return numNames[num] + " Hundred" + soFar
    }

    private fun convertEnglish(number: Long): String {
        if (number == 0L) {
            return "Zero"
        }
        var num = number
        var prefix = ""
        if (num < 0) {
            num = -num
            prefix = "Negative "
        }
        var current = ""
        var place = 0
        
        // Indian numbering system for English (Crore, Lakh, Thousand)
        if (num == 0L) return "Zero"
        
        var n = num
        var result = ""
        
        val crore = (n / 10000000).toInt()
        n %= 10000000
        val lakh = (n / 100000).toInt()
        n %= 100000
        val thousand = (n / 1000).toInt()
        n %= 1000
        val rest = n.toInt()
        
        if (crore > 0) {
            result += convertEnglishLessThanOneThousand(crore) + " Crore"
        }
        if (lakh > 0) {
            result += convertEnglishLessThanOneThousand(lakh) + " Lakh"
        }
        if (thousand > 0) {
            result += convertEnglishLessThanOneThousand(thousand) + " Thousand"
        }
        if (rest > 0) {
            result += convertEnglishLessThanOneThousand(rest)
        }
        
        return (prefix + result).trim()
    }

    private fun convertBangla(number: Long): String {
        if (number == 0L) return "শূন্য"
        var num = number
        var prefix = ""
        if (num < 0) {
            num = -num
            prefix = "মাইনাস "
        }
        
        var n = num
        var result = ""
        
        val crore = (n / 10000000).toInt()
        n %= 10000000
        val lakh = (n / 100000).toInt()
        n %= 100000
        val thousand = (n / 1000).toInt()
        n %= 1000
        val hundred = (n / 100).toInt()
        n %= 100
        val rest = n.toInt()
        
        if (crore > 0) {
            result += " " + convertBangla(crore.toLong()) + " কোটি"
        }
        if (lakh > 0) {
            result += " " + banglaNumNames[lakh] + " লাখ"
        }
        if (thousand > 0) {
            result += " " + banglaNumNames[thousand] + " হাজার"
        }
        if (hundred > 0) {
            result += " " + banglaNumNames[hundred] + " শত"
        }
        if (rest > 0) {
            result += " " + banglaNumNames[rest]
        }
        
        return (prefix + result).trim()
    }

    fun convert(amount: Double, isBangla: Boolean): String {
        val wholePart = amount.toLong()
        if (isBangla) {
            return convertBangla(wholePart) + " টাকা মাত্র"
        } else {
            return convertEnglish(wholePart) + " Taka Only"
        }
    }
}
