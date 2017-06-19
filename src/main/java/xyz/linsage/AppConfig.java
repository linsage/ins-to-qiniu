package xyz.linsage;

import com.jfinal.config.*;
import com.jfinal.plugin.ehcache.EhCachePlugin;
import com.jfinal.template.Engine;
import org.apache.log4j.PropertyConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import xyz.linsage.controller.IndexController;

import java.awt.*;
import java.net.URI;

/**
 * 启动类
 *
 * @author linjingjie
 * @create 2017-06-13  上午9:41
 */
public class AppConfig extends JFinalConfig {
    private static final int PORT = 9090;
    private static final String BASE_URL = "http://localhost:" + PORT;
    private static final String WEB_PATH = "src/main/webapp";
    private static final String CONTEXT_PATH = "/";

    /**
     * 开发模式启动
     **/
    @SuppressWarnings("deprecation")
    public static void main(String[] args) throws Exception {
        Server server = new Server(PORT);
        WebAppContext context = new WebAppContext(WEB_PATH, CONTEXT_PATH);
        server.setHandler(context);
        try {
            server.start();
            //打开浏览器
            System.out.println(BASE_URL);
            Desktop.getDesktop().browse(new URI(BASE_URL));
            server.join();
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    @Override
    public void configConstant(Constants me) {
        loadPropertyFile("config.properties");
        //全局变量
        Constant.Qiniu.accessKey = getProperty("qiuniu.accessKey");
        Constant.Qiniu.secretKey = getProperty("qiuniu.secretKey");
        Constant.Qiniu.bucket = getProperty("qiuniu.bucket");
        Constant.Qiniu.domain = getProperty("qiuniu.domain");
        Constant.Qiniu.separator = getProperty("qiuniu.separator");
    }

    @Override
    public void configRoute(Routes me) {
        me.add("/", IndexController.class);
    }

    @Override
    public void configEngine(Engine me) {

    }

    @Override
    public void configPlugin(Plugins me) {
        //配置缓存插件
        me.add(new EhCachePlugin());
    }

    @Override
    public void configInterceptor(Interceptors me) {

    }

    @Override
    public void configHandler(Handlers me) {

    }
}
