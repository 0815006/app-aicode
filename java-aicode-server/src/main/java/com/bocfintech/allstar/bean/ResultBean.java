package com.bocfintech.allstar.bean;

import com.bocfintech.allstar.constants.ErrorEnum;

public class ResultBean<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ResultBean<T> success(T data) {
        ResultBean<T> result = new ResultBean<>();
        result.code = 200;
        result.message = "success";
        result.data = data;
        return result;
    }

    public static <T> ResultBean<T> success() {
        return success(null);
    }

    public static <T> ResultBean<T> error(String message) {
        ResultBean<T> result = new ResultBean<>();
        result.code = 500;
        result.message = message;
        return result;
    }

    // getter和setter方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public static ResultBean error(ErrorEnum error, String tip)
    {
        if ( tip != null && !tip.trim().equals("") )
        {
            return error(error.getCode(), tip);//tip = "，" + tip;
        }
        else
        {
            return error(error.getCode(), error.getErrMsg());//tip = "";
        }

    }
    public static ResultBean error(int code, String msg)
    {
        ResultBean r = new ResultBean();
        r.setCode(code);
        r.setMessage(msg);
        return r;
    }
}
