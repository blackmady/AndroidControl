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

import com.android.ddmlib.IShellOutputReceiver;
import com.google.common.collect.Lists;
import com.yeetor.adb.AdbDevice;
import com.yeetor.adb.AdbForward;
import com.yeetor.adb.AdbServer;
import com.yeetor.adb.AdbUtils;
import com.yeetor.touch.AbstractTouchEventService;
import com.yeetor.touch.TouchServiceException;
import com.yeetor.touch.scrcpy.message.ScControlMsg;
import com.yeetor.touch.scrcpy.message.ScrollEventMsg;
import com.yeetor.touch.scrcpy.message.TouchEventMsg;
import com.yeetor.util.Constant;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.yeetor.adb.AdbDevice.SCREEN_HEIGHT;
import static com.yeetor.adb.AdbDevice.SCREEN_WIDTH;

public class ScrcpyTouchService extends AbstractTouchEventService {

    private static final Logger LOGGER = Logger.getLogger(ScrcpyTouchService.class);

    private static final String REMOTE_DIR = "/data/local/tmp";
    private static final String EXECUTE_BIN = "scrcpy-server.jar";
    private static final int DEBUGGER_PORT = 5005;

    /**
     * debug时手动adb启动scrcpy-server，只需要直接连接端口
     */
    private static final boolean isDebug = false;

    private Thread scrcpyServerCmdThread;
    private Thread scrcpySocketThread;
    private AdbForward forward;
    private Socket videoSocket;
    private Socket controlSocket;
    private OutputStream controlSocketOutputStream;

    private final int screenWidth;
    private final int screenHeight;

    /**
     * minitouch协议按压和松开分成两个报文发送，且松开不携带坐标数据，而scrcpy按压松开均需要坐标数据，因此需要保存上次坐标信息
     */
    private int x;
    private int y;
    private int pressure;

    public ScrcpyTouchService(AdbDevice adbDevice) {
        super(adbDevice);
        LOGGER.info("============> create scrcpy touch service:" + adbDevice.getSerialNumber());
        this.screenWidth = Integer.parseInt(adbDevice.getProperty(SCREEN_WIDTH));
        this.screenHeight = Integer.parseInt(adbDevice.getProperty(SCREEN_HEIGHT));
        this.forceInstall = true;
    }

    @Override
    protected boolean isInstalled() {
        // 判断指定路径下是否存在scrcpy-server.jar
        String result = AdbServer.executeShellCommand(this.device.getIDevice(),
                String.format("ls %s/%s", REMOTE_DIR, EXECUTE_BIN));
        LOGGER.info("check scrcpy server is install:" + result);
        return !StringUtils.contains(result, "No such file or directory");
    }

    @Override
    public void install() throws TouchServiceException {
        if (isDebug) {
            return;
        }
        // adb push scrcpy-server.jar to /data/local/tmp
        File scrcpyServer = Constant.getScrcpyServerJar();
        if (!scrcpyServer.exists()) {
            throw new TouchServiceException("scrcpy server jar is not exists:" + scrcpyServer.getAbsolutePath());
        }
        String remotePath = REMOTE_DIR + "/" + EXECUTE_BIN;
        AdbServer.server().executePushFile(device.getIDevice(), scrcpyServer.getAbsolutePath(), remotePath);
        LOGGER.info("push to phone:" + remotePath);
        AdbServer.executeShellCommand(device.getIDevice(), "chmod 777 " + remotePath);
    }

