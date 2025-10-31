package com.example.demo.model;

import jakarta.validation.constraints.NotBlank;

public class AskRequest {

    @NotBlank
    private String question;

    public AskRequest() { }

    public AskRequest(String question) {
        this.question = question;
    }

    public String getQuestion() {
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
    }
}