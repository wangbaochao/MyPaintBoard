package com.app.superxlcr.mypaintboard.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.app.superxlcr.mypaintboard.model.Line;
import com.app.superxlcr.mypaintboard.model.Point;
import com.app.superxlcr.mypaintboard.model.Protocol;
import com.app.superxlcr.mypaintboard.utils.ProtocolListener;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by superxlcr on 2017/1/21.
 * 绘画控制模块
 */

public class DrawController {

    private static DrawController instance;

    public static DrawController getInstance() {
        if (instance == null) {
            synchronized (DrawController.class) {
                if (instance == null) {
                    instance = new DrawController();
                }
            }
        }
        return instance;
    }

    private ProtocolListener sendDrawListener; // 发送绘制条目用监听器
    private ProtocolListener receiveDrawListener; // 接收绘制条目用监听器
    private ProtocolListener getDrawListListener; // 获取绘制条目用监听器
    private ProtocolListener uploadPicListener; // 上传图片用监听器
    private ProtocolListener receiveBgPicListener; // 接收背景图片推送监听器
    private ProtocolListener clearDrawListener; // 清除线段绘制监听器
    private ProtocolListener clearDrawPushListener; // 清除线段绘制推送监听器

    private DrawController() {
        sendDrawListener = null;
        receiveDrawListener = null;
    }

