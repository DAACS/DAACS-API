package com.daacs.unit.service

import com.daacs.component.HystrixCommandFactory
import com.daacs.framework.exception.InvalidLightSideOutputException
import com.daacs.framework.exception.NALightsideOutputException
import com.daacs.model.assessment.user.CompletionScore
import com.daacs.service.LightSideService
import com.daacs.service.LightSideServiceImpl
import com.daacs.service.hystrix.*
import com.lambdista.util.Try
import org.apache.commons.fileupload.FileItemStream
import org.omg.CORBA.CompletionStatus
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Path
import java.nio.file.Paths
/**
 * Created by chostetter on 8/5/16.
 */
class LightSideServiceSpec extends Specification {

    LightSideService lightSideService;
    HystrixCommandFactory hystrixCommandFactory;

    String lightSideDirString;
    String lightSideModelsDirString;
    String lightSideOutputDirString;

    Path lightSideDir;
    Path lightSideModelsDir;
    Path lightSideOutputDir;
    Path predictScript;

    LightSideDirCheckHystrixCommand lightSideDirCheckHystrixCommand
    WriteCsvHystrixCommand writeCsvHystrixCommand
    LightSideInputCheckHystrixCommand lightSideInputCheckHystrixCommand
    ExecuteCommandHystrixCommand executeCommandHystrixCommand
    ReadCsvHystrixCommand readCsvHystrixCommand
    DeleteFileHystrixCommand deleteFileHystrixCommand
    WriteFileHystrixCommand writeFileHystrixCommand

    def setup(){
        hystrixCommandFactory = Mock(HystrixCommandFactory)

        lightSideDirCheckHystrixCommand = Mock(LightSideDirCheckHystrixCommand)
        writeCsvHystrixCommand = Mock(WriteCsvHystrixCommand)
        lightSideInputCheckHystrixCommand = Mock(LightSideInputCheckHystrixCommand)
        executeCommandHystrixCommand = Mock(ExecuteCommandHystrixCommand)
        readCsvHystrixCommand = Mock(ReadCsvHystrixCommand)
        deleteFileHystrixCommand = Mock(DeleteFileHystrixCommand)
        writeFileHystrixCommand = Mock(WriteFileHystrixCommand)

        hystrixCommandFactory.getLightSideDirCheckHystrixCommand(*_) >> lightSideDirCheckHystrixCommand
        hystrixCommandFactory.getWriteCsvHystrixCommand(*_) >> writeCsvHystrixCommand
        hystrixCommandFactory.getLightSideInputCheckHystrixCommand(*_) >> lightSideInputCheckHystrixCommand
        hystrixCommandFactory.getExecuteCommandHystrixCommand(*_) >> executeCommandHystrixCommand
        hystrixCommandFactory.getReadCsvHystrixCommand(*_) >> readCsvHystrixCommand
        hystrixCommandFactory.getDeleteFileHystrixCommand(*_) >> deleteFileHystrixCommand
        hystrixCommandFactory.getWriteFileHystrixCommand(*_) >> writeFileHystrixCommand

        lightSideDirString = "lightside_dir"
        lightSideModelsDirString = "lightside_models_dir"
        lightSideOutputDirString = "lightside_output_dir"

        lightSideDir = Paths.get(lightSideDirString)
        lightSideModelsDir = Paths.get(lightSideModelsDirString)
        lightSideOutputDir = Paths.get(lightSideOutputDirString)
        predictScript = lightSideDir.resolve("scripts/predict.sh")

        lightSideService = new LightSideServiceImpl(
                hystrixCommandFactory: hystrixCommandFactory,
                lightSideDirString: lightSideDirString,
                lightSideModelsDirString: lightSideModelsDirString,
                lightSideOutputDirString: lightSideOutputDirString
        )
    }

