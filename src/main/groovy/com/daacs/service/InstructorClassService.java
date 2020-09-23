package com.daacs.service;

import com.daacs.model.ClassScoreResults;
import com.daacs.model.InstructorClass;
import com.daacs.model.PendingStudent;
import com.daacs.model.User;
import com.daacs.model.dto.InstructorClassUserScore;
import com.daacs.model.dto.SendClassInviteRequest;
import com.lambdista.util.Try;
import com.opencsv.CSVWriter;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by mgoldman on 6/8/20.
 */
public interface InstructorClassService {
    Try<InstructorClass> getClass(String id);

    Try<List<InstructorClass>> getClasses(String[] instructorIds, Integer limit, Integer offset);

    Try<InstructorClass> createClass(InstructorClass instructorClass);

    Try<InstructorClass> updateClass(InstructorClass instructorClass);

    Try<InstructorClass> saveClass(InstructorClass instructorClass);

    Try<Void> classAssessmentTaken(String studentId, String assessmentId);

    Try<Void> sendClassInvites(SendClassInviteRequest classInviteRequest);

    Try<Void> sendPendingInvite(User student, PendingStudent pendingStudent);

    Try<Void> acceptInvite(String instructorClassId, String studentId);

    Try<List<InstructorClassUserScore>> getStudentScores(String classId);

    Try<ClassScoreResults> getStudentScoresCSV(String classId);

    void writeScoresToCSV(CSVWriter csvWriter, ClassScoreResults results) throws IOException;

    Try<Void> uploadClasses(MultipartFile classesFile);
}
