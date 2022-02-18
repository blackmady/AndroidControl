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

package com.yeetor.touch.minitouch;

import com.android.ddmlib.IShellOutputReceiver;
import com.yeetor.adb.AdbDevice;
import com.yeetor.adb.AdbForward;
import com.yeetor.adb.AdbServer;
import com.yeetor.adb.AdbUtils;
import com.yeetor.touch.AbstractTouchEventService;
import com.yeetor.touch.TouchEventServiceListener;
import com.yeetor.touch.TouchServiceException;
import com.yeetor.util.Constant;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by harry on 2017/4/19.
 */
public class Minitouch extends AbstractTouchEventService {

    private static final String MINITOUCH_BIN_DIR = "resources" + File.separator + "minicap-bin";
    private static final String REMOTE_PATH = "/data/local/tmp";
    private static final String MINITOUCH_BIN = "minitouch";

    private Thread minitouchThread, minitouchInitialThread;
    private Socket minitouchSocket;
    private OutputStream minitouchOutputStream;
    private AdbForward forward;

    @Override
    protected boolean isInstalled() {
        if (device == null || device.getIDevice() == null) {
            return false;
        }
        String s = AdbServer.executeShellCommand(device.getIDevice(), String.format("%s/%s -i", REMOTE_PATH, MINITOUCH_BIN));
        // TODO: 这里简单处理了一下
        return s.startsWith("{");
    }

    @Override
    public void install() throws TouchServiceException {
        String sdk = device.getProperty(Constant.PROP_SDK);
        String abi = device.getProperty(Constant.PROP_ABI);

        if (StringUtils.isEmpty(sdk) || StringUtils.isEmpty(abi)) {
            throw new TouchServiceException("cant not get device info. please check device is connected");
        }

        sdk = sdk.trim();
        abi = abi.trim();

        File minitouch_bin = Constant.getMinitouchBin(abi);
        if (minitouch_bin == null || !minitouch_bin.exists()) {
            throw new TouchServiceException("File: " + minitouch_bin.getAbsolutePath() + " not exists!");
        }
        try {
            AdbServer.server().executePushFile(device.getIDevice(), minitouch_bin.getAbsolutePath(), REMOTE_PATH + "/" + MINITOUCH_BIN);
        } catch (Exception e) {
            throw new TouchServiceException(e.getMessage());
        }

        AdbServer.executeShellCommand(device.getIDevice(), "chmod 777 " + REMOTE_PATH + "/" + MINITOUCH_BIN);
    }

    public Minitouch(AdbDevice device) {
        super(device);
    }

    public Minitouch(String serialNumber) {
        this(AdbServer.server().getDevice(serialNumber));
    }

    public Minitouch() {
        this(AdbServer.server().getFirstDevice());
    }


    @Override
    public void start() throws TouchServiceException {
        forward = AdbUtils.createForward(device);
        if(forward == null){
            throw new TouchServiceException("create forward failed");
        }
        String command = "/data/local/tmp/minitouch" + " -n " + forward.getLocalAbstract();
        minitouchThread = startMinitouchThread(command);
        minitouchInitialThread = startInitialThread("127.0.0.1", forward.getPort());
    }

    @Override
    public void kill() {
        onClose();
        if (minitouchThread != null) {
            minitouchThread.stop();
        }
        // 关闭socket
        if (minitouchSocket != null && minitouchSocket.isConnected()) {
            try {
                minitouchSocket.close();
            } catch (IOException e) {
            }
            minitouchSocket = null;
        }
    }

    @Override
    public void sendTouchEvent(String str) {
        if (minitouchOutputStream == null) {
            return;
        }
        try {
            minitouchOutputStream.write(str.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendKeyEvent(int k) {
        AdbServer.executeShellCommand(device.getIDevice(), "input keyevent " + k);
    }

    @Override
    public void inputText(String str) {
        AdbServer.executeShellCommand(device.getIDevice(), "input text " + str);
    }

    private Thread startMinitouchThread(final String command) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    device.getIDevice().executeShellCommand(command, new IShellOutputReceiver() {
                        @Override
                        public void addOutput(byte[] bytes, int offset, int len) {
                            System.out.println(new String(bytes, offset, len));
                        }
                        @Override
                        public void flush() {}
                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                    }, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        return thread;
    }

    private Thread startInitialThread(final String host, final int port) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int tryTime = 200;
                while (true) {
                    Socket socket = null;
                    byte[] bytes = new byte[256];
                    try {
                        socket = new Socket(host, port);
                        InputStream inputStream = socket.getInputStream();
                        OutputStream outputStream = socket.getOutputStream();
                        int n = inputStream.read(bytes);

                        if (n == -1) {
                            Thread.sleep(10);
                            socket.close();
                        } else {
                            minitouchSocket = socket;
                            minitouchOutputStream = outputStream;
                            onStartup(true);
                            break;
                        }
                    } catch (Exception ex) {
                        if (socket != null) {
                            try {
                                socket.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        continue;
                    }
                    tryTime--;
                    if (tryTime == 0) {
                        onStartup(false);
                        break;
                    }
                }
            }
        });
        thread.start();
        return thread;
    }
}
