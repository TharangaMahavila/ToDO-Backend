package Listner;

/**
 * @author:Tharanga Mahavila <tharangamahavila@gmail.com>
 * @since : 2021-01-11
 **/

import org.apache.commons.dbcp2.BasicDataSource;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

@WebListener
public class ContextListner implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Properties properties = new Properties();
        try {
            properties.load(this.getClass().getResourceAsStream("/application.properties"));
            BasicDataSource bds = new BasicDataSource();
            bds.setUsername(properties.getProperty("mysql.username"));
            bds.setPassword(properties.getProperty("mysql.password"));
            bds.setUrl(properties.getProperty("mysql.url"));
            bds.setDriverClassName(properties.getProperty("mysql.driver_class"));
            bds.setInitialSize(5);
            bds.setMaxTotal(10);
            sce.getServletContext().setAttribute("cp",bds);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        BasicDataSource bds = (BasicDataSource) sce.getServletContext().getAttribute("cp");
        try {
            bds.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
