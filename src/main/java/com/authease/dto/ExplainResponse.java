package com.authease.dto;

public class ExplainResponse {

    private String text;
    private String source; // TEMPLATE | AI

    public ExplainResponse() {}

    public ExplainResponse(String text, String source) {
        this.text = text;
        this.source = source;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
