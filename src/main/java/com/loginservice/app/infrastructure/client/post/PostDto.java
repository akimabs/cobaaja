package com.loginservice.app.infrastructure.client.post;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PostDto {
    private Long userId;
    private Long id;
    private String title;
    private String body;
}
