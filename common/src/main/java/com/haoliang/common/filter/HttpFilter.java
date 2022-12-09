package com.haoliang.common.filter;

import com.haoliang.common.constant.SystemConstants;
import com.haoliang.common.model.dto.HeaderInfo;
import com.haoliang.common.model.ThreadLocalManager;
import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author Dominick Li
 * @Description
 * @CreateTime 2022/12/8 17:57
 **/
@Component
public class HttpFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HeaderInfo headerInfo = new HeaderInfo();
        headerInfo.setLanguage(request.getHeader(SystemConstants.LANGUAGE));
        headerInfo.setToken(request.getHeader(SystemConstants.TOKEN_NAME));
        //在ThreadLocal中添加当前线程的id
        ThreadLocalManager.add(headerInfo);
        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void destroy() {
    }

}
