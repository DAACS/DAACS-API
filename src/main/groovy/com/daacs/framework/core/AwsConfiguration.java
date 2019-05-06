package com.daacs.framework.core;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by chostetter on 12/23/16.
 */

@Configuration
public class AwsConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AwsConfiguration.class);

    @Value("${aws.accessKeyId}")
    private String accessKeyId;

    @Value("${aws.secretKeyId}")
    private String secretKeyId;

    @Value("${aws.region}")
    private String region;

    @Bean
    public AmazonS3 amazonS3(){
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(accessKeyId, secretKeyId);

        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
                .withRegion(region)
                .build();
    }
}