package com.golden.template.extension

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
