package com.daacs.unit.component

import com.daacs.component.utils.DefaultCatgoryGroup
import com.daacs.component.utils.UpgradeAssessmentSchemaUtilsImpl
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.assessment.user.WritingPromptUserAssessment
import com.daacs.model.item.CATItemGroup
import com.daacs.model.item.DefaultItemAnswer
import com.daacs.model.item.Difficulty
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.model.item.ItemGroup
import com.daacs.model.item.ItemGroupTransition
import com.daacs.service.AssessmentCategoryGroupService
import com.daacs.service.AssessmentService
import com.daacs.service.UserAssessmentService
import com.google.common.collect.Range
import com.lambdista.util.Try
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by lhorne on 9/20/18.
 */
class UpgradeAssessmentSchemaUtilsSpec extends Specification {

    AssessmentService assessmentService
    AssessmentCategoryGroupService assessmentCategoryGroupService;
    UserAssessmentService userAssessmentService
    DaacsOrikaMapper orikaMapper
    UpgradeAssessmentSchemaUtilsImpl upgradeAssessmentSchemaUtils
    CATAssessment dummyCATAssessment
    Exception failureException
    AssessmentCategoryGroup dummyGroup


    static List<ItemGroup> dummyItemGroups = [
    new ItemGroup(
            items: [ //first item chosen
            new Item(possibleItemAnswers: [new ItemAnswer(id: "1", content: "11", score: 111), new ItemAnswer(id: "2", content: "22", score: 222)]),
    new Item(possibleItemAnswers: [new ItemAnswer(id: "3", content: "33", score: 333)])
    ]),
    new ItemGroup(
            items: [ //no item chosen
            new Item(possibleItemAnswers: []),
    new Item(possibleItemAnswers: null)
    ]),
    new ItemGroup(
            items: [ //second item chosen
            new Item(possibleItemAnswers: []),
    new Item(possibleItemAnswers: [new ItemAnswer(id: "4", content: "44", score: 444), new ItemAnswer(id: "5", content: "55", score: 555)])
    ]),
    new ItemGroup(
            possibleItemAnswers: [new ItemAnswer(id: "6", content: "66", score: 666)],
    items: [ //no backfilling necessary
    new Item(possibleItemAnswers: []),
    new Item(possibleItemAnswers: [new ItemAnswer(id: "4", content: "44", score: 444), new ItemAnswer(id: "5", content: "55", score: 555)])
    ])
    ]

    static UserAssessment dummyCATUserAssessment = new CATUserAssessment(
            assessmentCategory: AssessmentCategory.MATHEMATICS
    )
    static UserAssessment dummyWritingUserAssessment = new WritingPromptUserAssessment(
            assessmentCategory: AssessmentCategory.WRITING
    )

    def setup() {
        failureException = new Exception()
        assessmentService = Mock(AssessmentService)
        userAssessmentService = Mock(UserAssessmentService)
        assessmentCategoryGroupService = Mock(AssessmentCategoryGroupService)
        orikaMapper = new DaacsOrikaMapper()
        upgradeAssessmentSchemaUtils = new UpgradeAssessmentSchemaUtilsImpl(1L, orikaMapper, assessmentService, userAssessmentService, assessmentCategoryGroupService)
        dummyCATAssessment = new CATAssessment(
                schemaVersion: 2L,
                assessmentType: AssessmentType.CAT,
                itemGroupTransitions: [
                        new ItemGroupTransition(
                                groupDifficulty: Difficulty.EASY,
                                transitionMap: [
                                        (Difficulty.EASY)  : Range.atMost((Integer) 2),
                                        (Difficulty.MEDIUM): Range.atLeast((Integer) 3)
                                ]
                        ),
                        new ItemGroupTransition(
                                groupDifficulty: Difficulty.MEDIUM,
                                transitionMap: [
                                        (Difficulty.EASY)  : Range.atMost((Integer) 2),
                                        (Difficulty.MEDIUM): null,
                                        (Difficulty.HARD)  : Range.atLeast((Integer) 5)
                                ]
                        ),
                        new ItemGroupTransition(
                                groupDifficulty: Difficulty.HARD,
                                transitionMap: [
                                        (Difficulty.MEDIUM): Range.atMost((Integer) 4),
                                        (Difficulty.HARD)  : Range.atLeast((Integer) 5)
                                ]
                        )
                ]
        )
        dummyGroup = new AssessmentCategoryGroup(
                id: "id",
                label: "lebleele",
                assessmentCategory: "MATHEMATICS"
        )
    }

