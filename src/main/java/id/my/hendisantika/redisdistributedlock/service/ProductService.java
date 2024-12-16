package id.my.hendisantika.redisdistributedlock.service;

import id.my.hendisantika.redisdistributedlock.dto.MakeOrderRequest;
import id.my.hendisantika.redisdistributedlock.entity.Product;
import id.my.hendisantika.redisdistributedlock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-distributed-lock
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 16/12/24
 * Time: 11.16
 * To change this template use File | Settings | File Templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {
    private final LockRegistry lockRegistry;
    private final ProductRepository productRepository;
    private final RedissonClient redissonClient;

    private void updateStock(MakeOrderRequest request, Product product) {
        int newStock = product.getStock() - request.getStock();
        product.setStock(newStock);
        product.setUpdatedBy(request.getUpdatedBy());
        productRepository.save(product);
    }

    Product checkStock(MakeOrderRequest request) throws Exception {
        var optionalProduct = productRepository.findById(request.getId());
        if (optionalProduct.isEmpty()) {
            log.info("Product not found");
            throw new Exception("Product not found");
        }

        final var product = optionalProduct.get();
        log.info("{}-{}", product.getStock(), request.getStock());
        if (product.getStock() < request.getStock()) {
            log.info("{} out of stock", product.getStock());
            throw new Exception(product.getName() + " out of stock");
        }
        return product;
    }

    public String makeOrder(MakeOrderRequest request) throws Exception {
        String key = "ID-" + request.getId().toString();
        RLock lock = redissonClient.getLock(key);

        boolean lockAcquired = lock.tryLock(5, TimeUnit.SECONDS);
        if (lockAcquired) {
            try {
                final var product = checkStock(request);
                updateStock(request, product);

                Thread.sleep(request.getDelay());

                return "ORDER COMPLETED" + "\n\r" +
                        product.getName() + " stock count is : " + product.getStock();
            } finally {
                if (lockAcquired) {
                    lock.unlock();
                }
            }
        } else {
            log.info("The product you want to buy is already locked");
        }
        return checkStock(request).toString();
    }

    public String order(MakeOrderRequest request) throws Exception {
        String key = "ID-" + request.getId().toString();
        Lock lock = lockRegistry.obtain(key);
        boolean lockAcquired = lock.tryLock(5, TimeUnit.SECONDS);

        if (lockAcquired) {
            try {
                final var product = checkStock(request);
                updateStock(request, product);
                Thread.sleep(request.getDelay());

                return "ORDER COMPLETED" + "\n\r" +
                        product.getName() + " stock count is : " + product.getStock();
            } finally {
                lock.unlock();
            }
        } else {
            log.info("The product you want to buy is already locked");
        }
        checkStock(request);
        return "OK";
    }
}
