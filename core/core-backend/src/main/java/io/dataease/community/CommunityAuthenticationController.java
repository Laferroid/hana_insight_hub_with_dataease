package io.dataease.community;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/setting/authentication")
public class CommunityAuthenticationController {

    @GetMapping("/status")
    public List<Object> status() {
        return Collections.emptyList();
    }
}
