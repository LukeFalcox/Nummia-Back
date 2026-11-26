package com.devfalco.nummia.assistant.ai;

import java.util.List;

public record AiReply(String reply, String source, List<String> suggestions) {
}
