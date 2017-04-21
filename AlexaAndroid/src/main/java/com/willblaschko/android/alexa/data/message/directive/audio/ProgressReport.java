package com.willblaschko.android.alexa.data.message.directive.audio;

/**
 * If this no exist, not need use.
 */
public class ProgressReport {
    /**
     * Specifies (in milliseconds) when to send the ProgressReportDelayElapsed event to AVS.
     * ProgressReportDelayElapsed must only be sent once at the specified interval.
     * Please note: Some music providers do not require this report.
     * If the report is not required, progressReportDelayInMilliseconds will not appear in the payload.
     */
    private long progressReportDelayInMilliseconds;

    /**
     * Specifies (in milliseconds) when to emit a ProgressReportIntervalElapsed event to AVS.
     * ProgressReportIntervalElapsed must be sent periodically at the specified interval.
     * Please note: Some music providers do not require this report.
     * If the report is not required, progressReportIntervalInMilliseconds will not appear in the payload.
     * */
    private long progressReportIntervalInMilliseconds;

    public long getProgressReportDelayInMilliseconds() {
        return progressReportDelayInMilliseconds;
    }

    public long getProgressReportIntervalInMilliseconds() {
        return progressReportIntervalInMilliseconds;
    }

    public void setProgressReportDelayInMilliseconds(long progressReportDelayInMilliseconds) {
        this.progressReportDelayInMilliseconds = progressReportDelayInMilliseconds;
    }

    public void setProgressReportIntervalInMilliseconds(long progressReportIntervalInMilliseconds) {
        this.progressReportIntervalInMilliseconds = progressReportIntervalInMilliseconds;
    }

    public boolean isRequired() {
        return progressReportDelayInMilliseconds > 0 || progressReportIntervalInMilliseconds > 0;
    }
}