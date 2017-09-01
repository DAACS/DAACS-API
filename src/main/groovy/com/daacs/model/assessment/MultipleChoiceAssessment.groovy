package com.daacs.model.assessment

import com.daacs.model.item.ItemGroup
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 7/6/16.
 */

@JsonIgnoreProperties(["metaClass"])
class MultipleChoiceAssessment extends ItemGroupAssessment<ItemGroup> {

}
