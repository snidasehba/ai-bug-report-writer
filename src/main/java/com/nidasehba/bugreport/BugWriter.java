package com.nidasehba.bugreport;

/** Turns a (redacted) failure into a bug report. */
public interface BugWriter {

    BugReport write(FailureContext failure);

    String name();
}
