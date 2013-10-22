/**
 *
 */
package com.zipwhip.website.controllers;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class ComponentController implements InitializingBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComponentController.class);

    @Autowired
    JmsTemplate jmsTemplate;

    @Autowired
    CuratorFramework curatorFramework;

    @RequestMapping(value = "/jms/send", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> sendJms(HttpServletRequest request) {
        String path = request.getParameter("path");
        String message = request.getParameter("message");

        jmsTemplate.convertAndSend(path, message);

        return Collections.singletonMap("sent", (Object)Boolean.TRUE);
    }

    @RequestMapping(value = "/jms/receive", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> receiveJms(HttpServletRequest request) {
        String path = request.getParameter("path");

        return Collections.singletonMap(path, jmsTemplate.receiveAndConvert(path));
    }

    @RequestMapping(value = "/curator/lock", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, String> lockCurator(HttpServletRequest request) throws Exception {
        final String path = request.getParameter("path");

        InterProcessLock lock = null;
        try {
            lock = lockOrFail(path);
            Thread.sleep(5000);
        } catch (Exception e) {
            return Collections.singletonMap("error", e.getMessage());
        } finally {
            if (lock == null) {
                return Collections.singletonMap("error", "lock was null!");
            }
            lock.release();
        }

        return Collections.singletonMap("success", Boolean.TRUE.toString());
    }

    private InterProcessMutex lockOrFail(String lockPath) throws Exception {
        InterProcessMutex mutex;

        try {
            mutex = new InterProcessMutex(curatorFramework, lockPath);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Illegal path: " + lockPath + " - " + e.getMessage());
        }

        if (!mutex.acquire(10, TimeUnit.SECONDS)) {
            throw new RuntimeException(String.format("Unable to acquire lock for %s in 10 seconds. Giving up???!", lockPath));
        }

        return mutex;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (curatorFramework.getState() != CuratorFrameworkState.STARTED) {
            curatorFramework.start();
        }
    }
}
