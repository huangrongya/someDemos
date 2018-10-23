package com.etekcity.vbmp.timing;

import com.alibaba.fastjson.parser.ParserConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.etekcity.vbmp.**.dao")
public class TimingApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TimingApplication.class);
    }

    public static void main(String[] args) throws Exception {
        ParserConfig.getGlobalInstance().addAccept("com.etekcity.vbmp.");
        SpringApplication.run(TimingApplication.class, args);
    }
}
