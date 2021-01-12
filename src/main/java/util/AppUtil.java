package util;

import java.io.IOException;
import java.util.Properties;

/**
 * @author:Tharanga Mahavila <tharangamahavila@gmail.com>
 * @since : 2021-01-11
 **/
public class AppUtil {

    public static String getAppSecretKey() throws IOException {
        Properties properties = new Properties();
        properties.load(AppUtil.class.getResourceAsStream("/application.properties"));
        return properties.getProperty("app.key");
    }
}
