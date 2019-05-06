package com.daacs.unit.service

import com.daacs.component.utils.CategoryGroupUtils
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.model.assessment.*
import com.daacs.repository.AssessmentCategoryGroupRepository
import com.daacs.service.hystrix.AssessmentCategoryGroupServiceImpl
import com.lambdista.util.Try
import spock.lang.Specification

import java.util.stream.Collectors


/**
 * Created by mgoldman on 2/28/19.
 */
class AssessmentCategoryGroupServiceSpec extends Specification {

    Exception failureException
    AssessmentCategoryGroupServiceImpl assessmentCategoryGroupService;
    AssessmentCategoryGroupRepository groupRepository;
    CategoryGroupUtils categoryGroupUtils
    AssessmentCategoryGroup dummyGroup
    AssessmentCategoryGroup dummyGroup2

    DaacsOrikaMapper daacsOrikaMapper;


    def setup() {
        failureException = new Exception()
        daacsOrikaMapper = new DaacsOrikaMapper();
        groupRepository = Mock(AssessmentCategoryGroupRepository);
        categoryGroupUtils = Mock(CategoryGroupUtils)
        assessmentCategoryGroupService = new AssessmentCategoryGroupServiceImpl(
                assessmentCategoryGroupRepository: groupRepository,
                daacsOrikaMapper: daacsOrikaMapper,
                categoryGroupUtils: categoryGroupUtils)

        dummyGroup = new AssessmentCategoryGroup(
                id: "id",
                label: "TDE"
        )

        dummyGroup2 = new AssessmentCategoryGroup(
                id: "id2",
                label: "DefJam"
        )

    }

    def "assessmentCategoryGroupService: success"() {
        when:
        Try<List<AssessmentCategoryGroup>> maybeGroups = assessmentCategoryGroupService.getCategoryGroups()

        then:
        1 * groupRepository.getCategoryGroups() >> new Try.Success<List<AssessmentCategoryGroup>>([dummyGroup])
        then:
        maybeGroups.isSuccess()
    }

    def "assessmentCategoryGroupService: failure"() {
        when:
        Try<List<AssessmentCategoryGroup>> maybeGroups = assessmentCategoryGroupService.getCategoryGroups()

        then:
        1 * groupRepository.getCategoryGroups() >> new Try.Failure<List<AssessmentCategoryGroup>>(failureException)
        then:
        maybeGroups.isFailure()
        maybeGroups.failed().get() == failureException
    }

    def "createCategoryGroup: success"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroup(dummyGroup)

