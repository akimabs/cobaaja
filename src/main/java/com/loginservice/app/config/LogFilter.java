package com.loginservice.app.config;

import java.nio.charset.StandardCharsets;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class LogFilter implements WebFilter {
    
    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        
        // Log request
        ServerHttpRequest request = exchange.getRequest();
        logRequest(request);
        
        // Decorate request to capture body
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(request) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().doOnNext(dataBuffer -> {
                    String body = dataBuffer.toString(StandardCharsets.UTF_8);
                    log.info("Request Body: {}", body);
                });
            }
        };
        
        // Decorate response to capture body
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                Flux<DataBuffer> buffer = Flux.from(body);
                DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                
                return super.writeWith(buffer.map(dataBuffer -> {
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    DataBufferUtils.release(dataBuffer);
                    
                    String responseBody = new String(content, StandardCharsets.UTF_8);
                    long duration = System.currentTimeMillis() - startTime;
                    
                    log.info(
                        "Response :: status: {}, duration: {}ms, body: {}",
                        getDelegate().getStatusCode(),
                        duration,
                        responseBody
                    );
                    
                    return dataBufferFactory.wrap(content);
                }));
            }
        };
        
        return chain.filter(exchange.mutate()
                .request(requestDecorator)
                .response(responseDecorator)
                .build())
            .doOnEach(signal -> {
                if (signal.isOnComplete() || signal.isOnError()) {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info(
                        "Request completed :: status: {}, duration: {}ms",
                        exchange.getResponse().getStatusCode(),
                        duration
                    );
                }
            });
    }
    
    private void logRequest(ServerHttpRequest request) {
        log.info(
            "Request received :: requestId: {}, ip: {}, method: {}, path: {}, headers: {}",
            request.getId(),
            request.getRemoteAddress(),
            request.getMethod(),
            request.getPath(),
            request.getHeaders().entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase("Authorization"))
                .toList()
        );
    }
}