package com.daacs.model.assessment.user

import com.daacs.model.item.ItemGroup
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class MultipleChoiceUserAssessment extends ItemGroupUserAssessment<ItemGroup> {

}
