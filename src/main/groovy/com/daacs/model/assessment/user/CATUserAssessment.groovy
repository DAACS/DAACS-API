package com.daacs.model.assessment.user

import com.daacs.model.item.CATItemGroup
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
/**
 * Created by chostetter on 7/5/16.
 */

@JsonIgnoreProperties(["metaClass"])
public class CATUserAssessment extends ItemGroupUserAssessment<CATItemGroup>{

}
