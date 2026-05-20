package id.ac.ui.cs.advprog.mysawitbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.MidtransProperties;
import id.ac.ui.cs.advprog.mysawitbe.common.infrastructure.external.R2Properties;

@SpringBootApplication
@EnableConfigurationProperties({ R2Properties.class, MidtransProperties.class })
public class MysawitBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MysawitBeApplication.class, args);
    }

}
