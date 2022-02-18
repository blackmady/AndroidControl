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

/**
 * Date: 2022/2/16
 *
 * @author alienhe
 */
public interface ScControlMsg {

    int TYPE_INJECT_KEYCODE = 0;
    int TYPE_INJECT_TEXT = 1;
    int TYPE_INJECT_TOUCH_EVENT = 2;
    int TYPE_INJECT_SCROLL_EVENT = 3;
    int TYPE_BACK_OR_SCREEN_ON = 4;
    int TYPE_EXPAND_NOTIFICATION_PANEL = 5;
    int TYPE_EXPAND_SETTINGS_PANEL = 6;
    int TYPE_COLLAPSE_PANELS = 7;
    int TYPE_GET_CLIPBOARD = 8;
    int TYPE_SET_CLIPBOARD = 9;
    int TYPE_SET_SCREEN_POWER_MODE = 10;
    int TYPE_ROTATE_DEVICE = 11;
    
    byte[] serialize();

}
