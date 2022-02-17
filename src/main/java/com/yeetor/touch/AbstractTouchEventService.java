/*
 *
 * MIT License
 *
 * Copyright (c) 2017 朱辉 https://blog.yeetor.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.yeetor.touch;

import com.yeetor.adb.AdbDevice;
import com.yeetor.adb.AdbUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTouchEventService implements TouchEventService{

    protected final AdbDevice device;

    // 是否每次都覆盖安装刷新
    protected boolean forceInstall;

    protected List<TouchEventServiceListener> listenerList = new ArrayList<TouchEventServiceListener>();

    public AbstractTouchEventService(AdbDevice adbDevice){
        this.device = adbDevice;
        try {
            installService();
        } catch (TouchServiceException e) {
            e.printStackTrace();
        }
    }

    protected void installService() throws TouchServiceException {
        if(device == null){
            throw new TouchServiceException("device can not be null");
        }

        if(isInstalled() && !forceInstall){
           return;
        }

        install();
    }

    protected abstract boolean isInstalled();

    @Override
    public void addEventListener(TouchEventServiceListener listener) {
        if (listener != null) {
            this.listenerList.add(listener);
        }
    }

    protected void onStartup(boolean success) {
        for (TouchEventServiceListener listener : listenerList) {
            listener.onStartup(this, success);
        }
    }

    protected void onClose() {
        for (TouchEventServiceListener listener : listenerList) {
            listener.onClose(this);
        }
    }

}
