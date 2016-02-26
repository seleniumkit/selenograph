package ru.qatools.selenograph.gridrouter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext;

/**
 * @author Ilya Sadykov
 */
public class JettyProxyInitializer implements ServletContextListener {
    private final static Logger LOGGER = LoggerFactory.getLogger(JettyProxyInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Servlet context initialized");
        int tpSize = Integer.valueOf(getWebApplicationContext(sce.getServletContext()).getEnvironment()
                .getProperty("grid.config.jetty.executor.threads.count", "500"));
        sce.getServletContext().setAttribute("org.eclipse.jetty.server.Executor", newFixedThreadPool(tpSize));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Servlet context destroyed");
    }
}
