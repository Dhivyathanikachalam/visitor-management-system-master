package com.java.vms.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
//import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> objectRedisTemplate
            (RedisConnectionFactory redisConnectionFactory)
    {
        RedisTemplate<String, Object> objectTemplate = new RedisTemplate<>();

        ObjectMapper objectMapper = new ObjectMapper();
        //objectMapper.registerModule(new JavaTimeModule());
        objectMapper.findAndRegisterModules(); // Will automatically find and register modules which ever is needed.
        //objectMapper.registerModule(new Hibernate6Module());
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.findAndRegisterModules();
        // To ensure that the serialized objects are retained properly while deserializing.
        objectMapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                ObjectMapper.DefaultTyping.NON_FINAL
        );
        //serializer.setObjectMapper(objectMapper); //Deprecated
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, Object.class);
        objectTemplate.setConnectionFactory(redisConnectionFactory);
        objectTemplate.setKeySerializer(new StringRedisSerializer());
        objectTemplate.setValueSerializer(serializer);
        // Ensure Redis Template is properly initialized
        objectTemplate.afterPropertiesSet();
        return objectTemplate;
    }
}
