package kr.ac.capdi.allday;

import org.springframework.context.annotation.*;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
    @Bean FilterRegistrationBean<AuthFilter> authRegistration(AuthFilter filter){var bean=new FilterRegistrationBean<>(filter);bean.setEnabled(false);return bean;}
    @Bean SecurityFilterChain security(HttpSecurity http,AuthFilter filter) throws Exception {
        return http.csrf(c->c.disable()).cors(c->c.disable()).formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .sessionManagement(c->c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(c->c.requestMatchers("/health","/formLogin","/refresh").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS,"/**").permitAll().anyRequest().authenticated())
            .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
    }
}
