package com.daacs.service;

import com.daacs.framework.serializer.Views;
import com.daacs.model.User;
import com.daacs.model.assessment.Assessment;
import com.daacs.model.assessment.user.UserAssessment;
import com.daacs.repository.DownloadTokenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.lambdista.util.Try;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.text.MessageFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by chostetter on 8/3/16.
 */
@Service
public class DownloadServiceImpl implements DownloadService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DownloadTokenRepository downloadTokenRepository;

    @Override
    public void writeUserAssessmentsToStream(List<UserAssessment> userAssessments, OutputStream outputStream) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);

        try {
            userAssessments.forEach(userAssessment -> {
                try {
                    String zipEntryName = getFileNameForUserAssessment(userAssessment);

                    zipOutputStream.putNextEntry(new ZipEntry(zipEntryName));
                    writeUserAssessmentToStream(userAssessment, zipOutputStream, false);
                    zipOutputStream.closeEntry();
                }
                catch (IOException ex) {
                    throw new UncheckedIOException(ex);
                }
            });
        }
        finally {
            zipOutputStream.finish();
            zipOutputStream.close();
            outputStream.close();
        }
    }

    @Override
    public void writeUserAssessmentToStream(UserAssessment userAssessment, OutputStream outputStream, boolean closeStreamWhenDone) throws IOException {
        try {
            outputStream.write(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(userAssessment));
        } finally {
            if (closeStreamWhenDone){
                outputStream.close();
            }
        }
    }

    @Override
    public <T extends Views> void writeAssessmentToStream(Assessment assessment, Class<T> viewClass, OutputStream outputStream, boolean closeStreamWhenDone) throws IOException {
        try {
            outputStream.write(objectMapper.writerWithView(viewClass).withDefaultPrettyPrinter().writeValueAsBytes(assessment));
        } finally {
            if (closeStreamWhenDone){
                outputStream.close();
            }
        }
    }

    @Override
    public String getFileNameForUserAssessment(UserAssessment userAssessment){
        return MessageFormat.format("{0}_{1}_{2}.json",
                userAssessment.getUsername(),
                userAssessment.getAssessmentCategory(),
                userAssessment.getId());
    }

    @Override
    public String storeUser(User user){
        return downloadTokenRepository.storeUser(user);
    }

    @Override
    public Try<User> retrieveUser(String token) {
        return downloadTokenRepository.retrieveUser(token);
    }
}
