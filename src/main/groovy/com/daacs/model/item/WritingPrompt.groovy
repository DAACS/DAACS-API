package com.daacs.model.item

import com.daacs.framework.serializer.Views
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonView
import io.swagger.annotations.ApiModelProperty
import org.springframework.format.annotation.DateTimeFormat

import javax.validation.constraints.NotNull
import java.time.Instant

/**
 * Created by chostetter on 7/25/16.
 */
@JsonIgnoreProperties(["metaClass"])
public class WritingPrompt {

    @JsonView([Views.NotExport])
    String id = UUID.randomUUID().toString();

    @NotNull
    String content = "";

    @NotNull
    int minWords;

    @JsonView([Views.NotExport])
    String sample = "";

    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonView([Views.NotExport])
    Instant startDate;

    @ApiModelProperty(dataType = "java.lang.String")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonView([Views.NotExport])
    Instant completeDate;

}