        then:
        1 * categoryGroupUtils.validateIdFormat(_) >> true
        1 * groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Success<AssessmentCategoryGroup>(null)
        1 * groupRepository.createCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        then:
        maybeGroup.isSuccess()
    }

    def "createCategoryGroup: failure on create"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroup(dummyGroup)

        then:
        1 * categoryGroupUtils.validateIdFormat(_) >> true
        1 * groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Success<AssessmentCategoryGroup>(null)
        1 * groupRepository.createCategoryGroup(dummyGroup) >> new Try.Failure<Void>(failureException)
        0 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        then:
        maybeGroup.isFailure()
        maybeGroup.failed().get() == failureException
    }

    def "createCategoryGroup: failure on getById"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroup(dummyGroup)

        then:
        1 * categoryGroupUtils.validateIdFormat(_) >> true
        1 * groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Success<AssessmentCategoryGroup>(null)
        1 * groupRepository.createCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Failure<AssessmentCategoryGroup>(failureException)
        then:
        maybeGroup.isFailure()
        maybeGroup.failed().get() == failureException
    }

    def "createCategoryGroup: failure validateIdFormat"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroup(dummyGroup)

        then:
        1 * categoryGroupUtils.validateIdFormat(_) >> false
        0 * groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Success<AssessmentCategoryGroup>(null)
        0 * groupRepository.createCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        0 * groupRepository.getCategoryGroupById("id") >>  new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        then:
        maybeGroup.isFailure()
    }

    def "createCategoryGroup: failure id already in use"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroup(dummyGroup)

        then:
        1 * categoryGroupUtils.validateIdFormat(_) >> true
        1 * groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        0 * groupRepository.createCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        0 * groupRepository.getCategoryGroupById("id") >>  new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        then:
        maybeGroup.isFailure()
    }

    def "createCategoryGroup: failure getCategoryGroupByIdOrNull fails"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroup(dummyGroup)

        then:
        1 * categoryGroupUtils.validateIdFormat(_) >> true
        1 * groupRepository.getCategoryGroupByIdOrNull(_) >> new Try.Failure<AssessmentCategoryGroup>(failureException)
        0 * groupRepository.createCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        0 * groupRepository.getCategoryGroupById("id") >>  new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        then:
        maybeGroup.isFailure()
        maybeGroup.failed().get() == failureException
    }

    def "updateCategoryGroup: updateCategoryGroup success"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.updateCategoryGroup("id", dummyGroup2)

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * groupRepository.updateCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        1 * groupRepository.getCategoryGroupById(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup2)
        then:
        maybeGroup.isSuccess()
        maybeGroup.get() == dummyGroup2
    }

    def "updateCategoryGroup: failure on first get by id"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.updateCategoryGroup("id", dummyGroup2)

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Failure<AssessmentCategoryGroup>(failureException)
        0 * groupRepository.updateCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        0 * groupRepository.getCategoryGroupById(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup2)
        then:
        maybeGroup.isFailure()
    }

    def "updateCategoryGroup: failure on update"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.updateCategoryGroup("id", dummyGroup2)

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * groupRepository.updateCategoryGroup(dummyGroup) >> new Try.Failure<Void>(failureException)
        0 * groupRepository.getCategoryGroupById(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup2)
        then:
        maybeGroup.isFailure()
    }

    def "updateCategoryGroup: failure on second get by id"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.updateCategoryGroup("id", dummyGroup2)

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * groupRepository.updateCategoryGroup(dummyGroup) >> new Try.Success<Void>(null)
        1 * groupRepository.getCategoryGroupById(_) >> new Try.Failure<AssessmentCategoryGroup>(failureException)
        then:
        maybeGroup.isFailure()
    }

    def "deleteCategoryGroup: success"() {
        when:
        Try<Void> maybeDeleted = assessmentCategoryGroupService.deleteCategoryGroup("id")

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * groupRepository.deleteCategoryGroup("id") >> new Try.Success<Void>(null)
        then:
        maybeDeleted.isSuccess()
    }

    def "deleteCategoryGroup: fails on getById"() {
        when:
        Try<Void> maybeDeleted = assessmentCategoryGroupService.deleteCategoryGroup("id")

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Failure<AssessmentCategoryGroup>(failureException)
        0 * groupRepository.deleteCategoryGroup("id") >> new Try.Success<Void>(null)
        then:
        maybeDeleted.isFailure()
    }

    def "deleteCategoryGroup: fails on delete"() {
        when:
        Try<Void> maybeDeleted = assessmentCategoryGroupService.deleteCategoryGroup("id")

        then:
        1 * groupRepository.getCategoryGroupById("id") >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * groupRepository.deleteCategoryGroup("id") >> new Try.Failure<Void>(failureException)
        then:
        maybeDeleted.isFailure()
    }

    def "getGlobalGroupIds: success"() {
        when:
        Try<List<String>> maybeIds = assessmentCategoryGroupService.getGlobalGroupIds()

        then:
        1 * groupRepository.getGlobalGroups() >> new Try.Success<List<AssessmentCategoryGroup>>([dummyGroup])
        then:
        maybeIds.isSuccess()
        maybeIds.get() == ["id"]
    }

    def "getGlobalGroupIds: fails "() {
        when:
        Try<List<String>> maybeIds = assessmentCategoryGroupService.getGlobalGroupIds()

        then:
        1 * groupRepository.getGlobalGroups() >> new Try.Failure<List<AssessmentCategoryGroup>>(failureException)
        then:
        maybeIds.isFailure()
    }
}
