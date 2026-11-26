package com.devfalco.nummia.assistant.ai;

/** Abstração do provedor de IA (serviço externo, n8n, LangGraph, regras locais...). */
public interface AiClient {

    AiReply chat(AiRequest request);
}
