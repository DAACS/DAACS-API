package com.daacs.service;

import com.daacs.framework.exception.BadInputException;
import com.daacs.framework.exception.InvalidObjectException;
import com.daacs.framework.serializer.DaacsOrikaMapper;
import com.daacs.model.*;
import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.Domain;
import com.daacs.model.assessment.user.CompletionStatus;
import com.daacs.model.assessment.user.DomainScore;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.model.dto.AssessmentScore;
import com.daacs.model.dto.InstructorClassUserScore;
import com.daacs.model.dto.SendClassInviteRequest;
import com.daacs.repository.InstructorClassRepository;
import com.lambdista.util.Try;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

@Service
public class InstructorClassServiceImpl implements InstructorClassService {
    private static final Logger log = LoggerFactory.getLogger(InstructorClassServiceImpl.class);

    @Autowired
    private InstructorClassRepository classRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PendingStudentService pendingStudentService;

    @Autowired
    private UserAssessmentService userAssessmentService;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private DaacsOrikaMapper orikaMapper;

    @Autowired
    private MailService mailService;

    @Override
    public Try<InstructorClass> getClass(String id) {
        return classRepository.getClass(id);
    }

    @Override
    public Try<List<InstructorClass>> getClasses(String[] instructorIds, Integer limit, Integer offset) {
        return classRepository.getClasses(instructorIds, limit, offset);
    }


