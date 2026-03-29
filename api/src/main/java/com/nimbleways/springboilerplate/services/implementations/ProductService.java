package com.nimbleways.springboilerplate.services.implementations;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.nimbleways.springboilerplate.entities.Product;
import com.nimbleways.springboilerplate.entities.ProductType;
import com.nimbleways.springboilerplate.entities.Order;
import com.nimbleways.springboilerplate.repositories.OrderRepository;
import com.nimbleways.springboilerplate.repositories.ProductRepository;
import com.nimbleways.springboilerplate.services.NotificationService;
import com.nimbleways.springboilerplate.exceptions.OrderNotFoundException;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final OrderRepository orderRepository;

    public ProductService(ProductRepository productRepository, NotificationService notificationService, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.orderRepository = orderRepository;
    }

    public Long processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));

        for (Product product : order.getItems()) {
            processProduct(product);
        }

        return order.getId();
    }

    public void processProduct(Product product) {
        ProductType productType = product.getType();
        switch (productType) {
            case NORMAL ->
                handleNormalProduct(product);
            case SEASONAL ->
                handleSeasonalProduct(product);
            case EXPIRABLE ->
                handleExpiredProduct(product);
        }
    }

    public void handleNormalProduct(Product product) {
        if (product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else if (product.getLeadTime() > 0) {
            notifyDelay(product.getLeadTime(), product);
        }
    }

    public void notifyDelay(int leadTime, Product product) {
        product.setLeadTime(leadTime);
        productRepository.save(product);
        notificationService.sendDelayNotification(leadTime, product.getName());
    }

    public void handleSeasonalProduct(Product product) {
        LocalDate today = LocalDate.now();
        boolean inSeason = today.isAfter(product.getSeasonStartDate()) && today.isBefore(product.getSeasonEndDate());

        if (inSeason && product.getAvailable() > 0) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            if (today.plusDays(product.getLeadTime()).isAfter(product.getSeasonEndDate())) {
                notificationService.sendOutOfStockNotification(product.getName());
                product.setAvailable(0);
                productRepository.save(product);
            } else if (product.getSeasonStartDate().isAfter(today)) {
                notificationService.sendOutOfStockNotification(product.getName());
                productRepository.save(product);
            } else {
                notifyDelay(product.getLeadTime(), product);
            }
        }

    }

    public void handleExpiredProduct(Product product) {
        if (product.getAvailable() > 0 && product.getExpiryDate().isAfter(LocalDate.now())) {
            product.setAvailable(product.getAvailable() - 1);
            productRepository.save(product);
        } else {
            notificationService.sendExpirationNotification(product.getName(), product.getExpiryDate());
            product.setAvailable(0);
            productRepository.save(product);
        }
    }
}
