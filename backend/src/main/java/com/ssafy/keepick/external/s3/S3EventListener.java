package com.ssafy.keepick.external.s3;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.keepick.global.exception.BaseException;
import com.ssafy.keepick.global.exception.ErrorCode;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;



@Slf4j
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3", matchIfMissing = true)
@Component
@RequiredArgsConstructor
public class S3EventListener {

    private final S3EventParser s3EventParser;
    private final S3EventProcessor s3EventProcessor;

    @SqsListener("${app.aws.sqs.queue-name}")
    public void receiveS3Event(String message) {
        try {
            log.info("SQS 메세지 수신: {}", message);

            JsonNode s3Event = s3EventParser.parse(message);
            List<JsonNode> records = s3EventParser.extractRecords(s3Event);

            for (JsonNode record : records) {
                s3EventProcessor.handle(record);
            }

        } catch (Exception e) {
            log.error("SQS message 처리중 오류 발생", e);
            throw new BaseException(ErrorCode.INTERNAL_S3_ERROR);
        }
    }
}