package com.daacs.unit.service

import com.daacs.framework.exception.BadInputException
import com.daacs.framework.exception.IncompatibleStatusException
import com.daacs.framework.exception.RepoNotFoundException
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.*
import com.daacs.model.item.*
import com.daacs.repository.AssessmentRepository
import com.daacs.repository.UserAssessmentRepository
import com.daacs.service.ItemGroupService
import com.daacs.service.ItemGroupServiceImpl
import com.google.common.collect.Range
import com.lambdista.util.Try
import spock.lang.Specification
import spock.lang.Unroll

import java.math.RoundingMode
/**
 * Created by chostetter on 6/22/16.
 */
class ItemGroupServiceSpec extends Specification {

    ItemGroupService itemGroupService;
    ItemGroupService spiedItemGroupService;
    private UserAssessmentRepository userAssessmentRepository;
    private AssessmentRepository assessmentRepository;

    List<CATItemGroup> assessmentItemGroups
    List<ItemGroupTransition> itemGroupTransitions
    List<CATItemGroup> userItemGroups

    ItemGroup submittedItemGroup
    CATUserAssessment catUserAssessment
    CATAssessment catAssessment

    MultipleChoiceUserAssessment multChoiceUserAssessment
    MultipleChoiceAssessment multChoiceAssessment

    MultipleChoiceUserAssessment likertUserAssessment
    MultipleChoiceAssessment likertAssessment

    WritingAssessment writingAssessment
    WritingPromptUserAssessment writingPromptUserAssessment

    String userId = UUID.randomUUID().toString()

