package com.javaweb.ai.service;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
class ChatbotGuardrails {
    private static final String SYSTEM_PROMPT = """
            You are a real-estate assistant for a property management platform.
            Help users clarify buying or renting needs, explain listing information, suggest next steps,
            and recommend contacting an agent when the user needs human support.
            Guardrails:
            - Do not provide legal, tax, investment, loan, or financial advice as a professional conclusion.
            - For complex legal or financial questions, recommend contacting a qualified professional or agent.
            - Do not reveal private owner, customer, internal pricing, identity, phone, email, or account data.
            - Do not claim that appointments, leads, transactions, or contracts were created unless the system explicitly says so.
            Keep responses concise and practical.
            """;

    String systemPrompt() {
        return SYSTEM_PROMPT;
    }

    boolean needsProfessionalReferral(String content) {
        String normalized = content.toLowerCase(Locale.ROOT);
        return normalized.contains("phap ly")
                || normalized.contains("pháp lý")
                || normalized.contains("luat")
                || normalized.contains("luật")
                || normalized.contains("tax")
                || normalized.contains("thue")
                || normalized.contains("thuế")
                || normalized.contains("loan")
                || normalized.contains("mortgage")
                || normalized.contains("vay")
                || normalized.contains("lai suat")
                || normalized.contains("lãi suất");
    }

    String fallbackReply(String content, String aiErrorMessage) {
        if (needsProfessionalReferral(content)) {
            return "Toi co the ho tro thong tin bat dong san o muc tham khao, nhung voi cau hoi phap ly, thue, vay von hoac tai chinh chuyen sau, ban nen trao doi truc tiep voi nhan vien phu trach hoac chuyen gia phu hop.";
        }
        if (aiErrorMessage != null && !aiErrorMessage.isBlank()) {
            return "Hien AI assistant chua san sang de tra loi tu dong. Ban co the mo ta nhu cau mua hoac thue, ngan sach, khu vuc va thoi gian mong muon; nhan vien phu trach se dung thong tin do de tu van tiep.";
        }
        return "Ban co the cho toi biet nhu cau mua hoac thue, ngan sach, khu vuc uu tien va so phong mong muon de toi ho tro dinh huong lua chon phu hop.";
    }
}
