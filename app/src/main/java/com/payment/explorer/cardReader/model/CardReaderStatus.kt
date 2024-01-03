package com.payment.explorer.cardReader.model

enum class CardReaderStatus {
    ABORT,
    READY,
    PROCESSING,
    FAIL,
    CARD_READ_OK,
    SUCCESS
}