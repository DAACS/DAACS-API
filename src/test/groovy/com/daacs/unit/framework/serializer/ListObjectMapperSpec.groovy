package com.daacs.unit.framework.serializer

import com.daacs.framework.serializer.GroovyMapperFactory
import com.daacs.framework.serializer.ListObjectMapper
import com.daacs.model.assessment.AnalysisDomain
import com.daacs.model.assessment.Domain
import com.daacs.model.assessment.ScoringDomain
import com.daacs.model.dto.assessmentUpdate.*
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import ma.glasnost.orika.MappingContext
import ma.glasnost.orika.impl.DefaultMapperFactory
import ma.glasnost.orika.metadata.Type
import spock.lang.Specification

/**
 * Created by alandistasio on 10/25/16.
 */
class ListObjectMapperSpec extends Specification {

    ListObjectMapper listObjectMapper
    MappingContext context
    Type<DomainRequest> domainRequestType
    Type<Domain> domainType

    def setup() {
        GroovyMapperFactory mapperFactory = new GroovyMapperFactory(new DefaultMapperFactory.Builder());

        mapperFactory.registerClassMap(mapperFactory.classMap(ScoringDomainRequest.class, ScoringDomain.class)
                .byDefault()
                .toClassMap());

        mapperFactory.registerClassMap(mapperFactory.classMap(AnalysisDomainRequest.class, AnalysisDomain.class)
                .byDefault()
                .toClassMap());

        listObjectMapper = new ListObjectMapper(mapperFactory)
        context = Mock(MappingContext)

        domainRequestType = new Type<DomainRequest>(null, DomainRequest.class, null, new Type<DomainRequest>(null, DomainRequest.class, null))
        domainType = new Type<Domain>(null, Domain.class, null, new Type<Domain>(null, Domain.class, null))
    }

    def "map item correctly"() {
        setup:
        List<DomainRequest> domainRequests = [new ScoringDomainRequest(content: "bar", id: "1")]
        List<Domain> domains = [new ScoringDomain(content: "foo", id: "1")]

        when:
        1 * context.getResolvedSourceType() >> domainRequestType
        1 * context.getResolvedDestinationType() >> domainType

        listObjectMapper.mapAtoB(domainRequests, domains, context)

        then:
        domains.get(0).content == "bar"
    }

    def "map multiple items correctly"() {
        setup:
        List<DomainRequest> domainRequests = [new ScoringDomainRequest(content: "bar", id: "1"), new ScoringDomainRequest(content: "banana", id: "2")]
        List<Domain> domains = [new ScoringDomain(content: "foo", id: "2"), new ScoringDomain(content: "foo", id: "1")]

        when:
        1 * context.getResolvedSourceType() >> domainRequestType
        1 * context.getResolvedDestinationType() >> domainType

        listObjectMapper.mapAtoB(domainRequests, domains, context)

        then:
        domains.get(0).content == "banana"
        domains.get(0).id == "2"
        domains.get(1).content == "bar"
        domains.get(1).id == "1"
    }

    def "map multiple creates new entry when not found in dest list"() {
        setup:
        List<DomainRequest> domainRequests = [new ScoringDomainRequest(content: "banana", id: "1"), new ScoringDomainRequest(content: "bar", id: "2")]
        List<Domain> domains = [new ScoringDomain(content: "foo", id: "2")]

        when:
        1 * context.getResolvedSourceType() >> domainRequestType
        1 * context.getResolvedDestinationType() >> domainType

        listObjectMapper.mapAtoB(domainRequests, domains, context)

        then:
        domains.size() == 2
        domains.get(0).content == "bar"
        domains.get(0).id == "2"
        domains.get(1).content == "banana"
        domains.get(1).id == "1"
    }

    def "map multiple creates new entries when not found in dest list"() {
        setup:
        List<DomainRequest> domainRequests = [new ScoringDomainRequest(content: "banana"), new ScoringDomainRequest(content: "apple"), new ScoringDomainRequest(content: "bar", id: "2")]
        List<Domain> domains = [new ScoringDomain(content: "foo", id: "2")]

        when:
        1 * context.getResolvedSourceType() >> domainRequestType
        1 * context.getResolvedDestinationType() >> domainType

        listObjectMapper.mapAtoB(domainRequests, domains, context)

        then:
        domains.size() == 3
        domains.get(0).content == "bar"
        domains.get(0).id == "2"
        domains.get(1).content == "banana"
        domains.get(2).content == "apple"
    }

    def "nested lists map correctly"() {
        setup:
        List<ItemRequest> itemRequests = [new ItemRequest(
                id: "1",
                question: "first",
                possibleItemAnswers: [new ItemAnswerRequest(id: "1a", content: "foo"),
                                      new ItemAnswerRequest(id: "1b", content: "bar")])]
        List<Item> items = [new Item(
                id: "1",
                question: "old",
                possibleItemAnswers: [new ItemAnswer(id: "1a", content: "nope"),
                                      new ItemAnswer(id: "1b", content: "nope")])]

        when:
        //this doesn't matter because we're not creating new objects in this test
        1 * context.getResolvedSourceType() >> {
            return domainRequestType
        }
        1 * context.getResolvedDestinationType() >> {
            return domainType
        }

        listObjectMapper.mapAtoB(itemRequests, items, context)

        then:
        items.size() == 1
        items.get(0).question == "first"
        items.get(0).id == "1"

        items.get(0).possibleItemAnswers.size() == 2
        items.get(0).possibleItemAnswers.get(0).id == "1a"
        items.get(0).possibleItemAnswers.get(0).content == "foo"
        items.get(0).possibleItemAnswers.get(1).id == "1b"
        items.get(0).possibleItemAnswers.get(1).content == "bar"
    }
}