    /**
     * 发送绘制条目
     * @param context 上下文
     * @param handler 用于回调消息
     * @param time 发送时间
     * @param roomId 房间id
     * @param line 绘制条目
     * @return 是否发送成功
     */
    public boolean sendDraw(final Context context, final Handler handler, long time, int roomId, Line line) {
        try {
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(roomId);
            // line (pointNumber + point (x , y) + color + width + isEraser + width + height)
            jsonArray.put(line.getPointList().size());
            for (Point point : line.getPointList()) {
                jsonArray.put(point.getX());
                jsonArray.put(point.getY());
            }
            jsonArray.put(line.getColor());
            jsonArray.put(line.getPaintWidth());
            jsonArray.put(line.isEraser());
            jsonArray.put(line.getWidth());
            jsonArray.put(line.getHeight());
            Protocol sendProtocol = new Protocol(Protocol.DRAW, time, jsonArray);
            // 注册监听器
            sendDrawListener = new ProtocolListener() {
                @Override
                public boolean onReceive(Protocol protocol) {
                    int order = protocol.getOrder();
                    if (order == Protocol.DRAW) {
                        // 通过handler返回协议信息
                        Message message = handler.obtainMessage();
                        message.obj = protocol;
                        handler.sendMessage(message);
                        // 移除监听器
                        CommunicationController.getInstance(context).removeListener(sendDrawListener);
                        return true;
                    }
                    return false;
                }
            };
            CommunicationController.getInstance(context).registerListener(sendDrawListener);
            // 发送信息
            return CommunicationController.getInstance(context).sendProtocol(sendProtocol);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 设置接收绘制推送监听器
     * @param context 上下文
     * @param handler 用于接收回调消息
     */
    public void setReceiveDrawHandler(Context context, final Handler handler) {
        // 清除旧监听器
        if (receiveDrawListener != null) {
            CommunicationController.getInstance(context).removeListener(receiveDrawListener);
        }
        // 连接服务器
        CommunicationController.getInstance(context).connectServer();
        // 注册监听器
        receiveDrawListener = new ProtocolListener() {
            @Override
            public boolean onReceive(Protocol protocol) {
                int order = protocol.getOrder();
                if (order == Protocol.DRAW_PUSH) {
                    // 通过handler返回协议信息
                    Message message = handler.obtainMessage();
                    message.obj = protocol;
                    handler.sendMessage(message);
                    return true;
                }
                return false;
            }
        };
        CommunicationController.getInstance(context).registerListener(receiveDrawListener);
    }

    /**
     * 获取绘制条目列表
     * @param context 上下文
     * @param handler 用于回调消息
     * @param time 发送时间
     * @param roomId 房间id
     * @return 是否发送成功
     */
    public boolean getDrawList(final Context context, final Handler handler, long time, int roomId) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(roomId);
        Protocol sendProtocol = new Protocol(Protocol.GET_DRAW_LIST, time, jsonArray);
        // 注册监听器
        getDrawListListener = new ProtocolListener() {
            @Override
            public boolean onReceive(Protocol protocol) {
                int order = protocol.getOrder();
                if (order == Protocol.GET_DRAW_LIST) {
                    // 通过handler返回协议信息
                    Message message = handler.obtainMessage();
                    message.obj = protocol;
                    handler.sendMessage(message);
                    // 移除监听器
                    CommunicationController.getInstance(context).removeListener(getDrawListListener);
                    return true;
                }
                return false;
            }
        };
        CommunicationController.getInstance(context).registerListener(getDrawListListener);
        // 发送信息
        return CommunicationController.getInstance(context).sendProtocol(sendProtocol);
    }

    /**
     * 请求传输图片
     * @param context 上下文
     * @param handler 用于回调消息
     * @return 是否发送成功
     */
    public boolean askUploadPic(final Context context, final Handler handler) {
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(Protocol.UPLOAD_PIC_ASK);
        Protocol sendProtocol = new Protocol(Protocol.UPLOAD_PIC, System.currentTimeMillis(), jsonArray);
        // 注册监听器
        uploadPicListener = new ProtocolListener() {
            @Override
            public boolean onReceive(Protocol protocol) {
                int order = protocol.getOrder();
                if (order == Protocol.UPLOAD_PIC) {
                    // 通过handler返回协议信息
                    Message message = handler.obtainMessage();
                    message.obj = protocol;
                    handler.sendMessage(message);
                    // 移除监听器
                    CommunicationController.getInstance(context).removeListener(uploadPicListener);
                    return true;
                }
                return false;
            }
        };
        CommunicationController.getInstance(context).registerListener(uploadPicListener);
        // 发送信息
        return CommunicationController.getInstance(context).sendProtocol(sendProtocol);
    }

    /**
     * 设置接收背景图片推送监听器
     * @param context 上下文
     * @param handler 用于回调消息处理器
     */
    public void setReceiveBgPicHandler(Context context, final Handler handler) {
        // 清除旧监听器
        if (receiveBgPicListener != null) {
            CommunicationController.getInstance(context).removeListener(receiveBgPicListener);
        }
        // 连接服务器
        CommunicationController.getInstance(context).connectServer();
        // 注册监听器
        receiveBgPicListener = new ProtocolListener() {
            @Override
            public boolean onReceive(Protocol protocol) {
                int order = protocol.getOrder();
                if (order == Protocol.BG_PIC_PUSH) {
                    // 通过handler返回协议信息
                    Message message = handler.obtainMessage();
                    message.obj = protocol;
                    handler.sendMessage(message);
                    return true;
                }
                return false;
            }
        };
        CommunicationController.getInstance(context).registerListener(receiveBgPicListener);
    }

    /**
     * 清除房间绘制线段
     * @param context 上下文
     * @param handler 回调处理器
     * @return 是否发送成功
     */
    public boolean clearDraw(final Context context, final Handler handler) {
        JSONArray jsonArray = new JSONArray();
        Protocol sendProtocol = new Protocol(Protocol.CLEAR_DRAW, System.currentTimeMillis(), jsonArray);
        // 注册监听器
        clearDrawListener = new ProtocolListener() {
            @Override
            public boolean onReceive(Protocol protocol) {
                int order = protocol.getOrder();
                if (order == Protocol.CLEAR_DRAW) {
                    // 通过handler返回协议信息
                    Message message = handler.obtainMessage();
                    message.obj = protocol;
                    handler.sendMessage(message);
                    // 移除监听器
                    CommunicationController.getInstance(context).removeListener(clearDrawListener);
                    return true;
                }
                return false;
            }
        };
        CommunicationController.getInstance(context).registerListener(clearDrawListener);
        // 发送信息
        return CommunicationController.getInstance(context).sendProtocol(sendProtocol);
    }

    /**
     * 设置清除绘制线段推送回调器
     * @param context 上下文
     * @param handler 回调器
     */
    public void setClearDrawPushHandler(Context context, final Handler handler) {
        // 清除旧监听器
        if (clearDrawPushListener != null) {
            CommunicationController.getInstance(context).removeListener(clearDrawPushListener);
        }
        // 连接服务器
        CommunicationController.getInstance(context).connectServer();
        // 注册监听器
        clearDrawPushListener = new ProtocolListener() {
            @Override
            public boolean onReceive(Protocol protocol) {
                int order = protocol.getOrder();
                if (order == Protocol.CLEAR_DRAW_PUSH) {
                    // 通过handler返回协议信息
                    Message message = handler.obtainMessage();
                    message.obj = protocol;
                    handler.sendMessage(message);
                    return true;
                }
                return false;
            }
        };
        CommunicationController.getInstance(context).registerListener(clearDrawPushListener);
    }
}
