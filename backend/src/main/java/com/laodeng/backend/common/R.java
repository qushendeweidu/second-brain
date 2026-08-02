package com.laodeng.backend.common;

import cn.hutool.http.HttpStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/2/18 11:18
 * @description 相应通用类
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code = NORMAL_CODE;

    private String msg = "";

    private T data;

    public static final String SUCCESS = "成功";

    public static final String FAILURE = "失败";

    public static final Integer ERROR_CODE = HttpStatus.HTTP_INTERNAL_ERROR;

    public static final Integer NORMAL_CODE = HttpStatus.HTTP_OK;

    // 给正确响应做数据封装的构造方法
    public R(Integer code, T data, String msg) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 用来给错误响应做消息封装的构造方法
    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static<T> R<T> success(){
        return new R<>(NORMAL_CODE, SUCCESS);
    }

    public static<T> R<T> success(T data){
        return new R<>(NORMAL_CODE, data, SUCCESS);
    }

    public static<T> R<T> error(ErrorCode errorCode){
        return new R<>(errorCode.getCode(), errorCode.getMessage());
    }

    public static<T> R<T> error(ErrorCode errorCode, String msg){
        return new R<>(errorCode.getCode(), msg);
    }

    public static<T> R<T> error(){
        return new R<>(ERROR_CODE, FAILURE);
    }

}
