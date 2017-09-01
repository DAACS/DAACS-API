package com.daacs.service;

import com.daacs.component.HystrixCommandFactory;
import com.daacs.framework.exception.NotEnabledException;
import com.lambdista.util.Try;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Created by chostetter on 4/6/17.
 */
@Service
public class CanvasServiceImpl implements CanvasService {

    @Value("${canvas.enabled}")
    private boolean enabled;

    @Value("${canvas.courseId}")
    private Integer courseId;

    @Value("${canvas.assignmentId}")
    private Integer assignmentId;

    @Autowired
    private HystrixCommandFactory hystrixCommandFactory;

    @Override
    public boolean isEnabled(){
        return enabled;
    }

    @Override
    public Try<String> markAssignmentCompleted(String sisId){
        if(!isEnabled()){
            return new Try.Failure<>(new NotEnabledException("canvas", "Canvas is not enabled"));
        }

        return hystrixCommandFactory.getCanvasUpdateSubmissionHystrixCommand(
                "CanvasServiceImpl-markSubmissionCompleted",
                courseId,
                assignmentId,
                sisId,
                Arrays.asList(new BasicNameValuePair("submission[posted_grade]", "complete"))).execute();
    }
}
