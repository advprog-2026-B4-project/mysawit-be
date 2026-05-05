package id.ac.ui.cs.advprog.mysawitbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class MysawitBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysawitBeApplication.class, args);
    }

}
