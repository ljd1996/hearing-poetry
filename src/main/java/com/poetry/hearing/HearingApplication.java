package com.poetry.hearing;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@MapperScan("com.poetry.hearing.dao")
@SpringBootApplication
@EnableCaching
public class HearingApplication{

	public static void main(String[] args) {
		SpringApplication.run(HearingApplication.class, args);
	}
}