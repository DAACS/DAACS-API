package com.daacs.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.lambdista.util.Try;
import com.daacs.component.AwsHystrixCommandFactory;
import com.daacs.framework.exception.GenericFileException;
import com.daacs.framework.exception.InvalidObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

@Service
public class S3ServiceImpl implements S3Service {
    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    @Autowired
    private AmazonS3 s3;

    @Value("${aws.bucketName}")
    private String bucketName;

    @Value("${aws.ImagesFolder}")
    private String imagesFolder;

    @Autowired
    private AwsHystrixCommandFactory awsHystrixCommandFactory;

    @Override
    public Try<URL> storeImage(MultipartFile multipartFile, String fileName) {
        return storeFile(multipartFile, fileName, imagesFolder);
    }

    @Override
    public Try<URL> storeFile(MultipartFile multipartFile, String fileName, String folderLocation) {

        String fileLocation = folderLocation + "/" + fileName;
        Try<URL> maybePublicUrl = getPublicUrl(fileLocation);
        if (maybePublicUrl.isFailure()) {
            return maybePublicUrl;
        }

        //turn MultipartFile into File
        File file = new File(fileName);
        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(multipartFile.getBytes());
            fos.close();
        } catch (IOException ex) {
            return new Try.Failure<>(new GenericFileException(fileName, ex));
        }

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(multipartFile.getContentType());

        Try<PutObjectResult> maybeResult = awsHystrixCommandFactory.getS3PutFileCommand("S3ServiceImpl-storeFile", file, fileLocation, objectMetadata).execute();
        if (maybeResult.isFailure()) {
            return new Try.Failure<>(maybeResult.failed().get());
        }
        log.info("Emitted {} to DAACS S3 bucket. ETag = {}.", folderLocation, maybeResult.get().getETag());

        file.delete();
        return maybePublicUrl;
    }

    @Override
    public Try<URL> getPublicUrl(String fileLocation) {
        try {
            return new Try.Success<>(s3.getUrl(bucketName, fileLocation));
        } catch (IllegalArgumentException ex) {
            return new Try.Failure<>(new InvalidObjectException("url", "unable to determine public url", ex));
        }
    }
}