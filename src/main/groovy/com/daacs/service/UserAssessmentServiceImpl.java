package com.daacs.service;

import com.daacs.component.PrereqEvaluatorFactory;
import com.daacs.component.prereq.AssessmentPrereqEvaluator;
import com.daacs.framework.exception.*;
import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.model.User;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.*;
import com.daacs.model.dto.SaveWritingSampleRequest;
import com.daacs.model.dto.UpdateUserAssessmentRequest;
import com.daacs.model.item.CATItemGroup;
import com.daacs.model.item.Item;
import com.daacs.model.item.ItemGroup;
import com.daacs.model.item.WritingPrompt;
import com.daacs.model.prereqs.Prerequisite;
import com.daacs.repository.AssessmentRepository;
import com.daacs.repository.UserAssessmentRepository;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/14/16.
 */

@Service
public class UserAssessmentServiceImpl implements UserAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(UserAssessmentServiceImpl.class);

    @Autowired
    private UserAssessmentRepository userAssessmentRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ScoringService scoringService;

    @Autowired
    private DaacsOrikaMapper daacsOrikaMapper;

    @Autowired
    private PrereqEvaluatorFactory prereqEvaluatorFactory;

    @Autowired
    private CanvasService canvasService;

    @Autowired
    private LtiService ltiService;

    protected static String adminRole = "ROLE_ADMIN";

    private List<CompletionStatus> validTakenStatuses = new ArrayList<CompletionStatus>() {{
        add(CompletionStatus.COMPLETED);
        add(CompletionStatus.GRADED);
        add(CompletionStatus.GRADING_FAILURE);
    }};

    @Override
    public Try<List<UserAssessmentSummary>> getSummaries(String userId, String assessmentId, Instant takenDate) {
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(userId, assessmentId, takenDate);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessmentSummary> userAssessmentSummaries = maybeUserAssessments.get().stream()
                .map(userAssessment -> daacsOrikaMapper.map(userAssessment, UserAssessmentSummary.class))
                .collect(Collectors.toList());

        return new Try.Success<>(userAssessmentSummaries);
    }

    @Override
    public Try<List<UserAssessmentSummary>> getSummariesByGroup(String userId, String groupId, Instant takenDate) {
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessmentsByGroupId(userId, groupId, takenDate);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessmentSummary> userAssessmentSummaries = maybeUserAssessments.get().stream()
                .map(userAssessment -> daacsOrikaMapper.map(userAssessment, UserAssessmentSummary.class))
                .collect(Collectors.toList());

        return new Try.Success<>(userAssessmentSummaries);
    }

    @Override
    public Try<List<UserAssessmentSummary>> getSummaries(List<CompletionStatus> statuses, List<ScoringType> scoringTypes, String userId, int limit, int offset) {
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(statuses, scoringTypes, userId, limit, offset);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessmentSummary> userAssessmentSummaries = maybeUserAssessments.get().stream()
                .map(userAssessment -> daacsOrikaMapper.map(userAssessment, UserAssessmentSummary.class))
                .collect(Collectors.toList());

        return new Try.Success<>(userAssessmentSummaries);
    }

    @Override
    public Try<List<UserAssessmentSummary>> getCompletedUserAssessmentSummaries(Instant startDate, Instant endDate) {
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getCompletedUserAssessments(startDate, endDate);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessmentSummary> userAssessmentSummaries = maybeUserAssessments.get().stream()
                .map(userAssessment -> daacsOrikaMapper.map(userAssessment, UserAssessmentSummary.class))
                .collect(Collectors.toList());

        return new Try.Success<>(userAssessmentSummaries);
    }

    @Override
    public Try<List<UserAssessment>> gradeUserAssessments(AssessmentCategory[] assessmentCategories, CompletionStatus completionStatus, Instant startDate, Instant endDate, boolean dryRun) {

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(assessmentCategories, completionStatus, startDate, endDate);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessment> userAssessments = maybeUserAssessments.get();
        log.info("Submitting user assessments for regrading. dryRun=" + dryRun);

        for (UserAssessment userAssessment : userAssessments) {

            log.info(userAssessment.toString());
            if (!dryRun) {
                if (completionStatus == CompletionStatus.GRADED) {

                    //previously graded assessments only need to be queued
                    Try<Void> maybeQueueResults = messageService.queueUserAssessmentForGrading(userAssessment);
                    if (maybeQueueResults.isFailure()) {
                        return new Try.Failure<>(maybeQueueResults.failed().get());

                    }
                } else if (completionStatus == CompletionStatus.COMPLETED) {

                    //ungraded assessments can follow normal flow for grading
                    UpdateUserAssessmentRequest assessmentRequest = new UpdateUserAssessmentRequest();
                    assessmentRequest.setId(userAssessment.getId());
                    assessmentRequest.setStatus(completionStatus);
                    assessmentRequest.setUserId(userAssessment.getUserId());
                    assessmentRequest.setDomainScores(userAssessment.getDomainScores());
                    assessmentRequest.setOverallScore(userAssessment.getOverallScore());

                    Try<UserAssessment> maybeUpdateUserAssessment = updateUserAssessment(userAssessment.getUserId(), null, assessmentRequest);
                    if (maybeUpdateUserAssessment.isFailure()) {
                        return new Try.Failure<>(maybeUpdateUserAssessment.failed().get());
                    }

                } else {
                    return new Try.Failure<>(new BadInputException("UserAssessment", "Invalid completionStatus [" + completionStatus + "]"));
                }
            }
        }

        return new Try.Success<>(userAssessments);
    }

    @Override
    public Try<UserAssessmentSummary> getLatestSummary(String userId, String assessmentId) {
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId);
        if (maybeUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        return new Try.Success<>(daacsOrikaMapper.map(maybeUserAssessment.get(), UserAssessmentSummary.class));
    }

    @Override
    public Try<UserAssessmentSummary> getLatestSummaryByGroup(String userId, String groupId) {
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessmentByGroup(userId, groupId);
        if (maybeUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        return new Try.Success<>(daacsOrikaMapper.map(maybeUserAssessment.get(), UserAssessmentSummary.class));
    }

    @Override
    public Try<UserAssessment> createUserAssessment(User user, String assessmentId) {
        Try<List<Assessment>> maybeAssessments = assessmentRepository.getAssessments(true, null);
        if (maybeAssessments.isFailure()) {
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        Optional<Assessment> maybeAssessment = maybeAssessments.get().stream()
                .filter(assessment -> assessment.getId().equals(assessmentId))
                .findFirst();

        if (!maybeAssessment.isPresent()) {
            return new Try.Failure<>(new RepoNotFoundException("Assessment"));
        }

        Assessment assessment = maybeAssessment.get();

        List<String> assessmentIds = maybeAssessments.get().stream()
                .map(Assessment::getId)
                .collect(Collectors.toList());

        Try<Map<String, UserAssessment>> maybeLatestUserAssessments = userAssessmentRepository.getLatestUserAssessments(user.getId(), assessmentIds);
        if (maybeLatestUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeLatestUserAssessments.failed().get());
        }

        Map<String, UserAssessment> latestUserAssessments = maybeLatestUserAssessments.get();

        UserAssessment latestUserAssessment = latestUserAssessments.get(assessmentId);
        if (latestUserAssessment != null) {

            if (latestUserAssessment.getStatus() == CompletionStatus.IN_PROGRESS ||
                    latestUserAssessment.getStatus() == CompletionStatus.COMPLETED) {
                return new Try.Success<>(latestUserAssessment);
            }
        }

        //get them all for prereq evaluation
        Try<List<UserAssessment>> maybeAllUserAssessments = userAssessmentRepository.getUserAssessments(user.getId());
        if (maybeAllUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeAllUserAssessments.failed().get());
        }

        AssessmentPrereqEvaluator prereqEvaluator = prereqEvaluatorFactory.getAssessmentPrereqEvaluator(maybeAllUserAssessments.get());
        List<Prerequisite> failedPrereqs = prereqEvaluator.getFailedPrereqs(assessment);
        if (failedPrereqs.size() > 0) {
            return new Try.Failure<>(new FailedPrereqException(assessmentId, failedPrereqs));
        }

        UserAssessment userAssessment;

        switch (assessment.getAssessmentType()) {
            case CAT:
                userAssessment = daacsOrikaMapper.map(assessment, CATUserAssessment.class);
                break;

            case LIKERT:
            case MULTIPLE_CHOICE:
                userAssessment = daacsOrikaMapper.map(assessment, MultipleChoiceUserAssessment.class);
                break;

            case WRITING_PROMPT:
                userAssessment = daacsOrikaMapper.map(assessment, WritingPromptUserAssessment.class);
                break;

            default:
                return new Try.Failure<>(new IncompatibleTypeException(
                        "Assessment",
                        new AssessmentType[]{AssessmentType.LIKERT, AssessmentType.MULTIPLE_CHOICE, AssessmentType.CAT, AssessmentType.WRITING_PROMPT},
                        assessment.getAssessmentType()));
        }

        daacsOrikaMapper.map(user, userAssessment);
        userAssessment.setAssessmentCategoryGroupId(assessment.getAssessmentCategoryGroup().getId());

        Try<Void> maybeResults = userAssessmentRepository.insertUserAssessment(userAssessment);
        if (maybeResults.isFailure()) {
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(userAssessment);
    }

    @Override
    public Try<List<ItemGroup>> getAnswers(String userId, String assessmentId, String domainId, Instant takenDate) {

        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessment(userId, assessmentId, takenDate);
        if (maybeUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        UserAssessment userAssessment = maybeUserAssessment.get();
        if (userAssessment.getStatus() == CompletionStatus.IN_PROGRESS) {
            return new Try.Failure<>(new IncompatibleStatusException("UserAssessment", new CompletionStatus[]{CompletionStatus.IN_PROGRESS}, userAssessment.getStatus(), userAssessment.getId()));
        }

        List<ItemGroup> itemGroups = null;

        if (userAssessment instanceof CATUserAssessment) {
            List<CATItemGroup> catItemGroups = ((CATUserAssessment) userAssessment).getItemGroups();

            itemGroups = catItemGroups.stream()
                    .map(catItemGroup -> (ItemGroup) catItemGroup)
                    .collect(Collectors.toList());
        }

        if (userAssessment instanceof MultipleChoiceUserAssessment) {
            itemGroups = ((MultipleChoiceUserAssessment) userAssessment).getItemGroups();
        }


        if (itemGroups != null) {
            itemGroups.forEach(itemGroup -> {
                List<Item> items = itemGroup.getItems().stream()
                        .filter(item -> item.getDomainId().equals(domainId))
                        .collect(Collectors.toList());

                itemGroup.setItems(items);
            });

            itemGroups = itemGroups.stream()
                    .filter(itemGroup -> itemGroup.getItems().size() > 0)
                    .collect(Collectors.toList());

            return new Try.Success<>(itemGroups);
        }

        return new Try.Failure<>(new IncompatibleTypeException(
                "UserAssessment",
                new AssessmentType[]{AssessmentType.LIKERT, AssessmentType.MULTIPLE_CHOICE, AssessmentType.CAT},
                userAssessment.getAssessmentType()));
    }


    @Override
    public Try<WritingPrompt> getWritingSample(String userId, String assessmentId, Instant takenDate) {

        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessment(userId, assessmentId, takenDate);
        if (maybeUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        if (maybeUserAssessment.get().getAssessmentType() != AssessmentType.WRITING_PROMPT) {
            return new Try.Failure<>(new IncompatibleTypeException(
                    "UserAssessment",
                    new AssessmentType[]{AssessmentType.WRITING_PROMPT},
                    maybeUserAssessment.get().getAssessmentType()));
        }

        WritingPrompt writingPrompt = ((WritingPromptUserAssessment) maybeUserAssessment.get()).getWritingPrompt();
        return new Try.Success<>(writingPrompt);
    }


    @Override
    public Try<List<UserAssessmentTakenDate>> getTakenDates(String userId, String groupId) {
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessmentsByGroupId(userId, groupId, null);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessmentTakenDate> takenDates = maybeUserAssessments.get().stream()
                .filter(userAssessment -> validTakenStatuses.contains(userAssessment.getStatus()))
                .map(userAssessment -> {
                    UserAssessmentTakenDate takenDate = new UserAssessmentTakenDate();
                    takenDate.setAssessmentId(userAssessment.getAssessmentId());
                    takenDate.setTakenDate(userAssessment.getTakenDate());
                    takenDate.setUserId(userId);

                    return takenDate;
                })
                .collect(Collectors.toList());

        return new Try.Success<>(takenDates);
    }

    @Override
    public Try<WritingPrompt> saveWritingSample(String userId, String assessmentId, SaveWritingSampleRequest saveWritingSampleRequest) {
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId);
        if (maybeUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        if (maybeUserAssessment.get().getAssessmentType() != AssessmentType.WRITING_PROMPT) {
            return new Try.Failure<>(new IncompatibleTypeException(
                    "UserAssessment",
                    new AssessmentType[]{AssessmentType.WRITING_PROMPT},
                    maybeUserAssessment.get().getAssessmentType()));
        }

        if (maybeUserAssessment.get().getStatus() != CompletionStatus.IN_PROGRESS) {
            return new Try.Failure<>(new IncompatibleStatusException("UserAssessment", new CompletionStatus[]{CompletionStatus.IN_PROGRESS}, maybeUserAssessment.get().getStatus(), maybeUserAssessment.get().getId()));
        }

        WritingPromptUserAssessment userAssessment = (WritingPromptUserAssessment) maybeUserAssessment.get();

        WritingPrompt writingPrompt = userAssessment.getWritingPrompt();
        if (writingPrompt == null) {
            Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(assessmentId);
            if (maybeAssessment.isFailure()) {
                return new Try.Failure<>(maybeAssessment.failed().get());
            }

            writingPrompt = ((WritingAssessment) maybeAssessment.get()).getWritingPrompt();
        }

        daacsOrikaMapper.map(saveWritingSampleRequest, writingPrompt);

        userAssessment.setWritingPrompt(writingPrompt);

        Try<Void> maybeResults = userAssessmentRepository.saveUserAssessment(userAssessment);
        if (maybeResults.isFailure()) {
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(userAssessment.getWritingPrompt());
    }

    @Override
    public Try<UserAssessment> updateUserAssessment(String userId, List<String> actingUserRoles, UpdateUserAssessmentRequest updateUserAssessmentRequest) {

        Try<UserAssessment> maybeUpdatedUserAssessment = null;
        boolean queueForAutoGrade = false;
        switch (updateUserAssessmentRequest.getStatus()) {
            case COMPLETED:

                Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getUserAssessmentById(userId, updateUserAssessmentRequest.getId());
                if (maybeUserAssessment.isFailure()) {
                    return new Try.Failure<>(maybeUserAssessment.failed().get());
                }

                UserAssessment userAssessment = maybeUserAssessment.get();
                userAssessment.setProgressPercentage(1.0);
                userAssessment.setCompletionDate(Instant.now());
                userAssessment.setStatus(CompletionStatus.COMPLETED);

                boolean canAutoGrade = scoringService.canAutoGradeFromUpdate(userAssessment);

                if (canAutoGrade) {
                    maybeUpdatedUserAssessment = scoringService.autoGradeUserAssessment(userAssessment);
                }

                queueForAutoGrade = (!canAutoGrade || maybeUpdatedUserAssessment.isFailure());

                if (queueForAutoGrade) {
                    maybeUpdatedUserAssessment = new Try.Success<>(userAssessment);
                }

                break;

            case GRADED:
                if (!actingUserRoles.contains(adminRole)) {
                    return new Try.Failure<>(new InsufficientPermissionsException("User"));
                }

                if (updateUserAssessmentRequest.getUserId() == null
                        || updateUserAssessmentRequest.getDomainScores() == null) {
                    return new Try.Failure<>(new BadInputException("UserAssessment", "UserId and domainScores must be provided"));
                }

                maybeUpdatedUserAssessment = scoringService.manualGradeUserAssessment(
                        updateUserAssessmentRequest.getUserId(),
                        updateUserAssessmentRequest.getId(),
                        updateUserAssessmentRequest.getDomainScores(),
                        updateUserAssessmentRequest.getOverallScore());

                if (maybeUpdatedUserAssessment.isFailure()) {
                    return new Try.Failure<>(maybeUpdatedUserAssessment.failed().get());
                }

                break;

            default:
                return new Try.Failure<>(new BadInputException("UserAssessment", "Invalid completionStatus"));
        }

        UserAssessment updatedUserAssessment = maybeUpdatedUserAssessment.get();

        Try<Void> maybeSavedResults = userAssessmentRepository.saveUserAssessment(updatedUserAssessment);
        if (maybeSavedResults.isFailure()) {
            return new Try.Failure<>(maybeSavedResults.failed().get());
        }

        if (queueForAutoGrade) {
            Try<Void> maybeQueueResults = messageService.queueUserAssessmentForGrading(updatedUserAssessment);
            if (maybeQueueResults.isFailure()) {
                return new Try.Failure<>(maybeQueueResults.failed().get());
            }
        }

        if (ltiService.isEnabled()) {

            Try<CompletionSummary> maybeCompletionSummary = getCompletionSummary(userId);
            if (maybeCompletionSummary.isFailure()) {
                return new Try.Failure<>(maybeCompletionSummary.failed().get());
            }

            if (maybeCompletionSummary.get().getHasCompletedAllCategories()) {
                Try<Void> maybeLtiUpdateGrades = ltiService.updateGrades(userId);

                if (maybeLtiUpdateGrades.isFailure()) {
                    return new Try.Failure<>(maybeLtiUpdateGrades.failed().get());
                }
            }
        } else if (canvasService.isEnabled()) {
            Try<Void> maybeQueueCanvasSubmissionUpdate = messageService.queueCanvasSubmissionUpdate(updatedUserAssessment.getUserId());
            if (maybeQueueCanvasSubmissionUpdate.isFailure()) {
                return new Try.Failure<>(maybeQueueCanvasSubmissionUpdate.failed().get());
            }
        }

        return maybeUpdatedUserAssessment;
    }

    @Override
    public Try<UserAssessment> getUserAssessment(String userId, String userAssessmentId) {
        return userAssessmentRepository.getUserAssessmentById(userId, userAssessmentId);
    }

    @Override
    public Try<Void> saveUserAssessment(UserAssessment userAssessment) {
        return userAssessmentRepository.saveUserAssessment(userAssessment);
    }

    @Override
    public Try<WritingPrompt> getWritingPrompt(String userId, String assessmentId) {
        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId);
        if (maybeUserAssessment.isFailure()) {
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        UserAssessment userAssessment = maybeUserAssessment.get();

        if (userAssessment.getAssessmentType() != AssessmentType.WRITING_PROMPT) {
            return new Try.Failure<>(new IncompatibleTypeException("UserAssessment", new AssessmentType[]{AssessmentType.WRITING_PROMPT}, userAssessment.getAssessmentType()));
        }

        if (userAssessment.getStatus() != CompletionStatus.IN_PROGRESS) {
            return new Try.Failure<>(new IncompatibleStatusException("UserAssessment", new CompletionStatus[]{CompletionStatus.IN_PROGRESS}, userAssessment.getStatus(), userAssessment.getId()));
        }

        WritingPrompt writingPrompt = ((WritingPromptUserAssessment) userAssessment).getWritingPrompt();

        if (writingPrompt != null) {
            return new Try.Success<>(writingPrompt);
        }

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(assessmentId);
        if (maybeAssessment.isFailure()) {
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        return new Try.Success<>(((WritingAssessment) maybeAssessment.get()).getWritingPrompt());
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessmentsForManualGrading() {
        List<ScoringType> scoringTypes = new ArrayList<ScoringType>() {{
            add(ScoringType.MANUAL);
        }};

        Try<List<Assessment>> maybeAssessments = assessmentRepository.getAssessments(scoringTypes, true);
        if (maybeAssessments.isFailure()) {
            return new Try.Failure<>(maybeAssessments.failed().get());
        }

        List<CompletionStatus> statuses = new ArrayList<CompletionStatus>() {{
            add(CompletionStatus.COMPLETED);
        }};

        List<String> manuallyGradableAssessmentIds = maybeAssessments.get().stream()
                .map(assessment -> assessment.getId())
                .collect(Collectors.toList());

        return userAssessmentRepository.getUserAssessments(statuses, manuallyGradableAssessmentIds);
    }

    @Override
    public Try<CompletionSummary> getCompletionSummary(String userId) {

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessments(Arrays.asList(CompletionStatus.COMPLETED, CompletionStatus.GRADED, CompletionStatus.GRADING_FAILURE), null, userId, null, null);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        Set<AssessmentCategory> completedAssessmentCategories = maybeUserAssessments.get().stream()
                .map(UserAssessment::getAssessmentCategory).collect(Collectors.toSet());

        CompletionSummary completionSummary = new CompletionSummary();
        completionSummary.setUserId(userId);
        completionSummary.setHasCompletedAllCategories(completedAssessmentCategories.containsAll(Arrays.asList(AssessmentCategory.values())));

        return new Try.Success<>(completionSummary);
    }

    @Override
    public Try<List<UserAssessment>> getUserAssessmentsByAssessmentId(String assessmentId) {
        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentRepository.getUserAssessmentsByAssessmentId(assessmentId);
        return maybeUserAssessments;
    }

    @Override
    public Try<Void> bulkUserAssessmentSave(List<UserAssessment> userAssessments) {

        for (UserAssessment userAssessment : userAssessments) {
            Try<Void> maybeSaved = userAssessmentRepository.saveUserAssessment(userAssessment);
            if (maybeSaved.isFailure()) {
                return maybeSaved;
            }
        }
        return new Try.Success<>(null);
    }
}
