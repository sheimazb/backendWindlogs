package com.windlogs.tickets.enums;

/**
 * Enum representing the possible statuses of a solution
 */
public enum SolutionStatus {
    DRAFT,       // Solution is in draft mode, not yet submitted
    SUBMITTED,   // Solution has been submitted for review
    APPROVED,    // Solution has been approved
    REJECTED,    // Solution has been rejected
    IMPLEMENTED  // Solution has been implemented
}
