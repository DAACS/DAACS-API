package com.daacs.unit.service

import com.daacs.framework.serializer.ObjectMapperConfig
import com.daacs.framework.serializer.Views
import com.daacs.model.User
import com.daacs.model.assessment.*
import com.daacs.model.assessment.user.CATUserAssessment
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.model.item.Item
import com.daacs.model.item.ItemAnswer
import com.daacs.model.item.ItemGroup
import com.daacs.repository.DownloadTokenRepository
import com.daacs.service.DownloadService
import com.daacs.service.DownloadServiceImpl
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.lambdista.util.Try
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Created by chostetter on 8/5/16.
 */
class DownloadServiceSpec extends Specification {

    DownloadService downloadService
    ObjectMapper objectMapper
    ObjectWriter objectWriterWithView
    DownloadTokenRepository downloadTokenRepository

    List<UserAssessment> userAssessments = [
            new CATUserAssessment(
                    id: "1",
                    assessmentCategory: AssessmentCategory.MATHEMATICS,
                    username: "user123"
            )
    ];

    WritingAssessment writingAssessment = new WritingAssessment(
            id: "1",
            assessmentType: AssessmentType.WRITING_PROMPT,
            assessmentCategory: AssessmentCategory.WRITING
    )

    CATAssessment catAssessment = new CATAssessment(
            id: "2",
            assessmentType: AssessmentType.CAT,
            assessmentCategory: AssessmentCategory.MATHEMATICS
    )

    MultipleChoiceAssessment multipleChoiceAssessment  = new MultipleChoiceAssessment(
            id: "3",
            assessmentType: AssessmentType.MULTIPLE_CHOICE,
            assessmentCategory: AssessmentCategory.COLLEGE_SKILLS,
            content: ["landing": "whatever we need to say here"],
            itemGroups: [
                    new ItemGroup(id: "itemgroup-1", items: [
                            new Item(id: "item-1", possibleItemAnswers: [ new ItemAnswer(id: "answer-1", score: 1) ]),
                            new Item(id: "item-2", possibleItemAnswers: [ new ItemAnswer(id: "answer-2", score: 1) ])])
            ]

    )

    def setup(){
        downloadTokenRepository = Mock(DownloadTokenRepository)
        objectMapper = new ObjectMapperConfig().objectMapper()
        downloadService = new DownloadServiceImpl(objectMapper: objectMapper, downloadTokenRepository: downloadTokenRepository)

        objectWriterWithView = objectMapper.writerWithView(Views.Export.class).withDefaultPrettyPrinter()
    }

    def "writeUserAssessmentsToStream: success"(){
        setup:
        PipedInputStream inStream = new PipedInputStream();
        ZipInputStream zipInputStream = new ZipInputStream(inStream);
        List<ZipEntry> zipEntries = [];
        List<String> userAssessmentJson = [];

        when:
        downloadService.writeUserAssessmentsToStream(userAssessments, new PipedOutputStream(inStream))

        then:
        notThrown(Exception)

        then:
        ZipEntry entry;
        while((entry = zipInputStream.getNextEntry()) != null){
            zipEntries.add(entry);

            StringBuilder stringBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = zipInputStream.read(buffer, 0, 1024)) >= 0) {
                stringBuilder.append(new String(buffer, 0, read));
            }

            userAssessmentJson.add(stringBuilder.toString());
        }

        then:
        zipEntries.size() == 1
        zipEntries.get(0).getName() == "user123_MATHEMATICS_1.json"
        zipEntries.get(0).getSize() == objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(userAssessments.get(0)).size()
        userAssessmentJson.get(0) == objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userAssessments.get(0))
    }

    def "writeUserAssessmentsToStream: throws IOException"(){
        setup:
        OutputStream outputStream = Mock(OutputStream)
        outputStream.write(_) >> {throw new IOException()}

        when:
        downloadService.writeUserAssessmentsToStream(userAssessments, outputStream)

        then:
        thrown(IOException)
    }

    def "writeUserAssessmentToStream: success"(){
        setup:
        PipedInputStream inStream = new PipedInputStream();

        when:
        downloadService.writeUserAssessmentToStream(userAssessments.get(0), new PipedOutputStream(inStream), true)

        then:
        notThrown(Exception)

        then:
        StringBuilder stringBuilder = new StringBuilder()
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inStream.read(buffer, 0, 1024)) >= 0) {
            stringBuilder.append(new String(buffer, 0, read));
        }

        stringBuilder.toString() == objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(userAssessments.get(0))
    }

    def "writeUserAssessmentToStream: throws IOException"(){
        setup:
        OutputStream outputStream = Mock(OutputStream)
        outputStream.write(_) >> {throw new IOException()}

        when:
        downloadService.writeUserAssessmentToStream(userAssessments.get(0), outputStream, true)

        then:
        thrown(IOException)
    }

    def "writeAssessmentToStream: writing success"(){
        setup:
        PipedInputStream inStream = new PipedInputStream();

        when:
        downloadService.writeAssessmentToStream(writingAssessment, Views.Export.class, new PipedOutputStream(inStream), true)

        then:
        notThrown(Exception)

        then:
        StringBuilder stringBuilder = new StringBuilder()
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inStream.read(buffer, 0, 1024)) >= 0) {
            stringBuilder.append(new String(buffer, 0, read));
        }

        stringBuilder.toString() == objectWriterWithView.writeValueAsString(writingAssessment)
    }

    def "writeAssessmentToStream: cat success"(){
        setup:
        PipedInputStream inStream = new PipedInputStream();

        when:
        downloadService.writeAssessmentToStream(catAssessment, Views.Export.class, new PipedOutputStream(inStream), true)

        then:
        notThrown(Exception)

        then:
        StringBuilder stringBuilder = new StringBuilder()
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inStream.read(buffer, 0, 1024)) >= 0) {
            stringBuilder.append(new String(buffer, 0, read));
        }

        stringBuilder.toString() == objectWriterWithView.writeValueAsString(catAssessment)
    }

    def "writeAssessmentToStream: multipleChoice success"(){
        setup:
        PipedInputStream inStream = new PipedInputStream();

        when:
        downloadService.writeAssessmentToStream(multipleChoiceAssessment, Views.Export.class, new PipedOutputStream(inStream), true)

        then:
        notThrown(Exception)

        then:
        StringBuilder stringBuilder = new StringBuilder()
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inStream.read(buffer, 0, 1024)) >= 0) {
            stringBuilder.append(new String(buffer, 0, read));
        }

        stringBuilder.toString() == objectWriterWithView.writeValueAsString(multipleChoiceAssessment)
    }

    def "writeAssessmentToStream: throws IOException"(){
        setup:
        OutputStream outputStream = Mock(OutputStream)
        outputStream.write(_) >> {throw new IOException()}

        when:
        downloadService.writeAssessmentToStream(writingAssessment, Views.Export.class, outputStream, true)

        then:
        thrown(IOException)
    }

    def "storeUser: success"(){
        setup:
        User user = new User()

        when:
        String key = downloadService.storeUser(user)

        then:
        1 * downloadTokenRepository.storeUser(user) >> "abc123"

        then:
        key == "abc123"
    }

    def "retrieveUser: success"(){
        setup:
        User user = new User()

        when:
        Try<User> maybeUser = downloadService.retrieveUser("abc123")

        then:
        1 * downloadTokenRepository.retrieveUser("abc123") >> new Try.Success<User>(user)

        then:
        maybeUser.get() == user
    }
}
