package org.javacord.core.util.handler.message;

import com.fasterxml.jackson.databind.JsonNode;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerChannel;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.MessageDeleteEvent;
import org.javacord.core.event.message.MessageDeleteEventImpl;
import org.javacord.core.util.cache.MessageCacheImpl;
import org.javacord.core.util.gateway.PacketHandler;

import java.util.Optional;

/**
 * Handles the message delete bulk packet.
 */
public class MessageDeleteBulkHandler extends PacketHandler {

    /**
     * Creates a new instance of this class.
     *
     * @param api The api.
     */
    public MessageDeleteBulkHandler(DiscordApi api) {
        super(api, true, "MESSAGE_DELETE_BULK");
    }

    @Override
    public void handle(JsonNode packet) {
        long channelId = Long.parseLong(packet.get("channel_id").asText());

        api.getTextChannelById(channelId).ifPresent(channel -> {
            for (JsonNode messageIdJson : packet.get("ids")) {
                long messageId = messageIdJson.asLong();
                MessageDeleteEvent event = new MessageDeleteEventImpl(api, messageId, channel);

                api.getCachedMessageById(messageId)
                        .ifPresent(((MessageCacheImpl) channel.getMessageCache())::removeMessage);
                api.removeMessageFromCache(messageId);

                Optional<Server> optionalServer = channel.asServerChannel().map(ServerChannel::getServer);
                api.getEventDispatcher().dispatchMessageDeleteEvent(
                        optionalServer.flatMap(Optional::<Object>of).orElse(api),
                        messageId,
                        optionalServer.orElse(null),
                        channel,
                        event);
            }
        });
    }
}