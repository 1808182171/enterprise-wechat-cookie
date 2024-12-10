package com.example.wx_demo.util;

import com.alibaba.fastjson.JSONObject;
import kong.unirest.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * http请求工具类
 * zq
 */
public final class HttpUtil {
    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);

    static final String POST = "POST";
    static final String GET = "GET";

    static final int CONN_TIMEOUT = 30000;// ms
    static final int READ_TIMEOUT = 30000;// ms

    private final static UnirestInstance unirest = Unirest.spawnInstance();

    /**
     * post 方式发送http请求.
     *
     * @param strUrl
     * @return
     */
    public static HttpResponse<String> doPost(String strUrl, Map<String, Object> map, Map<String, String> headers) {
        return send(strUrl, POST, map, headers);
    }

    public static HttpResponse<String> doPost(String strUrl) {
        return send(strUrl, POST, null, null);
    }

    public static HttpResponse<String> doPost(String strUrl, Map<String, Object> map) {
        return send(strUrl, POST, map, null);
    }

    /**
     * get方式发送http请求.
     *
     * @param strUrl
     * @return
     */
    public static HttpResponse<String> doGet(String strUrl, Map<String, Object> map, Map<String, String> headers) {
        return send(strUrl, GET, map, headers);
    }

    public static HttpResponse<String> doGet(String strUrl, Map<String, Object> map) {
        return send(strUrl, GET, map, null);
    }

    /**
     * get方式发送http请求.
     *
     * @param strUrl
     * @return
     */
    public static HttpResponse<String> doGet(String strUrl) {
        return send(strUrl, GET, null, null);
    }

    /**
     * @param strUrl    请求路径
     * @param reqmethod 请求方式
     * @param map       请求参数
     * @param headers   请求体
     * @return
     */
    public static HttpResponse<String> send(String strUrl, String reqmethod, Map<String, Object> map, Map<String, String> headers) {
        if (reqmethod.equalsIgnoreCase(POST)) {
            HttpRequestWithBody post = unirest.post(strUrl);
            if (map != null) {
                JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(map));
                String json = jsonObject.toJSONString();
                RequestBodyEntity body = post.body(json);
                if (headers != null) {
                    body.headers(headers);
                }
                return body.asString();
            }
            if (headers != null) {
                post.headers(headers);
            }
            return post.asString();
        } else {
            GetRequest get = unirest.get(strUrl);
            if (map != null) {
                get.queryString(map);
            }
            if (headers != null) {
                get.headers(headers);
            }
            return get.asString();
        }
    }



    /**
     * 发送重定向请求
     * @param url 地址
     * @param tmpCookie 临时cookie
     * @return
     */
    public static Map<String, String> sendRedirectRequest(String url, String tmpCookie) {
        Map<String, String> map = new HashMap<>();
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONN_TIMEOUT)
                .setConnectionRequestTimeout(READ_TIMEOUT)
                .setSocketTimeout(CONN_TIMEOUT)
                .setRedirectsEnabled(false)
                .build(); // 不允许重定向
        CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(config).build();
        String location = null;
        int responseCode = 0;

        CloseableHttpResponse response = null;
        try {
            HttpGet request = new HttpGet(url);
            if (tmpCookie != null) {
                request.setHeader(HttpHeaders.COOKIE, "wwrtx.tmp_sid=" + tmpCookie);
            }
            response = httpClient.execute(request);
            responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 302) {
                org.apache.http.Header locationHeader = response.getFirstHeader("Location");
                location = locationHeader.getValue();
                org.apache.http.Header[] headers = response.getHeaders("Set-Cookie");
                // 获取 wwrtx.tmp_sid 的 cookie
                for (org.apache.http.Header header : headers) {
                    String cookieStr = header.getValue();
                    if (cookieStr.contains("wwrtx.tmp_sid") || cookieStr.contains("wwrtx.sid")) {
                        String[] cookieArr = cookieStr.split(";");
                        for (String c : cookieArr) {
                            if (c.contains("wwrtx.tmp_sid")) {
                                String[] cookieSplit = c.split("=");
                                String cookieValue = cookieSplit[1];
                                map.put("location", location);
                                map.put("tmpCookie", cookieValue);
                                return map;
                            }

                            if (c.contains("wwrtx.sid")) {
                                String[] cookieSplit = c.split("=");
                                String cookieValue = cookieSplit[1];
                                map.put("cookie", cookieValue);
                                return map;
                            }
                        }
                    }
                }
                map.put("location", location);
            } else {
                // 其他响应码全部置为失败
                log.warn("Unexpected response code: {}", responseCode);
                return null;
            }
        } catch (IOException e) {
            // 错误全部置为失败
            log.error("Error sending redirect request: {}", e.getMessage(), e);
            return null;
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("Error closing response: {}", e.getMessage(), e);
            }
        }

        return map;
    }

}
