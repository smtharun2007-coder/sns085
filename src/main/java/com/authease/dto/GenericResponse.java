package com.authease.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericResponse {

    private String status;
    private String message;

    public GenericResponse() {
        this.status = "OK";
    }

    public GenericResponse(String status) {
        this.status = status;
    }

    public GenericResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public static GenericResponse ok() {
        return new GenericResponse("OK");
    }

    public static GenericResponse ok(String message) {
        return new GenericResponse("OK", message);
    }

    public String getStatus() {
        return status;
    }

    public boolean isOk() {
        return "OK".equalsIgnoreCase(status);
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
