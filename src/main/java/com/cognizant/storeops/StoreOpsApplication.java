package com.cognizant.storeops;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** StoreOps entry point. */
@SpringBootApplication
public class StoreOpsApplication {

    /**
     * Starts the application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(StoreOpsApplication.class, args);
    }

    /**
     * The clock every service reads. Injected rather than read statically so a due-date or SLA test
     * can pin it; StoreOps runs across regions and a test that passes in one timezone is a flake
     * that gets blamed on the harness.
     *
     * @return the system UTC clock
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
