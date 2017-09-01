package com.daacs.framework.core;

import com.daacs.framework.serializer.db.CATAssessmentReadConverter;
import com.daacs.framework.serializer.db.MultipleChoiceAssessmentReadConverter;
import com.daacs.framework.serializer.db.WritingAssessmentReadConverter;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoDbFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;

/**
 * Database connection configuration.
 *
 */
@Configuration
@EnableTransactionManagement
public class DatabaseConfiguration {

    @Value("${mongodb.host}")
    private String host;

    @Value("${mongodb.port}")
    private int port;

    @Value("${mongodb.database}")
    private String database;

    @Value("${mongodb.user}")
    private String user;

    @Value("${mongodb.password}")
    private String password;

    @Bean
    public MongoDbFactory mongoDbFactory() throws Exception {
        List<MongoCredential> mongoCredentials = new ArrayList<>();
        mongoCredentials.add(MongoCredential.createCredential(user, database, password.toCharArray()));

        MongoClient mongoClient = new MongoClient(new ServerAddress(host, port), mongoCredentials);

        return new SimpleMongoDbFactory(mongoClient, database);
    }

    @Bean
    @Autowired
    public MongoTemplate mongoTemplate(MongoDbFactory simpleMongoDbFactory, MongoConverter mongoConverter) throws Exception {
        return new MongoTemplate(simpleMongoDbFactory, mongoConverter);
    }

    @Bean
    @Autowired
    public MongoConverter mongoConverter(MongoDbFactory simpleMongoDbFactory, @Qualifier("defaultMongoConverter") MappingMongoConverter defaultMongoConverter) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(simpleMongoDbFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());

        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(new CATAssessmentReadConverter(defaultMongoConverter));
        converters.add(new MultipleChoiceAssessmentReadConverter(defaultMongoConverter));
        converters.add(new WritingAssessmentReadConverter(defaultMongoConverter));

        converter.setCustomConversions(new CustomConversions(converters));
        converter.afterPropertiesSet();

        return converter;
    }

    @Bean(name = "defaultMongoConverter")
    @Autowired
    public MappingMongoConverter defaultMongoConverter(MongoDbFactory mongoDbFactory){
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, new MongoMappingContext());
        converter.afterPropertiesSet();
        return converter;
    }

    @Bean
    public MongoDbFactory simpleMongoDbFactory() throws Exception {
        List<MongoCredential> mongoCredentials = new ArrayList<>();
        mongoCredentials.add(MongoCredential.createCredential(user, database, password.toCharArray()));

        MongoClient mongoClient = new MongoClient(new ServerAddress(host, port), mongoCredentials);

        return new SimpleMongoDbFactory(mongoClient, database);
    }

}
