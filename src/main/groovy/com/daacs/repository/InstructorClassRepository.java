package com.daacs.repository;

import com.daacs.model.InstructorClass;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by mgoldman on 6/8/20.
 */
public interface InstructorClassRepository {

    Try<InstructorClass> getClass(String id);
    Try<InstructorClass> getClassByNameAndInstructor(String instructorId, String className);
    Try<Void> saveClass(InstructorClass instructorClass);
    Try<Void> insertClass(InstructorClass instructorClass);
    Try<List<InstructorClass>> getClasses(String[] instructorIds, Integer limit, Integer offset);

    Try<List<InstructorClass>> getClassByStudentAndAssessmentId(String studentId, String assessmentId);
}

