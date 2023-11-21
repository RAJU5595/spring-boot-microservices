package com.programmingtechie.orderservice.service;

import com.programmingtechie.orderservice.dto.InventoryResponse;
import com.programmingtechie.orderservice.dto.OrderLineItemDto;
import com.programmingtechie.orderservice.dto.OrderRequest;
import com.programmingtechie.orderservice.event.OrderPlacedEvent;
import com.programmingtechie.orderservice.model.Order;
import com.programmingtechie.orderservice.model.OrderLineItem;
import com.programmingtechie.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;

    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;

    @Qualifier("inventoryClient")
    private final WebClient.Builder webClient;

    public String placeOrder(OrderRequest orderRequest){

        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .orderLineItemList(orderRequest.getOrderLineItemDtoList().stream().map(this::mapToOrderLineItemEntity).toList())
                .build();

        List<String> skuCodes = order.getOrderLineItemList().stream().map(orderLineItem -> orderLineItem.getSkuCode()).toList();

        // call inventory service, place the order if product is in stock
        InventoryResponse[] inventoryResponseArray = webClient.build().get()
                .uri("http://inventory-service/api/inventory/getInventoryDetails",
                        uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean result = Arrays.stream(inventoryResponseArray).allMatch(inventoryResponse -> inventoryResponse.isInStock());

        if(result){
            orderRepository.save(order);
            kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
            return "Order Placed Successfully";
        }else{
            throw new IllegalArgumentException("Product is not in stock, please try again");
        }

    }

    private OrderLineItem mapToOrderLineItemEntity(OrderLineItemDto orderLineItemDto) {
        return OrderLineItem.builder()
                .quantity(orderLineItemDto.getQuantity())
                .price(orderLineItemDto.getPrice())
                .skuCode(orderLineItemDto.getSkuCode()).build();
    }

}
