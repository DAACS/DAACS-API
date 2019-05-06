package com.daacs.unit.service

import com.daacs.component.PrereqEvaluatorFactory
import com.daacs.component.utils.CategoryGroupUtils
import com.daacs.component.utils.CategoryGroupUtilsImpl
import com.daacs.component.utils.UpgradeAssessmentSchemaUtils
import com.daacs.component.prereq.AssessmentPrereqEvaluator
import com.daacs.framework.serializer.DaacsOrikaMapper
import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.framework.validation.annotations.group.CreateGroup
import com.daacs.framework.validation.annotations.group.UpdateGroup
import com.daacs.model.User
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.CompletionStatus
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.dto.AssessmentResponse
import com.daacs.model.dto.UpdateAssessmentRequest
import com.daacs.model.dto.UpdateLightSideModelsRequest
import com.daacs.model.dto.WritingUpdateAssessmentRequest
import com.daacs.model.dto.assessmentUpdate.ScoringDomainRequest
import com.daacs.model.prereqs.AssessmentPrereq
import com.daacs.model.prereqs.PrereqType
import com.daacs.repository.AssessmentRepository
import com.daacs.repository.UserAssessmentRepository
import com.daacs.service.AssessmentCategoryGroupService
import com.daacs.service.AssessmentServiceImpl
import com.daacs.service.LightSideService
import com.daacs.service.ValidatorService
import com.fasterxml.jackson.databind.ObjectMapper
import com.lambdista.util.Try
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.FileItemStream
import org.springframework.web.multipart.MultipartFile
import spock.lang.Specification

import java.time.Instant
import java.util.function.Function
import java.util.stream.Collectors

/**
 * Created by chostetter on 6/22/16.
 */
class AssessmentServiceSpec extends Specification {

    CategoryGroupUtils defaultCategoryGroupUtils
    AssessmentServiceImpl assessmentService;
    AssessmentCategoryGroupService assessmentCategoryGroupService
    AssessmentRepository assessmentRepository;
    UserAssessmentRepository userAssessmentRepository;
    LightSideService lightSideService;

    UpgradeAssessmentSchemaUtils upgradeAssessmentSchemaUtils;
    PrereqEvaluatorFactory prereqEvaluatorFactory;
    AssessmentPrereqEvaluator assessmentPrereqEvaluator;
    ValidatorService validatorService;
    DaacsOrikaMapper daacsOrikaMapper;
    ObjectMapper objectMapper;

    String defaultAssessmentCategoryGroupIds = "[groupId1,groupId2,groupId3]"

    User dummyUser = new User("username", "Mr", "Dummy");

    List<Assessment> dummyAssessments = [
            new CATAssessment(
                    id: "assessment-1",
                    assessmentType: AssessmentType.CAT,
                    assessmentCategory: AssessmentCategory.MATHEMATICS,
                    assessmentCategoryGroup: new AssessmentCategoryGroup(
                            id: "groupId1"
                    ),
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
                    assessmentCategoryGroup: new AssessmentCategoryGroup(
                            id: "groupId2"
                    ),
                    scoringType: ScoringType.MANUAL,
                    domains: [
                            new ScoringDomain(id: "domain-1",subDomains: [])
                    ]
            ),
            new WritingAssessment(
                    id: "assessment-3",
                    assessmentType: AssessmentType.WRITING_PROMPT,
                    assessmentCategory: AssessmentCategory.WRITING,
                    assessmentCategoryGroup: new AssessmentCategoryGroup(
                            id: "groupId3"
                    ),
                    scoringType: ScoringType.LIGHTSIDE,
                    domains: [
                            new ScoringDomain(id: "domain-1",subDomains: [])
                    ]
            )
    ]

    List<UserAssessment> dummyUserAssessments = [
            new CATUserAssessment(
                    assessmentId: "assessment-1",
                    assessmentType: AssessmentType.CAT,
                    assessmentCategory: AssessmentCategory.MATHEMATICS,
                    assessmentCategoryGroupId: "groupId1"
            )
    ]

