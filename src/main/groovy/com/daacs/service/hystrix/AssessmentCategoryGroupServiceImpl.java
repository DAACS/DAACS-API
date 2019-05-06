package com.daacs.service.hystrix;

import com.daacs.component.utils.CategoryGroupUtils;
import com.daacs.framework.exception.AlreadyExistsException;
import com.daacs.framework.exception.InvalidIdFormatException;
import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.AssessmentCategoryGroup;
import com.daacs.repository.AssessmentCategoryGroupRepository;
import com.daacs.service.AssessmentCategoryGroupService;
import com.lambdista.util.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by mgoldman on 2/28/19.
 */
@Service
public class AssessmentCategoryGroupServiceImpl implements AssessmentCategoryGroupService {
    private static final Logger log = LoggerFactory.getLogger(AssessmentCategoryGroupServiceImpl.class);

    @Autowired
    private AssessmentCategoryGroupRepository assessmentCategoryGroupRepository;

    @Autowired
    private DaacsOrikaMapper daacsOrikaMapper;

    @Autowired
    private CategoryGroupUtils categoryGroupUtils;

    @Override
    public Try<List<String>> getGlobalGroupIds() {
        Try<List<AssessmentCategoryGroup>> maybeGroups = assessmentCategoryGroupRepository.getGlobalGroups();
        if (maybeGroups.isFailure()) {
            return new Try.Failure<>(maybeGroups.failed().get());
        }

        List<String> groupIds = maybeGroups.get().stream()
                .map(assessmentCategoryGroup -> assessmentCategoryGroup.getId())
                .collect(Collectors.toList());

        return new Try.Success<>(groupIds);
    }

    @Override
    public Try<List<AssessmentCategoryGroup>> getCategoryGroups() {
        return assessmentCategoryGroupRepository.getCategoryGroups();
    }

    @Override
    public Try<AssessmentCategoryGroup> createCategoryGroup(AssessmentCategoryGroup categoryGroup) {
        String id = categoryGroup.getId();

        boolean valid = categoryGroupUtils.validateIdFormat(id);
        if (!valid) {
            return new Try.Failure<>(new InvalidIdFormatException(id));
        }

        //check if ID already in use
        Try<AssessmentCategoryGroup> maybeGroup = assessmentCategoryGroupRepository.getCategoryGroupByIdOrNull(categoryGroup.getId());
        if (maybeGroup.isFailure()) {
            return new Try.Failure<>(maybeGroup.failed().get());
        }
        if (maybeGroup.get() != null) {
            return new Try.Failure<>(new AlreadyExistsException("ID","AssessmentCategoryGroup.ID",id));
        }

        Try<Void> maybeCreated = assessmentCategoryGroupRepository.createCategoryGroup(categoryGroup);
        if (maybeCreated.isFailure()) {
            return new Try.Failure<>(maybeCreated.failed().get());
        }

        return getCategoryGroupById(categoryGroup.getId());
    }


    @Override
    public Try<AssessmentCategoryGroup> updateCategoryGroup(String id, AssessmentCategoryGroup updateCategoryGroup) {

        Try<AssessmentCategoryGroup> maybeCategoryGroup = getCategoryGroupById(id);
        if (maybeCategoryGroup.isFailure()) {
            return new Try.Failure<>(maybeCategoryGroup.failed().get());
        }

        AssessmentCategoryGroup categoryGroup = maybeCategoryGroup.get();
        daacsOrikaMapper.map(updateCategoryGroup, categoryGroup);

        Try<Void> maybeUpdated = assessmentCategoryGroupRepository.updateCategoryGroup(categoryGroup);
        if (maybeUpdated.isFailure()) {
            return new Try.Failure<>(maybeUpdated.failed().get());
        }

        return getCategoryGroupById(id);
    }

    @Override
    public Try<Void> deleteCategoryGroup(String id) {

        Try<AssessmentCategoryGroup> maybeCategoryGroup = getCategoryGroupById(id);
        if (maybeCategoryGroup.isFailure()) {
            return new Try.Failure<>(maybeCategoryGroup.failed().get());
        }

        return assessmentCategoryGroupRepository.deleteCategoryGroup(id);
    }

    private Try<AssessmentCategoryGroup> getCategoryGroupById(String id) {
        return assessmentCategoryGroupRepository.getCategoryGroupById(id);
    }

    //only used by UpgradeAssessmentSchemaUtils to create default groups during backfill
    @Override
    public Try<AssessmentCategoryGroup> createCategoryGroupIfPossible(AssessmentCategoryGroup categoryGroup) {

        Try<AssessmentCategoryGroup> maybeCategoryGroup = assessmentCategoryGroupRepository.getCategoryGroupByIdOrNull(categoryGroup.getId());
        if (maybeCategoryGroup.isFailure()) {
            return new Try.Failure<>(maybeCategoryGroup.failed().get());
        }
        AssessmentCategoryGroup createdCategoryGroup = maybeCategoryGroup.get();
        if (createdCategoryGroup != null) {
            return new Try.Success<>(createdCategoryGroup);
        }

        Try<AssessmentCategoryGroup> maybeCreated = createCategoryGroup(categoryGroup);
        if (maybeCreated.isFailure()) {
            return new Try.Failure<>(maybeCreated.failed().get());
        }

        return maybeCreated;
    }
}
