package id.my.hendisantika.redisdistributedlock.controller;

import id.my.hendisantika.redisdistributedlock.dto.MakeOrderRequest;
import id.my.hendisantika.redisdistributedlock.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-distributed-lock
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 16/12/24
 * Time: 11.19
 * To change this template use File | Settings | File Templates.
 */
@RestController
@RequestMapping("api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping("/make-order/v1")
    public ResponseEntity makeOrder(@RequestBody MakeOrderRequest request) throws Exception {
        final var response = productService.makeOrder(request);
        return ResponseEntity.ok(response);
    }
}
