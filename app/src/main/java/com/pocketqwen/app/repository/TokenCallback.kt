package com.pocketqwen.app.repository

interface TokenCallback {
    fun onToken(text: String)

    /** Called once when native generation ends; status is one of the documented terminal reasons. */
    fun onComplete(status: String)
}
