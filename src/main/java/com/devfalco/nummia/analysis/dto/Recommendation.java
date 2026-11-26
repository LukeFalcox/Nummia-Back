package com.devfalco.nummia.analysis.dto;

import com.devfalco.nummia.notification.Severity;

public record Recommendation(String type, Severity severity, String title, String message) {
}
