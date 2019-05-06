package com.daacs.service;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.IncompatibleTypeException;
import com.daacs.framework.exception.InvalidLightSideOutputException;
import com.daacs.framework.exception.NALightsideOutputException;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;
import com.daacs.model.assessment.user.CompletionScore;
import com.lambdista.util.Try;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by chostetter on 8/15/16.
 */
@Service
public class LightSideServiceImpl implements LightSideService {

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Value("${lightside.dir}")
    private String lightSideDirString;

    @Value("${lightside.models.dir}")
    private String lightSideModelsDirString;

    @Value("${lightside.output.dir}")
    private String lightSideOutputDirString;

    private static final String encoding = "UTF-8";

    private Path getPredictScript() {
        return getLightSideDir().resolve("scripts/predict.sh");
    }

    private Path getLightSideDir() {
        return Paths.get(lightSideDirString);
    }

    private Path getLightSideModelsDir() {
        return Paths.get(lightSideModelsDirString);
    }

    private Path getLightSideOutputDir() {
        return Paths.get(lightSideOutputDirString);
    }

    @Override
    public Try<Void> setupFileSystem() {
        return hystrixCommandFactory.getLightSideDirCheckHystrixCommand("LightSideServiceImpl-dirSetup", getLightSideModelsDir(), getLightSideOutputDir(), getPredictScript()).execute();
    }

    @Override
    public Try<Void> createInputFile(String fileName, String text) {
        Path inputFile = getLightSideOutputDir().resolve(fileName);
        List<String[]> writableLines = new ArrayList<String[]>() {{
            add(new String[]{"text"});
            add(new String[]{text});
        }};

        return hystrixCommandFactory.getWriteCsvHystrixCommand("LightSideImpl-createInputFile", inputFile, writableLines).execute();
    }

    @Override
    public Try<Void> predict(String modelFileName, String inputFileName, String outputFileName) {
        Path modelFile = getLightSideModelsDir().resolve(modelFileName);
        Path inputFile = getLightSideOutputDir().resolve(inputFileName);
        Path outputFile = getLightSideOutputDir().resolve(outputFileName);

        Try<Void> maybeChecked = hystrixCommandFactory.getLightSideInputCheckHystrixCommand("LightSideServiceImpl-inputCheck", modelFile, inputFile, outputFile).execute();
        if (maybeChecked.isFailure()) {
            return new Try.Failure<>(maybeChecked.failed().get());
        }

        String scriptCommand = MessageFormat.format(
                "{0} {1} {2} {3} {4}",
                getPredictScript().toAbsolutePath().toString(),
                modelFile.toAbsolutePath().toString(),
                encoding,
                inputFile.toAbsolutePath().toString(),
                outputFile.toAbsolutePath().toString());

        Try<String> maybeOutput = hystrixCommandFactory.getExecuteCommandHystrixCommand("LightSideServiceImpl-predict", scriptCommand, null, getLightSideDir().toFile()).execute();
        if (maybeOutput.isFailure()) {
            return new Try.Failure<>(maybeOutput.failed().get());
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<CompletionScore> readOutputFile(String fileName) {
        Path outputFile = getLightSideOutputDir().resolve(fileName);
        Try<List<String[]>> maybeLines = hystrixCommandFactory.getReadCsvHystrixCommand("LightSideImpl-readOutputFile", outputFile).execute();
        if (maybeLines.isFailure()) {
            return new Try.Failure<>(maybeLines.failed().get());
        }

        List<String[]> lines = maybeLines.get();
        if (lines.size() != 2) {
            return new Try.Failure<>(new InvalidLightSideOutputException(outputFile.toString()));
        }

        try {
            if (lines.get(1)[0].equals("NA")) {
                return new Try.Failure<>(new NALightsideOutputException(outputFile.toString()));
            }
            CompletionScore completionScore = CompletionScore.getEnum(lines.get(1)[0]);
            return new Try.Success<>(completionScore);
        } catch (Exception ex) {
            return new Try.Failure<>(new InvalidLightSideOutputException(outputFile.toString(), ex));
        }
    }

    @Override
    public Try<Void> cleanUpFiles(String inputFileName, String outputFileName) {
        Try<Void> maybeDeletedInputfile = hystrixCommandFactory.getDeleteFileHystrixCommand("LightSideServiceImpl-deleteInputFile", getLightSideOutputDir().resolve(inputFileName)).execute();
        if (maybeDeletedInputfile.isFailure()) {
            return new Try.Failure<>(maybeDeletedInputfile.failed().get());
        }

        Try<Void> maybeDeletedOutputfile = hystrixCommandFactory.getDeleteFileHystrixCommand("LightSideServiceImpl-deleteOutputFile", getLightSideOutputDir().resolve(outputFileName)).execute();
        if (maybeDeletedOutputfile.isFailure()) {
            return new Try.Failure<>(maybeDeletedOutputfile.failed().get());
        }

        return new Try.Success<>(null);
    }

    @Override
    public Try<String> saveUploadedModelFile(MultipartFile file) {

        String outputFileName = file.getOriginalFilename();
        String contentType = FilenameUtils.getExtension(outputFileName);
        if (!contentType.equals("xml")) {
            return new Try.Failure<>(new IncompatibleTypeException("FileItemStream", new String[]{"xml"}, contentType));
        }

        try {
            InputStream inputStream = file.getInputStream();

            Try<Void> maybeWroteFile = hystrixCommandFactory.getWriteFileHystrixCommand("LightSideServiceImpl-saveUploadedModelFile", inputStream, getLightSideModelsDir().resolve(outputFileName)).execute();
            if (maybeWroteFile.isFailure()) {
                return new Try.Failure<>(maybeWroteFile.failed().get());
            }

            inputStream.close();
        } catch (Exception ex) {
            return new Try.Failure<>(new FailureTypeException("saveModelFile.failed", "failed to save model files", FailureType.RETRYABLE, ex));
        }

        return new Try.Success<>(outputFileName);
    }
}
