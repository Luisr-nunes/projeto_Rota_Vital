package com.hemorede.config;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.hemorede.dto.TelemetriaLeituraDTO;

@Configuration
public class TelemetriaQueueConfig {

    @Bean
    public BlockingQueue<TelemetriaLeituraDTO> telemetriaQueue() {
        return new LinkedBlockingQueue<>(1000);
    }
}