package kr.ac.capdi.allday;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.*;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfiguration {
    @Bean FilterRegistrationBean<CorsFilter> cors(@Value("${allday.cors-origin}") String origin) {
        var c=new CorsConfiguration();c.setAllowedOrigins(java.util.Arrays.stream(origin.split(",")).map(String::trim).toList());
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type","Idempotency-Key","ngrok-skip-browser-warning"));
        c.setExposedHeaders(List.of("X-Request-Id","Location"));c.setAllowCredentials(false);
        var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",c);
        var bean=new FilterRegistrationBean<>(new CorsFilter(source));bean.setOrder(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);return bean;
    }
}
