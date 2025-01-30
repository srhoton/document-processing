package com.steverhoton.poc.docprocessing;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSEvent.SQSMessage;

@Named("docprocessing")
public class DocProcessing implements RequestHandler<SQSEvent, Void> {

    @Override
    public Void handleRequest(SQSEvent docEvent, Context context) {
        for (SQSMessage message: docEvent.getRecords()){
            context.getLogger().log("Processing document " + message.getBody());
        }
        context.getLogger().log("Doc processing complete");
        return null;
    }
}
