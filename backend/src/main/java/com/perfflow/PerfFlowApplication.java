package com.perfflow;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
//
 // PerfFlow 绩效考核系统启动类。
 //

 //
@EnableAsync
@EnableScheduling
@MapperScan("com.perfflow.module.**.mapper")
@SpringBootApplication
public class PerfFlowApplication {

    // 应用启动入口
    public static void main(String[] args) {

        SpringApplication.run(PerfFlowApplication.class, args);
    }
}
