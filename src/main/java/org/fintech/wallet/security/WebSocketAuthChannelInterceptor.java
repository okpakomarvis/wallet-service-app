
package org.fintech.wallet.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String auth = accessor.getFirstNativeHeader("Authorization");
            if (auth == null || !auth.startsWith("Bearer ")) {
                log.warn("WebSocket CONNECT missing Authorization header");
                return message;
            }

            String token = auth.substring(7);
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("WebSocket CONNECT invalid token");
                return message;
            }

            UUID userId = jwtTokenProvider.getUserId(token);
            accessor.setUser(new StompPrincipal(userId.toString()));
            log.info("WebSocket CONNECT authenticated userId={}", userId);
        }
        return message;
    }
}
