package com.daacs.service.hystrix.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.lambdista.util.Try;

import java.io.File;

/**
 * Created by chostetter on 12/22/16.
 */
public class S3PutFileCommand extends S3HystrixCommand<PutObjectResult> {

    private PutObjectRequest request;

    public S3PutFileCommand(String hystrixGroupKey, String hystrixCommandKey, AmazonS3 s3, String bucketName, File file, String fileLocation, ObjectMetadata metadata) {
        super(hystrixGroupKey, hystrixCommandKey, s3);
        request = buildPutRequest(bucketName, file, fileLocation, metadata);
    }

    @Override
    protected Try<PutObjectResult> run() throws Exception {
        try {
            PutObjectResult result = s3.putObject(request);
            return createSuccess(result);
        } catch (Throwable t) {
            return failedExecutionFallback(t);
        }
    }

    protected PutObjectRequest buildPutRequest(String bucketName, File file, String fileLocation, ObjectMetadata metadata) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileLocation, file);
        putObjectRequest.setMetadata(metadata);
        putObjectRequest.setGeneralProgressListener(progressEvent ->
                log.debug("[{}] Uploaded {} bytes for {}", hystrixCommandKey,
                        progressEvent.getBytesTransferred(), fileLocation));

        return putObjectRequest;
    }
}