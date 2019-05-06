package com.daacs.unit.repository

import com.daacs.component.HystrixCommandFactory
import com.daacs.component.utils.CategoryGroupUtils
import com.daacs.component.utils.CategoryGroupUtilsImpl
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.assessment.AssessmentCategoryGroup
import com.daacs.repository.AssessmentCategoryGroupRepository
import com.daacs.repository.AssessmentCategoryGroupRepositoryImpl
import com.daacs.repository.hystrix.*
import com.lambdista.util.Try
import spock.lang.Specification

/**
 * Created by mgoldman on 2/28/19.
 */
class AssessmentCategoryGroupRepositorySpec extends Specification {

    AssessmentCategoryGroupRepository groupRepository
    CategoryGroupUtils categoryGroupUtils

    HystrixCommandFactory hystrixCommandFactory
    MongoFindByIdCommand mongoFindByIdCommand
    MongoInsertCommand mongoInsertCommand
    MongoFindCommand mongoFindCommand
    MongoSaveCommand mongoSaveCommand
    MongoDeleteByIdCommand mongoDeleteByIdCommand

    def setup() {

        mongoFindByIdCommand = Mock(MongoFindByIdCommand)
        mongoInsertCommand = Mock(MongoInsertCommand)
        mongoSaveCommand = Mock(MongoSaveCommand)
        mongoFindCommand = Mock(MongoFindCommand)
        mongoDeleteByIdCommand = Mock(MongoDeleteByIdCommand)

        categoryGroupUtils = new CategoryGroupUtilsImpl()
        hystrixCommandFactory = Mock(HystrixCommandFactory)
        hystrixCommandFactory.getMongoFindByIdCommand(*_) >> mongoFindByIdCommand
        hystrixCommandFactory.getMongoInsertCommand(*_) >> mongoInsertCommand
        hystrixCommandFactory.getMongoFindCommand(*_) >> mongoFindCommand
        hystrixCommandFactory.getMongoSaveCommand(*_) >> mongoSaveCommand
        hystrixCommandFactory.getMongoDeleteByIdCommand(*_) >> mongoDeleteByIdCommand

        groupRepository = new AssessmentCategoryGroupRepositoryImpl(hystrixCommandFactory: hystrixCommandFactory, categoryGroupUtils: categoryGroupUtils)
    }

    def "getCategoryGroupById: success"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = groupRepository.getCategoryGroupById(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Success<AssessmentCategoryGroup>(Mock(AssessmentCategoryGroup))
        maybeGroup.isSuccess()
    }

    def "getCategoryGroupById: failed, return failure"() {
        when:
        Try<AssessmentCategoryGroup> maybeGroup = groupRepository.getCategoryGroupById(UUID.randomUUID().toString())

        then:
        1 * mongoFindByIdCommand.execute() >> new Try.Failure<AssessmentCategoryGroup>(new RepoNotFoundException("not found"))
        maybeGroup.isFailure()
        maybeGroup.failed().get() instanceof RepoNotFoundException
    }

    def "updateCategoryGroup: success"() {
        when:
        Try<Void> maybeGroup = groupRepository.updateCategoryGroup(Mock(AssessmentCategoryGroup))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Success<Void>(null)
        maybeGroup.isSuccess()
    }

    def "updateCategoryGroup: failed, return failure"() {
        when:
        Try<Void> maybeGroup = groupRepository.updateCategoryGroup(Mock(AssessmentCategoryGroup))

        then:
        1 * mongoSaveCommand.execute() >> new Try.Failure<Void>(new RepoNotFoundException("not found"))
        maybeGroup.isFailure()
        maybeGroup.failed().get() instanceof RepoNotFoundException
    }

    def "createCategoryGroup: success"() {
        when:
        Try<Void> maybeGroup = groupRepository.createCategoryGroup(Mock(AssessmentCategoryGroup))

        then:
        1 * mongoInsertCommand.execute() >> new Try.Success<Void>(null)
        maybeGroup.isSuccess()
    }

    def "createCategoryGroup: failed, return failure"() {
        when:
        Try<Void> maybeGroup = groupRepository.createCategoryGroup(Mock(AssessmentCategoryGroup))

        then:
        1 * mongoInsertCommand.execute() >> new Try.Failure<Void>(new RepoNotFoundException("not found"))
        maybeGroup.isFailure()
        maybeGroup.failed().get() instanceof RepoNotFoundException
    }

    def "deleteCategoryGroup: success"() {
        when:
        Try<Void> maybeGroup = groupRepository.deleteCategoryGroup("id")

        then:
        1 * mongoDeleteByIdCommand.execute() >> new Try.Success<Void>(null)
        maybeGroup.isSuccess()
    }

    def "deleteCategoryGroup: failed, return failure"() {
        when:
        Try<Void> maybeGroup = groupRepository.deleteCategoryGroup("id")

        then:
        1 * mongoDeleteByIdCommand.execute() >> new Try.Failure<Void>(new RepoNotFoundException("not found"))
        maybeGroup.isFailure()
        maybeGroup.failed().get() instanceof RepoNotFoundException
    }

    def "getCategoryGroups: success"() {
        when:
        Try<List<AssessmentCategoryGroup>> maybeGroups = groupRepository.getCategoryGroups()

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<AssessmentCategoryGroup>>([Mock(AssessmentCategoryGroup)])
        maybeGroups.isSuccess()
    }

    def "getCategoryGroups: failed, return failure"() {
        when:
        Try<List<AssessmentCategoryGroup>> maybeGroups = groupRepository.getCategoryGroups()

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<AssessmentCategoryGroup>>(new RepoNotFoundException("not found"))
        maybeGroups.isFailure()
        maybeGroups.failed().get() instanceof RepoNotFoundException
    }

    def "getGlobalGroups: success"() {
        when:
        Try<List<AssessmentCategoryGroup>> maybeGroups = groupRepository.getGlobalGroups()

        then:
        1 * mongoFindCommand.execute() >> new Try.Success<List<AssessmentCategoryGroup>>([Mock(AssessmentCategoryGroup)])
        maybeGroups.isSuccess()
    }

    def "getGlobalGroups: failed, return failure"() {
        when:
        Try<List<AssessmentCategoryGroup>> maybeGroups = groupRepository.getGlobalGroups()

        then:
        1 * mongoFindCommand.execute() >> new Try.Failure<List<AssessmentCategoryGroup>>(new RepoNotFoundException("not found"))
        maybeGroups.isFailure()
        maybeGroups.failed().get() instanceof RepoNotFoundException
    }
}
