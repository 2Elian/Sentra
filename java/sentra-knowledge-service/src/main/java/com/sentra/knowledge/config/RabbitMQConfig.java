package com.sentra.knowledge.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 */
@Configuration
public class RabbitMQConfig {

    public static final String OCR_QUEUE = "sentra.ocr.queue";
    public static final String KB_BUILD_QUEUE = "sentra.kb_build.queue";
    public static final String OCR_DEAD_LETTER_QUEUE = "sentra.dlq.ocr";
    public static final String KB_BUILD_DEAD_LETTER_QUEUE = "sentra.dlq.kb_build";

    public static final String OCR_EXCHANGE = "sentra.ocr.exchange";
    public static final String KB_BUILD_EXCHANGE = "sentra.kb_build.exchange";
    public static final String OCR_DEAD_LETTER_EXCHANGE = "sentra.dlx.ocr";
    public static final String KB_BUILD_DEAD_LETTER_EXCHANGE = "sentra.dlx.kb_build";

    public static final String OCR_ROUTING_KEY = "ocr";
    public static final String KB_BUILD_ROUTING_KEY = "kb_build";

    /**
     * OCR任务队列
     */
    @Bean
    public Queue ocrQueue() {
        return QueueBuilder.durable(OCR_QUEUE)
                .withArgument("x-dead-letter-exchange", OCR_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", OCR_ROUTING_KEY)
                .build();
    }

    /**
     * 知识库构建任务队列
     */
    @Bean
    public Queue kbBuildQueue() {
        return QueueBuilder.durable(KB_BUILD_QUEUE)
                .withArgument("x-dead-letter-exchange", KB_BUILD_DEAD_LETTER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", KB_BUILD_ROUTING_KEY)
                .build();
    }

    /**
     * OCR死信队列
     */
    @Bean
    public Queue ocrDeadLetterQueue() {
        return QueueBuilder.durable(OCR_DEAD_LETTER_QUEUE).build();
    }

    /**
     * 知识库构建死信队列
     */
    @Bean
    public Queue kbBuildDeadLetterQueue() {
        return QueueBuilder.durable(KB_BUILD_DEAD_LETTER_QUEUE).build();
    }

    /**
     * OCR交换机
     */
    @Bean
    public DirectExchange ocrExchange() {
        return new DirectExchange(OCR_EXCHANGE, true, false);
    }

    /**
     * 知识库构建交换机
     */
    @Bean
    public DirectExchange kbBuildExchange() {
        return new DirectExchange(KB_BUILD_EXCHANGE, true, false);
    }

    /**
     * OCR死信交换机
     */
    @Bean
    public DirectExchange ocrDeadLetterExchange() {
        return new DirectExchange(OCR_DEAD_LETTER_EXCHANGE, true, false);
    }

    /**
     * 知识库构建死信交换机
     */
    @Bean
    public DirectExchange kbBuildDeadLetterExchange() {
        return new DirectExchange(KB_BUILD_DEAD_LETTER_EXCHANGE, true, false);
    }

    /**
     * OCR队列绑定
     */
    @Bean
    public Binding ocrBinding() {
        return BindingBuilder.bind(ocrQueue())
                .to(ocrExchange())
                .with(OCR_ROUTING_KEY);
    }

    /**
     * 知识库构建队列绑定
     */
    @Bean
    public Binding kbBuildBinding() {
        return BindingBuilder.bind(kbBuildQueue())
                .to(kbBuildExchange())
                .with(KB_BUILD_ROUTING_KEY);
    }

    /**
     * OCR死信队列绑定
     */
    @Bean
    public Binding ocrDeadLetterBinding() {
        return BindingBuilder.bind(ocrDeadLetterQueue())
                .to(ocrDeadLetterExchange())
                .with(OCR_ROUTING_KEY);
    }

    /**
     * 知识库构建死信队列绑定
     */
    @Bean
    public Binding kbBuildDeadLetterBinding() {
        return BindingBuilder.bind(kbBuildDeadLetterQueue())
                .to(kbBuildDeadLetterExchange())
                .with(KB_BUILD_ROUTING_KEY);
    }

    /**
     * JSON消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
