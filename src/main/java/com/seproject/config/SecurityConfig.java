package com.seproject.config;

import com.seproject.account.common.authentication.entrypoint.CustomAuthenticationEntryPoint;
import com.seproject.account.common.authentication.handler.failure.CustomAuthenticationFailureHandler;
import com.seproject.account.common.authentication.handler.success.FormLoginAuthenticationSuccessHandler;
import com.seproject.account.common.authentication.handler.success.OidcAuthenticationSuccessHandler;
import com.seproject.account.common.authorize.url.UrlFilterInvocationSecurityMetaDataSource;
import com.seproject.account.common.authorize.url.UrlResourcesFactoryBean;
import com.seproject.account.common.authorize.handler.CustomAccessDeniedHandler;
import com.seproject.account.filter.CorsFilter;
import com.seproject.account.filter.CustomFilterSecurityInterceptor;
import com.seproject.account.filter.IpFilter;
import com.seproject.account.social.repository.CustomOidcUserService;
import com.seproject.account.Ip.application.IpService;
import com.seproject.account.filter.JwtFilter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;
import org.springframework.security.access.vote.RoleVoter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@AllArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;
    private final OidcAuthenticationSuccessHandler oidcAuthenticationSuccessHandler;
    private final FormLoginAuthenticationSuccessHandler formLoginAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler formLoginFailureHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final IpService ipService;
    private final JwtFilter jwtFilter;
    private UrlResourcesFactoryBean urlResourcesFactoryBean;
    private final CorsFilter corsFilter;

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().antMatchers("/static/**").antMatchers("/error");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.cors().disable();

//        http.headers()
//                .xssProtection()
//            .and()
//                .contentSecurityPolicy("script-src 'self'");

        http.sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.authorizeRequests()
//                .antMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll();

        http.formLogin()
                .usernameParameter("username")
                .passwordParameter("password")
                .loginProcessingUrl("/formLogin")
                .successHandler(formLoginAuthenticationSuccessHandler)
                .failureHandler(formLoginFailureHandler)
                .permitAll();

        http.oauth2Login(oauth2 -> oauth2.userInfoEndpoint(userInfoEndpointConfig ->
                userInfoEndpointConfig.oidcUserService(customOidcUserService))
                .successHandler(oidcAuthenticationSuccessHandler));

        http.logout()
                .invalidateHttpSession(true)
                .clearAuthentication(true);

        http.exceptionHandling()
                .accessDeniedHandler(accessDeniedHandler)
                .authenticationEntryPoint(authenticationEntryPoint);

        http.addFilterBefore(jwtFilter,UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(new IpFilter(ipService,accessDeniedHandler), UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(filterSecurityInterceptor(),FilterSecurityInterceptor.class);
        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public List<AccessDecisionVoter<?>> accessDecisionVoters() {
        List<AccessDecisionVoter<?>> voters = new ArrayList<>();
        voters.add(new RoleVoter());
        return voters;
    }

    @Bean
    public FilterRegistrationBean<FilterSecurityInterceptor> filterRegistrationBean() throws Exception {
        FilterRegistrationBean<FilterSecurityInterceptor> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(filterSecurityInterceptor());
        filterRegistrationBean.setEnabled(false);
        return filterRegistrationBean;
    }

//    @Bean
//    public FilterRegistrationBean<XssEscapeServletFilter> xssEscapeServletFilterFilterRegistrationBean() throws Exception {
//        FilterRegistrationBean<XssEscapeServletFilter> filterRegistrationBean = new FilterRegistrationBean<>();
//        filterRegistrationBean.setFilter(new XssEscapeServletFilter());
//        filterRegistrationBean.setOrder(1);
//        filterRegistrationBean.addUrlPatterns("/*");
//        return filterRegistrationBean;
//    }
//
//    @Bean
//    public MappingJackson2HttpMessageConverter jsonEscapeConverter() {
//        SimpleModule simpleModule = new SimpleModule();
//        simpleModule = simpleModule.addSerializer(LocalDateTime.class, new CustomLocalDateTimeSerializer());
////        simpleModule = simpleModule.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeSerializer());
//        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules().registerModule(simpleModule);
//        objectMapper.getFactory().setCharacterEscapes(new HTMLCharacterEscapes());
//        return new MappingJackson2HttpMessageConverter(objectMapper);
//    }

    @Bean
    public FilterSecurityInterceptor filterSecurityInterceptor() throws Exception {
        String[] permitAllRequest = {"/", "/login", "/index/**"};
        CustomFilterSecurityInterceptor filterSecurityInterceptor = new CustomFilterSecurityInterceptor(permitAllRequest);
        filterSecurityInterceptor.setSecurityMetadataSource(urlFilterInvocationSecurityMetaDataSource());
        filterSecurityInterceptor.setAccessDecisionManager(new AffirmativeBased(accessDecisionVoters()));
        filterSecurityInterceptor.setAuthenticationManager(authenticationManager());

        return filterSecurityInterceptor;
    }

    @Bean
    public UrlFilterInvocationSecurityMetaDataSource urlFilterInvocationSecurityMetaDataSource() throws Exception {
        return new UrlFilterInvocationSecurityMetaDataSource(urlResourcesFactoryBean);
    }

}
