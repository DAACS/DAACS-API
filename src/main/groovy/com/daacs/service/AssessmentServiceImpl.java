package com.daacs.service;

import com.daacs.component.PrereqEvaluatorFactory;
import com.daacs.component.prereq.PrereqEvaluator;
import com.daacs.framework.exception.IncompatibleTypeException;
import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.framework.exception.RepoNotFoundException;
import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.framework.serializer.Views;
import com.daacs.framework.validation.annotations.group.CreateGroup;
import com.daacs.framework.validation.annotations.group.UpdateGroup;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.model.assessment.user.UserAssessmentSummary;
import com.daacs.model.dto.UpdateAssessmentRequest;
import com.daacs.model.dto.UpdateLightSideModelsRequest;
import com.daacs.repository.AssessmentRepository;
import com.daacs.repository.UserAssessmentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lambdista.util.Try;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.util.Streams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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
    private DaacsOrikaMapper daacsOrikaMapper;

    @Autowired
    private PrereqEvaluatorFactory prereqEvaluatorFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValidatorService validatorService;

    @Autowired
    private LightSideService lightSideService;

    private List<CompletionStatus> validTakenStatuses = new ArrayList<CompletionStatus>(){{
        add(CompletionStatus.COMPLETED);
        add(CompletionStatus.GRADED);
        add(CompletionStatus.GRADING_FAILURE);
    }};

    @Override
    public Try<Assessment> getAssessment(String id) {
        return assessmentRepository.getAssessment(id);
    }

    @Override
    public Try<Assessment> createAssessment(Assessment assessment) {
        Try<Void> maybeResults = assessmentRepository.insertAssessment(assessment);
        if(maybeResults.isFailure()){
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(assessment);
    }

    @Override
    public Try<Assessment> saveAssessment(Assessment assessment) {
        Try<Void> maybeResults = assessmentRepository.saveAssessment(assessment);
        if(maybeResults.isFailure()){
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(assessment);
    }

    @Override
    public Try<Assessment> updateAssessment(UpdateAssessmentRequest updateAssessmentRequest){

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(updateAssessmentRequest.getId());
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Assessment assessment = maybeAssessment.get();

        daacsOrikaMapper.map(updateAssessmentRequest, assessment);

        Try<Assessment> maybeSavedAssessment = saveAssessment(assessment);
        if(maybeSavedAssessment.isFailure()){
            return new Try.Failure<>(maybeSavedAssessment.failed().get());
        }

        return new Try.Success<>(maybeSavedAssessment.get());
    }

    @Override
    public Try<Assessment> updateWritingAssessment(UpdateLightSideModelsRequest updateLightSideModelsRequest){

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(updateLightSideModelsRequest.getAssessmentId());
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Assessment assessment = maybeAssessment.get();
        if(assessment.getAssessmentType() != AssessmentType.WRITING_PROMPT){
            return new Try.Failure<>(new IncompatibleTypeException("Assessment", new AssessmentType[]{ AssessmentType.WRITING_PROMPT }, assessment.getAssessmentType()));
        }

        Set<String> domainIds = new HashSet<>();
        findDomainsThatRequireLightSideFiles(domainIds, assessment.getDomains());

        LightSideConfig lightSideConfig = new LightSideConfig();
        FileItemIterator fileItemIterator = updateLightSideModelsRequest.getFileItemIterator();

        try {
            while(fileItemIterator.hasNext()) {
                //iterate over all POSTed fields, write files and pull out other data
                FileItemStream fileItem = fileItemIterator.next();
                String fieldName = fileItem.getFieldName();

                if(fileItem.isFormField()) {
                    //normal form field
                    if(fieldName.equals("scoringType")){
                        InputStream stream = fileItem.openStream();
                        String value = Streams.asString(stream);
                        stream.close();

                        assessment.setScoringType(ScoringType.valueOf(value));
                    }

                }
                else {
                    if(assessment.getScoringType() != ScoringType.LIGHTSIDE){
                        return new Try.Failure<>(new IncompatibleTypeException("Assessment", new ScoringType[]{ ScoringType.LIGHTSIDE }, assessment.getScoringType()));
                    }

                    String domainId = fieldName.replace("lightside_", "");
                    if(!domainIds.contains(domainId)){
                        return new Try.Failure<>(new InvalidObjectException(
                                "UpdateLightSideModelsRequest",
                                "must only contain file for each domain model " + domainIds));
                    }

                    //this is a file, stream to disk
                    Try<Void> maybeSaved = lightSideService.saveUploadedModelFile(fileItem);
                    if (maybeSaved.isFailure()) {
                        return new Try.Failure<>(maybeSaved.failed().get());
                    }

                    lightSideConfig.getDomainModels().put(domainId, fileItem.getName());
                }

            }
        }
        catch(FileUploadException ex){
            return new Try.Failure<>(ex);
        }
        catch(IOException ex){
            return new Try.Failure<>(ex);
        }

        if(assessment.getScoringType() == ScoringType.LIGHTSIDE){
            ((WritingAssessment) assessment).setLightSideConfig(lightSideConfig);
        }
        else{
            ((WritingAssessment) assessment).setLightSideConfig(null);
        }

        Try<Void> maybeValidated = validatorService.validate(assessment, WritingAssessment.class, UpdateGroup.class);
        if(maybeValidated.isFailure()){
            return new Try.Failure<>(maybeValidated.failed().get());
        }

        Try<Assessment> maybeUpdatedAssessment = saveAssessment(assessment);
        if(maybeUpdatedAssessment.isFailure()){
            return new Try.Failure<>(maybeUpdatedAssessment.failed().get());
        }

        return new Try.Success<>(maybeUpdatedAssessment.get());
    }

    private void findDomainsThatRequireLightSideFiles(Set<String> domainIds, List<Domain> domains) {
        domains.stream().forEach(domain -> {
            if (domain instanceof ScoringDomain) {
                ScoringDomain scoringDomain = (ScoringDomain) domain;
                if(!scoringDomain.getScoreIsSubDomainAverage()) {
                    domainIds.add(domain.getId());
                }
                if (scoringDomain.getSubDomains() != null && scoringDomain.getSubDomains().size() > 0) {
                    findDomainsThatRequireLightSideFiles(domainIds, scoringDomain.getSubDomains());
                }
            }
        });
    }

    @Override
    public Try<AssessmentContent> getContent(String assessmentId){

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(assessmentId);
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        AssessmentContent assessmentContent = daacsOrikaMapper.map(maybeAssessment.get(), AssessmentContent.class);

        return new Try.Success<>(assessmentContent);
    }

    @Override
    public Try<AssessmentContent> getContent(AssessmentCategory assessmentCategory){

        Try<List<Assessment>> maybeAssessments = getAssessments(true, Arrays.asList(assessmentCategory));
        if(maybeAssessments.isFailure()){
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<Assessment> assessments = maybeAssessments.get();
        if(assessments.size() == 0){
            return new Try.Failure<>(new RepoNotFoundException("Assessment"));
        }

        AssessmentContent assessmentContent = daacsOrikaMapper.map(assessments.get(0), AssessmentContent.class);

        return new Try.Success<>(assessmentContent);
    }

    @Override
    public Try<AssessmentContent> getContentForUserAssessment(String userId, AssessmentCategory assessmentCategory, Instant takenDate){

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, assessmentCategory, takenDate);
        if(maybeUserAssessments.isFailure()){
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessment> userAssessments = maybeUserAssessments.get();
        if(userAssessments.size() == 0){
            return new Try.Failure<>(new RepoNotFoundException("UserAssessment"));
        }

        Try<Assessment> maybeAssessment = getAssessment(userAssessments.get(0).getAssessmentId());
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Assessment assessment = maybeAssessment.get();

        AssessmentContent assessmentContent = daacsOrikaMapper.map(assessment, AssessmentContent.class);

        return new Try.Success<>(assessmentContent);
    }


    @Override
    public Try<List<Assessment>> getAssessments(Boolean enabled, List<AssessmentCategory> assessmentCategories){
        return assessmentRepository.getAssessments(enabled, assessmentCategories);
    }

    @Override
    public Try<List<AssessmentSummary>> getSummaries(String userId, Boolean enabled, List<AssessmentCategory> assessmentCategories){

        Try<List<Assessment>> maybeAssessments = getAssessments(enabled, assessmentCategories);
        if(maybeAssessments.isFailure()){
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<String> assessmentIds = maybeAssessments.get().stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());

        Try<List<UserAssessment>> maybeLatestUserAssessments = userAssessmentRepository.getLatestUserAssessments(userId, assessmentIds);
        if(maybeLatestUserAssessments.isFailure()){
            return new Try.Failure<>(maybeLatestUserAssessments.failed().get());
        }

        List<AssessmentSummary> assessmentSummaries = maybeAssessments.get().stream()
                .map(assessment -> {
                    AssessmentSummary assessmentSummary = daacsOrikaMapper.map(assessment, AssessmentSummary.class);

                    Optional<UserAssessment> myUserAssessment = maybeLatestUserAssessments.get().stream()
                            .filter(userAssessment -> userAssessment.getAssessmentId().equals(assessment.getId()))
                            .findFirst();

                    if(myUserAssessment.isPresent()){
                        UserAssessmentSummary userAssessmentSummary = daacsOrikaMapper.map(myUserAssessment.get(), UserAssessmentSummary.class);
                        assessmentSummary.setUserAssessmentSummary(userAssessmentSummary);
                    }

                    return assessmentSummary;
                })
                .collect(Collectors.toList());

        //get them all for prereq evaluation
        Try<List<UserAssessment>> maybeAllUserAssessments = userAssessmentRepository.getUserAssessments(userId);
        if(maybeAllUserAssessments.isFailure()){
            return new Try.Failure<>(maybeAllUserAssessments.failed().get());
        }

        PrereqEvaluator prereqEvaluator = prereqEvaluatorFactory.getAssessmentPrereqEvaluator(maybeAllUserAssessments.get());
        for(AssessmentSummary assessmentSummary : assessmentSummaries){
            prereqEvaluator.evaluatePrereqs(assessmentSummary);
        }

        return new Try.Success<>(assessmentSummaries);
    }

    @Override
    public Try<List<AssessmentCategorySummary>> getCategorySummaries(String userId, List<AssessmentCategory> assessmentCategories){

        Try<List<AssessmentSummary>> maybeAssessmentSummaries = getSummaries(userId, true, assessmentCategories);
        if(maybeAssessmentSummaries.isFailure()){
            return new Try.Failure<>(maybeAssessmentSummaries.failed().get());
        }

        Try<Map<AssessmentCategory, List<UserAssessment>>> maybeUserAssessmentsByCategory = userAssessmentRepository.getUserAssessmentsByCategory(userId);
        if(maybeUserAssessmentsByCategory.isFailure()){
            return new Try.Failure<>(maybeUserAssessmentsByCategory.failed().get());
        }

        Map<AssessmentCategory, List<UserAssessment>> userAssessmentsByCategory = maybeUserAssessmentsByCategory.get();

        List<AssessmentCategorySummary> assessmentCategorySummaries = new ArrayList<>();
        for(AssessmentCategory assessmentCategory : assessmentCategories){
            Optional<AssessmentSummary> maybeAssessmentSummary = maybeAssessmentSummaries.get().stream()
                    .filter(it -> it.getAssessmentCategory().equals(assessmentCategory))
                    .findFirst();

            if(maybeAssessmentSummary.isPresent()){
                AssessmentCategorySummary assessmentCategorySummary = new AssessmentCategorySummary();
                assessmentCategorySummary.setAssessmentCategory(assessmentCategory);
                assessmentCategorySummary.setEnabledAssessmentSummary(maybeAssessmentSummary.get());

                List<UserAssessment> userAssessments = userAssessmentsByCategory.get(assessmentCategory);
                if(userAssessments != null && userAssessments.size() > 0){
                    UserAssessment latestUserAssessment = userAssessments.get(0);
                    assessmentCategorySummary.setLatestUserAssessmentSummary(daacsOrikaMapper.map(latestUserAssessment, UserAssessmentSummary.class));

                    List<UserAssessment> completedUserAssessments = userAssessments.stream()
                            .filter(userAssessment -> validTakenStatuses.contains(userAssessment.getStatus()))
                            .collect(Collectors.toList());

                    assessmentCategorySummary.setUserHasTakenCategory(completedUserAssessments.size() > 0);
                }

                assessmentCategorySummaries.add(assessmentCategorySummary);
            }
        }

        return new Try.Success<>(assessmentCategorySummaries);
    }

    @Override
    public Try<Void> reloadDummyAssessments(){

        Try<List<Assessment>> maybeAssessments = getAssessments(true, Arrays.asList(AssessmentCategory.class.getEnumConstants()));
        if(maybeAssessments.isFailure()){
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<Assessment> assessments = maybeAssessments.get();

        Map<String, Class> pathClassMap = new HashMap<>();
        pathClassMap.put("classpath:/assessment_examples/mathematics.json", Assessment.class);
        pathClassMap.put("classpath:/assessment_examples/reading.json", Assessment.class);
        pathClassMap.put("classpath:/assessment_examples/srl.json", Assessment.class);
        pathClassMap.put("classpath:/assessment_examples/writing.json", Assessment.class);

        for(Map.Entry<String, Class> entry : pathClassMap.entrySet()){
            Try<?> maybeAssessment = buildAssessmentFromFile(entry.getKey(), entry.getValue());
            if(maybeAssessment.isFailure()){
                return new Try.Failure<>(maybeAssessment.failed().get());
            }

            Try<Assessment> maybeCreatedAssessment = createAssessment((Assessment) maybeAssessment.get());
            if(maybeCreatedAssessment.isFailure()){
                return new Try.Failure<>(maybeCreatedAssessment.failed().get());
            }
        }

        for(Assessment assessment : assessments){
            assessment.setEnabled(false);
            Try<Assessment> maybeSavedAssessment = saveAssessment(assessment);
            if(maybeSavedAssessment.isFailure()){
                return new Try.Failure<>(maybeSavedAssessment.failed().get());
            }
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<List<AssessmentStatSummary>> getAssessmentStats() {
        Try<List<Map>> maybeAssessments = assessmentRepository.getAssessmentStats();
        if (maybeAssessments.isFailure()){
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

    private <T extends Assessment> Try<T> buildAssessmentFromFile(String path, Class<T> valueType){

        DefaultResourceLoader loader = new DefaultResourceLoader();

        try{
            String json = new String(Files.readAllBytes(Paths.get(loader.getResource(path).getURI())));
            T assessment = objectMapper.readerWithView(Views.Admin.class).forType(valueType).readValue(json);

            Try<Void> maybeValidated = validatorService.validate(assessment, valueType, CreateGroup.class);
            if(maybeValidated.isFailure()){
                return new Try.Failure<>(maybeValidated.failed().get());
            }

            return new Try.Success<>(assessment);
        }
        catch(IOException ex){
            return new Try.Failure<>(ex);
        }
    }
}
