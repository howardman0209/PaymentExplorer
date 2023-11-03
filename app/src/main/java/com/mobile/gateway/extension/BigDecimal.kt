package com.mobile.gateway.extension

import java.math.BigDecimal

fun BigDecimal?.orZero(): BigDecimal = this ?: BigDecimal.ZERO
