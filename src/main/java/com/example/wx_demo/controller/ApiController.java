package com.example.wx_demo.controller;

import com.example.wx_demo.common.AjaxResult;
import com.example.wx_demo.service.ApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
        String qrcode = apiService.getQrCode(cookie);
        if (qrcode == null) {
            return AjaxResult.error("获取加入微信二维码失败,请管理员扫码登录企业微信平台");
        }
        return AjaxResult.success(qrcode);
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
}
