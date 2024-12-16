package id.my.hendisantika.redisdistributedlock.service;

import id.my.hendisantika.redisdistributedlock.dto.MakeOrderRequest;
import id.my.hendisantika.redisdistributedlock.entity.Product;
import id.my.hendisantika.redisdistributedlock.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RedissonClient;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.stereotype.Service;

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
            System.out.println("Product not found");
            throw new Exception("Product not found");
        }

        final var product = optionalProduct.get();
        System.out.println(product.getStock() + "-" + request.getStock());
        if (product.getStock() < request.getStock()) {
            System.out.println(product.getStock() + " out of stock");
            throw new Exception(product.getName() + " out of stock");
        }
        return product;
    }
}
