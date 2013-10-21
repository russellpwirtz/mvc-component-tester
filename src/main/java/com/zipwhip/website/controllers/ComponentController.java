/**
 *
 */
package com.zipwhip.website.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;

@Controller
public class ComponentController {

    @RequestMapping(value = "/memcached", method = RequestMethod.GET)
    @ResponseBody
    public Map<String, Object> evaluate(HttpServletRequest request) {
        String query = request.getParameter("query");

        return Collections.singletonMap("query", (Object) query);
    }

}
