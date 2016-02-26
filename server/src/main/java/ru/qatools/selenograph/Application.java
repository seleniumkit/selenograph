package ru.qatools.selenograph;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.spring.scope.RequestContextFilter;
import ru.yandex.qatools.camelot.features.LoadPluginResourceFeature;
import ru.yandex.qatools.camelot.web.SystemInfoResource;

import static java.lang.String.format;

/**
 * @author Ilya Sadykov
 */
public class Application extends ResourceConfig {

    public Application() {
        register(RequestContextFilter.class);
        register(JacksonFeature.class);
        register(LoadPluginResourceFeature.class);
        register(SystemInfoResource.class);
        registerFinder(packageScanner("resources"));
    }

    private PackageNamesScanner packageScanner(String path) {
        return new PackageNamesScanner(new String[]{format("%s%s", getClass().getPackage().getName(), path)}, true);
    }
}
