package com.jimuqu.ai.controller;

import com.jimuqu.ai.domain.ChatResponseVo;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Produces;
import org.noear.solon.core.util.MimeType;
import org.noear.solon.web.sse.SseEvent;
import reactor.core.publisher.Flux;

import java.io.IOException;

@Controller
@Mapping("/ai")
public class AiController {

    @Inject
    private ChatModel chatModel;

    @Mapping("/chat")
    @Produces(MimeType.TEXT_EVENT_STREAM_UTF8_VALUE)
//    @Produces(MimeType.APPLICATION_X_NDJSON_UTF8_VALUE)
    public Flux<SseEvent> chat(String message) throws IOException {
        return Flux.from(chatModel.prompt(message).stream())
                .filter(ChatResponse::hasContent)
                .map(resp -> new SseEvent().data(new ChatResponseVo(resp)));
    }

}
