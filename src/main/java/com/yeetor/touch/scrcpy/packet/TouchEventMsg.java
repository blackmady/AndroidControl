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

package com.yeetor.touch.scrcpy.packet;

import java.nio.ByteBuffer;

/**
 * Date: 2022/2/16
 *
 * @author alienhe
 */
public class TouchEventMsg implements ScControlMsg{

    private static final int TOUCH_EVENT_MSG_LENGTH = 26;

    private int action;

    private long pointerId;

    /**
     * position
     */
    private int x;
    private int y;
    private int screenWidth;
    private int screenHeight;

    private int pressure;

    private int buttons;

    public TouchEventMsg(int action, int x, int y, int screenWidth, int screenHeight, int pressure) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.pressure = pressure;
    }

    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(TOUCH_EVENT_MSG_LENGTH);
        byteBuffer.put((byte) TYPE_INJECT_TOUCH_EVENT);
        byteBuffer.put((byte) action);
        byteBuffer.putLong(pointerId);
        byteBuffer.putInt(x);
        byteBuffer.putInt(y);
        byteBuffer.putShort((short) screenWidth);
        byteBuffer.putShort((short) screenHeight);
        byteBuffer.putShort((short) pressure);
        byteBuffer.putInt(buttons);
        return byteBuffer.array();
    }
}
