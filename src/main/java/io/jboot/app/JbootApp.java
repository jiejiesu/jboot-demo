package io.jboot.app;

import com.jfinal.server.undertow.UndertowConfig;
import com.jfinal.server.undertow.UndertowServer;
import com.jfinal.server.undertow.WebBuilder;
import io.jboot.app.config.JbootConfigManager;
import io.jboot.app.undertow.JbootUndertowConfig;

import javax.servlet.DispatcherType;

/**
 * TODO.
 * <p>
 * User: sujie
 * Date: 2019-01-21
 * Time: 22:07
 */
public class JbootApp extends JbootApplication{
    public static void run(String[] args) {
        createServer(args).start();
    }

    /**
     * 创建 Undertow 服务器，public 用于可以给第三方创建创建着急的 Server
     *
     * @param args
     * @return 返回 UndertowServer
     */
    public static UndertowServer createServer(String[] args) {

        JbootConfigManager.me().parseArgs(args);

        JbootApplicationConfig appConfig = config(JbootApplicationConfig.class);

        appConfig.setJfinalConfig("io.jboot.core.AppConfig");
        printBannerInfo(appConfig);
        printApplicationInfo(appConfig);

        UndertowConfig undertowConfig = new JbootUndertowConfig(appConfig.getJfinalConfig());
        undertowConfig.addSystemClassPrefix("io.jboot.app");
        undertowConfig.addHotSwapClassPrefix("io.jboot");
        undertowConfig.addHotSwapClassPrefix("com.fuge");

        return UndertowServer.create(undertowConfig).setDevMode(isDevMode()).configWeb(webBuilder -> {
            tryAddMetricsSupport(webBuilder);
            tryAddShiroSupport(webBuilder);
        });
    }


    private static void tryAddMetricsSupport(WebBuilder webBuilder) {
        String url = config("jboot.metric.url");
        String reporter = config("jboot.metric.reporter");
        if (url != null && reporter != null) {
            webBuilder.addServlet("MetricsAdminServlet", "com.codahale.metrics.servlets.AdminServlet")
                    .addServletMapping("MetricsAdminServlet", url.endsWith("/*") ? url : url + "/*");
            webBuilder.addListener("io.jboot.support.metric.JbootMetricServletContextListener");
            webBuilder.addListener("io.jboot.support.metric.JbootHealthCheckServletContextListener");
        }
    }


    private static void tryAddShiroSupport(WebBuilder webBuilder) {
        String iniConfig = config("jboot.shiro.ini");
        if (iniConfig != null) {
            String urlMapping = config("jboot.shiro.urlMapping");
            if (urlMapping == null) urlMapping = "/*";
            webBuilder.addListener("org.apache.shiro.web.env.EnvironmentLoaderListener");
            webBuilder.addFilter("shiro", "io.jboot.support.shiro.JbootShiroFilter")
                    .addFilterUrlMapping("shiro", urlMapping, DispatcherType.REQUEST);

        }
    }


    private static void printBannerInfo(JbootApplicationConfig appConfig) {
        if (appConfig.isBannerEnable()) {
            System.out.println(Banner.getText(appConfig.getBannerFile()));
        }
    }

    private static void printApplicationInfo(JbootApplicationConfig appConfig) {
        System.out.println(appConfig.toString());
    }


    private static <T> T config(Class<T> clazz) {
        return JbootConfigManager.me().get(clazz);
    }

    private static String config(String key) {
        return JbootConfigManager.me().getConfigValue(key);
    }


    public static void setBootArg(String key, Object value) {
        JbootConfigManager.me().setBootArg(key, value);
    }


    private static Boolean devMode = null;

    public static boolean isDevMode() {
        if (devMode == null) {
            String appMode = JbootConfigManager.me().getConfigValue("jboot.app.mode");
            devMode = (null == appMode || "".equals(appMode.trim()) || "dev".equals(appMode));
        }
        return devMode;
    }


}
