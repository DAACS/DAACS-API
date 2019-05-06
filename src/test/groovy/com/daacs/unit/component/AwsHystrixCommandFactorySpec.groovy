package com.daacs.unit.component

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.daacs.component.AwsHystrixCommandFactory
import com.daacs.framework.hystrix.GavantHystrixCommand
import com.daacs.service.hystrix.s3.S3PutFileCommand
import spock.lang.Specification

/**
 * Created by mgoldman on 11/06/18
 */
class AwsHystrixCommandFactorySpec extends Specification {

    AwsHystrixCommandFactory hystrixCommandFactory

    def setup() {
        hystrixCommandFactory = new AwsHystrixCommandFactory(
                s3: Mock(AmazonS3),
                bucketName: "bucket"
        )
    }

    def "getS3PutFileCommand returns S3PutFileCommand"() {
        when:
        GavantHystrixCommand gavantHystrixCommand = hystrixCommandFactory.getS3PutFileCommand("test", Mock(File), "file_location", Mock(ObjectMetadata));

        then:
        gavantHystrixCommand instanceof S3PutFileCommand
    }

}