package com.daacs.resource

import com.daacs.model.ClassScoreResults
import com.daacs.model.ErrorResponse
import com.daacs.model.User
import com.daacs.model.dto.InstructorClassUserScore
import com.daacs.service.DownloadService
import com.daacs.service.InstructorClassService
import com.lambdista.util.Try
import com.opencsv.CSVWriter
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping(value = "/class-scores", produces = "application/json")
public class ClassScoresController extends AuthenticatedController {
    protected static final Logger log = LoggerFactory.getLogger(ClassScoresController.class);

    @Autowired
    private InstructorClassService instructorClassService;

    @Autowired
    private DownloadService downloadService;

    @ApiOperation(
            value = "list student class scores",
            response = InstructorClassUserScore,
            responseContainer = "List"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
    public List<InstructorClassUserScore> getStudentScores(
            @RequestParam(value = "classId", required = false, defaultValue = '0') String classId) {

        checkPermissions([ROLE_INSTRUCTOR, ROLE_ADMIN]);
        return instructorClassService.getStudentScores(classId).checkedGet()
    }

    @ApiOperation(
            value = "download student class scores"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/download", method = RequestMethod.GET, params = ["classId", "downloadToken"], produces = "application/json")
    public void downloadResults(HttpServletResponse response,
                                @RequestParam(value = "classId", required = true) String classId,
                                @RequestParam(value = "downloadToken") String downloadToken) throws IOException {

        checkToken(downloadToken)

        ClassScoreResults results = instructorClassService.getStudentScoresCSV(classId).checkedGet()

        String csvFileName = "ClassResults.csv";
        response.setContentType("text/csv");

        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"",
                csvFileName);
        response.setHeader(headerKey, headerValue);

        CSVWriter writer = new CSVWriter(response.getWriter());

        instructorClassService.writeScoresToCSV(writer, results)

    }

    private void checkToken(String token) {
        Try<User> maybeUser = downloadService.retrieveUser(token);

        if (maybeUser.isFailure()) {
            throw maybeUser.failed().get();
        }

        checkPermissions(maybeUser.get(), [ROLE_INSTRUCTOR, ROLE_ADMIN]);
    }
}