    @Override
    public void sendTouchEvent(String msg) {
        // todo 这里msg是minitouch的协议，可以封装一个自己的协议
        if (controlSocketOutputStream == null) {
            return;
        }

        // convert to scrcpy msg
        LOGGER.info("receive event:" + msg);
        if (StringUtils.isBlank(msg)) {
            return;
        }

        // minitouch 一次会提交多行命令，以c结尾代表commit提交
        int contact;

        for (String line : msg.split("\n")) {
            if (StringUtils.isBlank(line) || "\n".equals(line)) {
                continue;
            }

            // process every line
            List<String> packets = Lists.newArrayList();
            for (String c : line.split(" ")) {
                if (StringUtils.isBlank(c)) {
                    continue;
                }
                packets.add(c);
            }

            ScControlMsg eventMsg = null;
            // https://testerhome.com/topics/4400
            switch (packets.get(0)) {
            case "d":
                // d <contact> <x> <y> <pressure> 按下
                contact = Integer.parseInt(packets.get(1));
                x = Integer.parseInt(packets.get(2));
                y = Integer.parseInt(packets.get(3));
                pressure = Integer.parseInt(packets.get(4));

                // https://developer.android.com/reference/android/view/MotionEvent
                // action: ACTION_UP 1 / ACTION_DOWN 0
                eventMsg = new TouchEventMsg(0, contact, x, y, screenWidth, screenHeight, pressure);
                break;
            case "u":
                // u <contact> 松开
                contact = Integer.parseInt(packets.get(1));
                if (x == -1 || y == -1 || pressure == -1) {
                    LOGGER.error("松开事件没有对应的按压事件!");
                    break;
                }
                // action: ACTION_UP 1 / ACTION_DOWN 0
                eventMsg = new TouchEventMsg(1, contact, x, y, screenWidth, screenHeight, pressure);
                break;
            case "m":
                // m <contact> <x> <y> <pressure> 滑动
                contact = Integer.parseInt(packets.get(1));
                x = Integer.parseInt(packets.get(2));
                y = Integer.parseInt(packets.get(3));

                // action: ACTION_MOVE
                eventMsg = new TouchEventMsg(2, contact, x, y, screenWidth, screenHeight, pressure);
                break;
            case "c":
                // ignore the commit message
                break;
            default:
                LOGGER.info("暂不支持的事件:" + packets.get(0));
                return;
            }

            if (eventMsg != null) {
                try {
                    if (!controlSocket.isClosed()) {
                        controlSocketOutputStream.write(eventMsg.serialize());
                    } else {
                        LOGGER.error("control socket is closed!!!!");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void sendKeyEvent(int key) {
        // ref: scrcpy app/tests/test_control_msg_serialize.c:test_serialize_inject_keycode
        // can use scrcpy or use adb shell input keyevent
        AdbServer.executeShellCommand(device.getIDevice(), "input keyevent " + key);
    }

    @Override
    public void inputText(String text) {
        AdbServer.executeShellCommand(device.getIDevice(), "input text " + text);
    }

    @Override
    public void start() throws TouchServiceException {
        if (!isInstalled()) {
            throw new TouchServiceException("scrcpy server jar is not installed on the device");
        }
        // adb shell CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process ./ com.genymobile.scrcpy.Server 1.22 log_level=verbose bit_rate=8000000 tunnel_forward=true
        // todo version
        String command = String.format("CLASSPATH=%s app_process", REMOTE_DIR + "/" + EXECUTE_BIN);
        if (isDebug) {
            // https://github.com/Genymobile/scrcpy/blob/master/DEVELOP.md#debug-the-server
            int sdk = Integer.parseInt(this.device.getProperty(Constant.PROP_SDK));
            if (sdk >= 28) {
                /* Android 9 and above */
                command += "-XjdwpProvider:internal -XjdwpOptions:transport=dt_socket,suspend=y,server=y,address=";
            } else {
                /* Android 8 and below */
                command += " -agentlib:jdwp=transport=dt_socket,suspend=y,server=y,address=";
            }
            command += DEBUGGER_PORT;
        }
        // redmi note 9 android.media.MediaCodec$CodecException: Error 0xfffffff4
        // https://github.com/Genymobile/scrcpy/issues/833
        // pass an invalid encoder name to list all encoders
        command += " / com.genymobile.scrcpy.Server 1.22 log_level=verbose bit_rate=8000000 tunnel_forward=true send_dummy_byte=true";
        LOGGER.info("scrcpy start command:" + command);
        scrcpyServerCmdThread = startScrcpy(command);

        // adb forward tcp:port scrcpy
        // todo 定制scrcpy监听的端口名称
        String scrcpySocketName = "scrcpy";
        forward = AdbUtils.createForward(this.device, scrcpySocketName);
        if (forward == null) {
            throw new TouchServiceException("create scrcpy forward failed!");
        }

        // connect to scrcpy socket to send control message
        scrcpySocketThread = startScrcpyControl("127.0.0.1", forward.getPort());
    }

    private Thread startScrcpyControl(String host, int port) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                int tryTime = 200;
                while (true) {
                    // todo 修改scrcpy 去除多余的socket连接
                    Socket tmpVideoSocket = null;
                    Socket tmpControlsocket = null;
                    try {
                        byte[] bytes = new byte[256];
                        // 第一个socket为video socket
                        tmpVideoSocket = new Socket(host, port);
                        // read one byte to test the connection
                        int readNum = tmpVideoSocket.getInputStream().read(bytes);
                        if (readNum == -1) {
                            Thread.sleep(10);
                            tmpVideoSocket.close();
                            LOGGER.info("scrcpy video socket 建立失败!");
                            continue;
                        } else {
                            videoSocket = tmpVideoSocket;
                            LOGGER.info("scrcpy video socket 建立成功!");
                        }

                        // 第二个才为control socket
                        tmpControlsocket = new Socket(host, port);
                        readNum = tmpControlsocket.getInputStream().read(bytes);
                        if (readNum == -1) {
                            Thread.sleep(10);
                            tmpControlsocket.close();
                            LOGGER.info("scrcpy control socket 建立失败!");
                            continue;
                        } else {
                            controlSocket = tmpControlsocket;
                            controlSocketOutputStream = tmpControlsocket.getOutputStream();
                            LOGGER.info("scrcpy control socket 建立成功!!");
                            onStartup(true);
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (tmpVideoSocket != null) {
                            try {
                                tmpVideoSocket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }

                        if (tmpControlsocket != null) {
                            try {
                                tmpControlsocket.close();
                            } catch (IOException ex) {
                                ex.printStackTrace();
                            }
                        }
                    }

                    tryTime--;
                    LOGGER.info("retry to connect to scrcpy server socket:" + tryTime);
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

    private Thread startScrcpy(String command) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // wait forever
                    ScrcpyTouchService.this.device.getIDevice()
                            .executeShellCommand(command, new IShellOutputReceiver() {
                                @Override
                                public void addOutput(byte[] data, int offset, int length) {
                                    System.out.println("scrcpy start cmd output:" + new String(data, offset, length));
                                }

                                @Override
                                public void flush() {

                                }

                                @Override
                                public boolean isCancelled() {
                                    return false;
                                }
                            }, 0, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        if (!isDebug) {
            LOGGER.info("启动scrcpy server！");
            thread.start();
        }
        return thread;
    }

    @Override
    public void kill() {
        LOGGER.info("shutdown the scrcpy touch service:" + this.device.getSerialNumber());
        AdbUtils.removeForward(device, forward);
        onClose();
        if (scrcpyServerCmdThread != null) {
            scrcpyServerCmdThread.stop();
        }

        if (scrcpySocketThread != null) {
            scrcpySocketThread.stop();
        }

        if (controlSocket != null && controlSocket.isConnected()) {
            try {
                controlSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            controlSocket = null;
        }
    }
}
