package com.hydroyura.agents.piflow.agent.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PiJsonlParserTest {

    private final PiJsonlParser parser = new PiJsonlParser();

    @Test
    void accept_withTextDeltas_shouldAccumulateStreamedText() {
        parser.accept("""
                {"type":"message_update","assistantMessageEvent":{"type":"text_delta","contentIndex":0,"delta":"Hello "}}
                """);
        parser.accept("""
                {"type":"message_update","assistantMessageEvent":{"type":"text_delta","contentIndex":0,"delta":"world"}}
                """);

        assertThat(parser.finalText()).isEqualTo("Hello world");
    }

    @Test
    void accept_withMessageEnd_shouldUseAuthoritativeText() {
        parser.accept("""
                {"type":"message_update","assistantMessageEvent":{"type":"text_delta","contentIndex":0,"delta":"draft"}}
                """);
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"Hello world!"}],"stopReason":"end"}}
                """);

        assertThat(parser.finalText()).isEqualTo("Hello world!");
    }

    @Test
    void accept_withErrorStopReason_shouldMarkFailure() {
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}
                """);

        assertThat(parser.failed()).isTrue();
        assertThat(parser.failureReason()).isEqualTo("error");
    }

    @Test
    void accept_withAgentSettled_shouldMarkCompletion() {
        parser.accept("""
                {"type":"agent_settled","aborted":false}
                """);

        assertThat(parser.settled()).isTrue();
        assertThat(parser.failed()).isFalse();
    }

    @Test
    void accept_withAbortedSettled_shouldMarkFailure() {
        parser.accept("""
                {"type":"agent_settled","aborted":true}
                """);

        assertThat(parser.failed()).isTrue();
        assertThat(parser.failureReason()).isEqualTo("aborted");
    }

    @Test
    void accept_withUnrecognizedEventTypes_shouldNotObserveSupportedProtocol() {
        parser.accept("""
                {"type":"session","version":3,"id":"fake"}
                """);
        parser.accept("""
                {"type":"turn_start"}
                """);
        parser.accept("this is not json");

        assertThat(parser.supportedProtocolObserved()).isFalse();
    }

    @Test
    void accept_withRecognizedEvent_shouldObserveSupportedProtocol() {
        parser.accept("""
                {"type":"message_update","assistantMessageEvent":{"type":"text_delta","contentIndex":0,"delta":"hi"}}
                """);

        assertThat(parser.supportedProtocolObserved()).isTrue();
    }

    @Test
    void accept_withoutAgentSettled_shouldNotHaveRecognizedCompletion() {
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"done"}],"stopReason":"end"}}
                """);

        assertThat(parser.hasRecognizedCompletion()).isFalse();
    }

    @Test
    void accept_withAgentSettled_shouldHaveRecognizedCompletion() {
        parser.accept("""
                {"type":"agent_settled","aborted":false}
                """);

        assertThat(parser.hasRecognizedCompletion()).isTrue();
    }

    @Test
    void accept_withSpecificReasonFollowedBySuccessMessage_shouldPreserveSpecificReason() {
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}
                """);
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"fine"}],"stopReason":"end"}}
                """);

        assertThat(parser.failureReason()).isEqualTo("error");
        assertThat(parser.failed()).isTrue();
    }

    @Test
    void accept_withErrorThenRecoveredSettlement_shouldClearFailed() {
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}
                """);
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"fixed"}],"stopReason":"end"}}
                """);
        parser.accept("""
                {"type":"agent_settled","aborted":false}
                """);

        assertThat(parser.failed()).isFalse();
    }

    @Test
    void accept_withErrorAtFinalSettlement_shouldStayFailed() {
        parser.accept("""
                {"type":"message_end","message":{"role":"assistant","content":[{"type":"text","text":"oops"}],"stopReason":"error"}}
                """);
        parser.accept("""
                {"type":"agent_settled","aborted":false}
                """);

        assertThat(parser.failed()).isTrue();
    }
}
