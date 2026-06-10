package com.campushelp.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.campushelp.entity.ChatMessage;
import com.campushelp.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService extends ServiceImpl<ChatMessageMapper, ChatMessage> {

    /**
     * 发送消息
     */
    public ChatMessage sendMessage(Long orderId, Long senderId, Long receiverId,
                                    String content, Integer messageType) {
        ChatMessage message = new ChatMessage();
        message.setOrderId(orderId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMessageType(messageType != null ? messageType : 1);
        message.setStatus(1);

        this.save(message);
        return message;
    }

    /**
     * 获取订单聊天消息
     */
    public List<ChatMessage> getOrderMessages(Long orderId, int limit) {
        return this.list(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getOrderId, orderId)
                .orderByAsc(ChatMessage::getCreatedAt)
                .last("LIMIT " + limit));
    }
}
