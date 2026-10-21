package com.ksiracare.backend.exception;

/** A month outside what History covers (the current month and the one before). */
public class HistoryMonthNotAvailableException extends ApiException {

    public HistoryMonthNotAvailableException(String detail) {
        super(ErrorCode.HISTORY_MONTH_NOT_AVAILABLE, detail);
    }
}
