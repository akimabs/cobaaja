package com.loginservice.app.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient webClient() {
		HttpClient httpClient = HttpClient.create()
			.compress(true)
			.followRedirect(true)
			.responseTimeout(Duration.ofSeconds(2))
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1500);

		return WebClient.builder()
			.baseUrl("https://jsonplaceholder.typicode.com")
			.clientConnector(new ReactorClientHttpConnector(httpClient))
			.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}
}


