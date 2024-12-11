package com.example.wx_demo.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.wx_demo.common.WxCaptchaDto;
import com.example.wx_demo.util.HttpUtil;
import kong.unirest.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zq
 */
@Service
public class ApiService {

    /**
     * 企业微信前缀
     */
    private final String baseUri = "https://work.weixin.qq.com";

    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    /**
     * 获取微信插件二维码
     *
     * @param cookie
     * @return
     */

    public JSONObject getQrCode(String cookie) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.COOKIE, "wwrtx.sid=" + cookie);
        HttpResponse<String> response = HttpUtil.doGet(baseUri + "/wework_admin/wxplugin/getDetail", null, headers);
        String result = response.getBody();
        JSONObject dataJson = JSONObject.parseObject(result);
        JSONObject data = dataJson.getJSONObject("data");
        if (data == null) {
            return null;
        }
        return data;
    }

    /**
     * 获取微信登录二维码
     *
     * @return map
     */
    public Map<String, String> wxLoginQrcode() {
        Map<String, String> result = null;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("r", String.valueOf(System.currentTimeMillis()));
            params.put("login_type", "login_admin");
            HttpResponse<String> response = HttpUtil.doGet(baseUri + "/wework_admin/wwqrlogin/mng/get_key", params);
            JSONObject res = JSON.parseObject(response.getBody());
            String qrcodeKey = res.getJSONObject("data").getString("qrcode_key");
            result = new HashMap<>();
            result.put("qrcode_key", qrcodeKey);
//        result.put("qrcode_url", baseUri + "wwqrlogin/qrcode/" + qrcodeKey + "?login_type=login_admin");
            result.put("qrcode_url", baseUri + String.format("/wework_admin/wwqrlogin/mng/qrcode?qrcode_key=%s&login_type=login_admin", qrcodeKey));
        } catch (Exception e) {
            logger.error("获取微信登录二维码失败", e);
        }
        return result;
    }

    /**
     * 获取登录状态.返回加入企业微信邀请关注二维码
     *
     * @param qrcodeKey 二维码key
     * @return 包含状态的Map
     */
    public Map<String, String> pollingWxLoginQrcode(String qrcodeKey) {
        //返回结果Map
        Map<String, String> resultMap = new HashMap<>();
        resultMap.put("status", "0");
        resultMap.put("qrcode", "");
        resultMap.put("message", "获取失败");
        try {

            //参数
            Map<String, Object> params = new HashMap<>();
            params.put("r", String.valueOf(System.currentTimeMillis()));
            params.put("status", "");
            params.put("qrcode_key", qrcodeKey);
            HttpResponse<String> response = HttpUtil.doGet(baseUri + "/wework_admin/wwqrlogin/check", params);
            JSONObject res = JSON.parseObject(response.getBody());
            JSONObject resStatus = res.getJSONObject("data");
            if (resStatus == null) {
                resultMap.put("message", "二维码已过期");
                return resultMap;
            }

            String status = resStatus.getString("status");
            switch (status) {
                case "QRCODE_SCAN_NEVER":
                    logger.info("等待扫码");
                    resultMap.put("message", "等待扫码");
                    break;
                case "QRCODE_SCAN_FAIL":
                    logger.info("取消操作");
                    resultMap.put("message", "取消操作");
                    break;
                case "QRCODE_SCAN_ING":
                    if (!resStatus.containsKey("confirm_corpid")) {
                        logger.info("个人微信,已扫码");
                        resultMap.put("message", "个人微信,已扫码");

                    } else {
                        logger.info("已扫码,等待确认");
                        resultMap.put("message", "已扫码,等待确认");
                    }
                    break;
                case "QRCODE_SCAN_SUCC":
                    logger.info("已确认扫码,待获取cookie");
                    String authCode = resStatus.getString("auth_code");
                    JSONObject qrcodeJson = getLoginQrCode(authCode, qrcodeKey);
                    if (qrcodeJson != null) {

                        //判断是否是需要手机验证码
                        if (qrcodeJson.getString("need_mobile_verify") != null) {
                            resultMap.put("status", "-5");
                            resultMap.put("tlKey", qrcodeJson.get("tl_key").toString());
                            resultMap.put("referer", qrcodeJson.get("referer").toString());
                            resultMap.put("tmpSid", qrcodeJson.get("tmpId").toString());
                            resultMap.put("message", "已发送手机验证码，需要输入验证码登录");
                            return resultMap;
                        }

                        String qrCode = qrcodeJson.getString("qrCode");
                        resultMap.put("status", "1");
                        resultMap.put("qrcode", qrCode);
                        resultMap.put("message", "已确认扫码,获取成功");
                        //将qrcode存缓存
//                        setCache(WebContant.WX_QRCODE, qrcode);
                    } else {
                        resultMap.put("status", "-1");
                        resultMap.put("error", "扫码登录失败，该微信没有开启企业微信管理员");
                    }
                    break;
                default:
                    logger.info("未知状态");
                    resultMap.put("message", "未知状态");
            }
        } catch (Exception e) {
            resultMap.put("status", "-1");
            resultMap.put("error", "扫码登录异常");
            logger.error("扫码登录异常", e);
        }
        return resultMap;
    }


    /**
     * 获取登录cookie.
     *
     * @param authCode  授权码
     * @param qrcodeKey 二维码key
     * @return cookie
     */
    private JSONObject getLoginQrCode(String authCode, String qrcodeKey) {
        // 获取登录页面
        String loginpageUrl = baseUri + String.format("/wework_admin/loginpage_wx?_r=%s&wwqrlogin=1&auth_source=SOURCE_FROM_WEWORK&code=%s&qrcode_key=%s", (int) (Math.random() * 1000), authCode, qrcodeKey);
        Map<String, String> locationMap = HttpUtil.sendRedirectRequest(loginpageUrl, null);
        // 获取cookie失败
        if (locationMap == null) {
            return null;
        }
        // 获取登录跳转页面
        String location = locationMap.get("location");
        String tmpCookie = locationMap.get("tmpCookie");
        String locationUrl = baseUri + location + "&redirect_uri=https://work.weixin.qq.com/wework_admin/frame";
        Map<String, String> getCookieMap = HttpUtil.sendRedirectRequest(locationUrl, tmpCookie);
        if (getCookieMap == null) {
            return null;
        }
        String cookie = getCookieMap.get("cookie");

        if (cookie == null) {
            //第一次登录企业微信进入手机验证码验证
            location = getCookieMap.get("location");
            String captchaLocation = baseUri + location;
            if (location.contains("tl_key=")) {
                //取出下面字符串中的tl_key值
                String tl_key = location.substring(location.indexOf("tl_key=") + 7, location.indexOf("&redirect_url"));
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", "wwrtx.tmp_sid=" + tmpCookie);
                HttpUtil.doGet(captchaLocation, null, headers);
                HttpUtil.sendRedirectRequest(locationUrl, tmpCookie);
                JSONObject result = new JSONObject();
                result.put("tl_key", tl_key);
                result.put("referer", captchaLocation);
                result.put("tmpId", tmpCookie);
                result.put("need_mobile_verify", "ok");
                return result;
            }
        }

        //存缓存 项目中可自行修改
//        setCache(WebContant.WX_COOKIE, cookie);
        return getQrCode(cookie);
    }


