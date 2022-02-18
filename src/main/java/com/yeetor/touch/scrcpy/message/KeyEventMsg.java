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

public class KeyEventMsg  implements ScControlMsg{

    private static final Logger LOGGER = Logger.getLogger(KeyEventMsg.class);

    public static final int KEY_EVENT_MSG_LENGTH = 14;

    private int action;
    private int keycode;
    private int repeat;
    private int metaState;

    public KeyEventMsg() {
    }

    @Override
    public byte[] serialize() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(KEY_EVENT_MSG_LENGTH);
        byteBuffer.put((byte) TYPE_INJECT_KEYCODE);
        byteBuffer.put((byte) action);
        byteBuffer.putInt(keycode);
        byteBuffer.putInt(repeat);
        byteBuffer.putInt(metaState);
        return byteBuffer.array();
    }
}
