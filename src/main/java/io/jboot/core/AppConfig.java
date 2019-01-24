package io.jboot.core;

import com.fuge.ActivitiPlugin;
import com.jfinal.aop.Aop;
import com.jfinal.config.*;
import com.jfinal.core.Controller;
import com.jfinal.json.JsonManager;
import com.jfinal.kit.PropKit;
import com.jfinal.kit.StrKit;
import com.jfinal.log.Log;
import com.jfinal.plugin.activerecord.ActiveRecordPlugin;
import com.jfinal.plugin.activerecord.dialect.MysqlDialect;
import com.jfinal.plugin.druid.DruidPlugin;
import com.jfinal.server.undertow.UndertowServer;
import com.jfinal.template.Engine;
import com.jfinal.template.ext.directive.NowDirective;
import com.jfinal.weixin.sdk.api.ApiConfig;
import com.jfinal.weixin.sdk.api.ApiConfigKit;
import io.jboot.Jboot;
import io.jboot.aop.JbootAopFactory;
import io.jboot.aop.jfinal.JfinalHandlers;
import io.jboot.aop.jfinal.JfinalPlugins;
import io.jboot.components.rpc.JbootrpcManager;
import io.jboot.components.schedule.JbootScheduleManager;
import io.jboot.core.listener.JbootAppListenerManager;
import io.jboot.core.log.Slf4jLogFactory;
import io.jboot.db.JbootDbManager;
import io.jboot.support.shiro.JbootShiroManager;
import io.jboot.support.swagger.JbootSwaggerConfig;
import io.jboot.support.swagger.JbootSwaggerController;
import io.jboot.support.swagger.JbootSwaggerManager;
import io.jboot.utils.ArrayUtil;
import io.jboot.utils.ClassScanner;
import io.jboot.utils.ClassUtil;
import io.jboot.web.JbootJson;
import io.jboot.web.cache.ActionCacheHandler;
import io.jboot.web.controller.JbootControllerManager;
import io.jboot.web.controller.annotation.RequestMapping;
import io.jboot.web.directive.annotation.JFinalDirective;
import io.jboot.web.directive.annotation.JFinalSharedMethod;
import io.jboot.web.directive.annotation.JFinalSharedObject;
import io.jboot.web.directive.annotation.JFinalSharedStaticMethod;
import io.jboot.web.fixedinterceptor.FixedInterceptors;
import io.jboot.web.handler.JbootActionHandler;
import io.jboot.web.handler.JbootFilterHandler;
import io.jboot.web.handler.JbootHandler;
import io.jboot.web.render.JbootRenderFactory;
import io.jboot.wechat.JbootAccessTokenCache;
import io.jboot.wechat.JbootWechatConfig;

import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * TODO.
 * <p>
 * User: sujie
 * Date: 2019-01-21
 * Time: 15:29
 */
public class AppConfig extends JFinalConfig
{


    static final Log log = Log.getLog(JbootCoreConfig.class);
    private List<Routes.Route> routeList = new ArrayList<>();

    public AppConfig()
    {
        Aop.setAopFactory(new JbootAopFactory());
        Aop.inject(this);
        JbootAppListenerManager.me().onInit();
    }


    @Override
    public void configConstant(Constants constants)
    {

        loadPropertyFile("jboot.properties");
        PropKit.use("jboot.properties");
        //        constants.setDevMode(PropKit.getBoolean("devMode", true));
        //        constants.setViewType(ViewType.JFINAL_TEMPLATE);

        constants.setBaseUploadPath(System.getProperty("user.dir") + getProperty("upload.path"));

        constants.setRenderFactory(JbootRenderFactory.me());
        constants.setDevMode(Jboot.isDevMode());
        ApiConfigKit.setDevMode(Jboot.isDevMode());

        JbootWechatConfig config = Jboot.config(JbootWechatConfig.class);
        ApiConfig apiConfig = config.getApiConfig();
        if (apiConfig != null)
        {
            ApiConfigKit.putApiConfig(apiConfig);
        }

        constants.setLogFactory(Slf4jLogFactory.me());
        constants.setMaxPostSize(1024 * 1024 * 2000);
        constants.setReportAfterInvocation(false);

        constants.setControllerFactory(JbootControllerManager.me());
        constants.setJsonFactory(() -> new JbootJson());
        constants.setInjectDependency(true);

        JbootAppListenerManager.me().onJfinalConstantConfig(constants);

    }


