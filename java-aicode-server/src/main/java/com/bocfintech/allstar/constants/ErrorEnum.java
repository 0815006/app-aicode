package com.bocfintech.allstar.constants;

public enum ErrorEnum {
    操作失败(-1, "操作失败"),
    系统异常(1, "系统异常"),
    参数异常(2, "参数异常"),
    登录失败(3, "登录失败"),
    文件上传异常(4, "文件上传异常"),
    请求方式错误(5, "请求方式不支持"),
    请求路径异常(6, "请检查url是否正确"),
    权限异常(7, "权限不足"),
    记录不存在(8, "记录不存在"),
    未登陆异常(9, "尚未登陆"),
    Jenkins操作失败(10, "Jenkins操作失败"),
    数据解析异常(11, "数据解析异常"),
    Http接口响应异常(12, "Http接口响应异常"),
    主机连接失败(13, "主机连接失败"),
    数据库操作失败(14, "数据库操作失败"),

    用户名或密码错误(15, "用户名或密码错误");


    private int code;
    private String errMsg;

    public int getCode()
    {
        return code;
    }

    public void setCode(int code)
    {
        this.code = code;
    }

    public String getErrMsg()
    {
        return errMsg;
    }

    public void setErrMsg(String errMsg)
    {
        this.errMsg = errMsg;
    }

    ErrorEnum(int code, String errMsg)
    {
        this.code = code;
        this.errMsg = errMsg;
    }
}
