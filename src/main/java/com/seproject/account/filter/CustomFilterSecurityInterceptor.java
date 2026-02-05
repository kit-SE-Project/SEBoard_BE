package com.seproject.account.filter;

import org.springframework.security.access.intercept.InterceptorStatusToken;
import org.springframework.security.web.FilterInvocation;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CustomFilterSecurityInterceptor extends FilterSecurityInterceptor {

    private static final String FILTER_APPLIED = "__spring_security_filterSecurityInterceptor_filterApplied";

    private boolean observeOncePerRequest = true;
    private List<RequestMatcher> permitAll;


    public CustomFilterSecurityInterceptor(String... path) {
        permitAll = new ArrayList<>();
        for (int i = 0; i < path.length; i++) {
            permitAll.add(new AntPathRequestMatcher(path[i]));
        }
    }

    @Override
    protected InterceptorStatusToken beforeInvocation(Object object) {
        HttpServletRequest request = ((FilterInvocation) object).getHttpRequest();
        for (RequestMatcher requestMatcher : permitAll) {
            if(requestMatcher.matches(request)) {
                return null;
            }
        }

        return super.beforeInvocation(object);
    }

    public void invoke(FilterInvocation filterInvocation) throws IOException, ServletException {

        if (isApplied(filterInvocation) && this.observeOncePerRequest) {
            filterInvocation.getChain().doFilter(filterInvocation.getRequest(), filterInvocation.getResponse());
            return;
        }

        if (filterInvocation.getRequest() != null && this.observeOncePerRequest) {
            filterInvocation.getRequest().setAttribute(FILTER_APPLIED, Boolean.TRUE);
        }

        InterceptorStatusToken token = beforeInvocation(filterInvocation);

        try {
            filterInvocation.getChain().doFilter(filterInvocation.getRequest(), filterInvocation.getResponse());
        }
        finally {
            super.finallyInvocation(token);
        }
        super.afterInvocation(token, null);
    }

    private boolean isApplied(FilterInvocation filterInvocation) {
        return (filterInvocation.getRequest() != null)
                && (filterInvocation.getRequest().getAttribute(FILTER_APPLIED) != null);
    }

}
