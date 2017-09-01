package com.daacs.unit.component.grader

import com.daacs.component.grader.ItemGroupGrader
import com.daacs.framework.exception.InvalidObjectException
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.*
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.model.item.ItemGroup
import com.google.common.collect.Range
import com.lambdista.util.Try
import spock.lang.Specification
import spock.lang.Unroll
/**
 * Created by chostetter on 6/22/16.
 */
class ItemGroupGraderSpec extends Specification {

    ItemGroupGrader grader
    ItemGroupGrader spiedGrader

    MultipleChoiceUserAssessment userAssessment
    MultipleChoiceAssessment assessment

    List<ItemGroup> itemGroups
    List<Domain> domains

    def setup(){

        domains = [
                new ScoringDomain(
                        id: "domain-1",
                        rubric: new Rubric(
                                completionScoreMap: [(CompletionScore.HIGH)  : Range.closed((Double)0.0, (Double)7.0)],
                                supplementTable: [new SupplementTableRow(completionScore: CompletionScore.HIGH)]
                        ),
                        subDomains: [
                            new ScoringDomain(
                                    id: "domain-7",
                                    rubric: new Rubric(
                                            completionScoreMap: [
                                                    (CompletionScore.LOW)   : Range.closedOpen((Double)0.0, (Double)1.0),
                                                    (CompletionScore.MEDIUM): Range.closedOpen((Double)1.0, (Double)3.0),
                                                    (CompletionScore.HIGH)  : Range.closed((Double)3, (Double)5.0)
                                            ],
                                            supplementTable: [
                                                    new SupplementTableRow(completionScore: CompletionScore.LOW),
                                                    new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                                    new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                            ]
                                    )
                            )
                    ]
                ),
                new ScoringDomain(
                        id: "domain-2",
                        rubric: new Rubric(
                                completionScoreMap: [(CompletionScore.LOW)  : Range.closed((Double)0.0, (Double)3.0)],
                                supplementTable: [new SupplementTableRow(completionScore: CompletionScore.LOW)]
                        )
                ),
                new AnalysisDomain(
                        id: "domain-3"
                ),
                new ScoringDomain(
                        id: "domain-4",
                        rubric: new Rubric(
                                completionScoreMap: [
                                        (CompletionScore.LOW)   : Range.closedOpen((Double)0.0, (Double)0.334),
                                        (CompletionScore.MEDIUM): Range.closedOpen((Double)0.334, (Double)0.667),
                                        (CompletionScore.HIGH)  : Range.closed((Double)0.667, (Double)1.0)
                                ],
                                supplementTable: [
                                        new SupplementTableRow(completionScore: CompletionScore.LOW),
                                        new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                ]
                        ),
                        subDomains: [
                                new ScoringDomain(
                                        id: "domain-6",
                                        rubric: new Rubric(
                                                completionScoreMap: [
                                                        (CompletionScore.LOW)   : Range.closedOpen((Double)0.0, (Double)0.334),
                                                        (CompletionScore.MEDIUM): Range.closedOpen((Double)0.334, (Double)0.667),
                                                        (CompletionScore.HIGH)  : Range.closed((Double)0.667, (Double)1.0)
                                                ],
                                                supplementTable: [
                                                        new SupplementTableRow(completionScore: CompletionScore.LOW),
                                                        new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                                ]
                                        )
                                )
                        ]
                ),
                new ScoringDomain(
                        id: "domain-5",
                        rubric: new Rubric(
                                completionScoreMap: [
                                        (CompletionScore.LOW)   : Range.closedOpen((Double)0.0, (Double)0.334),
                                        (CompletionScore.MEDIUM): Range.closedOpen((Double)0.334, (Double)0.667),
                                        (CompletionScore.HIGH)  : Range.closed((Double)0.667, (Double)1.0)
                                ],
                                supplementTable: [
                                        new SupplementTableRow(completionScore: CompletionScore.LOW),
                                        new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                        new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                ]
                        )
                )
        ];

        itemGroups = [
                new ItemGroup(id: "itemgroup-1", items: [
                        new Item(id: "item-1", domainId: "domain-1", possibleItemAnswers: [new ItemAnswer(id: "answer-1", score: 1) ], chosenItemAnswerId: "answer-1"),
                        new Item(id: "item-2", domainId: "domain-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 2) ], chosenItemAnswerId: "answer-2")]),
                new ItemGroup(id: "itemgroup-2", items: [
                        new Item(id: "item-3", domainId: "domain-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-3", score: 2) ], chosenItemAnswerId: "answer-3"),
                        new Item(id: "item-4", domainId: "domain-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-4", score: 2) ], chosenItemAnswerId: "answer-4")])
        ]

