package com.daacs.resource

import com.daacs.framework.serializer.Views
import com.daacs.model.ErrorResponse
import com.daacs.model.User
import com.daacs.model.assessment.Assessment
import com.daacs.model.assessment.user.UserAssessment
import com.daacs.service.AssessmentService
import com.daacs.service.DownloadService
import com.daacs.service.UserAssessmentService
import com.fasterxml.jackson.annotation.JsonView
import com.lambdista.util.Try
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.FileCopyUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletResponse
import java.text.MessageFormat
import java.time.Instant
/**
 * Created by chostetter on 8/18/16.
 */
@RestController
@RequestMapping(value = "/download", produces = "application/json")
public class DownloadController extends AuthenticatedController {
    protected static final Logger log = LoggerFactory.getLogger(DownloadController.class);

    @Autowired
    private UserAssessmentService userAssessmentService;

    @Autowired
    private AssessmentService assessmentService;

    @Autowired
    private DownloadService downloadService;

    @ApiOperation(
            value = "get a download key",
            response = Map
    )
    @ApiResponses(value = [
            @ApiResponse(code = 400, response = ErrorResponse.class, message = "Invalid request"),
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/token", method = RequestMethod.GET, produces = "application/json")
    public Map<String, String> getDownloadToken(){

        checkPermissions([ROLE_ADMIN]);
        return ["downloadToken" : downloadService.storeUser(getLoggedInUser())];
    }

    @ApiOperation(
            value = "export user assessments for manual grading"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/manual-grade-user-assessments", method = RequestMethod.GET, params = ["downloadToken"], produces = "application/json")
    public void downloadManualGradeUserAssessments(HttpServletResponse response,
                                                 @RequestParam(value = "downloadToken") String downloadToken){

        checkToken(downloadToken);

        Try<List<UserAssessment>> maybeUserAssessments = userAssessmentService.getUserAssessmentsForManualGrading();
        if(maybeUserAssessments.isFailure()){
            throw maybeUserAssessments.failed().get();
        }

        PipedInputStream inStream = new PipedInputStream();
        OutputStream outputStream = new PipedOutputStream(inStream);

        Runnable task = {
            try{
                downloadService.writeUserAssessmentsToStream(maybeUserAssessments.get(), outputStream);
            }
            catch(IOException ex){
                log.error("Error while exporting User Assessments", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        };

        new Thread(task).start();

        String fileName = MessageFormat.format("UserAssessments_{0}.zip", Instant.now().toString());

        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentType("application/zip");

        FileCopyUtils.copy(inStream, response.getOutputStream());
    }

    @ApiOperation(
            value = "export user assessment"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/user-assessments", method = RequestMethod.GET, params = ["id", "userId", "downloadToken"], produces = "application/json")
    public void downloadUserAssessment(HttpServletResponse response,
                                     @RequestParam(value = "id") String id,
                                     @RequestParam(value = "userId") String userId,
                                     @RequestParam(value = "downloadToken") String downloadToken){

        checkToken(downloadToken);

        Try<UserAssessment> maybeUserAssessment = userAssessmentService.getUserAssesment(userId, id);
        if(maybeUserAssessment.isFailure()){
            throw maybeUserAssessment.failed().get();
        }

        PipedInputStream inStream = new PipedInputStream();
        OutputStream outputStream = new PipedOutputStream(inStream);

        Runnable task = {
            try{
                downloadService.writeUserAssessmentToStream(maybeUserAssessment.get(), outputStream, true);
            }
            catch(IOException ex){
                log.error("Error while exporting User Assessment", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        };

        new Thread(task).start();

        String fileName = downloadService.getFileNameForUserAssessment(maybeUserAssessment.get());

        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentType("application/json");

        FileCopyUtils.copy(inStream, response.getOutputStream());
    }

    @ApiOperation(
            value = "export assessment"
    )
    @ApiResponses(value = [
            @ApiResponse(code = 500, response = ErrorResponse.class, message = "Unknown error"),
            @ApiResponse(code = 503, response = ErrorResponse.class, message = "Retryable error")
    ])
    @RequestMapping(value = "/assessments", method = RequestMethod.GET, params = ["downloadToken", "id"], produces = "application/json")
    public void downloadAssessment(HttpServletResponse response,
        @RequestParam(value = "id") String id,
        @RequestParam(value = "downloadToken") String downloadToken){

        checkToken(downloadToken);

        Try<Assessment> maybeAssessment = assessmentService.getAssessment(id);
        if(maybeAssessment.isFailure()){
            throw maybeAssessment.failed().get();
        }

        PipedInputStream inStream = new PipedInputStream();
        OutputStream outputStream = new PipedOutputStream(inStream);

        Runnable task = {
            try{
                downloadService.writeAssessmentToStream(maybeAssessment.get(), Views.Export.class, outputStream, true);
            }
            catch(IOException ex){
                log.error("Error while exporting User Assessments", ex);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        };

        new Thread(task).start();

        String fileName = MessageFormat.format("Assessment_{0}.json", maybeAssessment.get().id);

        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        response.setContentType("application/json");

        FileCopyUtils.copy(inStream, response.getOutputStream());
    }

    private void checkToken(String token){
        Try<User> maybeUser = downloadService.retrieveUser(token);
        if(maybeUser.isFailure()){
            throw maybeUser.failed().get();
        }

        checkPermissions(maybeUser.get(), [ROLE_ADMIN]);
    }
}
