package com.sokeeper.web.controller.sub;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.sokeeper.web.dto.UserDto;

@Controller
public class ExampleController {

    @RequestMapping
    public void index(Map<String, Object> out) {

    }

    @RequestMapping
    public void home(UserDto user, Map<String, Object> out) {
        out.put("user", user);
        if (!StringUtils.hasText(user.getUsername())) {
            out.put("message", "username can not be empty.");
        }
    }

    @RequestMapping
    public ModelAndView switchToPage() {
        return new ModelAndView("sub/example/switched");
    } 
}
