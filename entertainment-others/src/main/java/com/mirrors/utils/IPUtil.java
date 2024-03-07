package com.mirrors.utils;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * IP 工具类，
 * 参考 @see <a href="https://cloud.tencent.com/developer/article/2182781">获取ip地址</a>；
 * 参考 @see <a href="https://blog.csdn.net/u013541707/article/details/108453328">获取ip地址</a>；
 *
 * @author mirrors
 * @version 1.0
 * @date 2024/1/9 16:39
 */
public class IPUtil {

    /**
     * 获取 HttpRequeset 的请求IP；
     * 不同的代理服务器加的请求头的名字不同，所以后端获取的时候就要遍历的查找了，这就是代码中ipHeadList为什么有很多的值了
     *
     * @param request
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        String ipAddr;
        try {
            // 拿到经过的代理服务器的ip地址列表，逗号分割
            ipAddr = request.getHeader("X-Forwarded-For");
            // 不断尝试获取ip
            if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
                ipAddr = request.getHeader("Proxy-Client-IP");
            }
            if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
                ipAddr = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ipAddr == null || ipAddr.length() == 0 || "unknown".equalsIgnoreCase(ipAddr)) {
                ipAddr = request.getRemoteAddr(); // localhost会被解析为 ipv6：0:0:0:0:0:0:0:1
                if (ipAddr.equals("127.0.0.1")) {
                    // 根据网卡取本机配置ip
                    InetAddress inetAddress = null;
                    try {
                        inetAddress = InetAddress.getLocalHost();
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    assert inetAddress != null;
                    ipAddr = inetAddress.getHostAddress();
                }
            }
            // 多个代理时，第一个ip为客户端真实ip，多个ip用','分割
            if (ipAddr != null && ipAddr.length() > 15) {// "***.***.***.***".length()
                if (ipAddr.indexOf(",") > 0) {
                    ipAddr = ipAddr.substring(0, ipAddr.indexOf(","));
                }
            }

        } catch (Exception e) {
            // 获取的 IP 实际上是代理服务器的地址，并不是客户端的 IP 地址
            ipAddr = request.getRemoteAddr();
        }
        return ipAddr;
    }

    /**
     * 获取ip地址
     *
     * @param request
     * @return
     */
    @Deprecated
    public static String getIpAddr2(HttpServletRequest request) {

        String ip = request.getHeader("x-forwarded-for");
        if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个ip值，第一个ip才是真实ip
            if (ip.contains(",")) {
                ip = ip.split(",")[0];
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
            if (ip.equals("127.0.0.1") || ip.endsWith("0:0:0:0:0:0:1")) {
                // 根据网卡取本机配置的IP
                InetAddress inet = null;
                try {
                    inet = InetAddress.getLocalHost();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                assert inet != null;
                ip = inet.getHostAddress();
            }
        }
        return ip;
    }
}
