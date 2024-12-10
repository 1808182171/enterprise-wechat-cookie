package com.example.wx_demo.common;


/**
 * 微信推送登录验证
 * @author zq
 */
public class WxCaptchaDto {
    //微信手机验证码
    private String code;
    //引用者
    private String referer;
    //临时sid
    private String tmpSid;
    //临时key
    private String tlKey;


    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getTmpSid() {
        return tmpSid;
    }

    public void setTmpSid(String tmpSid) {
        this.tmpSid = tmpSid;
    }

    public String getTlKey() {
        return tlKey;
    }

    public void setTlKey(String tlKey) {
        this.tlKey = tlKey;
    }
}
