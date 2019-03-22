package com.faforever.client.config.integration;

import com.faforever.client.integration.ChannelNames;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;

@Configuration
@IntegrationComponentScan("com.faforever.server.integration")
public class IntegrationConfig {

  private final ApplicationEventPublisher applicationEventPublisher;

  public IntegrationConfig(ApplicationEventPublisher applicationEventPublisher) {
    this.applicationEventPublisher = applicationEventPublisher;
  }

  /**
   * Reads messages from the standard server inbound channel. Since messages are resequenced, all messages must specify
   * the header {@link IntegrationMessageHeaderAccessor#SEQUENCE_NUMBER}.
   */
  @Bean
  public IntegrationFlow inboundFlow() {
    return IntegrationFlows
      .from(ChannelNames.SERVER_INBOUND)
      .channel(ChannelNames.INBOUND_DISPATCH)
      .get();
  }

  @Bean
  public IntegrationFlow dispatchFlow() {
    return IntegrationFlows
      .from(ChannelNames.INBOUND_DISPATCH)
      .handle(applicationEventPublishingMessageHandler())
      .get();
  }

  /**
   * Reads messages from the server outbound channel and sends it to the target adapter's channel.
   */
  @Bean
  public IntegrationFlow outboundFlow() {
    return IntegrationFlows
      .from(ChannelNames.SERVER_OUTBOUND)
      .channel(ChannelNames.WEB_OUTBOUND)
      .get();
  }

  /** Publishes inbound messages as {@link org.springframework.integration.event.core.MessagingEvent}. */
  @Bean
  public ApplicationEventPublishingMessageHandler applicationEventPublishingMessageHandler() {
    ApplicationEventPublishingMessageHandler handler = new ApplicationEventPublishingMessageHandler();
    handler.setApplicationEventPublisher(applicationEventPublisher);
    handler.setPublishPayload(true);
    return handler;
  }
}
