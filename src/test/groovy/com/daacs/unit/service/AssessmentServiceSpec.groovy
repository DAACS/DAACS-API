package com.daacs.service

import com.daacs.component.PrereqEvaluatorFactory
import com.daacs.component.prereq.AssessmentPrereqEvaluator
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.framework.validation.annotations.group.UpdateGroup
import com.daacs.model.User
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.dto.UpdateAssessmentRequest
import com.daacs.model.dto.UpdateLightSideModelsRequest
import com.daacs.model.dto.WritingUpdateAssessmentRequest
import com.daacs.model.prereqs.AssessmentPrereq
import com.daacs.model.prereqs.PrereqType
import com.daacs.repository.AssessmentRepository
import com.daacs.repository.UserAssessmentRepository
import com.fasterxml.jackson.databind.ObjectMapper
import com.lambdista.util.Try
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.apache.commons.fileupload.FileUploadException
import spock.lang.Specification

import java.time.Instant

/**
 * Created by chostetter on 6/22/16.
 */
class AssessmentServiceSpec extends Specification {

    AssessmentService assessmentService;
    AssessmentRepository assessmentRepository;
    UserAssessmentRepository userAssessmentRepository;
    LightSideService lightSideService;

    PrereqEvaluatorFactory prereqEvaluatorFactory;
    AssessmentPrereqEvaluator assessmentPrereqEvaluator;
    ValidatorService validatorService;
    DaacsOrikaMapper daacsOrikaMapper;
    ObjectMapper objectMapper;

    User dummyUser = new User("username", "Mr", "Dummy");

    List<Assessment> dummyAssessments = [
            new CATAssessment(
                    id: "assessment-1",
                    assessmentType: AssessmentType.CAT,
                    assessmentCategory: AssessmentCategory.MATHEMATICS,
                    label: "Mathematics",
                    enabled: true,
                    prerequisites: [
                            new AssessmentPrereq(
                                    prereqType: PrereqType.ASSESSMENT,
                                    reason: "You must complete the writing assessment first!",
                                    assessmentCategory: AssessmentCategory.WRITING,
                                    statuses: [CompletionStatus.COMPLETED])
                    ]
            ),
            new WritingAssessment(
                    id: "assessment-2",
                    assessmentType: AssessmentType.WRITING_PROMPT,
                    assessmentCategory: AssessmentCategory.WRITING,
                    scoringType: ScoringType.MANUAL,
                    domains: [
                            new ScoringDomain(id: "domain-1")
                    ]
            )
    ]

    List<UserAssessment> dummyUserAssessments = [
            new CATUserAssessment(
                    assessmentId: "assessment-1",
                    assessmentType: AssessmentType.CAT,
                    assessmentCategory: AssessmentCategory.MATHEMATICS
            )
    ]

    List<Map> dummyAssessmentStats = [
            ["assessmentId": "eeba6280-a367-431a-ae04-e0dc0882aa29",
             "status": "IN_PROGRESS",
             "assessmentCategory": "WRITING",
             "total": "1"],
            ["assessmentId": "eeba6280-a367-431a-ae04-e0dc0882aa29",
             "status": "COMPLETED",
             "assessmentCategory": "WRITING",
             "total": "2"],
            ["assessmentId": "eeba6280-a367-431a-ae04-e0dc0882aa29",
             "status": "GRADED",
             "assessmentCategory": "WRITING",
             "total": "3"],
            ["assessmentId": "aaba6280-a367-431a-ae04-e0dc0882aa29",
             "status": "IN_PROGRESS",
             "assessmentCategory": "READING",
             "total": "4"]
    ]

    UpdateAssessmentRequest updateAssessmentRequest;

    FileItemStream fileItemStream
    FileItemIterator fileItemIterator
    UpdateLightSideModelsRequest updateLightSideModelsRequest

