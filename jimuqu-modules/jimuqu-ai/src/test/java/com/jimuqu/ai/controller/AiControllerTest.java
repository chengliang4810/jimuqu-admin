package com.jimuqu.ai.controller;

import com.jimuqu.ai.domain.ChatRequestVo;
import org.junit.jupiter.api.Test;
import org.noear.solon.web.sse.SseEvent;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiControllerTest {

    @Test
    void returnsPromptWhenRequestHasNoMessage() throws Exception {
        AiController controller = new AiController();

        Flux<SseEvent> events = controller.chat(new ChatRequestVo());

        List<SseEvent> result = events.collectList().block();
        assertEquals(1, result.size());
        assertTrue(result.get(0).build().contains("data:请输入内容"));
    }
}
