package com.campushelp.controller.chat;

import com.campushelp.common.Result;
import com.campushelp.entity.ChatMessage;
import com.campushelp.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping("/send")
    public Result<ChatMessage> sendMessage(@RequestAttribute Long userId,
                                            @RequestParam Long orderId,
                                            @RequestParam Long receiverId,
                                            @RequestParam String content,
                                            @RequestParam(defaultValue = "1") Integer messageType) {
        return Result.success(chatService.sendMessage(orderId, userId, receiverId, content, messageType));
    }

    /**
     * 获取订单聊天记录
     */
    @GetMapping("/messages")
    public Result<List<ChatMessage>> getMessages(@RequestParam Long orderId,
                                                  @RequestParam(defaultValue = "100") int limit) {
        return Result.success(chatService.getOrderMessages(orderId, limit));
    }
}
