package com.pr1.newletsassemble.global.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T body,
        String code,
        String message
) {
    public static <T> ApiResponse<T> ok(T data){
        return new ApiResponse<>(true,data,null,null);
    }
    public static ApiResponse<Void> error(String code,String message){
        return new ApiResponse<>(false,null,code,message);
    }
}
