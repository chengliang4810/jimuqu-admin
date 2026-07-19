package com.jimuqu.common.idempotent.aspectj;

import com.jimuqu.common.idempotent.annotation.RepeatSubmit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepeatSubmitInterceptorTest {

    @Test
    void resolvesUpstreamDefaultMessageKey() throws Exception {
        RepeatSubmit annotation = getClass().getDeclaredMethod("submit").getAnnotation(RepeatSubmit.class);

        assertEquals("repeat.submit.message",
                RepeatSubmitInterceptor.resolveMessage(annotation.message()));
        assertEquals("custom", RepeatSubmitInterceptor.resolveMessage("custom"));
    }

    @RepeatSubmit
    private void submit() {
    }
}
