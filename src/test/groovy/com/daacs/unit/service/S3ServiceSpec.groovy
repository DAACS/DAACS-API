package com.daacs.unit.service

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectResult
import com.lambdista.util.Try
import com.daacs.component.AwsHystrixCommandFactory
import com.daacs.service.S3Service
import com.daacs.service.S3ServiceImpl
import com.daacs.service.hystrix.s3.S3PutFileCommand
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

/**
 * Created by mgoldman on 11/06/18
 */
class S3ServiceSpec extends Specification {

    Exception failureException = new Exception()
    AwsHystrixCommandFactory awsHystrixCommandFactory
    S3PutFileCommand s3PutFileCommand
    AmazonS3 amazonS3
    String folder
    S3Service s3Service
    URL s3URL

    MultipartFile multipartFile

    def setup() {
        awsHystrixCommandFactory = Mock(AwsHystrixCommandFactory)
        s3PutFileCommand = Mock(S3PutFileCommand)
        amazonS3 = new AmazonS3Client((AWSCredentials) null)
        folder = "afolder"
        s3URL = new URL("http://url.com")
        multipartFile = new MockMultipartFile("image.png", "image.png", "image/png")

        s3Service = new S3ServiceImpl(
                bucketName: "bucket",
                imagesFolder: "Images",
                awsHystrixCommandFactory: awsHystrixCommandFactory,
                s3: amazonS3)

    }

    def "storeFile: success"() {
        when:
        Try<URL> maybeResults = s3Service.storeFile(multipartFile, "new-image.png", folder)

        then:
        1 * awsHystrixCommandFactory.getS3PutFileCommand(_, _, "afolder/new-image.png", _) >> { args ->
            File file = args[1]
            ObjectMetadata objectMetadata = args[3]

            assert file.toString() == "new-image.png"
            assert file.exists()
            assert objectMetadata.getContentType() == "image/png"

            return s3PutFileCommand
        }
        1 * s3PutFileCommand.execute() >> new Try.Success<PutObjectResult>(new PutObjectResult())

        then:
        maybeResults.isSuccess()
        maybeResults.get().toString() == "https://bucket.s3.amazonaws.com/afolder/new-image.png"
    }

    def "storeFile: s3PutFileCommand fails, i fail"() {
        when:
        Try<URL> maybeResults = s3Service.storeFile(multipartFile, "new-image.png", folder)

        then:
        1 * awsHystrixCommandFactory.getS3PutFileCommand(_, _, "afolder/new-image.png", _) >> s3PutFileCommand
        1 * s3PutFileCommand.execute() >> new Try.Failure<PutObjectResult>(failureException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureException
    }

    def "getPublicUrl: success"() {
        when:
        Try<URL> maybeResults = s3Service.getPublicUrl("some/location.file")

        then:
        maybeResults.isSuccess()
        maybeResults.get().toString() == "https://bucket.s3.amazonaws.com/some/location.file"
    }

    def "getPublicUrl: success with urlencoded"() {
        when:
        Try<URL> maybeResults = s3Service.getPublicUrl("some/1-\$100 location.file")

        then:
        maybeResults.isSuccess()
        maybeResults.get().toString() == "https://bucket.s3.amazonaws.com/some/1-%24100%20location.file"
    }

    def "storeImage: success"() {
        when:
        Try<URL> maybeResults = s3Service.storeImage(multipartFile, "new-image.png")

        then:
        1 * awsHystrixCommandFactory.getS3PutFileCommand(_, _, "Images/new-image.png", _) >> { args ->
            File file = args[1]
            ObjectMetadata objectMetadata = args[3]

            assert file.toString() == "new-image.png"
            assert file.exists()
            assert objectMetadata.getContentType() == "image/png"

            return s3PutFileCommand
        }
        1 * s3PutFileCommand.execute() >> new Try.Success<PutObjectResult>(new PutObjectResult())

        then:
        maybeResults.isSuccess()
        maybeResults.get().toString() == "https://bucket.s3.amazonaws.com/Images/new-image.png"
    }

    def "storeImage: s3PutFileCommand fails, i fail"() {
        when:
        Try<URL> maybeResults = s3Service.storeImage(multipartFile, "new-image.png")

        then:
        1 * awsHystrixCommandFactory.getS3PutFileCommand(_, _, "Images/new-image.png", _) >> s3PutFileCommand
        1 * s3PutFileCommand.execute() >> new Try.Failure<PutObjectResult>(failureException)

        then:
        maybeResults.isFailure()
        maybeResults.failed().get() == failureException
    }
}