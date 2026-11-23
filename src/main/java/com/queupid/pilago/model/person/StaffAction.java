package com.queupid.pilago.model.person;

/** Things a staff member can do. Each staff subclass decides which ones it is allowed. */
public enum StaffAction {
    CALL_NEXT,
    SKIP,
    RECORD_PAYMENT,
    FORWARD_TO_CAPTAIN,
    PUT_ON_HOLD,
    VERIFY,
    RELEASE,
    VIEW_FOR_SIGNATURE,
    SIGN,
    RETURN_TO_SECRETARY,
    VIEW_DAILY_SUMMARY
}
