package com.nimbleways.springboilerplate.services.implementations;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.NotificationService;
import com.nimbleways.springboilerplate.utils.Annotations.UnitTest;

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
        Product product = new Product(null, 15, 0, ProductType.NORMAL, "RJ45 Cable", null, null, null);

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
    public void processOrder_notFoundOrder() {
        Mockito.when(orderRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(OrderNotFoundException.class, () -> productService.processOrder(99L));
    }

    @Test
    public void processOrder_decrementAvailableAfterProcessNormalProduct() {
        Product product = new Product(1L, 10, 15, ProductType.NORMAL, "RJ45 Cable", null, null, null);
        Order order = new Order(1L, Set.of(product));
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        productService.processOrder(1L);
        assertEquals(14, product.getAvailable());
        Mockito.verify(productRepository).save(product);
    }

    @Test
    public void processOrder_notifyOutOfStock() {
        Product product = new Product(1L, 5, 0, ProductType.NORMAL, "Cable", null, null, null);
        Order order = new Order(1L, Set.of(product));
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        productService.processOrder(1L);

        Mockito.verify(notificationService).sendDelayNotification(5, "Cable");

    }

    @Test
    public void processOrder_expirableWhenNotExpired() {
        Product product = new Product(1L, 0, 10, ProductType.EXPIRABLE, "Food", LocalDate.now().plusDays(5), null, null);
        Order order = new Order(1L, Set.of(product));
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        productService.processOrder(1L);

        assertEquals(9, product.getAvailable());

        Mockito.verify(productRepository).save(product);
    }

    @Test
    public void processOrder_expirableWhenExpired() {
        Product product = new Product(2L, 0, 15, ProductType.EXPIRABLE, "Food", LocalDate.now().minusDays(2), null, null);
        Order order = new Order(2L, Set.of(product));
        Mockito.when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        productService.processOrder(2L);
        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendExpirationNotification("Food", LocalDate.now().minusDays(2));
    }

    @Test
    public void processOrder_expirableWhenOutOfStock() {
        Product product = new Product(1L, 0, 0, ProductType.EXPIRABLE, "Food", LocalDate.now().plusDays(5), null, null);
        Order order = new Order(1L, Set.of(product));
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        productService.processOrder(1L);

        assertEquals(0, product.getAvailable());
        Mockito.verify(notificationService).sendExpirationNotification("Food", LocalDate.now().plusDays(5));

    }

    @Test
    public void processOrder_seasonaleNormal() {
        LocalDate today = LocalDate.now();
        Product product = new Product(1L, 0, 5, ProductType.SEASONAL, "Clothes", null, today.minusDays(5), today.plusDays(10));
        Order order = new Order(1L, Set.of(product));
        Mockito.when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        productService.processOrder(1L);
        assertEquals(4, product.getAvailable());
    }

}
