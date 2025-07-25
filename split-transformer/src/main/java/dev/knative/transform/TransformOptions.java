package dev.knative.transform;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import org.apache.camel.spi.Transformer;

@ApplicationScoped
public class TransformOptions {

    @Named("cloudevents-json")
    public Transformer myTransformer()  {
        return new CloudEventTransformer();
    }
}
