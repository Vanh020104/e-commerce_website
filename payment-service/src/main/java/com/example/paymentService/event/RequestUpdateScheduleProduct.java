package com.example.paymentService.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RequestUpdateScheduleProduct {
    private Long productId;
    private Long orderId;

}
