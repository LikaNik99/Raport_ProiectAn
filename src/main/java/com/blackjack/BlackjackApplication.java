package com.blackjack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class BlackjackApplication {

    private static final Logger logger = LoggerFactory.getLogger(BlackjackApplication.class);

    public static void main(String[] args) {
        logger.info("Starting Blackjack Server Application...");
        SpringApplication.run(BlackjackApplication.class, args);
        logger.info("Blackjack Server started successfully!");
    }

    /**
     * Bean pentru codificarea parolelor
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
