package com.jld.glassesserver.util;

/**
 * Created by lz on 2016/10/18.
 */

public class ConfigInfo {

    public static final String code = "number";
    /**接收到的信息的状态码*/
    //连接状态
    public static final String receive_conn_1 = "phone_00";//被动连接
    public static final String receive_conn_2 = "phone_01";//主动连接

    //信息请求
    public static final String receive_give_info = "phone_02";
    //修改设备名称
    public static final String receive_modify_name = "phone_03";
    //wifi连接设置
    public static final String receive_wifi_setting = "phone_04";
    //呼叫设置
    public static final String receive_call_setting = "phone_05";
    //相机设置
    public static final String setting_camera = "phone_06";

    /**返回信息时的状态码*/
    //连接状态
    public static final String send_conn_1 = "glasses_00";//被动连接
    public static final String send_conn_2 = "glasses_01";//主动连接

    //信息请求
    public static final String send_give_info = "glasses_02";
    //修改设备名称
    public static final String send_modify_name = "glasses_03";
    //wifi连接设置
    public static final String send_wifi_setting = "glasses_04";
    //呼叫设置
    public static final String send_call_setting = "glasses_05";
    //相机设置
    public static final String send_setting_camera = "glasses_06";

    //设置相机参数广播action
    public static final String send_setting_camera_broadcast_action = "send_setting_camera_broadcast_action";
}
