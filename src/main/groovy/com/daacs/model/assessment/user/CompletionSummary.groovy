package com.daacs.model.assessment.user

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.swagger.annotations.ApiModel
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
@ApiModel
public class CompletionSummary {

    String userId;
    Boolean hasCompletedAllCategories;
}
