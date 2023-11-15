package com.mobile.gateway.model

import com.mobile.gateway.R

enum class Category(val id: Int, val resourceId: Int) {
    UNKNOWN(0, R.string.label_unknown),
    GENERIC(1, R.string.label_category_generic),
    EMV(2, R.string.label_category_emv),
    ACQUIRER(3, R.string.label_category_acquirer),
    ;

    companion object {
        private val values = values()
        fun getById(id: Int?): Category = values.firstOrNull { it.id == id } ?: UNKNOWN
    }
}
