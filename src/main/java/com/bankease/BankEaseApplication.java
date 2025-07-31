package com.bankease;

import com.bankease.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BankEaseApplication implements CommandLineRunner {

    @Autowired
    private UserService userService;

    public static void main(String[] args) {
        SpringApplication.run(BankEaseApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if not exists
        userService.createAdminUser();
        System.out.println("BankEase Application Started Successfully!");
        System.out.println("Default Admin User - Username: admin, Password: admin123");
    }
}