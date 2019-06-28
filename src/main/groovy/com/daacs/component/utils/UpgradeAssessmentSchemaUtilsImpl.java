package com.daacs.component.utils;

import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.framework.serializer.json.RangeDeserializer;
import com.daacs.model.assessment.*;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.model.item.*;
import com.daacs.service.AssessmentCategoryGroupService;
import com.daacs.service.AssessmentService;
import com.daacs.service.UserAssessmentService;
import com.lambdista.util.Try;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lhorne on 9/18/18.
 */
@Component
public class UpgradeAssessmentSchemaUtilsImpl implements UpgradeAssessmentSchemaUtils {

    @Value("${schema.version}")
    private long schemaVersion;

    @Autowired
    private DaacsOrikaMapper orikaMapper;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private AssessmentCategoryGroupService assessmentCategoryGroupService;

    @Autowired
    private UserAssessmentService userAssessmentService;

    public UpgradeAssessmentSchemaUtilsImpl() {
    }

    public UpgradeAssessmentSchemaUtilsImpl(long schemaVersion, DaacsOrikaMapper orikaMapper, AssessmentService assessmentService, UserAssessmentService userAssessmentService, AssessmentCategoryGroupService assessmentCategoryGroupService) {
        this.schemaVersion = schemaVersion;
        this.orikaMapper = orikaMapper;
        this.assessmentService = assessmentService;
        this.userAssessmentService = userAssessmentService;
        this.assessmentCategoryGroupService = assessmentCategoryGroupService;
    }

    @Override
    public Try<Assessment> upgradeAssessmentSchema(Assessment assessment) {

        if (assessment.getSchemaVersion() == null || !assessment.getSchemaVersion().equals(schemaVersion)) {

            //20180918 - add global default possibleItemAnswer
            if (assessment.getSchemaVersion() == null) {
                assessment = addPossibleItemAnswerToItemGroup(assessment);
                assessment.setSchemaVersion(1L);
            }

            //20181210 - add exiting lightside model filenames to domains
            if (assessment.getSchemaVersion() == 1L) {
                assessment = addlightsideModelFilenameToDomain(assessment);
                assessment.setSchemaVersion(2L);
            }

            //20190211 - convert transitionMap ranges to Range<Double>
            if (assessment.getSchemaVersion() == 2L) {
                assessment = convertTransitionMap(assessment);
                assessment.setSchemaVersion(3L);
            }

            //20190304 - add assessmentCategoryGroups to assessment and userAssessments
            if (assessment.getSchemaVersion() == 3L) {
                Try<Assessment> maybeAssessment = addCategoryGroups(assessment);
                if (maybeAssessment.isFailure()) {
                    return maybeAssessment;
                }
                assessment = maybeAssessment.get();
                assessment.setSchemaVersion(5L); //not a typo
            }

            //20190416 - fix dasherized case on college skills group college-skills -> college_skills
            if (assessment.getSchemaVersion() == 4L) {
                Try<Assessment> maybeAssessment = fixCollegeSkills(assessment);
                if (maybeAssessment.isFailure()) {
                    return maybeAssessment;
                }
                assessment = maybeAssessment.get();
                assessment.setSchemaVersion(5L);
            }

            //additional upgrades here

            assessment.setSchemaVersion(schemaVersion);

            return assessmentService.saveAssessment(assessment);
        }

        //schema is up to date
        return new Try.Success<>(assessment);

    }

    private Assessment addPossibleItemAnswerToItemGroup(Assessment assessment) {

        if (assessment.getAssessmentType() == AssessmentType.LIKERT) {
                List<ItemGroup> itemGroups = ((ItemGroupAssessment) assessment).getItemGroups();
                for (ItemGroup itemGroup : ListUtils.emptyIfNull(itemGroups)) {

                    if (itemGroup.getPossibleItemAnswers() == null) {

                        itemGroup.setPossibleItemAnswers(new ArrayList<>());

                        //use first item's possibleAnswerItem as default if possible
                        Optional<Item> optionalItem = ListUtils.emptyIfNull(itemGroup.getItems())
                                .stream()
                                .filter(i -> i.getPossibleItemAnswers() != null && i.getPossibleItemAnswers().size() > 0)
                                .findFirst();

                        if (!optionalItem.isPresent()) {
                            continue;
                        }

                        itemGroup.setPossibleItemAnswers(
                                ListUtils.emptyIfNull(optionalItem.get().getPossibleItemAnswers())
                                        .stream()
                                        .map(pia -> orikaMapper.map(pia, DefaultItemAnswer.class))
                                        .collect(Collectors.toList()));

                        //reassign ids
                        itemGroup.getPossibleItemAnswers().stream().forEach(dia -> dia.setId(UUID.randomUUID().toString()));
                    }
                }
        }

        return assessment;
    }

    private Assessment addlightsideModelFilenameToDomain(Assessment assessment) {

        if (assessment instanceof WritingAssessment) {

            LightSideConfig lightSideConfig = ((WritingAssessment) assessment).getLightSideConfig();
            if (lightSideConfig == null || MapUtils.isEmpty(lightSideConfig.getDomainModels())) {
                return assessment;
            }

            for (Domain domain : ListUtils.emptyIfNull(assessment.getDomains())) {
                if (StringUtils.isEmpty(domain.getLightsideModelFilename())) {

                    //attempt to match this domain to an existing lightside config model
                    if (lightSideConfig.getDomainModels().containsKey(domain.getId())) {
                        domain.setLightsideModelFilename(lightSideConfig.getDomainModels().get(domain.getId()));
                    }
                }
            }
        }

        return assessment;
    }

