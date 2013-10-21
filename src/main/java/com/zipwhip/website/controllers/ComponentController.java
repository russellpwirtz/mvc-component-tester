/**
 *
 */
package com.zipwhip.website.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@Controller
public class ComponentController {

    @Autowired
    JmsTemplate jmsTemplate;

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

}
