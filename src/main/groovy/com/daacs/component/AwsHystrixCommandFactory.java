package com.daacs.component;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.daacs.service.hystrix.s3.S3PutFileCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class AwsHystrixCommandFactory {

    @Autowired
    private AmazonS3 s3;

    @Value("${aws.bucketName}")
    private String bucketName;

    private static final String hystrixGroupKey = "daacs";

    public S3PutFileCommand getS3PutFileCommand(String hystrixCommandKey, File file, String fileLocation, ObjectMetadata metadata){
        return new S3PutFileCommand(hystrixGroupKey, hystrixCommandKey, s3, bucketName, file, fileLocation, metadata);
    }

}