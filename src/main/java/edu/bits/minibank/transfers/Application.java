package edu.bits.minibank.transfers;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {
    public static void main(String[] args) {
        // Avoid overlong Java selector socket paths on Windows. Linux is unchanged.
        if (System.getProperty("os.name").startsWith("Windows")
                && System.getProperty("jdk.net.unixdomain.tmpdir") == null)
            System.setProperty("jdk.net.unixdomain.tmpdir", ".");
        SpringApplication.run(Application.class, args);
    }
}
