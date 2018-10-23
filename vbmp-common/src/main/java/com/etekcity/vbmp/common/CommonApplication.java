package com.etekcity.vbmp.common;

import com.alibaba.fastjson.parser.ParserConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@MapperScan(basePackages = {"com.etekcity.vbmp.common.comm.dao.mapper", "com.etekcity.vbmp.common.router.dao.mapper",
        "com.etekcity.vbmp.common.dao.mapper"})
public class CommonApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(CommonApplication.class);
    }

    public static void main(String[] args) {
        ParserConfig.getGlobalInstance().addAccept("com.etekcity.vbmp.");
        SpringApplication.run(CommonApplication.class, args);
    }
}
