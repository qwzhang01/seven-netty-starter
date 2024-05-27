package com.iwj.netty.autoconfigure;

import com.iwj.netty.annotation.EnableWebSocket;
import com.iwj.netty.pojo.Online;
import org.springframework.context.annotation.Bean;

@EnableWebSocket
public class NettyAutoConfiguration {
    @Bean
    public Online online() {
        return new Online();
    }
}
