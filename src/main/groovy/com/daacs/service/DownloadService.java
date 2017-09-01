package com.daacs.service;

import com.daacs.framework.serializer.Views;
import com.daacs.model.User;
import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.user.UserAssessment;
import com.lambdista.util.Try;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by chostetter on 8/3/16.
 */
public interface DownloadService {
    void writeUserAssessmentsToStream(List<UserAssessment> userAssessments, OutputStream outputStream) throws IOException;
    void writeUserAssessmentToStream(UserAssessment userAssessment, OutputStream outputStream, boolean closeStreamWhenDone) throws IOException;
    String getFileNameForUserAssessment(UserAssessment userAssessment);
    String storeUser(User user);
    Try<User> retrieveUser(String token);
    <T extends Views> void writeAssessmentToStream(Assessment assessment, Class<T> viewClass, OutputStream outputStream, boolean closeStreamWhenDone) throws IOException;
}
