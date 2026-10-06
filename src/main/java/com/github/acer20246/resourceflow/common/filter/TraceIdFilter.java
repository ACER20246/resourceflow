package com.github.acer20246.resourceflow.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {
    private static final String TRACE_ID="traceId";
    private static final String TRACE_ID_HEADER = "X-TraceId";

    /**
     * 获取当前请求的TraceID
     * @return
     */
    public static String getTraceId(){
        return MDC.get(TRACE_ID);
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if(traceId == null||traceId.isEmpty()){
            traceId = UUID.randomUUID().toString().replace("-","");
        }
        try {
            MDC.put(TRACE_ID,traceId);
            response.setHeader(TRACE_ID_HEADER,traceId);
            filterChain.doFilter(request,response);
        }finally {
            MDC.remove(TRACE_ID);
        }

    }
}
