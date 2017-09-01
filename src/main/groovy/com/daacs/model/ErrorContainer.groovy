package com.daacs.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Created by chostetter on 8/1/16.
 */
@JsonIgnoreProperties(["metaClass"])
public class ErrorContainer {
    String code;
    String detail;
    Map<String, Object> meta;
}
