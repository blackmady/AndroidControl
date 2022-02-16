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

package com.yeetor.touch.scrcpy;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.IShellOutputReceiver;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.yeetor.adb.AdbDevice;
import com.yeetor.adb.AdbForward;
import com.yeetor.adb.AdbServer;
import com.yeetor.adb.AdbUtils;
import com.yeetor.touch.AbstractTouchEventService;
import com.yeetor.touch.TouchServiceException;
import com.yeetor.util.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class ScrcpyTouchService extends AbstractTouchEventService {

    private static final Logger LOGGER = Logger.getLogger(ScrcpyTouchService.class);

    public static final String REMOTE_DIR = "/data/local/tmp";
    public static final String EXECUTE_BIN = "scrcpy-server.jar";

    private Thread scrcpyServerCmdThread;
    private Thread scrcpySocketThread;
    private Socket controlSocket;
    private OutputStream controlSocketOutputStream;

    public ScrcpyTouchService(AdbDevice adbDevice) {
        super(adbDevice);
    }

    @Override
    protected boolean isInstalled() {
        // 判断指定路径下是否存在scrcpy-server.jar
        String result = AdbServer.executeShellCommand(this.device.getIDevice(),
                String.format("ls %s/%s", REMOTE_DIR, EXECUTE_BIN));
        return !StringUtils.contains(result, "No such file or directory");
    }

    @Override
    public void install() throws TouchServiceException {
        // adb push scrcpy-server.jar to /data/local/tmp
        File scrcpyServer = Constant.getScrcpyServerJar();
        if (scrcpyServer == null || !scrcpyServer.exists()) {
            throw new TouchServiceException("scrcpy server jar is not exists");
        }
        String remotePath = REMOTE_DIR + "/" + EXECUTE_BIN;
        AdbServer.server().executePushFile(device.getIDevice(), scrcpyServer.getAbsolutePath(), remotePath);
        AdbServer.executeShellCommand(device.getIDevice(), "chmod 777 " + remotePath);
    }

    @Override
    public void sendEvent(String msg) {
        // todo 这里msg是minitouch的协议，可以封装一个自己的协议
        if(controlSocketOutputStream == null){
           return;
        }

        // convert to scrcpy msg

    }

    @Override
    public void sendKeyEvent(int key) {

    }

    @Override
    public void inputText(String text) {

    }

    @Override
    public void start() throws TouchServiceException {
        if (!isInstalled()) {
            throw new TouchServiceException("scrcpy server jar is not installed on the device");
        }
        // adb shell CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process ./ com.genymobile.scrcpy.Server 1.22 log_level=verbose bit_rate=8000000 tunnel_forward=true
        // todo version
        String command = String.format(
                "CLASSPATH=%s app_process ./ com.genymobile.scrcpy.Server 1.22 log_level=verbose bit_rate=8000000 tunnel_forward=true",
                REMOTE_DIR + "/" + EXECUTE_BIN);
        LOGGER.info("scrcpy start command:" + command);
        scrcpyServerCmdThread = startScrcpy(command);

        // adb forward tcp:port scrcpy
        // todo 定制scrcpy监听的端口名称
        String scrcpySocketName = "scrcpy";
        AdbForward forward = AdbUtils.createForward(this.device, scrcpySocketName);
        if(forward == null){
            throw new TouchServiceException("create scrcpy forward failed!");
        }
        // connect to scrcpy socket to send control message
        scrcpySocketThread = startScrcpyControl("127.0.0.1",forward.getPort());
    }

    private Thread startScrcpyControl(String host, int port) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                int tryTime = 200;
                while(true){
                    // todo 修改scrcpy 去除多余的socket连接
                    Socket socket = null;
                    try {
                        // 第一个socket为video socket
                        socket  = new Socket(host,port);
                        // 第二个才为control socket
                        socket = new Socket(host,port);

                        if(socket.isConnected()){
                            controlSocket = socket;
                            controlSocketOutputStream = socket.getOutputStream();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        if(socket != null){
                            try {
                                socket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                    tryTime--;
                    if (tryTime == 0) {
                        break;
                    }
                }
            }
        });

        thread.start();
        return thread;
    }

    private Thread startScrcpy(String command) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ScrcpyTouchService.this.device.getIDevice().executeShellCommand(command, new IShellOutputReceiver() {
                        @Override
                        public void addOutput(byte[] data, int offset, int length) {
                            System.out.println("scrcpy start cmd output:" + new String(data,offset,length));
                        }

                        @Override
                        public void flush() {

                        }

                        @Override
                        public boolean isCancelled() {
                            return false;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return thread;
    }

    @Override
    public void kill() {
        if(scrcpyServerCmdThread != null){
            scrcpyServerCmdThread.stop();
        }

        if(controlSocket != null && controlSocket.isConnected()){
            try {
                controlSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            controlSocket = null;
        }
    }
}