    @Override
    public Try<InstructorClass> saveClass(InstructorClass instructorClass) {
        Try<Void> maybeResults = classRepository.saveClass(instructorClass);
        if (maybeResults.isFailure()) {
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(instructorClass);
    }

    @Override
    public Try<InstructorClass> updateClass(InstructorClass updateClassRequest) {
        Try<InstructorClass> maybeClass = getClass(updateClassRequest.getId());
        if (maybeClass.isFailure()) {
            return new Try.Failure<>(maybeClass.failed().get());
        }

        InstructorClass instructorClass = maybeClass.get();
        // check if new instructor id is valid
        if (instructorClass.getInstructorId() != updateClassRequest.getInstructorId()) {
            Try<User> maybeInstructor = userService.getInstructorById(updateClassRequest.getInstructorId());
            if (maybeInstructor.isFailure()) {
                return new Try.Failure<>(maybeInstructor.failed().get());
            }
        }

        // check if updating assessment ids when canEditAssessments = false
        if (!instructorClass.getAssessmentIds().equals(updateClassRequest.getAssessmentIds())) {
            if (!instructorClass.getCanEditAssessments()) {
                return new Try.Failure<>(new InvalidObjectException("instructorClass.AssessmentIds", "Cannot change assessments after a user has completed one"));
            }
        }

        orikaMapper.map(updateClassRequest, instructorClass);

        return saveClass(instructorClass);
    }

    @Override
    public Try<InstructorClass> createClass(InstructorClass instructorClass) {

        // check if instructor id is valid
        Try<User> maybeInstructor = userService.getInstructorById(instructorClass.getInstructorId());
        if (maybeInstructor.isFailure()) {
            return new Try.Failure<>(maybeInstructor.failed().get());
        }

        Try<Void> maybeResults = classRepository.insertClass(instructorClass);
        if (maybeResults.isFailure()) {
            return new Try.Failure<>(maybeResults.failed().get());
        }

        return new Try.Success<>(instructorClass);
    }

    @Override
    public Try<Void> classAssessmentTaken(String studentId, String assessmentId) {

        //get classes
        Try<List<InstructorClass>> maybeClasses = classRepository.getClassByStudentAndAssessmentId(studentId, assessmentId);
        if (maybeClasses.isFailure()) {
            return new Try.Failure<>(maybeClasses.failed().get());
        }
        List<InstructorClass> classes = maybeClasses.get();

        //check if taken userAssessment is from one of the users classes
        for (InstructorClass instructorClass : classes) {
            for (String classAssessmentId : instructorClass.getAssessmentIds()) {

                if (classAssessmentId.equals(assessmentId)) {
                    //if so set class CanEditAssessments to false
                    instructorClass.setCanEditAssessments(false);
                    Try<InstructorClass> maybeSavedClass = updateClass(instructorClass);
                    if (maybeSavedClass.isFailure()) {
                        return new Try.Failure<>(maybeSavedClass.failed().get());
                    }
                }
            }
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<Void> sendClassInvites(SendClassInviteRequest classInviteRequest) {
        try {
            InstructorClass instructorClass = getClass(classInviteRequest.getClassId()).get();
            User instructor = userService.getUser(instructorClass.getInstructorId()).get();

            for (String email : classInviteRequest.getUserEmails()) {
                User student = userService.getUserIfExists(email).get();

                if(student == null){
                    pendingStudentService.inviteStudentToDaacs(email, classInviteRequest.getClassId(), classInviteRequest.getForceAccept(), instructor).get();
                    continue;
                }

                // add an accepted student invite to instructor class
                if (classInviteRequest.getForceAccept()) {
                    StudentClassInvite studentClassInvite = new StudentClassInvite(student.getId());
                    studentClassInvite.setInviteStatusAccepted(true);
                    instructorClass.addStudentInvite(studentClassInvite);
                    saveClass(instructorClass).get();
                }
                // send class invite email to student
                else {
                    addStudentInvite(instructorClass, student.getId()).get();
                    mailService.sendClassInviteEmail(student, instructor, instructorClass).get();
                }
            }

        } catch (Exception e) {
            //returning e.getCause() because every top level exception caught here would be a 'get of a failure exception'
            return new Try.Failure<>(e.getCause());
        }
        return new Try.Success<>(null);
    }

    @Override
    public Try<Void> sendPendingInvite(User student, PendingStudent pendingStudent) {
        try {

            for(PendingInvite pendingInvite: pendingStudent.getPendingInvites()){
                InstructorClass instructorClass = getClass(pendingInvite.getClassId()).get();
                User instructor = userService.getUser(instructorClass.getInstructorId()).get();

                // add an accepted student invite to instructor class
                if (pendingInvite.getForceAccept()) {
                    StudentClassInvite studentClassInvite = new StudentClassInvite(student.getId());
                    studentClassInvite.setInviteStatusAccepted(true);
                    instructorClass.addStudentInvite(studentClassInvite);
                    saveClass(instructorClass).get();
                }
                // send class invite email to student
                else {
                    addStudentInvite(instructorClass, student.getId()).get();
                    mailService.sendClassInviteEmail(student, instructor, instructorClass).get();
                }
            }

        } catch (Exception e) {
            //returning e.getCause() because every top level exception caught here would be a 'get of a failure exception'
            return new Try.Failure<>(e.getCause());
        }
        return new Try.Success<>(null);
    }

    public Try<Void> addStudentInvite(InstructorClass instructorClass, String studentId) {

        //don't update instructor class if invite already exists
        for (StudentClassInvite classInvite : instructorClass.getStudentInvites()) {
            if (classInvite.getStudentId().equals(studentId)) {
                return new Try.Success<>(null);
            }
        }

        instructorClass.getStudentInvites().add(new StudentClassInvite(studentId));
        Try<InstructorClass> maybeClass = saveClass(instructorClass);
        if (maybeClass.isFailure()) {
            return new Try.Failure<>(maybeClass.failed().get());
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<Void> acceptInvite(String instructorClassId, String studentId) {
        Try<InstructorClass> maybeClass = getClass(instructorClassId);
        if (maybeClass.isFailure()) {
            return new Try.Failure<>(maybeClass.failed().get());
        }

        InstructorClass instructorClass = maybeClass.get();

        //set users class invite status to accepted
        for (StudentClassInvite classInvite : instructorClass.getStudentInvites()) {
            if (classInvite.getStudentId().equals(studentId)) {
                classInvite.setInviteStatusAccepted(true);

                Try<InstructorClass> maybeSaved = saveClass(instructorClass);
                if (maybeSaved.isFailure()) {
                    return new Try.Failure<>(maybeSaved.failed().get());
                }

                return new Try.Success<>(null);
            }
        }

        //if user does not have an invite for this class
        return new Try.Failure<>(new BadInputException("StudentClassInvite", "user " + studentId + " has not been invited to class " + instructorClass.getName()));
    }

    @Override
    public Try<List<InstructorClassUserScore>> getStudentScores(String classId) {
        try {
            List<InstructorClassUserScore> scoresList = new ArrayList<>();
            InstructorClass instructorClass = getClass(classId).get();

            Map<String, StudentClassInvite> invites = new HashMap<>();
            List<String> studentIds = new ArrayList<>();

            for (StudentClassInvite classInvite : instructorClass.getStudentInvites()) {
                studentIds.add(classInvite.getStudentId());
                invites.put(classInvite.getStudentId(), classInvite);
            }

            List<User> students = userService.getUsersById(studentIds).get();

            for (User user : students) {
                //get classInvite
                StudentClassInvite classInvite = invites.get(user.getId());

                InstructorClassUserScore classUserScore = new InstructorClassUserScore(user);

                //set  InstructorClassUserScore.ClassInviteAccepted
                classUserScore.setClassInviteAccepted(classInvite.getInviteStatusAccepted());

                //get user assessments
                for (String assessmentId : instructorClass.getAssessmentIds()) {
                    UserAssessment userAssessment = userAssessmentService.getLatestUserAssessmentIfExists(user.getId(), assessmentId).get();

                    if (userAssessment != null && !userAssessment.getStatus().equals(CompletionStatus.IN_PROGRESS)) {
                        //set score
                        AssessmentScore assessmentScore = new AssessmentScore(userAssessment.getAssessmentId(), userAssessment.getAssessmentCategoryGroupId(), userAssessment.getOverallScore() == null ? null : userAssessment.getOverallScore().name());
                        classUserScore.getAssessmentScores().add(assessmentScore);
                    }
                }

                scoresList.add(classUserScore);

            }
            return new Try.Success<>(scoresList);
        } catch (Exception e) {
            //returning e.getCause() because every top level exception caught here would be a 'get of a failure exception'
            return new Try.Failure<>(e.getCause());
        }
    }

    @Override
    public Try<ClassScoreResults> getStudentScoresCSV(String classId) {

        ClassScoreResults results = new ClassScoreResults();
        List<String> headerList = new ArrayList<>();
        Map<String, Integer> domainScoreMap = new HashMap<>();

        try {
            headerList.add("first_name");
            headerList.add("last_name");

            InstructorClass instructorClass = getClass(classId).get();

            Map<String, StudentClassInvite> invites = new HashMap<>();
            List<String> studentIds = new ArrayList<>();

            for (StudentClassInvite classInvite : instructorClass.getStudentInvites()) {
                studentIds.add(classInvite.getStudentId());
                invites.put(classInvite.getStudentId(), classInvite);
            }

            List<User> students = userService.getUsersById(studentIds).get();

            for (User user : students) {
                //get classInvite
                StudentClassInvite classInvite = invites.get(user.getId());

                List<String> rowlist = new ArrayList<>();
                rowlist.add(user.getFirstName());
                rowlist.add(user.getLastName());

                //get user assessments
                for (String assessmentId : instructorClass.getAssessmentIds()) {
                    UserAssessment userAssessment = userAssessmentService.getLatestUserAssessmentIfExists(user.getId(), assessmentId).get();

                    int numDomains;
                    if (!domainScoreMap.containsKey(assessmentId)) {
                        Assessment assessment = assessmentService.getAssessment(assessmentId).get();

                        List<String> assessmentHeader = getAssessmentDetailsForHeader(assessment);
                        numDomains = assessmentHeader.size() - 1;
                        headerList.addAll(assessmentHeader);
                        domainScoreMap.put(assessmentId, numDomains);
                    } else {
                        numDomains = domainScoreMap.get(assessmentId);
                    }

                    if (userAssessment != null) {
                        rowlist.add(userAssessment.getOverallScore() == null ? null : userAssessment.getOverallScore().name());

                        //add domain scores
                        for (DomainScore domainScore : userAssessment.getDomainScores()) {
                            String domainRubricScore = null;
                            if (domainScore.getRawScore() != null) {
                                domainRubricScore = domainScore.getRubricScore().name();
                            }
                            rowlist.add(domainRubricScore);
                        }
                        //add null for domains with no domainScore
                        for (int i = userAssessment.getDomainScores().size(); i < numDomains; i++) {
                            rowlist.add(null);
                        }
                    }
                    // if user assessment didn't exist
                    else {
                        //add overall scores
                        rowlist.add(null);

                        //add domain scores
                        for (int i = 0; i < domainScoreMap.get(assessmentId); i++) {
                            rowlist.add(null);
                        }
                    }
                }

                results.getRows().add(rowlist.toArray(new String[0]));
            }

            results.setHeader(headerList.toArray(new String[0]));
        } catch (Exception e) {
            //returning e.getCause() because every top level exception caught here would be a 'get of a failure exception'
            return new Try.Failure<>(e.getCause());
        }

        return new Try.Success<>(results);
    }

    public List<String> getAssessmentDetailsForHeader(Assessment assessment) {
        List<String> headerList = new ArrayList<>();
        headerList.add(assessment.getAssessmentCategoryGroup().getId());

        for (Domain domain : assessment.getDomains()) {
            headerList.add(domain.getId());
        }

        return headerList;
    }

    @Override
    public void writeScoresToCSV(CSVWriter csvWriter, ClassScoreResults results) throws IOException {
        String[] header = results.getHeader();

        csvWriter.writeNext(header);
        csvWriter.writeAll(results.getRows());
        csvWriter.close();
    }

    @Override
    public Try<Void> uploadClasses(MultipartFile classesFile) {

        Map<String, InstructorClass> classMap = new HashMap<>();
        Set<String> classesToUpdate = new HashSet<>();

        Map<String, User> studentMap = new HashMap<>();
        Map<String, User> instructorMap = new HashMap<>();

        //keeps track of when to create pendingStudents
        Map<String,  Set<String> > pendingStudentsByClass = new HashMap<>();

        BufferedReader br;
        List<String> result = new ArrayList<>();

        //build classMap from input file
        try {

            InputStream is = classesFile.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));
            CSVReader reader = new CSVReader(br, ',', '"');

            String[] record = null;
            while ((record = reader.readNext()) != null) {

                String classId = null;
                String className = record[0];

                //get instructor
                User instructor = instructorMap.containsKey(record[1]) ? instructorMap.get(record[1]) : userService.getUserByUsername(record[1]).get();
                String instructorId = instructor.getId();
                //get student if exists
                User student = studentMap.containsKey(record[2]) ? studentMap.get(record[2]) : userService.getUserIfExists(record[2]).get();
                //assessment
                String assessmentId = record.length == 4 ? record[3] : null;

                //update maps (even if key/value pair already existed)
                instructorMap.put(record[1], instructor);
                studentMap.put(record[2], student);

                // update class map
                if (classMap.containsKey(className)) {
                    InstructorClass currentClass = classMap.get(className);
                    addStudentAndAssesment(student, currentClass, assessmentId);
                    classId = currentClass.getId();
                }
                // add class to class map
                else {
                    //check if class exists
                    InstructorClass instructorClass = classRepository.getClassByNameAndInstructor(instructorId, className).get();
                    InstructorClass newClass = new InstructorClass(className, instructorId);
                    if (instructorClass != null) {
                        newClass = instructorClass;
                        classesToUpdate.add(newClass.getId());
                    }

                    addStudentAndAssesment(student, newClass, assessmentId);
                    classMap.put(newClass.getName(), newClass);
                    classId = newClass.getId();
                }

                //if student was null create pending student
                if(student == null){
                    if(!pendingStudentsByClass.containsKey(classId) || !pendingStudentsByClass.get(classId).contains(record[2])){
                        pendingStudentService.inviteStudentToDaacs(record[2], classId, true, instructor).get();
                        updatePendingStudentsByClass(pendingStudentsByClass, classId, record[2]);
                    }
                }
            }
            reader.close();

        } catch (Exception e) {
            return new Try.Failure<>(e);
        }

        try {
            // iterate through map and create/update instructor classes
            for (Map.Entry<String, InstructorClass> entry : classMap.entrySet()) {
                InstructorClass newClass = entry.getValue();

                // create new instructor class
                if (!classesToUpdate.contains(newClass.getId())) {
                    classRepository.insertClass(newClass).get();
                }
                // update existing instructor class
                else {
                    saveClass(newClass).get();
                }
            }
        } catch (Exception e) {
            return new Try.Failure<>(e.getCause());
        }

        return new Try.Success<>(null);
    }

    private void addStudentAndAssesment(User student, InstructorClass instructorClass, String assessmentId) {
        if (student != null) {
            StudentClassInvite studentClassInvite = new StudentClassInvite(student.getId());
            studentClassInvite.setInviteStatusAccepted(true);
            instructorClass.addStudentInvite(studentClassInvite);
        }

        if (StringUtils.isNotBlank(assessmentId)) {
            instructorClass.addAssessmentId(assessmentId);
        }
    }

    private void updatePendingStudentsByClass( Map<String,  Set<String> > studentsInClasses, String classId, String studentEmail){
        if(studentsInClasses.containsKey(classId)){
            studentsInClasses.get(classId).add(studentEmail);
        }else{
            Set<String> usersSet = new HashSet<>();
            usersSet.add(studentEmail);
            studentsInClasses.put(classId,usersSet);
        }
    }
}
