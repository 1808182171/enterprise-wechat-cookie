package com.example.wx_demo.test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.wx_demo.util.HttpUtil;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 执行main方法，进行获取企业微信二维码获取cookie，再通过cookie获取企业微信中7天的微信插件邀请关注二维码
 */
public class TestApi {

    private static final Logger log = LoggerFactory.getLogger(TestApi.class);

    private final String baseUri = "https://work.weixin.qq.com";
    private final UnirestInstance unirest = Unirest.spawnInstance();
    private final Map<String, String> config = new HashMap<>();


    public static void main(String[] args) throws IOException {
        TestApi testApi = new TestApi();

        Map<String, String> qrcode = testApi.getQrcode();
        log.info("企业微信登录二维码：" + qrcode.get("qrcode_url"));
//        Runtime.getRuntime().exec("xdg-open " + qrcode.get("qrcode_url"));
        String qrcodeKey = qrcode.get("qrcode_key");

        boolean getStatus = false;
        while (!getStatus) {
            JSONObject resStatus = testApi.getStatus(qrcodeKey);

            String status = resStatus.getString("status");
            switch (status) {
                case "QRCODE_SCAN_NEVER":
                    log.info("等待扫码");
                    break;
                case "QRCODE_SCAN_FAIL":
                    log.info("取消操作");
                    break;
                case "QRCODE_SCAN_ING":
                    if (!resStatus.containsKey("confirm_corpid")) {
                        log.info("个人微信,已扫码");
                    } else {
                        log.info("已扫码,等待确认");
                    }
                    break;
                case "QRCODE_SCAN_SUCC":
                    log.info("已确认,获取cookie");
                    getStatus = true;
                    String authCode = resStatus.getString("auth_code");
                    String cookie = testApi.getCookie(authCode, qrcodeKey);
                    System.out.println("获取cookie: " + cookie);
                    String wxPlugQrCode = testApi.getWxPlugQrCode(cookie);
                    log.info("获取微信插件二维码: " + wxPlugQrCode);
                    break;
                default:
                    log.info("未知状态");
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 获取登录二维码.
     *
     * @return 包含qrcode_key和qrcode_url的Map
     */
    public Map<String, String> getQrcode() {
        Map<String, Object> params = new HashMap<>();
        params.put("r", String.valueOf(System.currentTimeMillis()));
        params.put("login_type", "login_admin");
        HttpResponse<String> response = unirest.get(baseUri + "/wework_admin/wwqrlogin/mng/get_key")
                .queryString(params)
                .asString();
        JSONObject res = JSON.parseObject(response.getBody());
        String qrcodeKey = res.getJSONObject("data").getString("qrcode_key");

        Map<String, String> result = new HashMap<>();
        result.put("qrcode_key", qrcodeKey);
//        result.put("qrcode_url", baseUri + "wwqrlogin/qrcode/" + qrcodeKey + "?login_type=login_admin");
        result.put("qrcode_url", baseUri + String.format("/wework_admin/wwqrlogin/mng/qrcode?qrcode_key=%s&login_type=login_admin", qrcodeKey));
        return result;
    }

    /**
     * 获取登录状态.
     *
     * @param qrcodeKey 二维码key
     * @return 包含状态的Map
     */
    public JSONObject getStatus(String qrcodeKey) {
        Map<String, Object> params = new HashMap<>();
        params.put("r", String.valueOf(System.currentTimeMillis()));
        params.put("status", "");
        params.put("qrcode_key", qrcodeKey);
        HttpResponse<String> response = unirest.get(baseUri + "/wework_admin/wwqrlogin/check")
                .queryString(params)
                .asString();
        JSONObject res = JSON.parseObject(response.getBody());
        return res.getJSONObject("data");
    }


    /**
     * 获取登录cookie.
     *
     * @param authCode  授权码
     * @param qrcodeKey 二维码key
     * @return cookie
     */
    public String getCookie(String authCode, String qrcodeKey) {
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
        //这里可以将cookie存入缓存
        return cookie;
    }


    /**
     * 获取微信插件中二维码
     *
     * @param cookie
     * @return
     */
    private String getWxPlugQrCode(String cookie) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.COOKIE, "wwrtx.sid=" + cookie);
        HttpResponse<String> response = HttpUtil.doGet(baseUri + "/wework_admin/wxplugin/getDetail", null, headers);
        String result = response.getBody();
        JSONObject dataJson = JSONObject.parseObject(result);
        JSONObject data = dataJson.getJSONObject("data");
        if (data == null) {
            return null;
        }
        //这里可以将二维码存入缓存 避免每次都请求一次
        return data.getString("qrCode");
    }


    /**
     * 获取企业信息.
     *
     * @return 企业信息
     */
//    public Map<String, String> getCorp() {
//        HttpResponse<String> response = unirest.get(baseUri + "wework_admin/corp/getCorp")
//                .asString();
//
//        JSONObject res = JSON.parseObject(response.getBody());
//        return res.getJSONObject("data").toJavaObject(HashMap.class);
//    }
}
