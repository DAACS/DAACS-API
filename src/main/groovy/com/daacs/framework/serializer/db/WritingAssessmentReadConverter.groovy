package com.daacs.framework.serializer.db

import com.daacs.model.assessment.WritingAssessment
import com.mongodb.DBObject
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
/**
 * Created by chostetter on 7/20/16.
 */
public class WritingAssessmentReadConverter extends AssessmentReadConverter<WritingAssessment> {

    WritingAssessmentReadConverter(MappingMongoConverter defaultMongoConverter) {
        super(defaultMongoConverter)
    }

    public WritingAssessment convert(DBObject source) {
        source = convertCommon(source);

        WritingAssessment assessment = defaultMongoConverter.read(WritingAssessment, source)
        return assessment;
    }

}
