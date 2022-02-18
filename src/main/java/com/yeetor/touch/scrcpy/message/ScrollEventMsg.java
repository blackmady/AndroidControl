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

package com.yeetor.touch.scrcpy.message;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;

public class ScrollEventMsg implements ScControlMsg{

    private static final Logger LOGGER = Logger.getLogger(ScrollEventMsg.class);

    public static final int SCROLL_EVENT_MSG_LENGTH = 25;

    /**
     * position
     */
    private int x;
    private int y;
    private int screenWidth;
    private int screenHeight;

    /**
     * 水平滚动/垂直滚动
     */
    private int hScroll;
    private int vScroll;
    private int buttons;

    public ScrollEventMsg() {
    }

    public ScrollEventMsg(int x, int y, int screenWidth, int screenHeight) {
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    @Override
    public byte[] serialize() {
        LOGGER.info("serialize scroll event message:" + this);
        ByteBuffer byteBuffer = ByteBuffer.allocate(SCROLL_EVENT_MSG_LENGTH);
        byteBuffer.put((byte) TYPE_INJECT_SCROLL_EVENT);
        byteBuffer.putInt(x);
        byteBuffer.putInt(y);
        byteBuffer.putShort((short) screenWidth);
        byteBuffer.putShort((short) screenHeight);
        byteBuffer.putInt(hScroll);
        byteBuffer.putInt(vScroll);
        byteBuffer.putInt(buttons);
        return byteBuffer.array();
    }

    @Override
    public String toString() {
        return "{" + "x=" + x + ", y=" + y + ", screenWidth=" + screenWidth + ", screenHeight="
                + screenHeight + ", hScroll=" + hScroll + ", vScroll=" + vScroll + ", buttons=" + buttons + '}';
    }
}
