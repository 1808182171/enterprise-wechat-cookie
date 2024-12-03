

# 企业微信cookie

#### 介绍
解决企业微信后台中内部企业微信插件邀请关注二维码七天时间的问题，这里是使用抓包来获取微信的cookie信息来发https请求获取cookie实现的。当然，有了cookie可以不止用于获取微信插件邀请关注二维码

企业微信后台SDK

- 获取qrcode
- 扫码登录获取cookie
- 获取微信插件邀请关注二维码

等等

## 要求

1. java >= 8
2. maven

## 用法
体验可以在com/example/wx_demo/test/TestApi.java类中执行main方法体验


获取登录二维码

```java

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
        result.put("qrcode_url", baseUri + String.format("/wework_admin/wwqrlogin/mng/qrcode?  	      qrcode_key=%s&login_type=login_admin", qrcodeKey));
        return result;
    }
/**
return
Map
(
    [qrcode_key] => eb67102ca70843de //二维码的key
    [qrcode_url] => https://work.weixin.qq.com/wwqrlogin/qrcode/eb67102ca70843de?login_type=login_admin //登录二维码url
)
*/
```

通过企业微信扫码后 查询登录状态

```java
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
return

JSONObject
(
    [data] => JSONObject
        (
            [status] => QRCODE_SCAN_NEVER //登录状态
            [auth_source] => SOURCE_FROM_WEWORK
            [corp_id] => 0
            [code_type] => 2
            [clientip] => 183.17.231.150
            [confirm_clientip] => 
        )

)

QRCODE_SCAN_NEVER 等待扫码
QRCODE_SCAN_FAIL 取消操作
QRCODE_SCAN_ING 已扫码,等待确认
QRCODE_SCAN_SUCC 已确认 登录

*/


```

当登录状态为QRCODE_SCAN_SUCC时 获取cookie

```java
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

//返回cookie
//$auth_code 为授权 code 在 QRCODE_SCAN_SUCC时 会返回



```

前端交互示例暂无


## 参与贡献

1. fork 当前库到你的名下
3. 在你的本地修改完成审阅过后提交到你的仓库
4. 提交 PR 并描述你的修改，等待合并

## License

[MIT license](https://opensource.org/licenses/MIT)