    private Assessment convertTransitionMap(Assessment assessment) {

        if (assessment.getAssessmentType() == AssessmentType.CAT) {
            List<ItemGroupTransition> itemGroupTransitions = ((CATAssessment) assessment).getItemGroupTransitions();

            RangeDeserializer rangeDeserializer = new RangeDeserializer();
            for (ItemGroupTransition itemGroupTransition : ListUtils.emptyIfNull(itemGroupTransitions)) {

                itemGroupTransition.getTransitionMap().replaceAll((k, v) -> rangeDeserializer.deserializeHelper((v == null ? "" : v.toString()).replace("-∞", "-INF").replace("+∞", "INF").replace("‥", ",")));
            }
        }

        return assessment;
    }

    private Try<Assessment> addCategoryGroups(Assessment assessment) {

        Try<Void> maybeSaved = backfillUserAssessmentGroupIds(assessment.getId());
        if (maybeSaved.isFailure()) {
            return new Try.Failure<>(maybeSaved.failed().get());
        }

        switch (assessment.getAssessmentCategory()) {
            case MATHEMATICS:
                return createDefaultGroup(DefaultCatgoryGroup.MATHEMATICS_LABEL, DefaultCatgoryGroup.MATHEMATICS_ID, AssessmentCategory.MATHEMATICS, assessment);

            case COLLEGE_SKILLS:
                return createDefaultGroup(DefaultCatgoryGroup.COLLEGE_SKILLS_LABEL, DefaultCatgoryGroup.COLLEGE_SKILLS_ID, AssessmentCategory.COLLEGE_SKILLS, assessment);

            case WRITING:
                return createDefaultGroup(DefaultCatgoryGroup.WRITING_LABEL, DefaultCatgoryGroup.WRITING_ID, AssessmentCategory.WRITING, assessment);

            case READING:
                return createDefaultGroup(DefaultCatgoryGroup.READING_LABEL, DefaultCatgoryGroup.READING_ID, AssessmentCategory.READING, assessment);
        }

        return new Try.Success<>(assessment);
    }

    private Try<Void> backfillUserAssessmentGroupIds(String assessmentId) {

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentService.getUserAssessmentsByAssessmentId(assessmentId);
        if (maybeUserAssessments.isFailure()) {
            return new Try.Failure<>(maybeUserAssessments.failed().get());
        }

        List<UserAssessment> userAssessmentsToSave = new ArrayList<>();
        for (UserAssessment userAssessment : maybeUserAssessments.get()) {

            switch (userAssessment.getAssessmentCategory()) {
                case MATHEMATICS:
                    userAssessment.setAssessmentCategoryGroupId(DefaultCatgoryGroup.MATHEMATICS_ID);
                    userAssessmentsToSave.add(userAssessment);
                    break;

                case COLLEGE_SKILLS:
                    userAssessment.setAssessmentCategoryGroupId(DefaultCatgoryGroup.COLLEGE_SKILLS_ID);
                    userAssessmentsToSave.add(userAssessment);
                    break;

                case WRITING:
                    userAssessment.setAssessmentCategoryGroupId(DefaultCatgoryGroup.WRITING_ID);
                    userAssessmentsToSave.add(userAssessment);
                    break;

                case READING:
                    userAssessment.setAssessmentCategoryGroupId(DefaultCatgoryGroup.READING_ID);
                    userAssessmentsToSave.add(userAssessment);
                    break;
            }

        }
        return userAssessmentService.bulkUserAssessmentSave(userAssessmentsToSave);
    }

    private Try<Assessment> createDefaultGroup(String label, String groupId, AssessmentCategory assessmentCategory, Assessment assessment) {
        AssessmentCategoryGroup assessmentCategoryGroup = new AssessmentCategoryGroup();
        assessmentCategoryGroup.setId(groupId);
        assessmentCategoryGroup.setLabel(label);
        assessmentCategoryGroup.setAssessmentCategory(assessmentCategory);

        //add group to group table if it doesn't exist
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroupIfPossible(assessmentCategoryGroup);
        if (maybeGroup.isFailure()) {
            return new Try.Failure<>(maybeGroup.failed().get());
        }

        assessment.setAssessmentCategoryGroup(assessmentCategoryGroup);

        return new Try.Success<>(assessment);
    }

    private Try<Assessment> fixCollegeSkills(Assessment assessment) {

        String oldCollegeSkillsId = "college-skills"; //don't change

        //rename group
        AssessmentCategoryGroup assessmentCategoryGroup = new AssessmentCategoryGroup();
        assessmentCategoryGroup.setId(DefaultCatgoryGroup.COLLEGE_SKILLS_ID);
        assessmentCategoryGroup.setLabel(DefaultCatgoryGroup.COLLEGE_SKILLS_LABEL);
        assessmentCategoryGroup.setAssessmentCategory(AssessmentCategory.COLLEGE_SKILLS);

            //add group to group table if it doesn't exist
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupService.createCategoryGroupIfPossible(assessmentCategoryGroup);
        if (maybeGroup.isFailure()) {
            return new Try.Failure<>(maybeGroup.failed().get());
        }

            //delete old group if it exists
        Try<Void> maybeDeleted = assessmentCategoryGroupService.deleteCategoryGroupIfExists(oldCollegeSkillsId);
        if (maybeDeleted.isFailure()) {
            return new Try.Failure<>(maybeDeleted.failed().get());
        }

        //backfill userAssessments
        Try<Void> maybeSaved = backfillUserAssessmentGroupIds(assessment.getId());
        if (maybeSaved.isFailure()) {
            return new Try.Failure<>(maybeSaved.failed().get());
        }

        if(assessment.getAssessmentCategoryGroup().getId().equals(oldCollegeSkillsId)){
            assessment.getAssessmentCategoryGroup().setId(DefaultCatgoryGroup.COLLEGE_SKILLS_ID);
        }

        return new Try.Success<>(assessment);
    }
}
