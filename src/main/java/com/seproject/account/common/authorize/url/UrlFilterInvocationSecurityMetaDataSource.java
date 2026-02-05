package com.seproject.account.common.authorize.url;

import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

public class UrlFilterInvocationSecurityMetaDataSource implements FilterInvocationSecurityMetadataSource {

    private final LinkedHashMap<RequestMatcher, List<ConfigAttribute>> requestMap;

    private final UrlResourcesFactoryBean urlResourcesFactoryBean;
    private boolean reset; //TODO : 나중에 제거

    public UrlFilterInvocationSecurityMetaDataSource(UrlResourcesFactoryBean urlResourcesFactoryBean) throws Exception {
        this.urlResourcesFactoryBean = urlResourcesFactoryBean;
        requestMap = urlResourcesFactoryBean.getObject();
        reset = false; //TODO : 나중에 제거
    }

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {

        //TODO : 나중에 제거
        if(!reset) {
            try{
                reset();
                reset = true;
            }
            catch (Exception e) {
                throw new IllegalArgumentException();
            }
        }

        FilterInvocation filterInvocation = (FilterInvocation) object;
        HttpServletRequest request = filterInvocation.getRequest();

        for(Map.Entry<RequestMatcher, List<ConfigAttribute>> entry : requestMap.entrySet())  {
            RequestMatcher requestMatcher = entry.getKey();
            List<ConfigAttribute> configAttributes = entry.getValue();
            if(requestMatcher.matches(request)) {
                return configAttributes;
            }
        }

        return null;
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        Set<ConfigAttribute> allAttributes = new HashSet<>();
        this.requestMap.values().forEach(allAttributes::addAll);
        return allAttributes;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        return FilterInvocation.class.isAssignableFrom(clazz);
    }

    public void reset() throws Exception {
        LinkedHashMap<RequestMatcher,List<ConfigAttribute>> requestMap = urlResourcesFactoryBean.getObject();
        this.requestMap.clear();
        this.requestMap.putAll(requestMap);
    }

}
