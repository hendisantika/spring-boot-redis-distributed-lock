package id.my.hendisantika.redisdistributedlock.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * Project : spring-boot-redis-distributed-lock
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Date: 16/12/24
 * Time: 11.13
 * To change this template use File | Settings | File Templates.
 */
@Data
public class MakeOrderRequest implements Serializable {
    private Long id;
    private int stock;
    private String updatedBy;
    private int delay;
}
