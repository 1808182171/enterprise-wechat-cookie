package com.example.wx_demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.example.wx_demo.common.AjaxResult;
import com.example.wx_demo.common.WxCaptchaDto;
import com.example.wx_demo.service.ApiService;
import com.example.wx_demo.util.HttpUtil;
import kong.unirest.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zq
 */
@RequestMapping("wx")
@RestController
public class ApiController {

    @Autowired
    private ApiService apiService;

    /**
     * 获取微信插件二维码
     *
     * @return
     */
    @RequestMapping(value = "getQrcode", method = RequestMethod.GET)
    public AjaxResult getQrcode() {


        //    String qrCode = getCache(RedisConstant.WX_QRCODE);
        //    if (qrCode != null) {
        //        return qrCode;
        //    }


        //  从缓存中拿cookie
        //  String cookie = getCache(RedisConstant.WX_COOKIE);
        //    if (cookie == null) {
        //        return AjaxResult.error("获取cookie失败,请管理员扫码登录企业微信平台");
        //    }
        String cookie = "";
        JSONObject qrCode = apiService.getQrCode(cookie);
        if (qrCode == null) {
            return AjaxResult.error("获取加入微信二维码失败,请管理员扫码登录企业微信平台");
        }
        return AjaxResult.success(qrCode.getString("qrCode"));
    }

    /**
     * 获取登录企业微信二维码
     *
     * @return
     */

    @RequestMapping(value = "wxLoginQrcode", method = RequestMethod.GET)
    public AjaxResult wxLoginQrcode() {
        Map<String, String> result = apiService.wxLoginQrcode();
        return AjaxResult.success(result);
    }

    /**
     * 前端轮询微信二维码扫描登录状态获取加入微信二维码
     */
    @RequestMapping(value = "pollingWxLoginQrcode", method = RequestMethod.GET)
    public AjaxResult pollingWxLoginQrcode(@RequestParam(value = "qrcodeKey", required = true) String qrcodeKey) {
        Map<String, String> map = apiService.pollingWxLoginQrcode(qrcodeKey);
        return AjaxResult.success(map);
    }

    /**
     * 通过手机验证码登录企业微信获取cookie获取微信插件二维码
     * @return
     */
    @RequestMapping(value = "confirmCaptcha", method = RequestMethod.POST)
    public AjaxResult confirmCaptcha(@RequestBody WxCaptchaDto wxCaptcha) {

        JSONObject restJson = apiService.confirmCaptcha(wxCaptcha);
        JSONObject jsonObject = restJson.getJSONObject("result");
        if (jsonObject != null) {
            return AjaxResult.error(jsonObject.getString("humanMessage"));
        }
        JSONObject data = restJson.getJSONObject("data");
        if (data != null && !data.isEmpty()) {
            return AjaxResult.error("请先扫码获取验证码");
        }
        return AjaxResult.success(jsonObject);
    }


    /**
     * 发送验证码
     * @param wxCaptcha 参数
     * @return
     */
    @RequestMapping(value = "sendCaptcha", method = RequestMethod.POST)
    public AjaxResult sendCaptcha(@RequestBody WxCaptchaDto wxCaptcha) {
        JSONObject jsonObject = apiService.sendCaptcha(wxCaptcha);
        if (jsonObject == null || jsonObject.getJSONObject("result") != null) {
            return AjaxResult.error("请先扫码");
        }
        return AjaxResult.success(jsonObject);
    }
}
