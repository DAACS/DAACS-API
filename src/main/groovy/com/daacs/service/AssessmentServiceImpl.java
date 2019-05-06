package com.daacs.service;

import com.daacs.component.PrereqEvaluatorFactory;
import com.daacs.component.utils.CategoryGroupUtils;
import com.daacs.component.utils.UpgradeAssessmentSchemaUtils;
import com.daacs.component.prereq.PrereqEvaluator;
import com.daacs.framework.exception.ConstraintViolationException;
import com.daacs.framework.exception.ErrorContainerException;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.framework.serializer.Views;
import com.daacs.framework.validation.annotations.group.CreateGroup;
import com.daacs.framework.validation.annotations.group.UpdateGroup;
import com.daacs.model.ErrorContainer;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.model.assessment.user.UserAssessmentSummary;
import com.daacs.model.dto.AssessmentResponse;
import com.daacs.model.dto.CATItemGroupUpdateAssessmentRequest;
import com.daacs.model.dto.ItemGroupUpdateAssessmentRequest;
import com.daacs.model.dto.UpdateAssessmentRequest;
import com.daacs.model.dto.assessmentUpdate.DomainRequest;
import com.daacs.model.dto.assessmentUpdate.ItemGroupRequest;
import com.daacs.model.dto.assessmentUpdate.ScoringDomainRequest;
import com.daacs.model.item.CATItemGroup;
import com.daacs.model.item.ItemGroup;
import com.daacs.model.prereqs.Prerequisite;
import com.daacs.repository.AssessmentRepository;
import com.daacs.repository.UserAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdista.util.Try;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.fileupload.FileUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/7/16.
 */

@Service
public class AssessmentServiceImpl implements AssessmentService {
    private static final Logger log = LoggerFactory.getLogger(AssessmentServiceImpl.class);

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private UserAssessmentRepository userAssessmentRepository;

    @Autowired
    private AssessmentCategoryGroupService assessmentCategoryGroupService;

    @Autowired
    private DaacsOrikaMapper daacsOrikaMapper;

    @Autowired
    private PrereqEvaluatorFactory prereqEvaluatorFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValidatorService validatorService;

    @Autowired
    private LightSideService lightSideService;

    @Autowired
    private UpgradeAssessmentSchemaUtils upgradeAssessmentSchemaUtils;

    @Autowired
    private CategoryGroupUtils categoryGroupUtils;

    private List<CompletionStatus> validTakenStatuses = new ArrayList<CompletionStatus>() {{
        add(CompletionStatus.COMPLETED);
        add(CompletionStatus.GRADED);
        add(CompletionStatus.GRADING_FAILURE);
    }};

    @Override
    public Try<Assessment> getAssessment(String id) {

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(id);
        if (maybeAssessment.isFailure()) {
            return maybeAssessment;
        }
        return upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(maybeAssessment.get());
    }