        userAssessment = new MultipleChoiceUserAssessment(
                itemGroups: itemGroups
        )
        assessment = new MultipleChoiceAssessment(
                scoringType: ScoringType.AVERAGE,
                overallRubric: new Rubric(
                        completionScoreMap: [(CompletionScore.HIGH)  : Range.closed((Double)0.0, (Double)1.0)],
                        supplementTable: [new SupplementTableRow(completionScore: CompletionScore.HIGH)]
                ),
                domains: domains
        )

        setupGrader()
    }

    def void setupGrader(){
        grader = new ItemGroupGrader(userAssessment, assessment)
        spiedGrader = Spy(ItemGroupGrader, constructorArgs: [userAssessment, assessment])
    }

    def "getDomainScoreDetails: returns score details for domains"(){
        when:
        Map<String, DomainScoreDetails> domainScoreDetails = grader.getDomainScoreDetails(itemGroups)

        then:
        domainScoreDetails.size() == 2
        domainScoreDetails.get("domain-1").scoreSum == 3
        domainScoreDetails.get("domain-2").scoreSum == 4
        domainScoreDetails.get("domain-1").numQuestions == 2
        domainScoreDetails.get("domain-2").numQuestions == 2
    }

    @Unroll
    def "getRubricScore: calculates rubric score successfully"(Double rawScore, String domainId, CompletionScore expectedScore){
        setup:
        Domain domain = domains.find{ it.id == domainId }

        when:
        Optional<CompletionScore> completionScore = grader.getRubricScore(rawScore, domain);

        then:
        if(expectedScore == null){
            !completionScore.isPresent()
        }
        else{
            completionScore.isPresent()
            completionScore.get() == expectedScore
        }

        where:
        rawScore | domainId   | expectedScore
        0.0      | "domain-1" | CompletionScore.HIGH
        0.5      | "domain-1" | CompletionScore.HIGH
        1.0      | "domain-1" | CompletionScore.HIGH
        1.5      | "domain-1" | null
        -0.5     | "domain-1" | null
        0.5      | "domain-2" | CompletionScore.LOW
        0.0      | "domain-4" | CompletionScore.LOW
        0.1      | "domain-4" | CompletionScore.LOW
        0.334    | "domain-4" | CompletionScore.LOW
        0.335    | "domain-4" | CompletionScore.MEDIUM
        0.5      | "domain-4" | CompletionScore.MEDIUM
        0.667    | "domain-4" | CompletionScore.MEDIUM
        0.668    | "domain-4" | CompletionScore.HIGH
        0.75     | "domain-4" | CompletionScore.HIGH
        1.0      | "domain-4" | CompletionScore.HIGH
        -0.5     | "domain-4" | null
        1.5      | "domain-4" | null

    }

    @Unroll
    def "getOverallScore: fails on unknown scoring types"(ScoringType scoringType, boolean expectFailure) {
        setup:
        setupGrader()
        assessment.scoringType = scoringType;

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        (1.._) * spiedGrader.getDomainScoreDetails(userAssessment.getItemGroups()) >> [
                "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 10)
        ]
        maybeCompletionScore.isFailure() == expectFailure

        where:
        scoringType           | expectFailure
        ScoringType.SUM       | false
        ScoringType.AVERAGE   | false
        ScoringType.LIGHTSIDE | true
        ScoringType.MANUAL    | true
    }

    @Unroll
    def "getOverallScore: successfully calculates overall score for average"(CompletionScore expectedScore, int domainScoreDetailsIndex){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.AVERAGE;

        List<Map<String, DomainScoreDetails>> domainScoreDetailsList = [
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 10)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 3)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 2, numQuestions: 3)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 10),
                        "domain-2": new DomainScoreDetails(scoreSum: 1, numQuestions: 10)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 3),
                        "domain-2": new DomainScoreDetails(scoreSum: 1, numQuestions: 3)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 2, numQuestions: 3),
                        "domain-2": new DomainScoreDetails(scoreSum: 1, numQuestions: 2)
                ]
        ]

        assessment.overallRubric.completionScoreMap = [
                (CompletionScore.LOW)  : Range.closedOpen((Double)0.0, (Double)0.25),
                (CompletionScore.MEDIUM)  : Range.closedOpen((Double)0.25, (Double)0.5),
                (CompletionScore.HIGH)  : Range.closed((Double)0.5, (Double)1.0)
        ]

        spiedGrader.getDomainScoreDetails(_) >> domainScoreDetailsList.get(domainScoreDetailsIndex)

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isSuccess()
        maybeCompletionScore.get() == expectedScore

        where:
        expectedScore          | domainScoreDetailsIndex
        CompletionScore.LOW    | 0
        CompletionScore.MEDIUM | 1
        CompletionScore.HIGH   | 2
        CompletionScore.LOW    | 3
        CompletionScore.MEDIUM | 4
        CompletionScore.HIGH   | 5
    }

    def "getOverallScore: fails if score is below average ranges"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.AVERAGE;

        assessment.overallRubric.completionScoreMap = [
                (CompletionScore.LOW)  : Range.closedOpen((Double)0.0, (Double)0.25),
                (CompletionScore.MEDIUM)  : Range.closedOpen((Double)0.25, (Double)0.5),
                (CompletionScore.HIGH)  : Range.closed((Double)0.5, (Double)1.0)
        ]

        spiedGrader.getDomainScoreDetails(_) >> [
                "domain-1": new DomainScoreDetails(scoreSum: -1, numQuestions: 10)
        ]

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidObjectException
    }

    def "getOverallScore: fails if score is above average ranges"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.AVERAGE;

        assessment.overallRubric.completionScoreMap = [
                (CompletionScore.LOW)  : Range.closedOpen((Double)0.0, (Double)0.25),
                (CompletionScore.MEDIUM)  : Range.closedOpen((Double)0.25, (Double)0.5),
                (CompletionScore.HIGH)  : Range.closed((Double)0.5, (Double)1.0)
        ]

        spiedGrader.getDomainScoreDetails(_) >> [
                "domain-1": new DomainScoreDetails(scoreSum: 20, numQuestions: 10)
        ]

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidObjectException
    }

    def "getOverallScore: fails if score is below sum ranges"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.SUM;

        assessment.overallRubric.completionScoreMap = [
                (CompletionScore.LOW)  : Range.closedOpen((Double)0.0, (Double)2.0),
                (CompletionScore.MEDIUM)  : Range.closedOpen((Double)2.0, (Double)5.0),
                (CompletionScore.HIGH)  : Range.closed((Double)5.0, (Double)10.0)
        ]

        spiedGrader.getDomainScoreDetails(_) >> [
                "domain-1": new DomainScoreDetails(scoreSum: -1, numQuestions: 10)
        ]

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidObjectException
    }

    def "getOverallScore: fails if score is above sum ranges"(){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.SUM;

        assessment.overallRubric.completionScoreMap = [
                (CompletionScore.LOW)  : Range.closedOpen((Double)0.0, (Double)2.0),
                (CompletionScore.MEDIUM)  : Range.closedOpen((Double)2.0, (Double)5.0),
                (CompletionScore.HIGH)  : Range.closed((Double)5.0, (Double)10.0)
        ]

        spiedGrader.getDomainScoreDetails(_) >> [
                "domain-1": new DomainScoreDetails(scoreSum: 20, numQuestions: 10)
        ]

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidObjectException
    }

    @Unroll
    def "getOverallScore: successfully calculates overall score for sum"(CompletionScore expectedScore, int domainScoreDetailsIndex){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.SUM;

        List<Map<String, DomainScoreDetails>> domainScoreDetailsList = [
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 10)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 2, numQuestions: 3)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 5, numQuestions: 3)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 10),
                        "domain-2": new DomainScoreDetails(scoreSum: 0.5, numQuestions: 10)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 3),
                        "domain-2": new DomainScoreDetails(scoreSum: 3, numQuestions: 3)
                ],
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 2, numQuestions: 3),
                        "domain-2": new DomainScoreDetails(scoreSum: 5, numQuestions: 2)
                ]
        ]

        assessment.overallRubric.completionScoreMap = [
                (CompletionScore.LOW)  : Range.closedOpen((Double)0.0, (Double)2.0),
                (CompletionScore.MEDIUM)  : Range.closedOpen((Double)2.0, (Double)5.0),
                (CompletionScore.HIGH)  : Range.closed((Double)5.0, (Double)10.0)
        ]

        spiedGrader.getDomainScoreDetails(_) >> domainScoreDetailsList.get(domainScoreDetailsIndex)

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isSuccess()
        maybeCompletionScore.get() == expectedScore

        where:
        expectedScore          | domainScoreDetailsIndex
        CompletionScore.LOW    | 0
        CompletionScore.MEDIUM | 1
        CompletionScore.HIGH   | 2
        CompletionScore.LOW    | 3
        CompletionScore.MEDIUM | 4
        CompletionScore.HIGH   | 5
    }

    def "getOverallScore: fails if no completionScoreMap"(){
        setup:
        assessment.overallRubric.completionScoreMap = null

        when:
        Try<CompletionScore> maybeCompletionScore = spiedGrader.getOverallScore()

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidObjectException
    }

    @Unroll
    def "getDomainScores: successfully calculates domain scores for average"(int domainScoreDetailsIndex, List<DomainScore> expectedScores){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.AVERAGE;

        List<Map<String, DomainScoreDetails>> domainScoreDetailsList = [
                [
                        "domain-4": new DomainScoreDetails(scoreSum: 1, numQuestions: 10),
                        "domain-6": new DomainScoreDetails(scoreSum: 6, numQuestions: 10),
                        "domain-3": new DomainScoreDetails(scoreSum: 12, numQuestions: 2)
                ],
                [
                        "domain-4": new DomainScoreDetails(scoreSum: 3, numQuestions: 5)
                ],
                [
                        "domain-4": new DomainScoreDetails(scoreSum: 3, numQuestions: 3)
                ],
                [
                        "domain-4": new DomainScoreDetails(scoreSum: 1, numQuestions: 10),
                        "domain-5": new DomainScoreDetails(scoreSum: 1, numQuestions: 2),
                        "domain-3": new DomainScoreDetails(scoreSum: 12, numQuestions: 2)
                ],
                [
                        "domain-4": new DomainScoreDetails(scoreSum: 3, numQuestions: 5),
                        "domain-5": new DomainScoreDetails(scoreSum: 3, numQuestions: 3)
                ],
                [
                        "domain-4": new DomainScoreDetails(scoreSum: 1, numQuestions: 4),
                        "domain-5": new DomainScoreDetails(scoreSum: 1, numQuestions: 4)
                ]
        ]

        spiedGrader.getDomainScoreDetails(_) >> domainScoreDetailsList.get(domainScoreDetailsIndex)

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        maybeDomainScores.isSuccess()
        maybeDomainScores.get().size() == expectedScores.size()
        checkScoreResults(maybeDomainScores.get(), expectedScores)

        where:
        domainScoreDetailsIndex | expectedScores
        0                       | [new DomainScore(domainId: "domain-4", rubricScore: CompletionScore.MEDIUM, rawScore: 0.35, subDomainScores: [new DomainScore(domainId: "domain-6", rubricScore: CompletionScore.MEDIUM, rawScore: 0.6)])]
        1                       | [new DomainScore(domainId: "domain-4", rubricScore: CompletionScore.MEDIUM, rawScore: 0.6)]
        2                       | [new DomainScore(domainId: "domain-4", rubricScore: CompletionScore.HIGH, rawScore: 1.0)]
        3                       | [new DomainScore(domainId: "domain-4", rubricScore: CompletionScore.LOW, rawScore: 0.1), new DomainScore(domainId: "domain-5", rubricScore: CompletionScore.MEDIUM, rawScore: 0.5)]
        4                       | [new DomainScore(domainId: "domain-4", rubricScore: CompletionScore.MEDIUM, rawScore: 0.6), new DomainScore(domainId: "domain-5", rubricScore: CompletionScore.HIGH, rawScore: 1.0)]
        5                       | [new DomainScore(domainId: "domain-4", rubricScore: CompletionScore.LOW, rawScore: 0.25), new DomainScore(domainId: "domain-5", rubricScore: CompletionScore.LOW, rawScore: 0.25)]

    }

    @Unroll
    def "getDomainScores: successfully calculates domain scores for sum"(int domainScoreDetailsIndex, List<DomainScore> expectedScores){
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.SUM;

        List<Map<String, DomainScoreDetails>> domainScoreDetailsList = [
                [
                        "domain-1": new DomainScoreDetails(scoreSum: 1, numQuestions: 10),
                        "domain-7": new DomainScoreDetails(scoreSum: 5, numQuestions: 10)
                ],
                [
                        "domain-2": new DomainScoreDetails(scoreSum: 3, numQuestions: 5)
                ],
                [:]
        ]

        spiedGrader.getDomainScoreDetails(_) >> domainScoreDetailsList.get(domainScoreDetailsIndex)

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        maybeDomainScores.isSuccess()
        maybeDomainScores.get().size() == expectedScores.size()
        checkScoreResults(maybeDomainScores.get(), expectedScores)

        where:
        domainScoreDetailsIndex | expectedScores
        0                       | [new DomainScore(domainId: "domain-1", rubricScore: CompletionScore.HIGH, rawScore: 6.0, subDomainScores: [new DomainScore(domainId: "domain-7", rubricScore: CompletionScore.HIGH, rawScore: 5.0)])]
        1                       | [new DomainScore(domainId: "domain-2", rubricScore: CompletionScore.LOW, rawScore: 3.0)]
        2                       | []
    }

    def checkScoreResults(List<DomainScore> domainScoresResult, List<DomainScore> expectedDomainScores){
        domainScoresResult.eachWithIndex { it, index ->
            assert it.domainId == expectedDomainScores.get(index).domainId
            assert it.rubricScore == expectedDomainScores.get(index).rubricScore
            assert it.rawScore == expectedDomainScores.get(index).rawScore
            if(it.subDomainScores != null){
                checkScoreResults(it.subDomainScores, expectedDomainScores.get(index).subDomainScores)
            }
        }

        return true
    }

    @Unroll
    def "getDomainScores: fails on bad scoring type"(ScoringType scoringType, boolean expectedFailure) {
        setup:
        setupGrader()
        assessment.scoringType = scoringType;
        spiedGrader.getRubricScore(*_) >> Optional.of(CompletionScore.MEDIUM)
        spiedGrader.getExistingDomainScore(_, _) >> new DomainScore(rubricScore: CompletionScore.MEDIUM)

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        maybeDomainScores.isFailure() == expectedFailure

        where:
        scoringType           | expectedFailure
        ScoringType.AVERAGE   | false
        ScoringType.SUM       | false
        ScoringType.LIGHTSIDE | true
        ScoringType.MANUAL    | false
    }

    def "getDomainScores: fails on no rubric score"() {
        setup:
        setupGrader()
        assessment.scoringType = ScoringType.AVERAGE;

        when:
        Try<List<DomainScore>> maybeDomainScores = spiedGrader.getDomainScores()

        then:
        1 * spiedGrader.getRubricScore(*_) >> Optional.empty()
        maybeDomainScores.isFailure()
    }

    def "grade: success"(){
        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        1 * spiedGrader.getDomainScores() >> new Try.Success<List<DomainScore>>([new DomainScore(rubricScore: CompletionScore.MEDIUM)])
        1 * spiedGrader.getOverallScore() >> new Try.Success<CompletionScore>(CompletionScore.MEDIUM)

        then:
        maybeUserAssessment.isSuccess()
        maybeUserAssessment.get().getDomainScores().size() == 1
        maybeUserAssessment.get().getDomainScores().get(0).getRubricScore() == CompletionScore.MEDIUM
        maybeUserAssessment.get().getOverallScore() == CompletionScore.MEDIUM
        maybeUserAssessment.get().getStatus() == CompletionStatus.GRADED
    }

    def "grade: getDomainScores fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        1 * spiedGrader.getDomainScores() >> new Try.Failure<List<DomainScore>>(new Exception())
        0 * spiedGrader.getOverallScore()

        then:
        maybeUserAssessment.isFailure()
    }

    def "grade: getOverallScore fails, i fail"(){
        when:
        Try<UserAssessment> maybeUserAssessment = spiedGrader.grade()

        then:
        1 * spiedGrader.getDomainScores() >> new Try.Success<List<DomainScore>>([new DomainScore(rubricScore: CompletionScore.MEDIUM)])
        1 * spiedGrader.getOverallScore() >> new Try.Failure<CompletionScore>(new Exception())

        then:
        maybeUserAssessment.isFailure()
    }

    @Unroll
    def "getDomainScore: scoreIsSubDomainAverage true, success"(CompletionScore expectedScore, Map<String, DomainScoreDetails> domainScoreDetailsMap){
        setup:
        setupGrader()

        Domain domain = new ScoringDomain(
                id: "domain-1",
                rubric: null,
                scoreIsSubDomainAverage: true,
                subDomains: [
                        new ScoringDomain(
                                id: "domain-2",
                                rubric: new Rubric(
                                        completionScoreMap: [
                                                (CompletionScore.LOW)   : Range.closedOpen((Double)0.0, (Double)1.0),
                                                (CompletionScore.MEDIUM): Range.closedOpen((Double)1.0, (Double)3.0),
                                                (CompletionScore.HIGH)  : Range.closed((Double)3, (Double)5.0)
                                        ],
                                        supplementTable: [
                                                new SupplementTableRow(completionScore: CompletionScore.LOW),
                                                new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                        ]
                                )
                        ),
                        new ScoringDomain(
                                id: "domain-3",
                                rubric: new Rubric(
                                        completionScoreMap: [
                                                (CompletionScore.LOW)   : Range.closedOpen((Double)0.0, (Double)1.0),
                                                (CompletionScore.MEDIUM): Range.closedOpen((Double)1.0, (Double)3.0),
                                                (CompletionScore.HIGH)  : Range.closed((Double)3, (Double)5.0)
                                        ],
                                        supplementTable: [
                                                new SupplementTableRow(completionScore: CompletionScore.LOW),
                                                new SupplementTableRow(completionScore: CompletionScore.MEDIUM),
                                                new SupplementTableRow(completionScore: CompletionScore.HIGH)
                                        ]
                                )
                        )
                ]
        )

        when:
        Try<DomainScore> maybeDomainScore = spiedGrader.getDomainScore(domain, domainScoreDetailsMap)

        then:
        maybeDomainScore.isSuccess()
        maybeDomainScore.get().rubricScore == expectedScore

        where:
        expectedScore          | domainScoreDetailsMap
        CompletionScore.HIGH   | ["domain-2": new DomainScoreDetails(scoreSum: 10, numQuestions: 10),"domain-3": new DomainScoreDetails(scoreSum: 50, numQuestions: 10)]
        CompletionScore.LOW    | ["domain-2": new DomainScoreDetails(scoreSum: 5, numQuestions: 10),"domain-3": new DomainScoreDetails(scoreSum: 5, numQuestions: 10)]
        CompletionScore.MEDIUM | ["domain-2": new DomainScoreDetails(scoreSum: 20, numQuestions: 10),"domain-3": new DomainScoreDetails(scoreSum: 20, numQuestions: 10)]
        CompletionScore.HIGH   | ["domain-2": new DomainScoreDetails(scoreSum: 30, numQuestions: 10),"domain-3": new DomainScoreDetails(scoreSum: 50, numQuestions: 10)]
    }
}
