package com.daacs.resource


import com.daacs.service.S3Service
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

import javax.servlet.annotation.MultipartConfig

/**
 * Created by mgoldman on 11/02/18.
 */
@MultipartConfig
@RestController
@RequestMapping(value = "", produces = "application/json")
public class UploadController extends AuthenticatedController {

    @Autowired

    S3Service s3Service

    @ApiOperation(
            value = "upload an image file",
            response = URL
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = URL, message = "Unable to get offer"),
            @ApiResponse(code = 500, response = URL, message = "Unknown error"),
            @ApiResponse(code = 503, response = URL, message = "Retryable error")
    ])
    @RequestMapping(value = "/files", method = RequestMethod.POST, produces = "application/json")
    public URL uploadFile(@RequestParam(value = "image", required = true) MultipartFile image) {

        String fileName = UUID.randomUUID().toString()+"_"+image.originalFilename

        return s3Service.storeImage(image, fileName).checkedGet()
    }
}