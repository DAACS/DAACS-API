package com.daacs.framework.serializer.db

import com.daacs.model.assessment.MultipleChoiceAssessment
import com.mongodb.DBObject
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
/**
 * Created by chostetter on 7/20/16.
 */
public class MultipleChoiceAssessmentReadConverter extends AssessmentReadConverter<MultipleChoiceAssessment> {

    MultipleChoiceAssessmentReadConverter(MappingMongoConverter defaultMongoConverter) {
        super(defaultMongoConverter)
    }

    public MultipleChoiceAssessment convert(DBObject source) {
        source = convertCommon(source);

        MultipleChoiceAssessment assessment = defaultMongoConverter.read(MultipleChoiceAssessment, source)
        return assessment;
    }

}
