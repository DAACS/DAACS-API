package com.daacs.framework.serializer;

import com.daacs.framework.serializer.json.InstantDeserializer;
import com.daacs.framework.serializer.json.InstantSerializer;
import com.daacs.framework.serializer.json.RangeDeserializer;
import com.daacs.framework.serializer.json.RangeSerializer;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.collect.Range;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.schema.configuration.ObjectMapperConfigured;

import java.time.Instant;

/**
 * Created by chostetter on 6/28/16.
 */
@Configuration
public class ObjectMapperConfig implements ApplicationListener<ObjectMapperConfigured> {

    @SuppressWarnings("deprecation")
    @Bean
    public ObjectMapper objectMapper(){
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.registerModule(new Jdk8Module());
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
        objectMapper.configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);

        return registerModules(objectMapper);
    }

    @Override
    public void onApplicationEvent(ObjectMapperConfigured event) {
        event.getObjectMapper().configure(MapperFeature.DEFAULT_VIEW_INCLUSION, true);
        registerModules(event.getObjectMapper());
    }

    private ObjectMapper registerModules(ObjectMapper objectMapper){
        SimpleModule serializerModule = new SimpleModule();
        serializerModule.setSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
                if (beanDesc.getBeanClass() == Range.class) {
                    return new RangeSerializer();
                }

                if (beanDesc.getBeanClass() == Instant.class) {
                    return new InstantSerializer();
                }

                return serializer;
            }
        });

        objectMapper.registerModule(serializerModule);

        SimpleModule deserializerModule = new SimpleModule();
        deserializerModule.setDeserializerModifier(new BeanDeserializerModifier() {

            @Override
            public JsonDeserializer<?> modifyDeserializer(DeserializationConfig config, BeanDescription beanDesc, JsonDeserializer<?> defaultDeserializer) {
                if (beanDesc.getBeanClass() == Range.class) {
                    return new RangeDeserializer();
                }

                if (beanDesc.getBeanClass() == Instant.class) {
                    return new InstantDeserializer();
                }

                return defaultDeserializer;
            }

        });

        objectMapper.registerModule(deserializerModule);

        return objectMapper;
    }
}
