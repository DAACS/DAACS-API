package com.daacs.repository;

import com.daacs.model.PendingStudent;
import com.daacs.model.User;
import com.daacs.model.UserSearchResult;
import com.lambdista.util.Try;

import java.util.List;

/**
 * Created by mgoldman
 */
public interface PendingStudentRepository {

    Try<PendingStudent> getPendStudent(String username);
    Try<Void> insertPendStudent(PendingStudent student);
    Try<Void> deletePendStudent(String id);
    Try<Void> updatePendStudent(PendingStudent student);
}
