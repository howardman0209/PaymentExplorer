package com.payment.explorer.model

import com.payment.explorer.R

enum class Tool(val id: Int, val resourceId: Int) {
    UNKNOWN(0, R.string.label_unknown),
    DES(1, R.string.label_tool_des),
    RSA(2, R.string.label_tool_rsa),
    AES(3, R.string.label_tool_aes),
    MAC(4, R.string.label_tool_mac),
    HASH(5, R.string.label_tool_hash),
    BITWISE(6, R.string.label_tool_bitwise),
    CONVERTER(7, R.string.label_tool_converter),
    TLV_PARSER(8, R.string.label_tool_tlv_parser),
    CARD_SIMULATOR(9, R.string.label_tool_card_simulator),
    EMV_KERNEL(10, R.string.label_tool_emv_kernel),
    ARQC(11, R.string.label_tool_arqc),
    ODA(12, R.string.label_tool_oda),
    PIN_BLOCK(13, R.string.label_tool_pin_block),
    HOST(14, R.string.label_tool_host)
    ;

    companion object {
        private val values = values()
        fun getById(id: Int?): Tool = values.firstOrNull { it.id == id } ?: UNKNOWN
    }
}