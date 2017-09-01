package com.daacs.framework.serializer;

import com.daacs.model.User;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.*;
import com.daacs.model.dto.assessmentUpdate.AnalysisDomainRequest;
import com.daacs.model.dto.assessmentUpdate.ScoringDomainRequest;
import com.daacs.model.prereqs.Prerequisite;
import com.google.common.collect.Range;
import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.metadata.Type;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Created by chostetter on 7/8/16.
 */

@Component
public class DaacsOrikaMapper extends ConfigurableGroovyOrikaMapper {

    @Override
    protected void configure(MapperFactory mapperFactory) {

        // register class maps, Mappers, ObjectFactories, and Converters
        mapperFactory.registerClassMap(mapperFactory.classMap(Assessment.class, AssessmentSummary.class)
                .field("id", "assessmentId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(Assessment.class, UserAssessment.class)
                .field("id", "assessmentId")
                .field("label", "assessmentLabel")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(Assessment.class, CATUserAssessment.class)
                .field("id", "assessmentId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(Assessment.class, MultipleChoiceUserAssessment.class)
                .field("id", "assessmentId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(Assessment.class, WritingPromptUserAssessment.class)
                .field("id", "assessmentId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(Assessment.class, AssessmentContent.class)
                .field("id", "assessmentId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(UserAssessment.class, UserAssessmentSummary.class)
                .field("id", "userAssessmentId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(User.class, UserAssessment.class)
                .field("id", "userId")
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(User.class, User.class)
                .mapNulls(false)
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(ScoringDomain.class, ScoringDomain.class)
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(AnalysisDomain.class, AnalysisDomain.class)
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(ScoringDomainRequest.class, ScoringDomain.class)
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(AnalysisDomainRequest.class, AnalysisDomain.class)
                .byDefault()
                .toClassMap());

        mapperFactory.registerMapper(new ListObjectMapper(mapperFactory));

        mapperFactory.getConverterFactory().registerConverter(new StringToInstantConverter());
        mapperFactory.getConverterFactory().registerConverter(new InstantToInstantConverter());
        mapperFactory.getConverterFactory().registerConverter(new RangeDoubleToRangeDoubleConverter());
        mapperFactory.getConverterFactory().registerConverter(new RangeIntegerToRangeIntegerConverter());
        mapperFactory.getConverterFactory().registerConverter(new PassThroughConverter(Prerequisite.class));
    }

    private class StringToInstantConverter extends CustomConverter<String, Instant> {
        @Override
        public Instant convert(String source, Type<? extends Instant> type) {
            return Instant.parse(source);
        }
    }

    private class InstantToInstantConverter extends CustomConverter<Instant, Instant> {
        @Override
        public Instant convert(Instant source, Type<? extends Instant> type) {
            return source;
        }
    }

    private class RangeDoubleToRangeDoubleConverter extends CustomConverter<Range<Double>, Range<Double>> {
        @Override
        public Range<Double> convert(Range<Double> source, Type<? extends Range<Double>> destinationType) {
            return source;
        }
    }

    private class RangeIntegerToRangeIntegerConverter extends CustomConverter<Range<Integer>, Range<Integer>> {
        @Override
        public Range<Integer> convert(Range<Integer> source, Type<? extends Range<Integer>> destinationType) {
            return source;
        }
    }
}


