package com.nimbleways.springboilerplate.services.implementations;

import java.util.Optional;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.NotificationService;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;

@ExtendWith(SpringExtension.class)
@UnitTest
public class ProductServiceUnitTests {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks 
    private ProductService productService;

    @Mock
    private OrderRepository orderRepository;

    @Test
    public void notifDelay() {
        // GIVEN
        Product product =new Product(null, 15, 0, ProductType.NORMAL, "RJ45 Cable", null, null, null);

        Mockito.when(productRepository.save(product)).thenReturn(product);

        // WHEN
        productService.notifyDelay(product.getLeadTime(), product);

        // THEN
        assertEquals(0, product.getAvailable());
        assertEquals(15, product.getLeadTime());
        Mockito.verify(productRepository, Mockito.times(1)).save(product);
        Mockito.verify(notificationService, Mockito.times(1)).sendDelayNotification(product.getLeadTime(), product.getName());
    }

    @Test
    public void processOrder_notFoundOrder(){
        Mockito.when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows( OrderNotFoundException.class, () -> productService.processOrder(99L));
    }

}