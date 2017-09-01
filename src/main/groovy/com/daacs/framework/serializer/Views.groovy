package com.daacs.framework.serializer

/**
 * Created by chostetter on 7/28/16.
 */
public class Views {
    public static class NotExport {}

    public static class Student extends NotExport {}

    public static class CompletedAssessment extends Student {}

    public static class Admin extends Student {}

    public static class Export {};
}