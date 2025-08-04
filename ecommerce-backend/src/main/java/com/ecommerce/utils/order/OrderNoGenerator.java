package com.ecommerce.utils.order;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * 订单编号生成器
 */
@Component
public class OrderNoGenerator {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Random RANDOM = new Random();
    
    /**
     * 生成订单编号
     * 格式：yyyyMMddHHmmss + 6位随机数
     */
    public String generate() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String randomSuffix = String.format("%06d", RANDOM.nextInt(1000000));
        return timestamp + randomSuffix;
    }
}