package com.microsoft.applicationinsights.smoketestapp;

import com.microsoft.applicationinsights.telemetry.RequestTelemetry;
import com.microsoft.applicationinsights.web.internal.ThreadContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String root() {
        return "OK";
    }


    @GetMapping("/test")
    public String test() {
        RequestTelemetry requestTelemetry = ThreadContext.getRequestTelemetryContext().getHttpRequestTelemetry();
        requestTelemetry.getProperties().put("myattr1", "myvalue1");
        requestTelemetry.getProperties().put("myattr2", "myvalue2");
        requestTelemetry.getContext().getUser().setId("myuser");
        requestTelemetry.setName("myspanname");
        requestTelemetry.setSource("mysource");
        return "OK!";
    }
}
