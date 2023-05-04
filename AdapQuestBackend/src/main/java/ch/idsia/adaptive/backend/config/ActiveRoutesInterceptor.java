package ch.idsia.adaptive.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ActiveRoutesInterceptor implements HandlerInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(ActiveRoutesInterceptor.class);

    @Value("${adapquest.controller.assistant}")
    private boolean assistant = true;
    @Value("${adapquest.controller.console}")
    private boolean console = true;
    @Value("${adapquest.controller.dashboard}")
    private boolean dashboard = true;
    @Value("${adapquest.controller.demo}")
    private boolean demo = true;
    @Value("${adapquest.controller.experiments}")
    private boolean experiments = true;
    @Value("${adapquest.controller.live}")
    private boolean live = true;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        final String path = request.getServletPath();

        if (path.contains("/assistant"))
            return assistant;
        if (path.contains("/console"))
            return console;
        if (path.contains("/dashboard"))
            return dashboard;
        if (path.contains("/demo"))
            return demo;
        if (path.contains("/experiments"))
            return experiments;
        if (path.contains("/live"))
            return live;

        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
