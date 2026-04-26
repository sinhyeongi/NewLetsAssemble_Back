package com.pr1.newletsassemble.auth.infra.session;

public enum RefreshTokenRotateResult {
    SUCCESS,
    NOT_FOUND,
    REUSED;
    public static RefreshTokenRotateResult from(Long result){
        if(result == null || result == 0){
            return NOT_FOUND;
        }else if(result == -1){
            return REUSED;
        }
        return SUCCESS;
    }
}
