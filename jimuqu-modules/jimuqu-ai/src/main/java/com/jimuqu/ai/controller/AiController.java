package com.jimuqu.ai.controller;

import com.jimuqu.ai.domain.ChatRequestVo;
import com.jimuqu.ai.domain.ChatResponseVo;
import com.jimuqu.common.core.utils.JsonUtil;
import org.noear.solon.ai.chat.ChatModel;
import org.noear.solon.ai.chat.ChatResponse;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Post;
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

    @Post
    @Mapping("/chat")
    @Produces(MimeType.TEXT_EVENT_STREAM_UTF8_VALUE)
    public Flux<SseEvent> chat(ChatRequestVo request) throws IOException {
        String content = request.getMessage().getContent();
        if(content == null || content.trim().isEmpty()) {
            return Flux.just(new SseEvent().data("请输入内容"));
        }
        return Flux.from(chatModel.prompt(content).stream())
                .filter(ChatResponse::hasContent)
                .map(resp -> {
                    System.out.println("resp: " + JsonUtil.toString(resp));
                    return new SseEvent().data(new ChatResponseVo(resp));
                });
    }

}