    @Override
    public void configRoute(Routes routes)
    {

        List<Class<Controller>> controllerClassList = ClassScanner.scanSubClass(Controller.class);
        if (ArrayUtil.isNotEmpty(controllerClassList))
        {
            for (Class<Controller> clazz : controllerClassList)
            {
                RequestMapping mapping = clazz.getAnnotation(RequestMapping.class);
                if (mapping == null || mapping.value() == null)
                {
                    continue;
                }

                if (StrKit.notBlank(mapping.viewPath()))
                {
                    routes.add(mapping.value(), clazz, mapping.viewPath());
                } else
                {
                    routes.add(mapping.value(), clazz);
                }
            }
        }

        JbootSwaggerConfig swaggerConfig = Jboot.config(JbootSwaggerConfig.class);
        if (swaggerConfig.isConfigOk())
        {
            routes.add(swaggerConfig.getPath(), JbootSwaggerController.class, swaggerConfig.getPath());
        }

        JbootAppListenerManager.me().onJfinalRouteConfig(routes);

        for (Routes.Route route : routes.getRouteItemList())
        {
            JbootControllerManager.me().setMapping(route.getControllerKey(), route.getControllerClass());
        }

        routeList.addAll(routes.getRouteItemList());
    }

    @Override
    public void configEngine(Engine engine)
    {

        /**
         * now 并没有被添加到默认的指令当中
         * 查看：EngineConfig
         */
        engine.addDirective("now", NowDirective.class);

        List<Class> directiveClasses = ClassScanner.scanClass();
        for (Class clazz : directiveClasses)
        {
            JFinalDirective jFinalDirective = (JFinalDirective) clazz.getAnnotation(JFinalDirective.class);
            if (jFinalDirective != null)
            {
                engine.addDirective(jFinalDirective.value(), clazz);
            }

            JFinalSharedMethod sharedMethod = (JFinalSharedMethod) clazz.getAnnotation(JFinalSharedMethod.class);
            if (sharedMethod != null)
            {
                engine.addSharedMethod(ClassUtil.newInstance(clazz));
            }

            JFinalSharedStaticMethod sharedStaticMethod = (JFinalSharedStaticMethod) clazz.getAnnotation(JFinalSharedStaticMethod.class);
            if (sharedStaticMethod != null)
            {
                engine.addSharedStaticMethod(clazz);
            }

            JFinalSharedObject sharedObject = (JFinalSharedObject) clazz.getAnnotation(JFinalSharedObject.class);
            if (sharedObject != null)
            {
                engine.addSharedObject(sharedObject.value(), ClassUtil.newInstance(clazz));
            }
        }

        JbootAppListenerManager.me().onJfinalEngineConfig(engine);
    }


    @Override
    public void configPlugin(Plugins plugins)
    {
        DruidPlugin druidPlugin = new DruidPlugin(getProperty("db.default.url"),
                getProperty("db.default.user"),
                getProperty("db.default.password"),
                getProperty("db.default.driver"));

        druidPlugin.setInitialSize(getPropertyToInt("db.default.poolInitialSize"));
        druidPlugin.setMaxPoolPreparedStatementPerConnectionSize(getPropertyToInt("db.default.poolMaxSize"));
        druidPlugin.setTimeBetweenConnectErrorMillis(getPropertyToInt("db.default.connectionTimeoutMillis"));
        plugins.add(druidPlugin);
        //        DbKit.addConfig(new Config(DbKit.MAIN_CONFIG_NAME, druidPlugin.getDataSource()));

        // 配置ActiveRecord插件
                ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);
                arp.setDialect(new MysqlDialect());
                plugins.add(arp);


        List<ActiveRecordPlugin> arps = JbootDbManager.me().getActiveRecordPlugins();
        for (ActiveRecordPlugin arp1 : arps)
        {
            plugins.add(arp1);
        }

        //配置Activiti插件
        ActivitiPlugin ap = new ActivitiPlugin();
        plugins.add(ap);