    def "upgradeAssessmentSchema"() {
        setup:
        Assessment assessment = new CATAssessment(assessmentType: AssessmentType.WRITING_PROMPT, assessmentCategory: AssessmentCategory.WRITING);

        when:
        Try<Assessment> maybeAssessment = upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(assessment)

        then:
        1 * assessmentService.saveAssessment(_) >> { args -> return new Try.Success<Assessment>((Assessment) args[0]) }
        1 * assessmentCategoryGroupService.createCategoryGroupIfPossible(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * userAssessmentService.getUserAssessmentsByAssessmentId(_) >> new Try.Success<List<UserAssessment>>([dummyCATUserAssessment])
        1 * userAssessmentService.bulkUserAssessmentSave([dummyCATUserAssessment]) >> new Try.Success<Void>(null)

        then:
        maybeAssessment.get().schemaVersion == 1L
    }

    def "upgradeAssessmentSchema: no upgrade necessary"() {
        setup:
        Assessment assessment = new CATAssessment(schemaVersion: 1L);

        when:
        Try<Assessment> maybeAssessment = upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(assessment)

        then:
        0 * assessmentService.saveAssessment(_)

        then:
        maybeAssessment.get().schemaVersion == 1L
    }

    def "addPossibleItemAnswerToItemGroup"() {
        setup:
        Assessment assessment = new MultipleChoiceAssessment(assessmentType: AssessmentType.LIKERT, itemGroups: dummyItemGroups);

        when:
        Assessment outputAssessment = upgradeAssessmentSchemaUtils.addPossibleItemAnswerToItemGroup(assessment)

        then:
        MultipleChoiceAssessment likertAssessment = (MultipleChoiceAssessment) outputAssessment
        likertAssessment.itemGroups.get(0).possibleItemAnswers.get(0).content == "11"
        likertAssessment.itemGroups.get(0).possibleItemAnswers.get(1).content == "22"
        likertAssessment.itemGroups.get(0).possibleItemAnswers.get(0).id != "1"
        likertAssessment.itemGroups.get(0).possibleItemAnswers.get(1).id != "2"

        likertAssessment.itemGroups.get(1).possibleItemAnswers.size() == 0

        likertAssessment.itemGroups.get(2).possibleItemAnswers.get(0).content == "44"
        likertAssessment.itemGroups.get(2).possibleItemAnswers.get(1).content == "55"

        likertAssessment.itemGroups.get(3).possibleItemAnswers.get(0).content == "66"

    }

    def "addPossibleItemAnswerToItemGroup  does nothing for non LIKERT itemGroup assessments"() {

        when:
        Assessment outputAssessment = upgradeAssessmentSchemaUtils.addPossibleItemAnswerToItemGroup(assessment)

        then:
        if(outputAssessment instanceof CATAssessment   ){
            ((CATAssessment)outputAssessment).itemGroups.get(0).possibleItemAnswers == null
        }
        else if(outputAssessment instanceof MultipleChoiceAssessment   ){
            ((MultipleChoiceAssessment)outputAssessment).itemGroups.get(0).possibleItemAnswers == null
        }


        where:
        assessment << [new CATAssessment (assessmentType: AssessmentType.CAT, itemGroups: dummyItemGroups),  new MultipleChoiceAssessment(assessmentType: AssessmentType.MULTIPLE_CHOICE, itemGroups: dummyItemGroups)]

    }


    def "addlightsideModelFilenameToDomain"() {
        setup:
        Assessment assessment = new WritingAssessment(
                domains: [
                        new AnalysisDomain(id: "1", lightsideModelFilename: "x.xml"), //already set, no overwrite
                        new AnalysisDomain(id: "2", lightsideModelFilename: "y.xml"), //not found, no change
                        new AnalysisDomain(id: "3", lightsideModelFilename: ""),
                        new AnalysisDomain(id: "3", lightsideModelFilename: null)],
                lightSideConfig: new LightSideConfig(domainModels: ["1": "1.xml", "3": "3.xml"]))

        when:
        Assessment outputAssessment = upgradeAssessmentSchemaUtils.addlightsideModelFilenameToDomain(assessment)

        then:
        WritingAssessment writingAssessment = (WritingAssessment) outputAssessment
        writingAssessment.domains.get(0).lightsideModelFilename == "x.xml"
        writingAssessment.domains.get(1).lightsideModelFilename == "y.xml"
        writingAssessment.domains.get(2).lightsideModelFilename == "3.xml"
        writingAssessment.domains.get(3).lightsideModelFilename == "3.xml"

    }

    def "addlightsideModelFilenameToDomain: success no change"(Assessment assessment) {
        when:
        Assessment outputAssessment = upgradeAssessmentSchemaUtils.addlightsideModelFilenameToDomain(assessment)

        then:
        noExceptionThrown()

        where:
        assessment                                   | _
        new CATAssessment()                          | _
        new MultipleChoiceAssessment()               | _
        new WritingAssessment(domains: null)         | _
        new WritingAssessment(lightSideConfig: null) | _
    }

    def "convertTransitionMap: success no change"(Assessment assessment) {
        when:
        Assessment outputAssessment = upgradeAssessmentSchemaUtils.convertTransitionMap(assessment)

        then:
        noExceptionThrown()

        where:
        assessment                                                      | _
        new MultipleChoiceAssessment(schemaVersion: 3L)                 | _
        new WritingAssessment(domains: null, schemaVersion: 3L)         | _
        new WritingAssessment(lightSideConfig: null, schemaVersion: 3L) | _
    }

    def "convertTransitionMap succeeds with CAT assessment"() {
        when:
        Assessment outputAssessment = upgradeAssessmentSchemaUtils.convertTransitionMap(dummyCATAssessment)

        then:
        noExceptionThrown()

        then:

        for (ItemGroupTransition itemGroupTransition : ((CATAssessment) outputAssessment).getItemGroupTransitions()) {
            for (Range range : itemGroupTransition.getTransitionMap().values()) {
                range instanceof Range<Double>

            }
        }
    }

    //does not mock createDefaultGroup or backfillUserAssessmentGroupIds
    def "addCategoryGroups all assessment categories"() {
        when:
        Try<Assessment> maybeOutputAssessment = upgradeAssessmentSchemaUtils.addCategoryGroups(assessment)

        then:
        1 * assessmentCategoryGroupService.createCategoryGroupIfPossible(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        1 * userAssessmentService.getUserAssessmentsByAssessmentId(_) >> new Try.Success<List<UserAssessment>>([userAssessment])
        1 * userAssessmentService.bulkUserAssessmentSave([userAssessment]) >> new Try.Success<Void>(null)
        noExceptionThrown()

        then:
        maybeOutputAssessment.isSuccess()
        maybeOutputAssessment.get().getAssessmentCategoryGroup().getId() == groupId

        where:
        assessment                                                                                  | groupId                               | userAssessment
        new CATAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS, schemaVersion: 3L)    | DefaultCatgoryGroup.MATHEMATICS_ID    | dummyCATUserAssessment
        new CATAssessment(assessmentCategory: AssessmentCategory.COLLEGE_SKILLS, schemaVersion: 3L) | DefaultCatgoryGroup.COLLEGE_SKILLS_ID | dummyCATUserAssessment
        new WritingAssessment(assessmentCategory: AssessmentCategory.WRITING, schemaVersion: 3L)    | DefaultCatgoryGroup.WRITING_ID        | dummyWritingUserAssessment
        new CATAssessment(assessmentCategory: AssessmentCategory.READING, schemaVersion: 3L)        | DefaultCatgoryGroup.READING_ID        | dummyCATUserAssessment
    }

    def "addCategoryGroups getUserAssessmentsForBackfill failure"() {
        when:
        Try<Assessment> maybeOutputAssessment = upgradeAssessmentSchemaUtils.addCategoryGroups(new CATAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS, schemaVersion: 3L))

        then:
        1 * userAssessmentService.getUserAssessmentsByAssessmentId(_) >> new Try.Failure<List<UserAssessment>>(failureException)
        0 * userAssessmentService.bulkUserAssessmentSave([dummyCATUserAssessment]) >> new Try.Success<Void>(null)
        0 * assessmentCategoryGroupService.createCategoryGroupIfPossible(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        noExceptionThrown()

        then:
        maybeOutputAssessment.isFailure()
        maybeOutputAssessment.failed().get() == failureException
    }

    def "addCategoryGroups bulkSaveForBackfill failure"() {
        when:
        Try<Assessment> maybeOutputAssessment = upgradeAssessmentSchemaUtils.addCategoryGroups(new CATAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS, schemaVersion: 3L))

        then:
        1 * userAssessmentService.getUserAssessmentsByAssessmentId(_) >> new Try.Success<List<UserAssessment>>([dummyCATUserAssessment])
        1 * userAssessmentService.bulkUserAssessmentSave([dummyCATUserAssessment]) >> new Try.Failure<Void>(failureException)
        0 * assessmentCategoryGroupService.createCategoryGroupIfPossible(_) >> new Try.Success<AssessmentCategoryGroup>(dummyGroup)
        noExceptionThrown()

        then:
        maybeOutputAssessment.isFailure()
        maybeOutputAssessment.failed().get() == failureException
    }

    def "addCategoryGroups createCategoryGroupIfPossible failure"() {
        when:
        Try<Assessment> maybeOutputAssessment = upgradeAssessmentSchemaUtils.addCategoryGroups(new CATAssessment(assessmentCategory: AssessmentCategory.MATHEMATICS, schemaVersion: 3L))

        then:
        1 * userAssessmentService.getUserAssessmentsByAssessmentId(_) >> new Try.Success<List<UserAssessment>>([dummyCATUserAssessment])
        1 * userAssessmentService.bulkUserAssessmentSave([dummyCATUserAssessment]) >> new Try.Success<Void>(null)
        1 * assessmentCategoryGroupService.createCategoryGroupIfPossible(_) >> new Try.Failure<AssessmentCategoryGroup>(failureException)
        noExceptionThrown()

        then:
        maybeOutputAssessment.isFailure()
        maybeOutputAssessment.failed().get() == failureException
    }
}
