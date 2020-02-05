/* 
 * Copyright 2011 Brian Ford
 * 
 * This file is part of Pocket Pauker.
 * 
 * Pocket Pauker is free software: you can redistribute it and/or modify it under the 
 * terms of the GNU General Public License as published by the Free Software Foundation, 
 * either version 3 of the License, or (at your option) any later version.
 * 
 * Pocket Pauker is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more 
 * details.
 * 
 * See http://www.gnu.org/licenses/.

*/
package com.daniel.mobilepauker2.model

import android.os.Bundle
import android.os.Handler
import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLStreamWriter
import com.daniel.mobilepauker2.utils.Constants
import com.daniel.mobilepauker2.utils.Log

class SaveLessonThreaded(private val mHandler: Handler) : Thread() {
    override fun run() {
        Log.d("SaveLessonThreaded::run", "entry")
        try {
            FlashCardXMLStreamWriter.saveLesson()
            sendMessage()
        } catch (e: SecurityException) {
            sendErrorMessage(e.message)
        }
        Log.d("SaveLessonThreaded::run", "After save lesson")
    }

    private fun sendMessage() {
        val msg = mHandler.obtainMessage()
        val b = Bundle()
        b.putBoolean(Constants.MESSAGE_BOOL_KEY, true)
        msg.data = b
        mHandler.sendMessage(msg)
    }

    private fun sendErrorMessage(message: String?) {
        val msg = mHandler.obtainMessage()
        val b = Bundle()
        b.putBoolean(Constants.MESSAGE_BOOL_KEY, false)
        b.getString(Constants.MESSAGE_MSG_KEY, message)
        msg.data = b
        mHandler.sendMessage(msg)
    }

}