    def "setupFileSystem: success"(){
        when:
        Try<Void> maybeResults = lightSideService.setupFileSystem()

        then:
        1 * hystrixCommandFactory.getLightSideDirCheckHystrixCommand(_, lightSideModelsDir, lightSideOutputDir, predictScript) >> lightSideDirCheckHystrixCommand
        1 * lightSideDirCheckHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "setupFileSystem: lightSideDirCheckHystrixCommand fails, i fail"(){
        when:
        Try<Void> maybeResults = lightSideService.setupFileSystem()

        then:
        1 * lightSideDirCheckHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "createInputFile: success"(){
        setup:
        String fileName = "test.csv"
        String text = "this is my text"

        when:
        Try<Void> maybeResults = lightSideService.createInputFile(fileName, text)

        then:
        1 * hystrixCommandFactory.getWriteCsvHystrixCommand(_, lightSideOutputDir.resolve(fileName), _) >> { args ->
            List<String[]> lines = args[2]
            assert lines.size() == 2
            assert lines.get(0)[0] == "text"
            assert lines.get(1)[0] == text

            return writeCsvHystrixCommand
        }

        1 * writeCsvHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeResults.isSuccess()
    }

    def "createInputFile: writeCsvHystrixCommand fails, i fail"(){
        setup:
        String fileName = "test.csv"
        String text = "this is my text"

        when:
        Try<Void> maybeResults = lightSideService.createInputFile(fileName, text)

        then:
        1 * writeCsvHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybeResults.isFailure()
    }

    def "predict: success"(){
        setup:
        String modelFileName = "model.xml"
        String inputFileName = "input.csv"
        String outputFileName = "output.csv"

        when:
        Try<Void> maybePredicted = lightSideService.predict(modelFileName, inputFileName, outputFileName)

        then:
        1 * hystrixCommandFactory.getLightSideInputCheckHystrixCommand(
                _,
                lightSideModelsDir.resolve(modelFileName),
                lightSideOutputDir.resolve(inputFileName),
                lightSideOutputDir.resolve(outputFileName)) >> lightSideInputCheckHystrixCommand
        1 * lightSideInputCheckHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        1 * hystrixCommandFactory.getExecuteCommandHystrixCommand(_, _, null, lightSideDir.toFile()) >> { args ->
            String command = args[1]
            assert command == predictScript.toAbsolutePath().toString() + " " +
                    lightSideModelsDir.resolve(modelFileName).toAbsolutePath().toString() + " UTF-8 " +
                    lightSideOutputDir.resolve(inputFileName).toAbsolutePath().toString() + " " +
                    lightSideOutputDir.resolve(outputFileName).toAbsolutePath().toString()

            return executeCommandHystrixCommand
        }

        1 * executeCommandHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybePredicted.isSuccess()
    }

    def "predict: lightSideInputCheckHystrixCommand fails, i fail"(){
        setup:
        String modelFileName = "model.xml"
        String inputFileName = "input.csv"
        String outputFileName = "output.csv"

        when:
        Try<Void> maybePredicted = lightSideService.predict(modelFileName, inputFileName, outputFileName)

        then:
        1 * lightSideInputCheckHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        0 * hystrixCommandFactory.getExecuteCommandHystrixCommand(*_)
        0 * executeCommandHystrixCommand.execute()

        then:
        maybePredicted.isFailure()
    }

    def "predict: executeCommandHystrixCommand fails, i fail"(){
        setup:
        String modelFileName = "model.xml"
        String inputFileName = "input.csv"
        String outputFileName = "output.csv"

        when:
        Try<Void> maybePredicted = lightSideService.predict(modelFileName, inputFileName, outputFileName)

        then:
        1 * lightSideInputCheckHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        1 * executeCommandHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybePredicted.isFailure()
    }

    def "readOutputFile: success"(){
        setup:
        String outputFileName = "output.csv"

        when:
        Try<CompletionScore> maybeCompletionScore = lightSideService.readOutputFile(outputFileName)

        then:
        1 * hystrixCommandFactory.getReadCsvHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> readCsvHystrixCommand
        1 * readCsvHystrixCommand.execute() >> new Try.Success<List<String[]>>([ (String[]) ["predicted", "text"].toArray(), (String[])["1", "some text"].toArray() ])

        then:
        maybeCompletionScore.isSuccess()
        maybeCompletionScore.get() == CompletionScore.LOW
    }

    def "readOutputFile: bad int score"(){
        setup:
        String outputFileName = "output.csv"

        when:
        Try<CompletionScore> maybeCompletionScore = lightSideService.readOutputFile(outputFileName)

        then:
        1 * hystrixCommandFactory.getReadCsvHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> readCsvHystrixCommand
        1 * readCsvHystrixCommand.execute() >> new Try.Success<List<String[]>>([ (String[]) ["predicted", "text"].toArray(), (String[])["7", "some text"].toArray() ])

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidLightSideOutputException
    }
    def "readOutputFile: bad str score"(){
        setup:
        String outputFileName = "output.csv"

        when:
        Try<CompletionScore> maybeCompletionScore = lightSideService.readOutputFile(outputFileName)

        then:
        1 * hystrixCommandFactory.getReadCsvHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> readCsvHystrixCommand
        1 * readCsvHystrixCommand.execute() >> new Try.Success<List<String[]>>([ (String[]) ["predicted", "text"].toArray(), (String[])["LOW", "some text"].toArray() ])

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidLightSideOutputException
    }

    @Unroll
    def "readOutputFile: bad output"(List<String[]> returnedLines){
        setup:
        String outputFileName = "output.csv"

        when:
        Try<CompletionScore> maybeCompletionScore = lightSideService.readOutputFile(outputFileName)

        then:
        1 * hystrixCommandFactory.getReadCsvHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> readCsvHystrixCommand
        1 * readCsvHystrixCommand.execute() >> new Try.Success<List<String[]>>(returnedLines)

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof InvalidLightSideOutputException

        where:
        returnedLines << [
                [ (String[]) ["asdf", "text"].toArray(), (String[])["asdf", "some text"].toArray() ],
                [ (String[]) [].toArray(), (String[])[].toArray() ],
                [],
                [ (String[]) ["asdf", "text"].toArray(), (String[])[].toArray() ]
        ]
    }

    def "readOutputFile: NA output"(List<String[]> returnedLines){
        setup:
        String outputFileName = "output.csv"

        when:
        Try<CompletionScore> maybeCompletionScore = lightSideService.readOutputFile(outputFileName)

        then:
        1 * hystrixCommandFactory.getReadCsvHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> readCsvHystrixCommand
        1 * readCsvHystrixCommand.execute() >> new Try.Success<List<String[]>>(returnedLines)

        then:
        maybeCompletionScore.isFailure()
        maybeCompletionScore.failed().get() instanceof NALightsideOutputException

        where:
        returnedLines << [
                [ (String[]) ["asdf", "text"].toArray(), (String[])["NA", "some text"].toArray() ],
                [ (String[]) [].toArray(), (String[])["NA"].toArray() ],
                [ (String[]) ["asdf", "text"].toArray(), (String[])["NA"].toArray() ]
        ]
    }

    def "readOutputFile: readCsvHystrixCommand fails, i fail"(){
        setup:
        String outputFileName = "output.csv"

        when:
        Try<CompletionScore> maybeCompletionScore = lightSideService.readOutputFile(outputFileName)

        then:
        1 * readCsvHystrixCommand.execute() >> new Try.Failure<List<String[]>>(new Exception())

        then:
        maybeCompletionScore.isFailure()
    }

    def "cleanUpFiles: success"(){
        setup:
        String inputFileName = "input.csv"
        String outputFileName = "output.csv"

        when:
        Try<Void> maybeCleaned = lightSideService.cleanUpFiles(inputFileName, outputFileName)

        then:
        1 * hystrixCommandFactory.getDeleteFileHystrixCommand(_, lightSideOutputDir.resolve(inputFileName)) >> deleteFileHystrixCommand
        1 * deleteFileHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        1 * hystrixCommandFactory.getDeleteFileHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> deleteFileHystrixCommand
        1 * deleteFileHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeCleaned.isSuccess()
    }

    def "cleanUpFiles: deleteFileHystrixCommand inputFile fails, i fail"(){
        setup:
        String inputFileName = "input.csv"
        String outputFileName = "output.csv"

        when:
        Try<Void> maybeCleaned = lightSideService.cleanUpFiles(inputFileName, outputFileName)

        then:
        1 * hystrixCommandFactory.getDeleteFileHystrixCommand(_, lightSideOutputDir.resolve(inputFileName)) >> deleteFileHystrixCommand
        1 * deleteFileHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        0 * hystrixCommandFactory.getDeleteFileHystrixCommand(_, lightSideOutputDir.resolve(outputFileName))
        0 * deleteFileHystrixCommand.execute()

        then:
        maybeCleaned.isFailure()
    }

    def "cleanUpFiles: deleteFileHystrixCommand outputFile fails, i fail"(){
        setup:
        String inputFileName = "input.csv"
        String outputFileName = "output.csv"

        when:
        Try<Void> maybeCleaned = lightSideService.cleanUpFiles(inputFileName, outputFileName)

        then:
        1 * hystrixCommandFactory.getDeleteFileHystrixCommand(_, lightSideOutputDir.resolve(inputFileName)) >> deleteFileHystrixCommand
        1 * deleteFileHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        1 * hystrixCommandFactory.getDeleteFileHystrixCommand(_, lightSideOutputDir.resolve(outputFileName)) >> deleteFileHystrixCommand
        1 * deleteFileHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybeCleaned.isFailure()
    }

    def "saveUploadedModelFile: success"(){
        setup:
        FileItemStream fileItemStream = Mock(FileItemStream)
        InputStream inputStream = Mock(InputStream)
        String outputFileName = "test_file.xml"

        when:
        Try<Void> maybeSavedFile = lightSideService.saveUploadedModelFile(fileItemStream)

        then:
        1 * fileItemStream.getContentType() >> "text/xml"
        1 * fileItemStream.getName() >> outputFileName
        1 * fileItemStream.openStream() >> inputStream

        then:
        1 * hystrixCommandFactory.getWriteFileHystrixCommand(_, inputStream, lightSideModelsDir.resolve(outputFileName)) >> writeFileHystrixCommand
        1 * writeFileHystrixCommand.execute() >> new Try.Success<Void>(null)

        then:
        maybeSavedFile.isSuccess()
    }

    def "saveUploadedModelFile: fails if content type is wrong"(){
        setup:
        FileItemStream fileItemStream = Mock(FileItemStream)

        when:
        Try<Void> maybeSavedFile = lightSideService.saveUploadedModelFile(fileItemStream)

        then:
        1 * fileItemStream.getContentType() >> "text/plain"

        then:
        0 * hystrixCommandFactory.getWriteFileHystrixCommand(*_)

        then:
        maybeSavedFile.isFailure()
    }

    def "saveUploadedModelFile: write file fails, i fail"(){
        setup:
        FileItemStream fileItemStream = Mock(FileItemStream)
        InputStream inputStream = Mock(InputStream)

        when:
        Try<Void> maybeSavedFile = lightSideService.saveUploadedModelFile(fileItemStream)

        then:
        1 * fileItemStream.getContentType() >> "text/xml"
        1 * fileItemStream.getName() >> "test_file.xml"
        1 * fileItemStream.openStream() >> inputStream

        then:
        1 * writeFileHystrixCommand.execute() >> new Try.Failure<Void>(new Exception())

        then:
        maybeSavedFile.isFailure()
    }

    def "saveUploadedModelFile: openStream throws exception"(){
        setup:
        FileItemStream fileItemStream = Mock(FileItemStream)

        when:
        Try<Void> maybeSavedFile = lightSideService.saveUploadedModelFile(fileItemStream)

        then:
        1 * fileItemStream.getContentType() >> "text/xml"
        1 * fileItemStream.openStream() >> { throw new IOException() }

        then:
        0 * writeFileHystrixCommand.execute()

        then:
        maybeSavedFile.isFailure()
    }
}