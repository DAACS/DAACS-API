package com.daacs.framework.exception;
import com.daacs.framework.hystrix.FailureType;
import com.daacs.framework.hystrix.FailureTypeException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lhorne on 5/16/17.
 */


public class NALightsideOutputException extends FailureTypeException {

    public NALightsideOutputException(String file) {
        super(
                getCode(),
                getDetail(file),
                FailureType.NOT_RETRYABLE,
                getMeta(file)
        );
    }

    public NALightsideOutputException(String file, Throwable t) {
        super(
                getCode(),
                getDetail(file),
                FailureType.NOT_RETRYABLE,
                getMeta(file),
                t
        );
    }

    public static String getCode(){
        return "lightside.NAOutput";
    }

    private static String getDetail(String file){
        return "NA LightSide output: " + file;
    }

    private static Map<String, Object> getMeta(String file){
        Map<String, Object> meta = new HashMap<>();
        meta.put("file", file);

        return meta;
    }
}