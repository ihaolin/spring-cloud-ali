package spring.cloud.ali.order.model;

import lombok.Data;

@Data
public class Order {

    private Long id;

    private Long userId;

    private String orderNo;
}
