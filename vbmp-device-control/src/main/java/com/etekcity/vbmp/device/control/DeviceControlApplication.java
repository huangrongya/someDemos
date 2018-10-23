package com.etekcity.vbmp.device.control;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import tk.mybatis.spring.annotation.MapperScan;

@MapperScan("com.etekcity.vbmp.device.control.dao.mapper")
@SpringBootApplication
public class DeviceControlApplication extends SpringBootServletInitializer {

    public static void main(String[] args) throws Exception {
        SpringApplication.run(DeviceControlApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(DeviceControlApplication.class);
    }
}
