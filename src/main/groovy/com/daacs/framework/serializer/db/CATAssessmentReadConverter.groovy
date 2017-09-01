package com.daacs.framework.serializer.db

import com.daacs.model.assessment.CATAssessment
import com.mongodb.DBObject
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
/**
 * Created by chostetter on 7/20/16.
 */
public class CATAssessmentReadConverter extends AssessmentReadConverter<CATAssessment> {

    CATAssessmentReadConverter(MappingMongoConverter defaultMongoConverter) {
        super(defaultMongoConverter)
    }

    public CATAssessment convert(DBObject source) {
        source = convertCommon(source);

        CATAssessment assessment = defaultMongoConverter.read(CATAssessment, source)
        return assessment;
    }

}
