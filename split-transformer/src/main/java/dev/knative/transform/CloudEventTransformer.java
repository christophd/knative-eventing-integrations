package dev.knative.transform;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.cloudevents.CloudEvent;
import org.apache.camel.cloudevents.CloudEvents;
import org.apache.camel.spi.DataType;
import org.apache.camel.spi.DataTypeTransformer;
import org.apache.camel.spi.Transformer;
import org.apache.camel.support.MessageHelper;

@DataTypeTransformer(name = "application-cloudevents+json",
        description = "Adds default CloudEvent (JSon binding) headers to the Camel message (such as content-type, event source, event type etc.)")
public class CloudEventTransformer extends Transformer {

    @Override
    public void transform(Message message, DataType fromType, DataType toType) {
        final Map<String, Object> headers = message.getHeaders();

        Map<String, Object> cloudEventAttributes = new HashMap<>();
        CloudEvent cloudEvent = CloudEvents.v1_0;
        for (CloudEvent.Attribute attribute : cloudEvent.attributes()) {
            if (headers.containsKey(attribute.id())) {
                cloudEventAttributes.put(attribute.json(), headers.get(attribute.id()));
            }
        }

        cloudEventAttributes.putIfAbsent(cloudEvent.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_VERSION).json(),
                cloudEvent.version());
        cloudEventAttributes.putIfAbsent(cloudEvent.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_ID).json(),
                message.getExchange().getExchangeId());
        cloudEventAttributes.putIfAbsent(cloudEvent.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_TYPE).json(),
                CloudEvent.DEFAULT_CAMEL_CLOUD_EVENT_TYPE);
        cloudEventAttributes.putIfAbsent(cloudEvent.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_SOURCE).json(),
                CloudEvent.DEFAULT_CAMEL_CLOUD_EVENT_SOURCE);

        cloudEventAttributes.putIfAbsent(cloudEvent.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_TIME).json(),
                cloudEvent.getEventTime(message.getExchange()));

        String body = MessageHelper.extractBodyAsString(message);
        cloudEventAttributes.putIfAbsent("data", body);
        cloudEventAttributes.putIfAbsent(cloudEvent.mandatoryAttribute(CloudEvent.CAMEL_CLOUD_EVENT_DATA_CONTENT_TYPE).json(),
                headers.getOrDefault(CloudEvent.CAMEL_CLOUD_EVENT_CONTENT_TYPE, "application/json"));

        headers.put(Exchange.CONTENT_TYPE, "application/cloudevents+json");

        message.setBody(createCouldEventJsonObject(cloudEventAttributes));

        cloudEvent.attributes().stream().map(CloudEvent.Attribute::id).forEach(headers::remove);
    }

    private String createCouldEventJsonObject(Map<String, Object> cloudEventAttributes) {
        StringBuilder builder = new StringBuilder("{");

        cloudEventAttributes.forEach((key, value) -> {
            if ("data".equals(key) && value instanceof String data) {
                if (data.trim().startsWith("{") || data.trim().startsWith("[")) {
                    builder.append(" ").append("\"").append(key).append("\"").append(":").append(data)
                            .append(",");
                } else {
                    builder.append(" ").append("\"").append(key).append("\"").append(":").append("\"").append(data).append("\"")
                            .append(",");
                }
            } else {
                builder.append(" ").append("\"").append(key).append("\"").append(":").append("\"").append(value).append("\"")
                        .append(",");
            }
        });

        if (!cloudEventAttributes.isEmpty()) {
            builder.deleteCharAt(builder.lastIndexOf(","));
        }

        return builder.append("}").toString();
    }
}
