package com.daacs.service;

import com.daacs.model.assessment.user.CompletionScore;
import com.lambdista.util.Try;
import org.apache.commons.fileupload.FileItemStream;

/**
 * Created by chostetter on 8/15/16.
 */
public interface LightSideService {
    Try<Void> predict(String modelFileName, String inputFileName, String outputFileName);
    Try<Void> createInputFile(String fileName, String text);
    Try<CompletionScore> readOutputFile(String fileName);
    Try<Void> cleanUpFiles(String inputFileName, String outputFileName);
    Try<Void> setupFileSystem();
    Try<Void> saveUploadedModelFile(FileItemStream fileItemStream);
}