    List<Map> dummyAssessmentStats = [
            ["assessmentId"      : "eeba6280-a367-431a-ae04-e0dc0882aa29",
             "status"            : "IN_PROGRESS",
             "assessmentCategory": "WRITING",
             "total"             : "1"],
            ["assessmentId"      : "eeba6280-a367-431a-ae04-e0dc0882aa29",
             "status"            : "COMPLETED",
             "assessmentCategory": "WRITING",
             "total"             : "2"],
            ["assessmentId"      : "eeba6280-a367-431a-ae04-e0dc0882aa29",
             "status"            : "GRADED",
             "assessmentCategory": "WRITING",
             "total"             : "3"],
            ["assessmentId"      : "aaba6280-a367-431a-ae04-e0dc0882aa29",
             "status"            : "IN_PROGRESS",
             "assessmentCategory": "READING",
             "total"             : "4"]
    ]

    UpdateAssessmentRequest updateAssessmentRequest;
    UpdateAssessmentRequest updateAssessmentRequest1;
    UpdateAssessmentRequest updateAssessmentRequestBadInput;

    FileItemStream fileItemStream
    FileItemIterator fileItemIterator
    UpdateLightSideModelsRequest updateLightSideModelsRequest
    MultipartFile dummyMultipartFile

    def setup() {
        defaultCategoryGroupUtils = new CategoryGroupUtilsImpl()
        daacsOrikaMapper = new DaacsOrikaMapper();
        dummyUser.setId(UUID.randomUUID().toString());

        assessmentCategoryGroupService = Mock(AssessmentCategoryGroupService)
        assessmentRepository = Mock(AssessmentRepository);
        userAssessmentRepository = Mock(UserAssessmentRepository);
        lightSideService = Mock(LightSideService)

        prereqEvaluatorFactory = Mock(PrereqEvaluatorFactory);
        assessmentPrereqEvaluator = Mock(AssessmentPrereqEvaluator);
        objectMapper = new ObjectMapperConfig().objectMapper();
        validatorService = Mock(ValidatorService);
        dummyMultipartFile = Mock(MultipartFile)

        updateAssessmentRequest = new WritingUpdateAssessmentRequest(
                id: "assessment-1",
                enabled: false
        )
        updateAssessmentRequest1 = new WritingUpdateAssessmentRequest(
                id: "assessment-2",
                scoringType: ScoringType.LIGHTSIDE,
                enabled: false,
                domains: [
                        new ScoringDomainRequest(id: "domain-2", lightsideModelFilename: "model-2.xml",subDomains: [])
                ]
        )

        updateAssessmentRequestBadInput = new WritingUpdateAssessmentRequest(
                id: "assessment-3",
                scoringType: ScoringType.LIGHTSIDE,
                enabled: true,
                domains: [
                        new ScoringDomainRequest(id: "domain-3")
                ]
        )

        upgradeAssessmentSchemaUtils = Mock(UpgradeAssessmentSchemaUtils)
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
                lightSideService: lightSideService,
                upgradeAssessmentSchemaUtils: upgradeAssessmentSchemaUtils,
                categoryGroupUtils: defaultCategoryGroupUtils,
                assessmentCategoryGroupService: assessmentCategoryGroupService
        );

    }

    def "getAssessmentSummaries: returns mapped assessment summaries"() {
        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries(dummyUser.getId(), true,null)

        then:
        1 * assessmentRepository.getAssessments(true,null) >> new Try.Success<List<Assessment>>(dummyAssessments)
        upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<Map<String, UserAssessment>>(dummyUserAssessments.stream().
                collect(Collectors.toMap(new Function<UserAssessment, String>() {
                    String apply(UserAssessment p) { return p.getAssessmentId(); }
                },
                        Function.<UserAssessment> identity())))
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

    def "getAssessmentSummaries: if repo call fails, i fail #1"() {
        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries("123", true, null);

        then:
        1 * assessmentRepository.getAssessments(_, null) >> new Try.Failure<List<Assessment>>(new Exception())
        maybeAssessmentSummaries.isFailure()
    }

    def "getAssessmentSummaries: if repo call fails, i fail #2"() {
        setup:
        List<Assessment> assessments = [new CATAssessment(id: "123")]

        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries("123", true, null);

        then:
        1 * assessmentRepository.getAssessments(true, null) >> new Try.Success<List<Assessment>>(assessments)
        1 * upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * userAssessmentRepository.getLatestUserAssessments(*_) >> new Try.Failure<List<UserAssessment>>(new Exception())
        maybeAssessmentSummaries.isFailure()
    }

    def "getAssessmentSummaries: if upgrade fails, i fail"() {
        setup:
        List<Assessment> assessments = [new CATAssessment(id: "123")]

        when:
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = assessmentService.getSummaries("123", true, null);

        then:
        1 * assessmentRepository.getAssessments(true, null) >> new Try.Success<List<Assessment>>(assessments)
        1 * upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Failure<Assessment>(new Exception())
        0 * userAssessmentRepository.getLatestUserAssessments(*_)

        then:
        maybeAssessmentSummaries.isFailure()
    }

    def "getAssessment: pass through to repo"() {
        when:
        assessmentService.getAssessment("123")

        then:
        1 * assessmentRepository.getAssessment("123") >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))
    }

    def "insertAssessment: pass through to repo, on success returns assessment"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.createAssessment(dummyAssessments.get(0))

        then:
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, CreateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.insertAssessment(dummyAssessments.get(0)) >> new Try.Success<Void>(null)

        then:
        maybeAssessment.isSuccess()
        maybeAssessment.get().data == dummyAssessments.get(0)
    }

    def "insertAssessment: validation failure on enabled assessment, i fail"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.createAssessment(dummyAssessments.get(0))

        then:
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, CreateGroup.class) >> new Try.Failure<Void>(new Exception())
        0 * assessmentRepository.insertAssessment(dummyAssessments.get(0))

        then:
        maybeAssessment.isFailure()
    }

    def "insertAssessment: validation failure on disabled assessment, i pass"() {
        setup:
        Assessment disabledAssessment = dummyAssessments.get(0)
        disabledAssessment.enabled = false

        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.createAssessment(disabledAssessment)

        then:
        1 * validatorService.validate(disabledAssessment, Assessment.class, CreateGroup.class) >> new Try.Failure<Void>(new Exception())
        1 * assessmentRepository.insertAssessment(disabledAssessment) >> new Try.Success<Void>(null)

        then:
        maybeAssessment.isSuccess()
        !maybeAssessment.get().data.enabled
        !maybeAssessment.get().data.isValid
    }

    def "insertAssessment: pass through to repo, if failure, i fail"() {
        when:
        Try<Assessment> maybeAssessment = assessmentService.createAssessment(dummyAssessments.get(0))

        then:
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, CreateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.insertAssessment(_) >> new Try.Failure<Void>(new Exception())

        then:
        maybeAssessment.isFailure()
    }

    def "reloadDummyAssessments: success"() {
        when:
        Try<Void> maybeResults = assessmentService.reloadDummyAssessments()

        then:
        assessmentRepository.insertAssessment(_) >> new Try.Success<Void>(null);
        assessmentRepository.getAssessments(_, null) >> new Try.Success<List<Assessment>>([]);
        validatorService.validate(*_) >> new Try.Success<Void>(null);

        then:
        maybeResults.isSuccess()
    }

    def "saveAssessment: pass through to repo, on success returns assessment"() {
        when:
        Try<Assessment> maybeAssessment = assessmentService.saveAssessment(dummyAssessments.get(0))

        then:
        1 * assessmentRepository.saveAssessment(dummyAssessments.get(0)) >> new Try.Success<Void>(null)
        maybeAssessment.isSuccess()
        maybeAssessment.get() == dummyAssessments.get(0)
    }

    def "saveAssessment: pass through to repo, if failure, i fail"() {
        when:
        Try<Assessment> maybeAssessment = assessmentService.saveAssessment(dummyAssessments.get(0))

        then:
        1 * assessmentRepository.saveAssessment(_) >> new Try.Failure<Void>(new Exception())
        maybeAssessment.isFailure()
    }

    def "getContent: success"() {
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContent(dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessment(dummyAssessments.get(0).getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))

        then:
        maybeAssessmentContent.isSuccess()
        maybeAssessmentContent.get().getAssessmentId() == dummyAssessments.get(0).getId()
        maybeAssessmentContent.get().getContent() == dummyAssessments.get(0).getContent()
    }

    def "getContent: getAssessment fails, i fail"() {
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContent(dummyAssessments.get(0).getId())

        then:
        1 * assessmentRepository.getAssessment(dummyAssessments.get(0).getId()) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "getContent 2: success"() {
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentByCategoryGroup("mathGroupId")

        then:
        1 * assessmentRepository.getAssessments(true, ["mathGroupId"]) >> new Try.Success<List<Assessment>>([dummyAssessments.get(0)])
        1 * upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))

        then:
        maybeAssessmentContent.isSuccess()
        maybeAssessmentContent.get().getAssessmentId() == dummyAssessments.get(0).getId()
        maybeAssessmentContent.get().getContent() == dummyAssessments.get(0).getContent()
    }

    def "getContent 2: getAssessment fails, i fail"() {
        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentByCategoryGroup("mathGroupId")

        then:
        1 * assessmentRepository.getAssessments(true, ["mathGroupId"]) >> new Try.Failure<List<Assessment>>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "getContentForUserAssessment: success"() {
        setup:
        Instant takenDate = Instant.now()

        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentForUserAssessment(dummyUser.getId(), "mathGroupId", takenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), "mathGroupId", takenDate) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(0)])
        1 * assessmentRepository.getAssessment(dummyUserAssessments.get(0).getAssessmentId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))

        then:
        maybeAssessmentContent.isSuccess()
        maybeAssessmentContent.get().getAssessmentId() == dummyAssessments.get(0).getId()
        maybeAssessmentContent.get().getContent() == dummyAssessments.get(0).getContent()
    }

    def "getContentForUserAssessment: getAssessment fails, i fail"() {
        setup:
        Instant takenDate = Instant.now()

        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentForUserAssessment(dummyUser.getId(), "mathGroupId", takenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), "mathGroupId", takenDate) >> new Try.Success<List<UserAssessment>>([dummyUserAssessments.get(0)])
        1 * assessmentRepository.getAssessment(dummyUserAssessments.get(0).getAssessmentId()) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "getContentForUserAssessment: getUserAssessments fails, i fail"() {
        setup:
        Instant takenDate = Instant.now()

        when:
        Try<AssessmentContent> maybeAssessmentContent = assessmentService.getContentForUserAssessment(dummyUser.getId(), "mathGroupId", takenDate)

        then:
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId(), "mathGroupId", takenDate) >> new Try.Failure<List<UserAssessment>>(new Exception())

        then:
        maybeAssessmentContent.isFailure()
    }

    def "updateAssessment: success"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.saveAssessment(_) >> { args ->
            Assessment assessment = args[0]
            assert !assessment.getEnabled()
            return new Try.Success<Assessment>(assessment)
        }

        then:
        maybeAssessment.isSuccess()
    }

    def "updateAssessment: writing assesment with no new domains, i pass"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(2))
        1 * validatorService.validate(dummyAssessments.get(2), Assessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.saveAssessment(_) >> { args ->
            Assessment assessment = args[0]
            assert !assessment.getEnabled()
            return new Try.Success<Assessment>(assessment)
        }

        then:
        maybeAssessment.isSuccess()
    }

    def "updateAssessment: writing assessment with new domains, i pass"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest1)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest1.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(2))
        1 * validatorService.validate(dummyAssessments.get(2), Assessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.saveAssessment(_) >> { args ->
            Assessment assessment = args[0]
            assert !assessment.getEnabled()
            return new Try.Success<Assessment>(assessment)
        }

        then:
        maybeAssessment.isSuccess()
    }

    def "updateAssessment: getAssessment fails, i fail"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Failure<Assessment>(new Exception())
        0 * validatorService.validate(dummyAssessments.get(0), Assessment.class, UpdateGroup.class)
        0 * assessmentRepository.saveAssessment(_)

        then:
        maybeAssessment.isFailure()
    }

    def "updateAssessment: save fails, i fail"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)
        1 * assessmentRepository.saveAssessment(_) >> new Try.Failure<Assessment>(new Exception())

        then:
        maybeAssessment.isFailure()
    }

    def "updateAssessment: validation failure on disabled assessment, i pass"() {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, UpdateGroup.class) >> new Try.Failure<Void>(new Exception())
        1 * assessmentRepository.saveAssessment(_) >> new Try.Success<Void>(null)

        then:
        maybeAssessment.isSuccess()
        !maybeAssessment.get().data.enabled
        !maybeAssessment.get().data.isValid
    }

    def "updateAssessment: validation failure on enabled assessment, i fail"() {
        setup:
        updateAssessmentRequest.enabled = true

        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.updateAssessment(updateAssessmentRequest)

        then:
        1 * assessmentRepository.getAssessment(updateAssessmentRequest.getId()) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * validatorService.validate(dummyAssessments.get(0), Assessment.class, UpdateGroup.class) >> new Try.Failure<Void>(new Exception())
        0 * assessmentRepository.saveAssessment(_)

        then:
        maybeAssessment.isFailure()
    }

    def "uploadLightSideModel: success"() {

        when:
        Try<String> maybeFileName = assessmentService.uploadLightSideModel(dummyMultipartFile)

        then:
        1 * lightSideService.saveUploadedModelFile(_) >> new Try.Success<String>("filename.ext")

        then:
        maybeFileName.isSuccess()
    }

    def "uploadLightSideModel: saveUploadedModelFile failure"() {

        when:
        Try<String> maybeFileName = assessmentService.uploadLightSideModel(dummyMultipartFile)

        then:
        1 * lightSideService.saveUploadedModelFile(_) >> new Try.Failure<Void>(null)

        then:
        maybeFileName.isFailure()
    }


    def "getAssessmentStats: success"() {
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

    def "getAssessmentStats: failure"() {
        when:
        Try<List<AssessmentStatSummary>> maybeAssessments = assessmentService.getAssessmentStats()

        then:
        1 * assessmentRepository.getAssessmentStats() >> new Try.Failure<List<Map>>(new Exception())

        then:
        maybeAssessments.isFailure()
    }

    def "getCategorySummaries: success"() {
        setup:
        Map<String, List<UserAssessment>> userAssessmentsByCategory = dummyUserAssessments.collectEntries {
            [(it.getAssessmentId()): [it]]
        }

        when:
        Try<List<AssessmentCategorySummary>> categorySummaries = assessmentService.getCategorySummaries(dummyUser.getId(), ["groupId1","groupId2"])

        then:
        1 * assessmentCategoryGroupService.getGlobalGroupIds() >> new Try.Success<Try<List<String>>> ([])
        1 * assessmentRepository.getAssessments(true,_) >> new Try.Success<List<Assessment>>(dummyAssessments)
        upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<Map<String, UserAssessment>>(dummyUserAssessments.stream().
                collect(Collectors.toMap(new Function<UserAssessment, String>() {
                    String apply(UserAssessment p) { return p.getAssessmentId() }
                },
                        Function.<UserAssessment> identity())))
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator

        then:
        1 * userAssessmentRepository.getUserAssessmentsByCategory(dummyUser.getId(), _) >> new Try.Success<Map<String, List<UserAssessment>>>(userAssessmentsByCategory);

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

    def "getCategorySummaries: getUserAssessmentsByCategory fails, i fail"() {
        when:
        Try<List<AssessmentCategorySummary>> categorySummaries = assessmentService.getCategorySummaries(dummyUser.getId(), null)

        then:
        1 * assessmentCategoryGroupService.getGlobalGroupIds() >> new Try.Success<Try<List<String>>> ([])
        1 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        1 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<Map<String, UserAssessment>>(dummyUserAssessments.stream().
                collect(Collectors.toMap(new Function<UserAssessment, String>() {
                    String apply(UserAssessment p) { return p.getAssessmentCategoryGroupId() }
                },
                        Function.<UserAssessment> identity())))
        1 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        1 * prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator

        then:
        1 * userAssessmentRepository.getUserAssessmentsByCategory(dummyUser.getId(), _) >> new Try.Failure<Map<AssessmentCategory, List<UserAssessment>>>(new Exception());

        then:
        categorySummaries.isFailure()
    }

    def "getCategorySummaries: getGlobalGroupIds fails, i fail"() {
        when:
        Try<List<AssessmentCategorySummary>> categorySummaries = assessmentService.getCategorySummaries(dummyUser.getId(), null)

        then:
        1 * assessmentCategoryGroupService.getGlobalGroupIds() >> new Try.Failure<Try<List<String>>> (new Exception())
        0 * assessmentRepository.getAssessments(true, _) >> new Try.Success<List<Assessment>>(dummyAssessments)
        upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(_) >> new Try.Success<Assessment>(dummyAssessments.get(0))
        0 * userAssessmentRepository.getLatestUserAssessments(dummyUser.getId(), _) >> new Try.Success<Map<String, UserAssessment>>(dummyUserAssessments.stream().
                collect(Collectors.toMap(new Function<UserAssessment, String>() {
                    String apply(UserAssessment p) { return p.getAssessmentCategoryGroupId() }
                },
                        Function.<UserAssessment> identity())))
        0 * userAssessmentRepository.getUserAssessments(dummyUser.getId()) >> new Try.Success<List<UserAssessment>>(dummyUserAssessments)
        0 * prereqEvaluatorFactory.getAssessmentPrereqEvaluator(_) >> assessmentPrereqEvaluator

        then:
        0 * userAssessmentRepository.getUserAssessmentsByCategory(dummyUser.getId(), _) >> new Try.Success<Map<String, List<UserAssessment>>>(null);
        then:
        categorySummaries.isFailure()
    }

    def "validateAndUpdateAssessment: validation failure"(Assessment assessment, boolean expectedSucess) {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.validateAndUpdateAssessment(assessment, UpdateGroup.class)

        then:
        1 * validatorService.validate(assessment, Assessment.class, UpdateGroup.class) >> new Try.Failure<Void>(new Exception())

        then:
        maybeAssessment.isSuccess() == expectedSucess
        if (maybeAssessment.isSuccess()) {
            !maybeAssessment.get().data.enabled
            !maybeAssessment.get().data.isValid
        }

        where:
        assessment                        | expectedSucess
        new CATAssessment(enabled: true)  | false
        new CATAssessment(enabled: false) | true
    }

    def "validateAndUpdateAssessment: validation success"(Assessment assessment) {
        when:
        Try<AssessmentResponse> maybeAssessment = assessmentService.validateAndUpdateAssessment(assessment, UpdateGroup.class)

        then:
        1 * validatorService.validate(assessment, Assessment.class, UpdateGroup.class) >> new Try.Success<Void>(null)

        then:
        maybeAssessment.isSuccess()
        maybeAssessment.get().data.isValid

        where:
        assessment                        | _
        new CATAssessment(enabled: true)  | _
        new CATAssessment(enabled: false) | _
    }
}
