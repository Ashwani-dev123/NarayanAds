package com.ads.narayan.ads.purchase_billing.listener

fun interface ConnectionListener {

    fun onConnectionState(connected: Boolean, disconnected: Boolean)

}