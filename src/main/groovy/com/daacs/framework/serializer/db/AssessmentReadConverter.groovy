package com.daacs.framework.serializer.db

import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.DomainType
import com.daacs.model.assessment.ScoringDomain
import com.mongodb.BasicDBObject
import com.mongodb.DBObject
import org.springframework.core.convert.converter.Converter
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
/**
 * Created by chostetter on 7/20/16.
 */
public abstract class AssessmentReadConverter<T extends Assessment> implements Converter<DBObject, T> {

    protected MappingMongoConverter defaultMongoConverter;

    AssessmentReadConverter(MappingMongoConverter defaultMongoConverter) {
        this.defaultMongoConverter = defaultMongoConverter
    }

    protected DBObject convertCommon(DBObject source){
        
        if(source.containsField("content") && source.get("content") instanceof String){
            ((BasicDBObject) source).append("content", [ "landing": source.get("content")]);
        }

        for(DBObject domainObject : (DBObject) source.get("domains")){
            convertDomain(domainObject);
        }

        return source;
    }

    private void convertDomain(DBObject domainObject){
        BasicDBObject domain = (BasicDBObject) domainObject;

        if(!domain.containsField("domainType")){
            domain.append("domainType", DomainType.SCORING.toString());
            domain.append("_class", ScoringDomain.class.getName());
        }

        if(domain.containsField("subDomains")){
            for(DBObject subDomainObject : (DBObject) domain.get("subDomains")){
                convertDomain(subDomainObject);
            }
        }
    }

    public abstract T convert(DBObject source);

}
