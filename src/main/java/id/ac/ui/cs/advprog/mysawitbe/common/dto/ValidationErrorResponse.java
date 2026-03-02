package id.ac.ui.cs.advprog.mysawitbe.common.dto;

import java.util.Map;

public record ValidationErrorResponse(
    Map<String, String> fieldErrors
) {}