    @Override
    public Try<AssessmentResponse> createAssessment(Assessment assessment) {

        Try<AssessmentResponse> maybeAssessmentResponse = validateAndUpdateAssessment(assessment, CreateGroup.class);
        if (maybeAssessmentResponse.isFailure()) {
            return new Try.Failure<>(maybeAssessmentResponse.failed().get());
        }
        AssessmentResponse assessmentResponse = maybeAssessmentResponse.get();

        Try<Void> maybeResults = assessmentRepository.insertAssessment(assessment);
        if (maybeResults.isFailure()) {
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(assessmentResponse);
    }

    @Override
    public Try<Assessment> saveAssessment(Assessment assessment) {
        Try<Void> maybeResults = assessmentRepository.saveAssessment(assessment);
        if (maybeResults.isFailure()) {
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(assessment);
    }

    @Override
    public Try<AssessmentResponse> updateAssessment(UpdateAssessmentRequest updateAssessmentRequest) {

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(updateAssessmentRequest.getId());
        if (maybeAssessment.isFailure()) {
            return new Try.Failure<>(maybeAssessment.failed().get());
        }
        Assessment assessment = maybeAssessment.get();
        daacsOrikaMapper.map(updateAssessmentRequest, assessment);

        //map itemGroups
        if (updateAssessmentRequest instanceof ItemGroupUpdateAssessmentRequest) { //if not a writing assessment
            List<ItemGroupRequest> itemGroupRequests = ((ItemGroupUpdateAssessmentRequest) updateAssessmentRequest).getItemGroups();

            if (itemGroupRequests != null) {

                if (updateAssessmentRequest instanceof CATItemGroupUpdateAssessmentRequest) {
                    List<CATItemGroup> itemGroups = new ArrayList<>();
                    for (ItemGroupRequest itemGroupRequest : itemGroupRequests) {
                        itemGroups.add(daacsOrikaMapper.map(itemGroupRequest, CATItemGroup.class));
                    }
                    ((CATAssessment) assessment).setItemGroups(itemGroups);
                } else {
                    List<ItemGroup> itemGroups = new ArrayList<>();
                    for (ItemGroupRequest itemGroupRequest : itemGroupRequests) {
                        itemGroups.add(daacsOrikaMapper.map(itemGroupRequest, ItemGroup.class));
                    }
                    ((ItemGroupAssessment) assessment).setItemGroups(itemGroups);
                }
            }
        }

        //map domains
        List<DomainRequest> domainRequests = updateAssessmentRequest.getDomains();
        if (domainRequests != null) {
            List<Domain> domains = new ArrayList<>();
            for (DomainRequest domainRequest : domainRequests) {
                if (domainRequest.getDomainType() == DomainType.SCORING) {
                    domains.add(daacsOrikaMapper.map(domainRequest, ScoringDomain.class));
                } else {
                    domains.add(daacsOrikaMapper.map(domainRequest, Domain.class));
                }
            }
            assessment.setDomains(domains);
        }

        //map prerequisite
        List<Prerequisite> prerequisites = updateAssessmentRequest.getPrerequisites();
        if (prerequisites != null) {
            assessment.setPrerequisites(prerequisites);
        }

        //attach new lightSide models to domain if it's a writing assignment
        if (assessment.getScoringType() == ScoringType.LIGHTSIDE) {

            LightSideConfig lightSideConfig;
            LightSideConfig oldLightSideConfig = ((WritingAssessment) assessment).getLightSideConfig();
            if (oldLightSideConfig != null) {
                lightSideConfig = oldLightSideConfig;
            } else {
                lightSideConfig = new LightSideConfig();
            }

            for (DomainRequest domain : domainRequests) {
                if (domain.getLightsideModelFilename() != null) {
                    lightSideConfig.getDomainModels().put(domain.getId(), domain.getLightsideModelFilename());
                }
                if (domain.getDomainType() == DomainType.SCORING) {
                    for (DomainRequest subDomain : ((ScoringDomainRequest) domain).getSubDomains()) {
                        if (subDomain.getLightsideModelFilename() != null) {
                            lightSideConfig.getDomainModels().put(subDomain.getId(), subDomain.getLightsideModelFilename());
                        }
                    }
                }

            }

            ((WritingAssessment) assessment).setLightSideConfig(lightSideConfig);
        }

        Try<AssessmentResponse> maybeAssessmentResponse = validateAndUpdateAssessment(assessment, UpdateGroup.class);
        if (maybeAssessmentResponse.isFailure()) {
            return new Try.Failure<>(maybeAssessmentResponse.failed().get());
        }
        AssessmentResponse assessmentResponse = maybeAssessmentResponse.get();

        Try<Assessment> maybeSavedAssessment = saveAssessment(assessment);
        if (maybeSavedAssessment.isFailure()) {
            return new Try.Failure<>(maybeSavedAssessment.failed().get());
        }

        return new Try.Success<>(assessmentResponse);
    }

    private Try<AssessmentResponse> validateAndUpdateAssessment(Assessment assessment, Class<?> type) {

        List<ErrorContainer> errors = new ArrayList<>();

        Try<Void> maybeValidated = validatorService.validate(assessment, Assessment.class, type);

        if (maybeValidated.isFailure()) {

            if (assessment.getEnabled() != null && assessment.getEnabled()) {
                //if assessment is enabled then validation should trigger failure
                return new Try.Failure<>(maybeValidated.failed().get());
            } else {
                //if assessment is disabled then validation does not trigger failure
                if (maybeValidated.failed().get() instanceof ConstraintViolationException) {
                    errors = ((ErrorContainerException) maybeValidated.failed().get()).getErrorContainers();
                }
                assessment.setIsValid(false);
            }

        } else {
            assessment.setIsValid(true);
        }


        return new Try.Success<>(new AssessmentResponse(assessment, errors));
    }

    @Override
    public Try<String> uploadLightSideModel(MultipartFile file) {

        if (file == null) {
            return new Try.Failure<>(new FileUploadException("uploadLightSideModel no file"));
        }

        //stream file to disk
        Try<String> maybeName = lightSideService.saveUploadedModelFile(file);
        if (maybeName.isFailure()) {
            return new Try.Failure<>(maybeName.failed().get());
        }

        return maybeName;
    }


    private void findDomainsThatRequireLightSideFiles(Set<String> domainIds, List<Domain> domains) {
        domains.stream().forEach(domain -> {
            if (domain instanceof ScoringDomain) {
                ScoringDomain scoringDomain = (ScoringDomain) domain;
                if (!scoringDomain.getScoreIsSubDomainAverage()) {
                    domainIds.add(domain.getId());
                }
                if (scoringDomain.getSubDomains() != null && scoringDomain.getSubDomains().size() > 0) {
                    findDomainsThatRequireLightSideFiles(domainIds, scoringDomain.getSubDomains());
                }
            }
        });
    }

    @Override
    public Try<AssessmentContent> getContent(String assessmentId) {

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(assessmentId);
        if (maybeAssessment.isFailure()) {
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        AssessmentContent assessmentContent = daacsOrikaMapper.map(maybeAssessment.get(), AssessmentContent.class);

        return new Try.Success<>(assessmentContent);
    }

    @Override
    public Try<AssessmentContent> getContentByCategoryGroup(String groupId) {

        Try<List<Assessment>> maybeAssessments = getAssessments(true, Arrays.asList(groupId));
        if (maybeAssessments.isFailure()) {
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<Assessment> assessments = maybeAssessments.get();
        if (assessments.size() == 0) {
            return new Try.Failure<>(new RepoNotFoundException("Assessment"));
        }

        AssessmentContent assessmentContent = daacsOrikaMapper.map(assessments.get(0), AssessmentContent.class);

        return new Try.Success<>(assessmentContent);
    }

    @Override
    public Try<AssessmentContent> getContentForUserAssessment(String userId, String groupId, Instant takenDate) {

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, groupId, takenDate);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessment> userAssessments = maybeUserAssessments.get();
        if (userAssessments.size() == 0) {
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        Try<Assessment> maybeAssessment = getAssessment(userAssessments.get(0).getAssessmentId());
        if (maybeAssessment.isFailure()) {
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Assessment assessment = maybeAssessment.get();

        AssessmentContent assessmentContent = daacsOrikaMapper.map(assessment, AssessmentContent.class);

        return new Try.Success<>(assessmentContent);
    }

    @Override
    public Try<List<Assessment>> getAssessments(Boolean enabled, List<String> groupIds) {

        Try<List<Assessment>> maybeAssessments = assessmentRepository.getAssessments(enabled, groupIds);
        if (maybeAssessments.isFailure()) {
            return maybeAssessments;
        }
        List<Assessment> assessments = maybeAssessments.get();

        for (Assessment assessment : ListUtils.emptyIfNull(assessments)) {

            Try<Assessment> maybeAssessment = upgradeAssessmentSchemaUtils.upgradeAssessmentSchema(assessment);
            if (maybeAssessment.isFailure()) {
                return new Try.Failure<>(maybeAssessment.failed().get());
            }
        }

        return new Try.Success<>(assessments);
    }

    @Override
    public Try<List<AssessmentSummary>> getSummaries(String userId, Boolean enabled, List<String> groupIds) {

        //get assessments by category
        Try<List<Assessment>> maybeAssessments = getAssessments(enabled, groupIds);
        if (maybeAssessments.isFailure()) {
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<String> assessmentIds = maybeAssessments.get().stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());

        Try<Map<String, UserAssessment>> maybeLatestUserAssessments = userAssessmentRepository.getLatestUserAssessments(userId, assessmentIds);
        if (maybeLatestUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeLatestUserAssessments.failed().get());
        }
        Map<String, UserAssessment> latestUserAssessments = maybeLatestUserAssessments.get();

        List<AssessmentSummary> assessmentSummaries = maybeAssessments.get().stream()
                .map(assessment -> {
                    AssessmentSummary assessmentSummary = daacsOrikaMapper.map(assessment, AssessmentSummary.class);

                    UserAssessment myUserAssessment = latestUserAssessments.get(assessment.getId());

                    if (myUserAssessment != null) {
                        UserAssessmentSummary userAssessmentSummary = daacsOrikaMapper.map(myUserAssessment, UserAssessmentSummary.class);
                        assessmentSummary.setUserAssessmentSummary(userAssessmentSummary);
                    }

                    return assessmentSummary;
                })
                .collect(Collectors.toList());

        //get them all for prereq evaluation
        Try<List<UserAssessment>> maybeAllUserAssessments = userAssessmentRepository.getUserAssessments(userId);
        if (maybeAllUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeAllUserAssessments.failed().get());
        }

        PrereqEvaluator prereqEvaluator = prereqEvaluatorFactory.getAssessmentPrereqEvaluator(maybeAllUserAssessments.get());
        for (AssessmentSummary assessmentSummary : assessmentSummaries) {
            prereqEvaluator.evaluatePrereqs(assessmentSummary);
        }

        return new Try.Success<>(assessmentSummaries);
    }

    @Override
    public Try<List<AssessmentCategorySummary>> getCategorySummaries(String userId, List<String> groupIds) {

        if(groupIds == null || groupIds.isEmpty()){
            groupIds = categoryGroupUtils.getDefaultIds();
        }

        //get global category ids and add to groupId list
        Try<List<String>> maybeGlobalIds = assessmentCategoryGroupService.getGlobalGroupIds();
        if (maybeGlobalIds.isFailure()) {
            return new Try.Failure<>(maybeGlobalIds.failed().get());
        }
        groupIds.addAll(maybeGlobalIds.get());

        //get AssessmentSummaries
        Try<List<AssessmentSummary>> maybeAssessmentSummaries = getSummaries(userId, true, groupIds);
        if (maybeAssessmentSummaries.isFailure()) {
            return new Try.Failure<>(maybeAssessmentSummaries.failed().get());
        }

        List<AssessmentSummary> assessmentSummaries = maybeAssessmentSummaries.get();

        //get UserAssessments
        Try<Map<String, List<UserAssessment>>> maybeUserAssessmentsByCategory = userAssessmentRepository.getUserAssessmentsByCategory(userId, groupIds);
        if (maybeUserAssessmentsByCategory.isFailure()) {
            return new Try.Failure<>(maybeUserAssessmentsByCategory.failed().get());
        }

        Map<String, List<UserAssessment>> userAssessmentsByCategory = maybeUserAssessmentsByCategory.get();

        //create AssessmentCategorySummaries
        List<AssessmentCategorySummary> assessmentCategorySummaries = new ArrayList<>();
        for (String groupId : groupIds) {
            Optional<AssessmentSummary> maybeAssessmentSummary = assessmentSummaries.stream()
                    .filter(it -> it.getAssessmentCategoryGroup().getId().equals(groupId))
                    .findFirst();

            if (maybeAssessmentSummary.isPresent()) {
                AssessmentSummary assessmentSummary = maybeAssessmentSummary.get();
                AssessmentCategorySummary assessmentCategorySummary = new AssessmentCategorySummary();
                assessmentCategorySummary.setEnabledAssessmentSummary(assessmentSummary);
                assessmentCategorySummary.setAssessmentCategory(assessmentSummary.getAssessmentCategory());

                List<UserAssessment> userAssessments = userAssessmentsByCategory.get(assessmentSummary.getAssessmentId());
                if (userAssessments != null && userAssessments.size() > 0) {
                    UserAssessment latestUserAssessment = userAssessments.get(0);
                    assessmentCategorySummary.setLatestUserAssessmentSummary(daacsOrikaMapper.map(latestUserAssessment, UserAssessmentSummary.class));

                    List<UserAssessment> completedUserAssessments = userAssessments.stream()
                            .filter(userAssessment -> validTakenStatuses.contains(userAssessment.getStatus()))
                            .collect(Collectors.toList());

                    assessmentCategorySummary.setUserHasTakenCategory(completedUserAssessments.size() > 0);
                }

                assessmentCategorySummary.setAssessmentCategoryGroupId(groupId);
                assessmentCategorySummaries.add(assessmentCategorySummary);
            }
        }

        return new Try.Success<>(assessmentCategorySummaries);
    }

    @Override
    public Try<Void> reloadDummyAssessments() {

        Try<List<Assessment>> maybeAssessments = getAssessments(true, null);
        if (maybeAssessments.isFailure()) {
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<Assessment> assessments = maybeAssessments.get();

        Map<String, Class> pathClassMap = new HashMap<>();
        pathClassMap.put("classpath:/assessment_examples/mathematics.json", Assessment.class);
        pathClassMap.put("classpath:/assessment_examples/reading.json", Assessment.class);
        pathClassMap.put("classpath:/assessment_examples/srl.json", Assessment.class);
        pathClassMap.put("classpath:/assessment_examples/writing.json", Assessment.class);

        for (Map.Entry<String, Class> entry : pathClassMap.entrySet()) {
            Try<?> maybeAssessment = buildAssessmentFromFile(entry.getKey(), entry.getValue());
            if (maybeAssessment.isFailure()) {
                return new Try.Failure<>(maybeAssessment.failed().get());
            }

            Try<AssessmentResponse> maybeCreatedAssessmentResponse = createAssessment((Assessment) maybeAssessment.get());
            if (maybeCreatedAssessmentResponse.isFailure()) {
                return new Try.Failure<>(maybeCreatedAssessmentResponse.failed().get());
            }
        }

        for (Assessment assessment : assessments) {
            assessment.setEnabled(false);
            Try<Assessment> maybeSavedAssessment = saveAssessment(assessment);
            if (maybeSavedAssessment.isFailure()) {
                return new Try.Failure<>(maybeSavedAssessment.failed().get());
            }
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<List<AssessmentStatSummary>> getAssessmentStats() {
        Try<List<Map>> maybeAssessments = assessmentRepository.getAssessmentStats();
        if (maybeAssessments.isFailure()) {
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        Map<String, AssessmentStatSummary> assessmentStatSummaries = new HashMap<>();
        maybeAssessments.get().stream().forEach(assessment -> {
            String assessmentId = assessment.get("assessmentId").toString();
            if (!assessmentStatSummaries.containsKey(assessmentId)) {
                AssessmentStatSummary summary = new AssessmentStatSummary();
                summary.setAssessmentId(assessmentId);
                summary.setAssessmentCategory(AssessmentCategory.valueOf(assessment.get("assessmentCategory").toString()));
                summary.setStat(new ArrayList<>());
                assessmentStatSummaries.put(assessmentId, summary);
            }
            AssessmentStatSummary summary = assessmentStatSummaries.get(assessmentId);
            int total = Integer.parseInt(assessment.get("total").toString());
            summary.setTotal(summary.getTotal() + total);
            AssessmentStat assessmentStat = new AssessmentStat();
            assessmentStat.setCount(total);
            assessmentStat.setCompletionStatus(CompletionStatus.valueOf(assessment.get("status").toString()));
            summary.getStat().add(assessmentStat);
        });

        return new Try.Success<>(assessmentStatSummaries.entrySet().stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()));
    }

    private <T extends Assessment> Try<T> buildAssessmentFromFile(String path, Class<T> valueType) {

        DefaultResourceLoader loader = new DefaultResourceLoader();

        try {
            String json = new String(Files.readAllBytes(Paths.get(loader.getResource(path).getURI())));
            T assessment = objectMapper.readerWithView(Views.Admin.class).forType(valueType).readValue(json);

            Try<Void> maybeValidated = validatorService.validate(assessment, valueType, CreateGroup.class);
            if (maybeValidated.isFailure()) {
                return new Try.Failure<>(maybeValidated.failed().get());
            }

            return new Try.Success<>(assessment);
        } catch (IOException ex) {
            return new Try.Failure<>(ex);
        }
    }
}
