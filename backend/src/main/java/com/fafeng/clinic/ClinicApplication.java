package com.fafeng.clinic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.fafeng.clinic.system.mapper",
        "com.fafeng.clinic.medicine.mapper",
        "com.fafeng.clinic.patient.mapper",
        "com.fafeng.clinic.clinic.mapper"
})
public class ClinicApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClinicApplication.class, args);
    }
}
