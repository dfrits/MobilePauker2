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

package com.daniel.mobilepauker2.model;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.daniel.mobilepauker2.model.xmlsupport.FlashCardXMLStreamWriter;
import com.daniel.mobilepauker2.utils.Constants;
import com.daniel.mobilepauker2.utils.Log;

public class SaveLessonThreaded extends Thread {
    private final Handler mHandler;

    public SaveLessonThreaded(Handler h) {
        mHandler = h;
    }

    public void run() {
        Log.d("SaveLessonThreaded::run", "entry");
        FlashCardXMLStreamWriter.saveLesson();
        Log.d("SaveLessonThreaded::run", "After save lesson");
        sendMessage();
    }

    private void sendMessage() {
        Message msg = mHandler.obtainMessage();
        Bundle b = new Bundle();
        b.putBoolean(Constants.MESSAGE_KEY, true);
        msg.setData(b);
        mHandler.sendMessage(msg);
    }
}
