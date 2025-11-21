package com.java.vms;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest // This annotation is used to bootstrap the Spring application context for integration testing. It loads your entire application and creates an ApplicationContext.
public class VmsApplicationTests {

    @Test
    void contextLoads(){
        // No assertions needed, the test will fail if the context fails to load
    }
}
