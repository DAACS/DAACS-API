package com.daacs.service;

import com.lambdista.util.Try;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

public interface S3Service {
    Try<URL> storeFile(MultipartFile multipartFile, String fileName, String folderLocation);

    Try<URL> getPublicUrl(String fileLocation);

    Try<URL> storeImage(MultipartFile multipartFile, String fileName);
}