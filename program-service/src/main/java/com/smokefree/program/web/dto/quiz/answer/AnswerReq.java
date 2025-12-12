package com.smokefree.program.web.dto.quiz.answer;

/**
 * DTO representing a request to submit an answer for a specific question.
 *
 * @param questionNo The sequence number or identifier of the question.
 * @param answer     The selected answer value (e.g., option index or ID).
 */
public record AnswerReq(Integer questionNo, Integer answer) {}
