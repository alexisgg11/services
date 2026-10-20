package com.alexis.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import javax.sql.DataSource;

@Configuration(proxyBeanMethods=false)
public class SpringConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiQueryParameterInterceptor()).addPathPatterns("/api/**");
    }
    @Bean public AppConfig appConfig() { return AppConfig.cargar(); }
    @Bean(destroyMethod="close") public ConexionDB conexionDB(AppConfig config) { return new ConexionDB(config); }
    @Bean(destroyMethod="") public DataSource dataSource(ConexionDB base) { return base.dataSource(); }
    @Bean public ApplicationRunner verificarBase(ConexionDB base) { return args -> base.verificar(); }
    @Bean public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> servidor(AppConfig config) {
        return factory -> { factory.setPort(config.port());
            if (factory instanceof org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory tomcat) {
                tomcat.addConnectorCustomizers(connector -> connector.setProperty("maxThreads", Integer.toString(config.httpThreads())));
            }
            try { factory.setAddress(java.net.InetAddress.getByName(config.host())); }
            catch (java.net.UnknownHostException error) { throw new IllegalArgumentException("APP_HOST no es válido."); }
        };
    }
}