/**
 * 确认验证码获取企业微信登录二维码.
 *
 * @param wxCaptcha
 * @return
 */
public JSONObject confirmCaptcha(WxCaptchaDto wxCaptcha) {
    try {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.COOKIE, "wwrtx.tmp_sid=" + wxCaptcha.getTmpSid());
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        headers.put(HttpHeaders.REFERER, wxCaptcha.getReferer());

        Map<String, Object> params = new HashMap<>();
        params.put("captcha", wxCaptcha.getCode());
        params.put("tl_key", wxCaptcha.getTlKey());

        HttpResponse<String> response = HttpUtil.doPost(baseUri + "/wework_admin/mobile_confirm/confirm_captcha", params, headers);
        String result = response.getBody();

        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("Response body is empty or null");
        }

        JSONObject resultJSON = JSONObject.parseObject(result);

        if (resultJSON != null) {
            JSONObject jsonObject = resultJSON.getJSONObject("result");

            if (jsonObject == null) {
                Map<String, String> stringStringMap = HttpUtil.sendRedirectRequest(baseUri + "/wework_admin/login/choose_corp?tl_key=" + wxCaptcha.getTlKey(), wxCaptcha.getTmpSid());
                if (stringStringMap != null) {
                    String cookie = stringStringMap.get("cookie");
                    JSONObject qrCodeJson = getQrCode(cookie);
                    resultJSON = new JSONObject();
                    resultJSON.put("status", "1");
                    if (qrCodeJson != null) {
                        String qrCode = qrCodeJson.getString("qrCode");
                        resultJSON.put("qrCode", qrCode);
                    }
                    return resultJSON;
                }
            } else {
                resultJSON.put("status", "-1");
            }
        }
        return resultJSON;
    } catch (Exception e) {
        // 处理异常情况
        JSONObject errorResult = new JSONObject();
        errorResult.put("status", "-1");
        errorResult.put("error", e.getMessage());
        return errorResult;
    }
}



    /**
     * 发送微信登录验证码
     *
     * @param wxCaptcha
     * @return
     */
    public JSONObject sendCaptcha(WxCaptchaDto wxCaptcha) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.COOKIE, "wwrtx.tmp_sid=" + wxCaptcha.getTmpSid());
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
        headers.put(HttpHeaders.REFERER, wxCaptcha.getReferer());
        Map<String, Object> params = new HashMap<>();
        params.put("tl_key", wxCaptcha.getTlKey());
        HttpResponse<String> response = HttpUtil.doPost(baseUri + "/wework_admin/mobile_confirm/confirm_captcha", params, headers);
        String result = response.getBody();
        JSONObject resultJSON = JSONObject.parseObject(result);
        if (resultJSON == null || resultJSON.getJSONObject("result").get("errCode") != null) {
            return resultJSON;
        }
        resultJSON = new JSONObject();
        resultJSON.put("status", "1");
        resultJSON.put("massage", "发送验证码成功");
        return resultJSON;
    }
}
