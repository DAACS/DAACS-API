package com.daacs.component.utils;

import com.daacs.model.assessment.Assessment;
import com.lambdista.util.Try;

/**
 * Created by lhorne on 9/18/18.
 */
public interface UpgradeAssessmentSchemaUtils {

    Try<Assessment> upgradeAssessmentSchema(Assessment assessment);

}

