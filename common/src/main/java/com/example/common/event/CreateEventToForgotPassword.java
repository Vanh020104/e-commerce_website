package com.example.common.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateEventToForgotPassword {
    private long id;
    private String email;
    private String secretKey;
}