    def setup(){

        writingAssessment = new WritingAssessment(id: "writing-1", assessmentType: AssessmentType.WRITING_PROMPT)
        writingPromptUserAssessment = new WritingPromptUserAssessment()

        likertUserAssessment = new MultipleChoiceUserAssessment(
                status: CompletionStatus.IN_PROGRESS,
                itemGroups: [new ItemGroup(id: "itemgroup-1", items: [
                        new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ]),
                        new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 2) ])])]
        )

        likertAssessment = new MultipleChoiceAssessment(
                id: "likert-1",
                assessmentType: AssessmentType.LIKERT,
                itemGroups: [new ItemGroup(id: "itemgroup-1", items: [
                        new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ]),
                        new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 2) ])])]
        )

        multChoiceUserAssessment = new MultipleChoiceUserAssessment(
                status: CompletionStatus.IN_PROGRESS,
                itemGroups: [
                        new ItemGroup(id: "itemgroup-1", items: [
                            new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ], chosenItemAnswerId: "answer-1"),
                            new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 2) ], chosenItemAnswerId: "answer-2")]),
                        new ItemGroup(id: "itemgroup-2", items: [
                             new Item(id: "item-3", possibleItemAnswers: [ new ItemAnswer(id: "answer-3", score: 1) ], chosenItemAnswerId: "answer-3"),
                             new Item(id: "item-4", possibleItemAnswers: [ new ItemAnswer(id: "answer-4", score: 2) ], chosenItemAnswerId: "answer-4")])
                ]
        )

        multChoiceAssessment = new MultipleChoiceAssessment(
                id: "multchoice-1",
                assessmentType: AssessmentType.MULTIPLE_CHOICE,
                itemGroups: [
                        new ItemGroup(id: "itemgroup-1", items: [
                                new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ]),
                                new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 2) ])]),
                        new ItemGroup(id: "itemgroup-2", items: [
                                new Item(id: "item-3", possibleItemAnswers: [ new ItemAnswer(id: "answer-3", score: 1) ]),
                                new Item(id: "item-4", possibleItemAnswers: [ new ItemAnswer(id: "answer-4", score: 2) ])])
                ]
        )

        submittedItemGroup = new ItemGroup(
                id: "itemgroup-1",
                items: [
                        new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ], chosenItemAnswerId: "answer-1"),
                        new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 2) ], chosenItemAnswerId: "answer-2")
                ]
        )

        catUserAssessment = new CATUserAssessment(
                status: CompletionStatus.IN_PROGRESS,
                itemGroups: [
                        new CATItemGroup(id: "itemgroup-1", items: [
                                new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ], chosenItemAnswerId: "answer-1"),
                                new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 1) ], chosenItemAnswerId: "answer-2")]),
                        new CATItemGroup(id: "itemgroup-2", items: [
                                new Item(id: "item-3", possibleItemAnswers: [ new ItemAnswer(id: "answer-3", score: 1) ], chosenItemAnswerId: "answer-3"),
                                new Item(id: "item-4", possibleItemAnswers: [ new ItemAnswer(id: "answer-4", score: 2) ], chosenItemAnswerId: "answer-4")])
                ]
        )

        catAssessment = new CATAssessment(
                id: "cat-1",
                startingDifficulty: Difficulty.MEDIUM,
                assessmentType: AssessmentType.CAT,
                minTakenGroups: 2,
                maxTakenGroups: 3,
                numQuestionsPerGroup: 2,
                itemGroups: [
                        new CATItemGroup(id: "itemgroup-1", difficulty: Difficulty.MEDIUM, items: [
                                new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ]),
                                new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 1) ])]),
                        new CATItemGroup(id: "itemgroup-2", difficulty: Difficulty.EASY, items: [
                                new Item(id: "item-3", possibleItemAnswers: [ new ItemAnswer(id: "answer-3", score: 1) ]),
                                new Item(id: "item-4", possibleItemAnswers: [ new ItemAnswer(id: "answer-4", score: 2) ])]),
                        new CATItemGroup(id: "itemgroup-3", difficulty: Difficulty.HARD, items: [
                                new Item(id: "item-5", possibleItemAnswers: [ new ItemAnswer(id: "answer-5", score: 3) ]),
                                new Item(id: "item-6", possibleItemAnswers: [ new ItemAnswer(id: "answer-6", score: 2) ])])
                ]
        )

        assessmentItemGroups = [
                new CATItemGroup(id: "1", difficulty: Difficulty.EASY),
                new CATItemGroup(id: "2", difficulty: Difficulty.EASY),
                new CATItemGroup(id: "3", difficulty: Difficulty.MEDIUM),
                new CATItemGroup(id: "4", difficulty: Difficulty.MEDIUM),
                new CATItemGroup(id: "5", difficulty: Difficulty.HARD),
                new CATItemGroup(id: "6", difficulty: Difficulty.HARD)
        ]

        itemGroupTransitions = [
                new ItemGroupTransition(
                        groupDifficulty: Difficulty.EASY,
                        transitionMap: [
                                (Difficulty.EASY): Range.atMost(2),
                                (Difficulty.MEDIUM): Range.atLeast(3)
                        ]
                ),
                new ItemGroupTransition(
                        groupDifficulty: Difficulty.MEDIUM,
                        transitionMap: [
                                (Difficulty.EASY): Range.atMost(2),
                                (Difficulty.MEDIUM): Range.closed(3, 4),
                                (Difficulty.HARD): Range.atLeast(5)
                        ]
                ),
                new ItemGroupTransition(
                        groupDifficulty: Difficulty.HARD,
                        transitionMap: [
                                (Difficulty.MEDIUM): Range.atMost(4),
                                (Difficulty.HARD): Range.atLeast(5)
                        ]
                )
        ]

        userItemGroups = [
                new CATItemGroup(
                        difficulty: Difficulty.EASY,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 2) ], chosenItemAnswerId: "1")]
                ),
                new CATItemGroup(
                        difficulty: Difficulty.EASY,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 3) ], chosenItemAnswerId: "1")]
                ),
                new CATItemGroup(
                        difficulty: Difficulty.MEDIUM,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 2) ], chosenItemAnswerId: "1")]
                ),
                new CATItemGroup(
                        difficulty: Difficulty.MEDIUM,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 4) ], chosenItemAnswerId: "1")]
                ),
                new CATItemGroup(
                        difficulty: Difficulty.MEDIUM,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 5) ], chosenItemAnswerId: "1")]
                ),
                new CATItemGroup(
                        difficulty: Difficulty.HARD,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 4) ], chosenItemAnswerId: "1")]
                ),
                new CATItemGroup(
                        difficulty: Difficulty.HARD,
                        items: [new Item(possibleItemAnswers: [ new ItemAnswer(id: "1", score: 5) ], chosenItemAnswerId: "1")]
                )
        ]

        userAssessmentRepository = Mock(UserAssessmentRepository)
        assessmentRepository = Mock(AssessmentRepository)

        spiedItemGroupService = Spy(ItemGroupServiceImpl, constructorArgs: [userAssessmentRepository, assessmentRepository])
        itemGroupService = new ItemGroupServiceImpl(userAssessmentRepository, assessmentRepository)

        assessmentRepository.getAssessment(catAssessment.getId()) >> new Try.Success<Assessment>(catAssessment)
        userAssessmentRepository.getLatestUserAssessment(userId, catAssessment.getId()) >> new Try.Success<UserAssessment>(catUserAssessment)

        assessmentRepository.getAssessment(writingAssessment.getId()) >> new Try.Success<Assessment>(writingAssessment)
        userAssessmentRepository.getLatestUserAssessment(userId, writingAssessment.getId()) >> new Try.Success<UserAssessment>(writingPromptUserAssessment)

        assessmentRepository.getAssessment(multChoiceAssessment.getId()) >> new Try.Success<Assessment>(multChoiceAssessment)
        userAssessmentRepository.getLatestUserAssessment(userId, multChoiceAssessment.getId()) >> new Try.Success<UserAssessment>(multChoiceUserAssessment)

        assessmentRepository.getAssessment(likertAssessment.getId()) >> new Try.Success<Assessment>(likertAssessment)
        userAssessmentRepository.getLatestUserAssessment(userId, likertAssessment.getId()) >> new Try.Success<UserAssessment>(likertUserAssessment)
    }

    @Unroll
    def "getRandomCATItemGroup: success"(Difficulty difficulty, List<String> possibleIds, List<CATItemGroup> alreadyTakenGroups){
        when:
        Optional<CATItemGroup> catItemGroup = itemGroupService.getRandomCATItemGroup(assessmentItemGroups, difficulty, alreadyTakenGroups)

        then:
        if(possibleIds.size() > 0){
            catItemGroup.isPresent()
            possibleIds.contains(catItemGroup.get().getId())
        }
        else{
            !catItemGroup.isPresent()
        }

        where:
        difficulty        | possibleIds | alreadyTakenGroups
        Difficulty.EASY   | ["1", "2"]  | []
        Difficulty.MEDIUM | ["3", "4"]  | []
        Difficulty.HARD   | ["5", "6"]  | []
        Difficulty.EASY   | ["2"]       | [new CATItemGroup(id: "1")]
        Difficulty.MEDIUM | ["3"]       | [new CATItemGroup(id: "4")]
        Difficulty.HARD   | ["6"]       | [new CATItemGroup(id: "5")]
        Difficulty.EASY   | []          | [new CATItemGroup(id: "1"), new CATItemGroup(id: "2")]
        Difficulty.MEDIUM | []          | [new CATItemGroup(id: "3"), new CATItemGroup(id: "4")]
        Difficulty.HARD   | []          | [new CATItemGroup(id: "5"), new CATItemGroup(id: "6")]
    }

    @Unroll
    def "isLastTwoGroupsSameDifficulty: success"(boolean expected, List<CATItemGroup> userItemGroups){
        when:
        boolean isLastTwoGroupsSameDifficulty = itemGroupService.isLastTwoGroupsSameDifficulty(userItemGroups)

        then:
        isLastTwoGroupsSameDifficulty == expected

        where:
        expected | userItemGroups
        false    | []
        false    | [new CATItemGroup(difficulty: Difficulty.EASY)]
        false    | [new CATItemGroup(difficulty: Difficulty.EASY), new CATItemGroup(difficulty: Difficulty.MEDIUM)]
        true     | [new CATItemGroup(difficulty: Difficulty.MEDIUM), new CATItemGroup(difficulty: Difficulty.MEDIUM)]
        true     | [new CATItemGroup(difficulty: Difficulty.EASY), new CATItemGroup(difficulty: Difficulty.MEDIUM), new CATItemGroup(difficulty: Difficulty.MEDIUM)]
        false    | [new CATItemGroup(difficulty: Difficulty.EASY), new CATItemGroup(difficulty: Difficulty.EASY), new CATItemGroup(difficulty: Difficulty.MEDIUM)]
        true     | [new CATItemGroup(difficulty: Difficulty.EASY), new CATItemGroup(difficulty: Difficulty.EASY), new CATItemGroup(difficulty: Difficulty.EASY)]
    }

    def "getScoreForItemGroup sums up item group scores"(){
        setup:
        CATItemGroup userItemGroup = new CATItemGroup(
                difficulty: Difficulty.MEDIUM,
                items: [
                        new Item(
                                possibleItemAnswers: [
                                        new ItemAnswer(id: "1", score: 1),
                                        new ItemAnswer(id: "2", score: 2),
                                        new ItemAnswer(id: "3", score: 3)
                                ],
                                chosenItemAnswerId: "1"
                        ),
                        new Item(
                                possibleItemAnswers: [
                                        new ItemAnswer(id: "4", score: 1),
                                        new ItemAnswer(id: "5", score: 2),
                                        new ItemAnswer(id: "6", score: 3)
                                ],
                                chosenItemAnswerId: "6"
                        ),
                        new Item(
                                possibleItemAnswers: [
                                        new ItemAnswer(id: "7", score: 1),
                                        new ItemAnswer(id: "8", score: 2),
                                        new ItemAnswer(id: "9", score: 3)
                                ],
                                chosenItemAnswerId: "8"
                        )
                ]
        )

        when:
        int score = itemGroupService.getScoreForItemGroup(userItemGroup)

        then:
        score == 6
    }

    @Unroll
    def "determineNextGroupDifficulty: success"(int userItemGroupsIndex, Difficulty expectedDifficulty){
        when:
        Try<Difficulty> maybeDifficulty = itemGroupService.determineNextGroupDifficulty(itemGroupTransitions, [userItemGroups.get(userItemGroupsIndex)])

        then:
        maybeDifficulty.isSuccess()
        maybeDifficulty.get() == expectedDifficulty

        where:
        userItemGroupsIndex | expectedDifficulty
        0                   | Difficulty.EASY
        1                   | Difficulty.MEDIUM
        2                   | Difficulty.EASY
        3                   | Difficulty.MEDIUM
        4                   | Difficulty.HARD
        5                   | Difficulty.MEDIUM
        6                   | Difficulty.HARD
    }

    def "determineNextGroupDifficulty: fails if no groups"(){
        when:
        Try<Difficulty> maybeDifficulty = itemGroupService.determineNextGroupDifficulty(itemGroupTransitions, [])

        then:
        maybeDifficulty.isFailure()
    }

    def "determineNextGroupDifficulty: fails if no transition group"(){
        setup:
        itemGroupTransitions.remove(0)

        when:
        Try<Difficulty> maybeDifficulty = itemGroupService.determineNextGroupDifficulty(itemGroupTransitions, [userItemGroups.get(0)])

        then:
        maybeDifficulty.isFailure()
    }

    def "determineNextGroupDifficulty: fails if no valid transition entry in the group"(){
        setup:
        itemGroupTransitions.get(0).getTransitionMap().put(Difficulty.EASY, Range.atMost(1))

        when:
        Try<Difficulty> maybeDifficulty = itemGroupService.determineNextGroupDifficulty(itemGroupTransitions, [userItemGroups.get(0)])

        then:
        maybeDifficulty.isFailure()
    }

    def "getItemById: success"(){
        setup:
        ItemGroup itemGroup = new ItemGroup(items: [new Item(id: "1")])

        when:
        Optional<Item> item = itemGroupService.getItemById(itemGroup, "1")

        then:
        item.isPresent()
    }

    def "getItemById: success, not present"(){
        setup:
        ItemGroup itemGroup = new ItemGroup(items: [])

        when:
        Optional<Item> item = itemGroupService.getItemById(itemGroup, "1")

        then:
        !item.isPresent()
    }

    def "applySubmittedAnswersToUserItemGroups: submittedItemGroup updates userAssessment"(){
        when:
        Try<UserAssessment> maybeUserAssessment = itemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment);

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> { arguments ->
            CATUserAssessment savingUserAssessment = arguments[0]
            List<ItemGroup> itemGroups = savingUserAssessment.getItemGroups()

            BigDecimal progressPercentage = new BigDecimal(savingUserAssessment.getProgressPercentage()).setScale(3, RoundingMode.HALF_UP);

            assert progressPercentage == 0.667
            assert itemGroups.get(0).getId() == "itemgroup-1"
            assert itemGroups.get(0).getItems().get(0).getId() == "item-1"
            assert itemGroups.get(0).getItems().get(1).getId() == "item-2"
            assert itemGroups.get(0).getItems().get(0).getChosenItemAnswerId() == "answer-1"
            assert itemGroups.get(0).getItems().get(1).getChosenItemAnswerId() == "answer-2"

            return new Try.Success<UserAssessment>(savingUserAssessment)
        }
        maybeUserAssessment.isSuccess()
    }

    def "applySubmittedAnswersToUserItemGroups: submittedItemGroup adds to userAssessment"(){
        setup:
        catUserAssessment.setItemGroups([])

        when:
        Try<UserAssessment> maybeUserAssessment = itemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment);

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> { arguments ->
            CATUserAssessment savingUserAssessment = arguments[0]
            List<ItemGroup> itemGroups = savingUserAssessment.getItemGroups()

            BigDecimal progressPercentage = new BigDecimal(savingUserAssessment.getProgressPercentage()).setScale(3, RoundingMode.HALF_UP);

            assert progressPercentage == 0.333
            assert itemGroups.get(0).getId() == "itemgroup-1"
            assert itemGroups.get(0).getItems().get(0).getId() == "item-1"
            assert itemGroups.get(0).getItems().get(1).getId() == "item-2"
            assert itemGroups.get(0).getItems().get(0).getChosenItemAnswerId() == "answer-1"
            assert itemGroups.get(0).getItems().get(1).getChosenItemAnswerId() == "answer-2"

            return new Try.Success<UserAssessment>(savingUserAssessment)
        }
        maybeUserAssessment.isSuccess()
    }

    def "applySubmittedAnswersToUserItemGroups: save fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = itemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment);

        then:
        1 * userAssessmentRepository.saveUserAssessment(_) >> new Try.Failure<UserAssessment>(new Exception())
        maybeUserAssessment.isFailure()
    }

    def "applySubmittedAnswersToUserItemGroups: submitted itemgroup doesn't match user assessment's itemgroup"(){
        setup:
        submittedItemGroup.getItems().remove(0)

        when:
        Try<UserAssessment> maybeUserAssessment = itemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment);

        then:
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof BadInputException
    }

    def "applySubmittedAnswersToUserItemGroups: submitted itemgroup doesn't match assessment's itemgroup"(){
        setup:
        catUserAssessment.setItemGroups([])
        submittedItemGroup.getItems().remove(0)

        when:
        Try<UserAssessment> maybeUserAssessment = itemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment);

        then:
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof BadInputException
    }

    def "applySubmittedAnswersToUserItemGroups: submitted itemgroup not on assessment"(){
        setup:
        catAssessment.setItemGroups([])

        when:
        Try<UserAssessment> maybeUserAssessment = itemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment);

        then:
        maybeUserAssessment.isFailure()
        maybeUserAssessment.failed().get() instanceof RepoNotFoundException
    }

    def "saveItemGroup: success for cat"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, catAssessment.getId(), submittedItemGroup)

        then:
        1 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment) >> new Try.Success<UserAssessment>(catUserAssessment)
        maybeItemGroup.isSuccess()
    }

    def "saveItemGroup: not in progress"(){
        setup:
        catUserAssessment.setStatus(CompletionStatus.COMPLETED)

        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, catAssessment.getId(), submittedItemGroup)

        then:
        0 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "saveItemGroup: wrong type"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, writingAssessment.getId(), submittedItemGroup)

        then:
        0 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "saveItemGroup: applySubmittedAnswersToUserItemGroups fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, catAssessment.getId(), submittedItemGroup)

        then:
        1 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, catUserAssessment, catAssessment) >> new Try.Failure<UserAssessment>(new Exception())
        maybeItemGroup.isFailure()
    }

    def "saveItemGroup: getAssessment fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, catAssessment.getId(), submittedItemGroup)

        then:
        1 * assessmentRepository.getAssessment(*_) >> new Try.Failure<Assessment>(new Exception())
        0 * userAssessmentRepository.getLatestUserAssessment(*_)
        0 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "saveItemGroup: getLatestUserAssessment fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, catAssessment.getId(), submittedItemGroup)

        then:
        1 * assessmentRepository.getAssessment(catAssessment.getId()) >> new Try.Success<Assessment>(catAssessment)
        1 * userAssessmentRepository.getLatestUserAssessment(*_) >> new Try.Failure<UserAssessment>(new Exception())
        0 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "saveItemGroup: success for likert"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, likertAssessment.getId(), submittedItemGroup)

        then:
        1 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, likertUserAssessment, likertAssessment) >> new Try.Success<UserAssessment>(likertUserAssessment)
        maybeItemGroup.isSuccess()
    }

    def "saveItemGroup: success for multipleChoice"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.saveItemGroup(userId, multChoiceAssessment.getId(), submittedItemGroup)

        then:
        1 * spiedItemGroupService.applySubmittedAnswersToUserItemGroups(submittedItemGroup, multChoiceUserAssessment, multChoiceAssessment) >> new Try.Success<UserAssessment>(multChoiceUserAssessment)
        maybeItemGroup.isSuccess()
    }

    def "getUnfinishedItemGroup: returns unfinished item group"(){
        setup:
        userItemGroups.get(0).getItems().get(0).setChosenItemAnswerId(null)

        when:
        Optional<ItemGroup> itemGroup = itemGroupService.getUnfinishedItemGroup(userItemGroups)

        then:
        itemGroup.isPresent()
        itemGroup.get().getId() == userItemGroups.get(0).getId()
    }

    def "getUnfinishedItemGroup: does not return unfinished item group"(){
        when:
        Optional<ItemGroup> itemGroup = itemGroupService.getUnfinishedItemGroup(userItemGroups)

        then:
        !itemGroup.isPresent()
    }

    def "getNextMultipleChoiceItemGroup: returns unfinished group"(){
        setup:
        //make it unfinished
        multChoiceUserAssessment.getItemGroups().get(0).getItems().get(0).setChosenItemAnswerId(null)

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment)

        then:
        maybeItemGroup.isSuccess()
        maybeItemGroup.get().getId() == multChoiceUserAssessment.getItemGroups().get(0).getId()
    }

    def "getNextMultipleChoiceItemGroup: completed first, unfinished second group"(){
        setup:
        //make it unfinished
        multChoiceUserAssessment.getItemGroups().get(1).getItems().get(0).setChosenItemAnswerId(null)

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment)

        then:
        maybeItemGroup.isSuccess()
        maybeItemGroup.get().getId() == multChoiceUserAssessment.getItemGroups().get(1).getId()
    }

    def "getNextMultipleChoiceItemGroup: none started, returns from assessment"(){
        setup:
        multChoiceUserAssessment.setItemGroups([])

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment)

        then:
        maybeItemGroup.isSuccess()
        maybeItemGroup.get().getId() == multChoiceAssessment.getItemGroups().get(0).getId()
    }

    def "getNextMultipleChoiceItemGroup: completed one, get the next one"(){
        setup:
        //delete the second one from the UA, make it look like we completed the first but not the second
        multChoiceUserAssessment.getItemGroups().remove(1)

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment)

        then:
        maybeItemGroup.isSuccess()
        maybeItemGroup.get().getId() == multChoiceAssessment.getItemGroups().get(1).getId()
    }

    def "getNextMultipleChoiceItemGroup: no more left"(){
        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment)

        then:
        maybeItemGroup.isSuccess()
        !maybeItemGroup.toOptional().isPresent()
        maybeItemGroup.get() == null
    }

    def "getNextCATItemGroup: returns unfinished group"(){
        setup:
        //make it unfinished
        catUserAssessment.getItemGroups().get(0).getItems().get(0).setChosenItemAnswerId(null)

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        maybeItemGroup.isSuccess()
        maybeItemGroup.get().getId() == catUserAssessment.getItemGroups().get(0).getId()
    }

    def "getNextCATItemGroup: completed first, unfinished second group"(){
        setup:
        //make it unfinished
        catUserAssessment.getItemGroups().get(1).getItems().get(0).setChosenItemAnswerId(null)

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        maybeItemGroup.isSuccess()
        maybeItemGroup.get().getId() == catUserAssessment.getItemGroups().get(1).getId()
    }

    def "getNextCATItemGroup: CAT determines next group"(){
        setup:
        catUserAssessment.getItemGroups().remove(1)

        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        1 * spiedItemGroupService.determineNextGroupDifficulty(*_) >> new Try.Success<Difficulty>(Difficulty.EASY)

        then:
        maybeItemGroup.isSuccess()
        ((CATItemGroup) maybeItemGroup.get()).getDifficulty() == Difficulty.EASY
        catAssessment.getItemGroups().find{ maybeItemGroup.get().getId() == it.getId() } != null
    }

    def "getNextCATItemGroup: all done"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        maybeItemGroup.isSuccess()
        !maybeItemGroup.toOptional().isPresent()
    }

    def "getNextCATItemGroup: ran out of groups"(){
        setup:
        catAssessment.minTakenGroups = 3
        catAssessment.maxTakenGroups = 4

        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        1 * spiedItemGroupService.determineNextGroupDifficulty(*_) >> new Try.Success<Difficulty>(Difficulty.EASY)

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextCATItemGroup: determineNextGroupDifficulty fails, i fail"(){
        setup:
        catUserAssessment.getItemGroups().remove(1)

        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        1 * spiedItemGroupService.determineNextGroupDifficulty(*_) >> new Try.Failure<Difficulty>(new Exception())

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextCATItemGroup: none started"(){
        setup:
        catUserAssessment.setItemGroups([])

        when:
        Try<ItemGroup> maybeItemGroup = itemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        maybeItemGroup.isSuccess()
        ((CATItemGroup) maybeItemGroup.get()).getDifficulty() == catAssessment.getStartingDifficulty()
        catAssessment.getItemGroups().find{ maybeItemGroup.get().getId() == it.getId() } != null
    }

    def "getNextCATItemGroup: getRandomCATItemGroup is null, i fail"(){
        setup:
        catUserAssessment.setItemGroups([])

        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment)

        then:
        1 * spiedItemGroupService.getRandomCATItemGroup(*_) >> Optional.empty()

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextItemGroup: cat success"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, catAssessment.getId())

        then:
        1 * spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment) >> new Try.Success<ItemGroup>(Mock(ItemGroup))
        0 * spiedItemGroupService.getNextMultipleChoiceItemGroup(*_)

        then:
        maybeItemGroup.isSuccess()
    }

    def "getNextItemGroup: fails if not IN_PROGRESS"(){
        setup:
        catUserAssessment.setStatus(CompletionStatus.COMPLETED)

        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, catAssessment.getId())

        then:
        maybeItemGroup.isFailure()
        maybeItemGroup.failed().get() instanceof IncompatibleStatusException
    }


    def "getNextItemGroup: getNextCATItemGroup fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, catAssessment.getId())

        then:
        1 * spiedItemGroupService.getNextCATItemGroup(userId, catUserAssessment, catAssessment) >> new Try.Failure<ItemGroup>(new Exception())
        0 * spiedItemGroupService.getNextMultipleChoiceItemGroup(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextItemGroup: likert success"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, likertAssessment.getId())

        then:
        1 * spiedItemGroupService.getNextMultipleChoiceItemGroup(userId, likertUserAssessment, likertAssessment) >> new Try.Success<ItemGroup>(Mock(ItemGroup))
        0 * spiedItemGroupService.getNextCATItemGroup(*_)

        then:
        maybeItemGroup.isSuccess()
    }

    def "getNextItemGroup: multiple choice success"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, multChoiceAssessment.getId())

        then:
        1 * spiedItemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment) >> new Try.Success<ItemGroup>(Mock(ItemGroup))
        0 * spiedItemGroupService.getNextCATItemGroup(*_)

        then:
        maybeItemGroup.isSuccess()
    }

    def "getNextItemGroup: getNextMultipleChoiceItemGroup fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, multChoiceAssessment.getId())

        then:
        1 * assessmentRepository.getAssessment(multChoiceAssessment.getId()) >> new Try.Success<Assessment>(multChoiceAssessment)
        1 * userAssessmentRepository.getLatestUserAssessment(userId, multChoiceAssessment.getId()) >> new Try.Success<UserAssessment>(multChoiceUserAssessment)

        then:
        1 * spiedItemGroupService.getNextMultipleChoiceItemGroup(userId, multChoiceUserAssessment, multChoiceAssessment) >> new Try.Failure<ItemGroup>(new Exception())
        0 * spiedItemGroupService.getNextCATItemGroup(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextItemGroup: getAssessment fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, catAssessment.getId())

        then:
        1 * assessmentRepository.getAssessment(catAssessment.getId()) >> new Try.Failure<Assessment>(new Exception())
        0 * userAssessmentRepository.getLatestUserAssessment(*_)
        0 * spiedItemGroupService.getNextCATItemGroup(*_)
        0 * spiedItemGroupService.getNextMultipleChoiceItemGroup(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextItemGroup: getLatestUserAssessment fails, i fail"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, catAssessment.getId())

        then:
        1 * assessmentRepository.getAssessment(catAssessment.getId()) >> new Try.Success<Assessment>(catAssessment)
        1 * userAssessmentRepository.getLatestUserAssessment(userId, catAssessment.getId()) >> new Try.Failure<UserAssessment>(new Exception())
        0 * spiedItemGroupService.getNextCATItemGroup(*_)
        0 * spiedItemGroupService.getNextMultipleChoiceItemGroup(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "getNextItemGroup: wrong type"(){
        when:
        Try<ItemGroup> maybeItemGroup = spiedItemGroupService.getNextItemGroup(userId, writingAssessment.getId())

        then:
        0 * spiedItemGroupService.getNextCATItemGroup(*_)
        0 * spiedItemGroupService.getNextMultipleChoiceItemGroup(*_)

        then:
        maybeItemGroup.isFailure()
    }

    def "calcProgressPercentage: success"(){
        when:
        Double percentage = itemGroupService.calcProgressPercentage(userItemGroups, 10);

        then:
        percentage == (double)(7/10)
    }
}