        JbootAppListenerManager.me().onJfinalPluginConfig(new JfinalPlugins(plugins));

    }


    @Override
    public void configInterceptor(Interceptors interceptors)
    {

        JbootAppListenerManager.me().onInterceptorConfig(interceptors);

        JbootAppListenerManager.me().onFixedInterceptorConfig(FixedInterceptors.me());
    }

    @Override
    public void configHandler(Handlers handlers)
    {

        //用于对jfinal的拦截器进行注入
        handlers.setActionHandler(new JbootActionHandler());

        //先添加用户的handler，再添加jboot自己的handler
        //用户的handler优先于jboot的handler执行
        JbootAppListenerManager.me().onHandlerConfig(new JfinalHandlers(handlers));

        handlers.add(new JbootFilterHandler());
        handlers.add(new ActionCacheHandler());
        handlers.add(new JbootHandler());

    }

    @Override
    public void afterJFinalStart()
    {

        /**
         * 配置微信accessToken的缓存
         */
        ApiConfigKit.setAccessTokenCache(new JbootAccessTokenCache());
        JsonManager.me().setDefaultDatePattern("yyyy-MM-dd HH:mm:ss");

        /**
         * 初始化
         */
        JbootrpcManager.me().init();
        JbootShiroManager.me().init(routeList);
        JbootScheduleManager.me().init();
        JbootSwaggerManager.me().init();

        JbootAppListenerManager.me().onJFinalStarted();
    }

    @Override
    public void beforeJFinalStop()
    {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        if (drivers != null)
        {
            while (drivers.hasMoreElements())
            {
                try
                {
                    Driver driver = drivers.nextElement();
                    DriverManager.deregisterDriver(driver);
                } catch (Exception e)
                {
                    log.error(e.toString(), e);
                }
            }
        }
        JbootAppListenerManager.me().onJFinalStop();
    }
    //    @Override
    //    public void configConstant(Constants constants)
    //    {
    //        super.configConstant(constants);
    //        loadPropertyFile("jboot.properties");
    //        PropKit.use("jboot.properties");
    //        constants.setDevMode(PropKit.getBoolean("devMode", true));
    //        constants.setViewType(ViewType.JFINAL_TEMPLATE);
    //
    //        constants.setBaseUploadPath(System.getProperty("user.dir") + getProperty("upload.path"));
    //
    //    }

    //    @Override
    //    public void configRoute(Routes routes)
    //    {
    ////        routes.add("/", HelloController.class, "/hello");
    //    }

    //    @Override
    //    public void configEngine(Engine engine)
    //    {
    //
    //    }

    //    @Override
    //    public void configPlugin(Plugins plugins)
    //    {
    //        super.configPlugin(plugins);
    //
    //        DruidPlugin druidPlugin = new DruidPlugin(getProperty("db.default.url"),
    //                getProperty("db.default.user"),
    //                getProperty("db.default.password"),
    //                getProperty("db.default.driver"));
    //
    //        druidPlugin.setInitialSize(getPropertyToInt("db.default.poolInitialSize"));
    //        druidPlugin.setMaxPoolPreparedStatementPerConnectionSize(getPropertyToInt("db.default.poolMaxSize"));
    //        druidPlugin.setTimeBetweenConnectErrorMillis(getPropertyToInt("db.default.connectionTimeoutMillis"));
    //        plugins.add(druidPlugin);
    //        //        DbKit.addConfig(new Config(DbKit.MAIN_CONFIG_NAME, druidPlugin.getDataSource()));
    //
    //        // 配置ActiveRecord插件
    //        ActiveRecordPlugin arp = new ActiveRecordPlugin(druidPlugin);
    //        arp.setDialect(new MysqlDialect());
    //        plugins.add(arp);
    //
    //        //配置Activiti插件
    //        ActivitiPlugin ap = new ActivitiPlugin();
    //        plugins.add(ap);
    //
    //        //        _MappingKit.mapping(arp);//注册所有model-bean
    //    }

    //    @Override
    //    public void configInterceptor(Interceptors interceptors)
    //    {
    //
    //    }
    //
    //    @Override
    //    public void configHandler(Handlers handlers)
    //    {
    //    }

    public static void main(String[] args)
    {
        //        JbootApplication.createServer(args).start();
        UndertowServer.start(AppConfig.class, 8090, true);

        //        JFinal.start("src/main/webapp", 8090, "/");

    }
}
