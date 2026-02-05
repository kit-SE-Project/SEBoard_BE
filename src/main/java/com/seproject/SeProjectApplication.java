package com.seproject;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seproject.admin.AdminAspect;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.persistence.EntityManager;

@Import({AdminAspect.class, InitRequiredData.class})
@SpringBootApplication
public class SeProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeProjectApplication.class, args);
    }
}