    def setup(){
        daacsOrikaMapper = new DaacsOrikaMapper();
        dummyUser.setId(UUID.randomUUID().toString());

        assessmentRepository = Mock(AssessmentRepository);
        userAssessmentRepository = Mock(UserAssessmentRepository);
        lightSideService = Mock(LightSideService)

        prereqEvaluatorFactory = Mock(PrereqEvaluatorFactory);
        assessmentPrereqEvaluator = Mock(AssessmentPrereqEvaluator);
        objectMapper = new ObjectMapperConfig().objectMapper();
        validatorService = Mock(ValidatorService);

        updateAssessmentRequest = new WritingUpdateAssessmentRequest(id: "assessment-1", enabled: false)

        fileItemStream = Mock(FileItemStream)
        fileItemIterator = Mock(FileItemIterator)
        fileItemIterator.next() >> fileItemStream
        updateLightSideModelsRequest = new UpdateLightSideModelsRequest(
                assessmentId: "assessment-2",
                fileItemIterator: fileItemIterator)

        assessmentService = new AssessmentServiceImpl(
                validatorService: validatorService,
                objectMapper: objectMapper,
                assessmentRepository: assessmentRepository,
                userAssessmentRepository: userAssessmentRepository,
                daacsOrikaMapper: daacsOrikaMapper,
                prereqEvaluatorFactory: prereqEvaluatorFactory,
                lightSideService: lightSideService);

    }

    def "getAssessmentSummaries: returns mapped assessment summaries"(){
        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries(dummyUser.getId(), true, Arrays.asList(AssessmentCategory.class.getEnumConstants()))

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator

        then:
        maybeAssessmentSummaries.isSuccess()
        AssessmentSummary assessmentSummary = maybeAssessmentSummaries.get().get(0)
        assessmentSummary.assessmentId == dummyAssessments.get(0).id
        assessmentSummary.assessmentType == dummyAssessments.get(0).assessmentType
        assessmentSummary.enabled == dummyAssessments.get(0).enabled
        assessmentSummary.prerequisites == dummyAssessments.get(0).prerequisites
        assessmentSummary.enabled == dummyAssessments.get(0).enabled
        assessmentSummary.label == dummyAssessments.get(0).label
        assessmentSummary.content == dummyAssessments.get(0).content
        assessmentSummary.userAssessmentSummary.assessmentId == dummyUserAssessments.get(0).assessmentId
    }

    def "getAssessmentSummaries: if repo call fails, i fail #1"(){
        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries("123", true, Arrays.asList(AssessmentCategory.class.getEnumConstants()));

        then:
        1 * assessmentRepository.getAssessments(_, _) >> new Try.Failure<List<Assessment>>(new Exception())
        maybeAssessmentSummaries.isFailure()
    }

