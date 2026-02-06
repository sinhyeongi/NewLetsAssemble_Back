package com.pr1.newletsassemble.global.error;

public class ApiException extends RuntimeException{
    private final ErrorCode errorCode;
    public ApiException(ErrorCode code){
        super(code.message());
        this.errorCode = code;
    }
    public ErrorCode code(){
        return errorCode;
    }
    public static ApiException of(ErrorCode code){
        return new ApiException(code);
    }
}
