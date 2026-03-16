package com.javaweb.model;

import java.util.ArrayList;
import java.util.List;

public class ErrorResponseDTO {
    private String error;
    List<String> detail = new ArrayList<>();

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public List<String> getDetail() {
        return detail;
    }

    public void setDetails(List<String> detail) {
        this.detail = detail;
    }


}
