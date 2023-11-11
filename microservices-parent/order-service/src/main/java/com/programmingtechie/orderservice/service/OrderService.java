package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dto.OrderLineItemDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItem;
import com.programmingtechie.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest){

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .orderLineItemList(orderRequest.getOrderLineItemDtoList().stream().map(this::mapToOrderLineItemEntity).toList())
                .build();

        orderRepository.save(order);
    }

    private OrderLineItem mapToOrderLineItemEntity(OrderLineItemDto orderLineItemDto) {
        return OrderLineItem.builder()
                .quantity(orderLineItemDto.getQuantity())
                .price(orderLineItemDto.getPrice())
                .skuCode(orderLineItemDto.getSkuCode()).build();
    }

}