    def "getAssessmentSummaries: if repo call fails, i fail #2"(){
        setup:
        List<Assessment> assessments = [ new CATAssessment(id: "123") ]

        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries("123", true, Arrays.asList(AssessmentCategory.class.getEnumConstants()));

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(assessments)
        1 * userAssessmentRepository.getLatestUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())
        maybeAssessmentSummaries.isFailure()
    }

    def "getAssessment: pass through to repo"(){
        when:
        assessmentService.getAssessment("123")

        then:
        1 * assessmentRepository.getAssessment("123")
    }

    def "insertAssessment: pass through to repo, on success returns assessment"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.createAssessment(dummyAssessments.get(0))

        then:
        1 * assessmentRepository.insertAssessment(dummyAssessments.get(0)) >> new Try.Success<Void>(null)
        maybeAssessment.isSuccess()
        maybeAssessment.get() == dummyAssessments.get(0)
    }

    def "insertAssessment: pass through to repo, if failure, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.createAssessment(dummyAssessments.get(0))

        then:
        1 * assessmentRepository.insertAssessment(_) >> new Try.Failure<Void>(new Exception())
        maybeAssessment.isFailure()
    }

    def "reloadDummyAssessments: success"(){
        when:
        Try<Void> maybeResults = assessmentService.reloadDummyAssessments()

        then:
        assessmentRepository.insertAssessment(_) >> new Try.Success<Void>(null);
        assessmentRepository.getAssessments(_, _) >> new Try.Success<List<Assessment>>([]);
        validatorService.validate(*_) >> new Try.Success<Void>(null);

        then:
        maybeResults.isSuccess()
    }

    def "saveAssessment: pass through to repo, on success returns assessment"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.saveAssessment(dummyAssessments.get(0))

        then:
        1 * assessmentRepository.saveAssessment(dummyAssessments.get(0)) >> new Try.Success<Void>(null)
        maybeAssessment.isSuccess()
        maybeAssessment.get() == dummyAssessments.get(0)
    }

    def "saveAssessment: pass through to repo, if failure, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.saveAssessment(dummyAssessments.get(0))

        then:
        1 * assessmentRepository.saveAssessment(_) >> new Try.Failure<Void>(new Exception())
        maybeAssessment.isFailure()
    }

    def "getContent: success"(){
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContent(dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessment(dummyAssessments.get(0).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))

        then:
        maybeAssessmentContent.isSuccess()
        maybeAssessmentContent.get().getAssessmentId() == dummyAssessments.get(0).getId()
        maybeAssessmentContent.get().getContent() == dummyAssessments.get(0).getContent()
    }

    def "getContent: getAssessment fails, i fail"(){
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContent(dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessment(dummyAssessments.get(0).getId()) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "getContent 2: success"(){
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContent(AssessmentCategory.MATHEMATICS)

        then:
        1 * assessmentRepository.getAssessments(true, [AssessmentCategory.MATHEMATICS]) >> new Try.Success<List<Assessment>>([dummyAssessments.get(0)])

        then:
        maybeAssessmentContent.isSuccess()
        maybeAssessmentContent.get().getAssessmentId() == dummyAssessments.get(0).getId()
        maybeAssessmentContent.get().getContent() == dummyAssessments.get(0).getContent()
    }

    def "getContent 2: getAssessment fails, i fail"(){
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContent(AssessmentCategory.MATHEMATICS)

        then:
        1 * assessmentRepository.getAssessments(true, [AssessmentCategory.MATHEMATICS]) >> new Try.Failure<List<Assessment>>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "getContentForUserAssessment: success"(){
        setup:
        Instant takenDate = Instant.now()

        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentForUserAssessment(dummyUser.getId(), AssessmentCategory.MATHEMATICS, takenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), AssessmentCategory.MATHEMATICS, takenDate) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(0)])
        1 * assessmentRepository.getAssessment(dummyUserAssessments.get(0).getAssessmentId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))

        then:
        maybeAssessmentContent.isSuccess()
        maybeAssessmentContent.get().getAssessmentId() == dummyAssessments.get(0).getId()
        maybeAssessmentContent.get().getContent() == dummyAssessments.get(0).getContent()
    }

    def "getContentForUserAssessment: getAssessment fails, i fail"(){
        setup:
        Instant takenDate = Instant.now()

        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentForUserAssessment(dummyUser.getId(), AssessmentCategory.MATHEMATICS, takenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), AssessmentCategory.MATHEMATICS, takenDate) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(0)])
        1 * assessmentRepository.getAssessment(dummyUserAssessments.get(0).getAssessmentId()) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "getContentForUserAssessment: getUserAssessments fails, i fail"(){
        setup:
        Instant takenDate = Instant.now()

        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentForUserAssessment(dummyUser.getId(), AssessmentCategory.MATHEMATICS, takenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), AssessmentCategory.MATHEMATICS, takenDate) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "updateAssessment: success"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * assessmentRepository.saveAssessment(_) >> { args ->
            Assessment assessment = args[0]
            assert !assessment.getEnabled()
            return new Try.Success<Assessment>(assessment)
        }

        then:
        maybeAssessment.isSuccess()
    }

    def "updateAssessment: getAssessment fails, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Failure<Assessment>(new Exception())
        0 * assessmentRepository.saveAssessment(_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateAssessment: save fails, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * assessmentRepository.saveAssessment(_) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: success"(){
        setup:
        WritingAssessment writingAssessment = (WritingAssessment) dummyAssessments.get(1)

        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(writingAssessment)

        //scoringType
        then:
        1 * fileItemIterator.hasNext() >> true
        1 * fileItemStream.isFormField() >> true
        1 * fileItemStream.getFieldName() >> "scoringType"
        1 * fileItemStream.openStream() >> new ByteArrayInputStream("LIGHTSIDE".bytes)

        //lightside_domain-1
        then:
        1 * fileItemIterator.hasNext() >> true
        1 * fileItemStream.isFormField() >> false
        1 * fileItemStream.getFieldName() >> "lightside_domain-1"
        1 * lightSideService.saveUploadedModelFile(fileItemStream) >> new Try.Success<Void>(null)
        1 * fileItemStream.getName() >> "domain-1.xml"

        then:
        1 * fileItemIterator.hasNext() >> false

        then:
        1 * validatorService.validate(writingAssessment, WritingAssessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.saveAssessment(writingAssessment) >> new Try.Success<Assessment>(writingAssessment)

        then:
        writingAssessment.getScoringType() == ScoringType.LIGHTSIDE
        writingAssessment.getLightSideConfig() != null
        writingAssessment.getLightSideConfig().getDomainModels().get("domain-1") == "domain-1.xml"

        then:
        maybeAssessment.isSuccess()
    }

    def "updateWritingAssessment: assessment not writing"(){
        setup:
        updateLightSideModelsRequest.assessmentId = "assessment-1"

        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-1") >> new Try.Success<Assessment>(dummyAssessments.get(0))
        0 * validatorService.validate(*_)
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: getAssessment fails, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Failure<Assessment>(new Exception())
        0 * validatorService.validate(*_)
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: validate fails, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(dummyAssessments.get(1))
        1 * validatorService.validate(dummyAssessments.get(1), WritingAssessment.class, UpdateGroup.class) >> new Try.Failure<Void>(new Exception())
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: saveAssessment fails, i fail"(){
        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(dummyAssessments.get(1))
        1 * validatorService.validate(dummyAssessments.get(1), WritingAssessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.saveAssessment(dummyAssessments.get(1)) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: fails if scoringType not LIGHTSIDE"(){
        setup:
        WritingAssessment writingAssessment = (WritingAssessment) dummyAssessments.get(1)

        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(writingAssessment)

        //lightside_overall
        then:
        1 * fileItemIterator.hasNext() >> true
        1 * fileItemStream.isFormField() >> false
        1 * fileItemStream.getFieldName() >> "lightside_overall"
        0 * lightSideService.saveUploadedModelFile(fileItemStream)

        then:
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: fails if file is not for domain or overall"(){
        setup:
        WritingAssessment writingAssessment = (WritingAssessment) dummyAssessments.get(1)

        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(writingAssessment)

        //scoringType
        then:
        1 * fileItemIterator.hasNext() >> true
        1 * fileItemStream.isFormField() >> true
        1 * fileItemStream.getFieldName() >> "scoringType"
        1 * fileItemStream.openStream() >> new ByteArrayInputStream("LIGHTSIDE".bytes)

        then:
        1 * fileItemIterator.hasNext() >> true
        1 * fileItemStream.isFormField() >> false
        1 * fileItemStream.getFieldName() >> "lightside_blah"
        0 * lightSideService.saveUploadedModelFile(fileItemStream)

        then:
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: FileUploadException"(){
        setup:
        WritingAssessment writingAssessment = (WritingAssessment) dummyAssessments.get(1)

        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(writingAssessment)

        then:
        1 * fileItemIterator.hasNext() >> { throw new FileUploadException() }
        then:
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateWritingAssessment: IOException"(){
        setup:
        WritingAssessment writingAssessment = (WritingAssessment) dummyAssessments.get(1)

        when:
        Try<Assessment> maybeAssessment = assessmentService.updateWritingAssessment(updateLightSideModelsRequest)

        then:
        1 * assessmentRepository.getAssessment("assessment-2") >> new Try.Success<Assessment>(writingAssessment)

        then:
        1 * fileItemIterator.hasNext() >> { throw new IOException() }
        then:
        0 * assessmentRepository.saveAssessment(*_)

        then:
        maybeAssessment.isFailure()
    }

    def "getAssessmentStats: success"(){
        when:
        Try<List<AssessmentStatSummary>> maybeAssessments = assessmentService.getAssessmentStats()

        then:
        1 * assessmentRepository.getAssessmentStats() >> new Try.Success<List<Map>>(dummyAssessmentStats)

        then:
        maybeAssessments.isSuccess()
        List<AssessmentStatSummary> summaries = maybeAssessments.get()
        summaries.size() == 2
        AssessmentStatSummary firstSummary = summaries.get(0)
        firstSummary.assessmentCategory == AssessmentCategory.WRITING
        firstSummary.total == 6
        firstSummary.stat.size() == 3
        AssessmentStatSummary secondSummary = summaries.get(1)
        secondSummary.assessmentCategory == AssessmentCategory.READING
        secondSummary.total == 4
        secondSummary.stat.size() == 1
    }

    def "getAssessmentStats: failure"(){
        when:
        Try<List<AssessmentStatSummary>> maybeAssessments = assessmentService.getAssessmentStats()

        then:
        1 * assessmentRepository.getAssessmentStats() >> new Try.Failure<List<Map>>(new Exception())

        then:
        maybeAssessments.isFailure()
    }

    def "getCategorySummaries: success"(){
        setup:
        Map<AssessmentCategory, List<UserAssessment>> userAssessmentsByCategory = dummyUserAssessments.collectEntries{
            [(it.getAssessmentCategory()) : [it]]
        }

        when:
        Try<List<AssessmentCategorySummary>> categorySummaries = assessmentService.getCategorySummaries(dummyUser.getId(), Arrays.asList(AssessmentCategory.class.getEnumConstants()))

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator

        then:
        1 * userAssessmentRepository.getUserAssessmentsByCategory(dummyUser.getId()) >> new Try.Success<Map<AssessmentCategory, List<UserAssessment>>>(userAssessmentsByCategory);

        then:
        categorySummaries.isSuccess()
        categorySummaries.get().size() == 2
        categorySummaries.get().get(0).assessmentCategory == AssessmentCategory.MATHEMATICS
        categorySummaries.get().get(0).enabledAssessmentSummary.getAssessmentId() == dummyAssessments.get(0).getId()
        categorySummaries.get().get(0).latestUserAssessmentSummary.getUserAssessmentId() == dummyUserAssessments.get(0).getId()
        categorySummaries.get().get(1).assessmentCategory == AssessmentCategory.WRITING
        categorySummaries.get().get(1).enabledAssessmentSummary.getAssessmentId() == dummyAssessments.get(1).getId()
        categorySummaries.get().get(1).latestUserAssessmentSummary == null
    }

    def "getCategorySummaries: getUserAssessmentsByCategory fails, i fail"(){
        when:
        Try<List<AssessmentCategorySummary>> categorySummaries = assessmentService.getCategorySummaries(dummyUser.getId(), Arrays.asList(AssessmentCategory.class.getEnumConstants()))

        then:
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator

        then:
        1 * userAssessmentRepository.getUserAssessmentsByCategory(dummyUser.getId()) >> new Try.Failure<Map<AssessmentCategory, List<UserAssessment>>>(new Exception());

        then:
        categorySummaries.isFailure()
    }
}
