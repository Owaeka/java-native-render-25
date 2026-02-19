package br.dev.brunovieira.authcentral;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class AuthCentralApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthCentralApplication.class, args);
    }

}
