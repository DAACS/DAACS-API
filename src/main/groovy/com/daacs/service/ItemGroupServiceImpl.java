package com.daacs.service;

import com.daacs.framework.exception.*;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.*;
import com.daacs.model.item.*;
import com.daacs.repository.AssessmentRepository;
import com.daacs.repository.UserAssessmentRepository;
import com.google.common.collect.Range;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by chostetter on 7/21/16.
 */

@Service
public class ItemGroupServiceImpl implements ItemGroupService {

    private static final Logger log = LoggerFactory.getLogger(ItemGroupServiceImpl.class);

    @Autowired
    private UserAssessmentRepository userAssessmentRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    private Random random = new Random();

    public ItemGroupServiceImpl(){}
    public ItemGroupServiceImpl(UserAssessmentRepository userAssessmentRepository, AssessmentRepository assessmentRepository) {
        this.userAssessmentRepository = userAssessmentRepository;
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    public Try<ItemGroup> getNextItemGroup(String userId, String assessmentId){

        /**
         * 1. are they in the middle of a questionitem group? return that group from the user assessment
         * 2. if they have not started any questionitem groups, serve up the first one (is this CAT? serve up a medium one)
         * 3. already completed questionitem groups, serve up the next one (is this CAT? do your CAT thing and serve up the correct difficulty)
         */

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(assessmentId);
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId);
        if(maybeUserAssessment.isFailure()){
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        UserAssessment userAssessment = maybeUserAssessment.get();
        if(userAssessment.getStatus() != CompletionStatus.IN_PROGRESS){
            return new Try.Failure<>(new IncompatibleStatusException("UserAssessment", new CompletionStatus[]{ CompletionStatus.IN_PROGRESS }, userAssessment.getStatus(), userAssessment.getId()));
        }

        Assessment assessment = maybeAssessment.get();

        switch(assessment.getAssessmentType()){
            case CAT:
                return getNextCATItemGroup(userId, (CATUserAssessment) userAssessment, (CATAssessment) assessment);

            case LIKERT:
            case MULTIPLE_CHOICE:
                return getNextMultipleChoiceItemGroup(userId, (MultipleChoiceUserAssessment) userAssessment, (MultipleChoiceAssessment) assessment);

            default:
                return new Try.Failure<>(new IncompatibleTypeException(
                        "UserAssessment",
                        new AssessmentType[]{ AssessmentType.LIKERT, AssessmentType.MULTIPLE_CHOICE, AssessmentType.CAT },
                        userAssessment.getAssessmentType()));
        }
    }

    @Override
    public Try<ItemGroup> getNextCATItemGroup(String userId, CATUserAssessment userAssessment, CATAssessment assessment){

        List<CATItemGroup> userItemGroups = userAssessment.getItemGroups();


        if(userItemGroups.isEmpty()){

            //never started a group, give them a random medium one.
            Optional<CATItemGroup> startingItemGroup = getRandomCATItemGroup(assessment.getItemGroups(), assessment.getStartingDifficulty(), userItemGroups);
            if(!startingItemGroup.isPresent()){
                return new Try.Failure<>(new InvalidObjectException("CATAssessment",
                        "CAT assessment does not have any " + assessment.getStartingDifficulty() + " level groups"));
            }

            return new Try.Success<>(startingItemGroup.get());
        }

        //Find out if they've started a group, return it
        List<ItemGroup> mappedUserItemGroups = userItemGroups.stream()
                .map(catItemGroup -> (ItemGroup) catItemGroup)
                .collect(Collectors.toList());

        Optional<ItemGroup> unfinishedItemGroup = getUnfinishedItemGroup(mappedUserItemGroups);
        if(unfinishedItemGroup.isPresent()){
            return new Try.Success<>(unfinishedItemGroup.get());
        }


        //do they get another group?
        if(userItemGroups.size() < assessment.getMinTakenGroups() ||
                (userItemGroups.size() >= assessment.getMinTakenGroups() &&
                        userItemGroups.size() < assessment.getMaxTakenGroups()
                        && !isLastTwoGroupsSameDifficulty(userItemGroups))){
            //lets figure out what their next group should be

            Try<Difficulty> nextDifficulty = determineNextGroupDifficulty(assessment.getItemGroupTransitions(), userItemGroups);
            if(nextDifficulty.isFailure()){
                return new Try.Failure<>(nextDifficulty.failed().get());
            }

            //get next group of calculated difficulty
            Optional<CATItemGroup> nextItemGroup = getRandomCATItemGroup(assessment.getItemGroups(), nextDifficulty.get(), userItemGroups);
            if(!nextItemGroup.isPresent()){
                return new Try.Failure<>(new InvalidObjectException("CATAssessment",
                        "Unable to transition to next group: CAT assessment does not have any more " + nextDifficulty.toString() +  " level groups"));
            }

            return new Try.Success<>(nextItemGroup.get());
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<ItemGroup> getNextMultipleChoiceItemGroup(String userId, MultipleChoiceUserAssessment userAssessment, MultipleChoiceAssessment assessment){

        List<ItemGroup> userItemGroups = userAssessment.getItemGroups();

        Optional<ItemGroup> unfinishedItemGroup = getUnfinishedItemGroup(userItemGroups);
        if(unfinishedItemGroup.isPresent()){
            return new Try.Success<>(unfinishedItemGroup.get());
        }

        if(userItemGroups.isEmpty()){
            //never started a group, give them the first one.
            return new Try.Success<>(assessment.getItemGroups().get(0));
        }

        //onto the next group!
        //lets first figure out which groups they've completed
        List<String> userItemGroupIds = userItemGroups.stream()
                .map(ItemGroup::getId)
                .collect(Collectors.toList());

        //find the next one that they haven't started
        ItemGroup nextItemGroup = assessment.getItemGroups().stream()
                .filter(itemGroup -> !userItemGroupIds.contains(itemGroup.getId()))
                .findFirst()
                .orElse(null);

        return new Try.Success<>(nextItemGroup);
    }

    @Override
    public Try<ItemGroup> saveItemGroup(String userId, String assessmentId, ItemGroup submittedItemGroup) {

        Try<Assessment> maybeAssessment = assessmentRepository.getAssessment(assessmentId);
        if(maybeAssessment.isFailure()){
            return new Try.Failure<>(maybeAssessment.failed().get());
        }

        Try<UserAssessment> maybeUserAssessment = userAssessmentRepository.getLatestUserAssessment(userId, assessmentId);
        if(maybeUserAssessment.isFailure()){
            return new Try.Failure<>(maybeUserAssessment.failed().get());
        }

        if(maybeUserAssessment.get().getStatus() != CompletionStatus.IN_PROGRESS){
            return new Try.Failure<>(new IncompatibleStatusException("UserAssessment", new CompletionStatus[]{ CompletionStatus.IN_PROGRESS }, maybeUserAssessment.get().getStatus(), maybeUserAssessment.get().getId()));
        }

        UserAssessment userAssessment = maybeUserAssessment.get();
        Assessment assessment = maybeAssessment.get();

        Try<UserAssessment> maybeAppliedAnswers;
        switch(assessment.getAssessmentType()){
            case CAT:
                maybeAppliedAnswers = applySubmittedAnswersToUserItemGroups(
                        submittedItemGroup,
                        (CATUserAssessment) userAssessment,
                        (CATAssessment) assessment);

                break;

            case LIKERT:
            case MULTIPLE_CHOICE:
                maybeAppliedAnswers = applySubmittedAnswersToUserItemGroups(
                        submittedItemGroup,
                        (MultipleChoiceUserAssessment) userAssessment,
                        (MultipleChoiceAssessment) assessment);

                break;

            default:
                return new Try.Failure<>(new IncompatibleTypeException(
                        "UserAssessment",
                        new AssessmentType[]{ AssessmentType.LIKERT, AssessmentType.MULTIPLE_CHOICE, AssessmentType.CAT },
                        userAssessment.getAssessmentType()));
        }

        if(maybeAppliedAnswers.isFailure()){
            return new Try.Failure<>(maybeAppliedAnswers.failed().get());
        }

        return new Try.Success<>(submittedItemGroup);
    }


    protected <T extends ItemGroup,
            U extends ItemGroupUserAssessment<T>,
            V extends ItemGroupAssessment<T>>
    Try<UserAssessment> applySubmittedAnswersToUserItemGroups(ItemGroup submittedItemGroup, U userAssessment, V assessment){

        Optional<T> assessmentItemGroup = assessment.getItemGroups().stream()
                .filter(itemGroup -> itemGroup.getId().equals(submittedItemGroup.getId()))
                .findFirst();

        if(!assessmentItemGroup.isPresent()){
            return new Try.Failure<>(new RepoNotFoundException("ItemGroup"));
        }

        Optional<T> userItemGroup = userAssessment.getItemGroups().stream()
                .filter(itemGroup -> itemGroup.getId().equals(submittedItemGroup.getId()))
                .findFirst();

        T itemGroupToUpdate = userItemGroup.isPresent()? userItemGroup.get() : assessmentItemGroup.get();

        for(Item item : itemGroupToUpdate.getItems()){

            Optional<Item> submittedItem = getItemById(submittedItemGroup, item.getId());
            if(!submittedItem.isPresent()){
                return new Try.Failure<>(new BadInputException("ItemGroup", "Submitted itemGroup does not match assessment's itemGroup"));
            }

            if(item.getChosenItemAnswer() == null){
                //can't change this if you've already answered
                item.setChosenItemAnswerId(submittedItem.get().getChosenItemAnswerId());

                item.setStartDate(submittedItem.get().getStartDate());
                item.setCompleteDate(submittedItem.get().getCompleteDate());
            }
        }

        if(!userItemGroup.isPresent()){
            userAssessment.getItemGroups().add(itemGroupToUpdate);
        }


        Integer numQuestions = null;
        if(assessment instanceof CATAssessment){
            numQuestions = ((CATAssessment) assessment).getMaxTakenGroups() * ((CATAssessment) assessment).getNumQuestionsPerGroup();
        }

        if(assessment instanceof MultipleChoiceAssessment){
            numQuestions = assessment.getItemGroups().stream()
                    .mapToInt(itemGroup1 -> itemGroup1.getItems().size())
                    .sum();
        }

        if(numQuestions != null){
            Double progressPercentage = calcProgressPercentage(userAssessment.getItemGroups(), numQuestions);
            userAssessment.setProgressPercentage(progressPercentage);
        }

        Try<Void> maybeResults = userAssessmentRepository.saveUserAssessment(userAssessment);
        if(maybeResults.isFailure()){
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(userAssessment);
    }

    private Optional<Item> getItemById(ItemGroup itemGroup, String itemId){
        return itemGroup.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();
    }

    protected Try<Difficulty> determineNextGroupDifficulty(List<ItemGroupTransition> itemGroupTransitions, List<CATItemGroup> alreadyTakenItemGroups){
        if(alreadyTakenItemGroups.size() < 1){
            return new Try.Failure<>(new BadInputException("ItemGroup", "alreadyTakenItemGroups should be > 0"));
        }

        CATItemGroup lastItemGroup = alreadyTakenItemGroups.get(alreadyTakenItemGroups.size() - 1);

        Optional<ItemGroupTransition> applicableItemGroupTransition = itemGroupTransitions.stream()
                .filter(itemGroupTransition -> itemGroupTransition.getGroupDifficulty() == lastItemGroup.getDifficulty())
                .findFirst();

        if(!applicableItemGroupTransition.isPresent()){
            return new Try.Failure<>(new InvalidObjectException("CATAssessment",
                    "CAT assessment does not have a way to transition from " + lastItemGroup.getDifficulty() + " question item groups"));
        }

        //determine our score and find which difficulty we need next
        Double lastScore = getScoreForItemGroup(lastItemGroup);
        Optional<Map.Entry<Difficulty, Range<Double>>> mapEntry = applicableItemGroupTransition.get().getTransitionMap().entrySet().stream()
                .filter(difficultyRangeEntry -> difficultyRangeEntry.getValue().contains(lastScore))
                .findFirst();

        if(!mapEntry.isPresent()){
            return new Try.Failure<>(new InvalidObjectException("CATAssessment",
                    "CAT assessment does not have a way to transition from " + lastItemGroup.getDifficulty() + " question item groups (map undefined for score " + lastScore + ")"));
        }

        return new Try.Success<>(mapEntry.get().getKey());
    }

    private boolean isLastTwoGroupsSameDifficulty(List<CATItemGroup> userItemGroups){
        int numGroups = userItemGroups.size();
        if(numGroups < 2) return false;

        return userItemGroups.get(numGroups - 1).getDifficulty() == userItemGroups.get(numGroups - 2).getDifficulty();
    }

    protected Optional<CATItemGroup> getRandomCATItemGroup(List<CATItemGroup> assessmentItemGroups, Difficulty difficulty, List<CATItemGroup> alreadyTakenItemGroups){

        //lets first figure out which groups they've completed
        List<String> userItemGroupIds = alreadyTakenItemGroups.stream()
                .map(ItemGroup::getId)
                .collect(Collectors.toList());

        List<CATItemGroup> difficultyItemGroups = assessmentItemGroups.stream()
                .filter(catItemGroup -> catItemGroup.getDifficulty() == difficulty && !userItemGroupIds.contains(catItemGroup.getId()))
                .collect(Collectors.toList());

        if(difficultyItemGroups.isEmpty()){
            return Optional.empty();
        }

        int randomIndex = random.nextInt(difficultyItemGroups.size());

        return Optional.of(difficultyItemGroups.get(randomIndex));
    }

    private Optional<ItemGroup> getUnfinishedItemGroup(List<ItemGroup> userItemGroups){

        if(userItemGroups.isEmpty()) return Optional.empty();

        return userItemGroups.stream()
                .filter(itemGroup ->
                        itemGroup.getItems().stream()
                                .anyMatch(item -> item.getChosenItemAnswer() == null)
                )
                .findFirst();
    }

    private <T extends ItemGroup> Double calcProgressPercentage(List<T> userItemGroups, int numTotalQuestions){
        Integer numQuestionsAnswered = userItemGroups.stream()
                .mapToInt(userItemGroup -> {
                    return userItemGroup.getItems().stream()
                            .filter(item -> item.getChosenItemAnswer() != null)
                            .collect(Collectors.toList())
                            .size();
                })
                .sum();

        return (double)numQuestionsAnswered / (double)numTotalQuestions;
    }

    private double getScoreForItemGroup(ItemGroup itemGroup){
        return itemGroup.getItems().stream()
                .filter(item -> item.getChosenItemAnswer() != null)
                .mapToDouble(item -> item.getChosenItemAnswer().getScore())
                .sum();
    }
}
