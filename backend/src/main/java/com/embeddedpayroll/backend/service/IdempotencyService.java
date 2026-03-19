package com.embeddedpayroll.backend.service;

import com.embeddedpayroll.backend.model.IdempotencyRecord;
import com.embeddedpayroll.backend.model.Organization;
import com.embeddedpayroll.backend.repository.IdempotencyRecordRepository;
import com.embeddedpayroll.backend.repository.OrganizationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OrganizationRepository organizationRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public <T> T execute(
        Long organizationId,
        String actorKey,
        String idempotencyKey,
        String requestMethod,
        String requestPath,
        Object requestBody,
        Class<T> responseType,
        Supplier<T> action
    ) {
        if (!StringUtils.hasText(idempotencyKey)) {
            return action.get();
        }

        String requestHash = requestHash(requestMethod, requestPath, requestBody);
        return idempotencyRecordRepository.findByOrganizationIdAndActorKeyAndIdempotencyKey(
            organizationId,
            actorKey,
            idempotencyKey
        )
            .map(existing -> replay(existing, requestHash, responseType))
            .orElseGet(() -> storeNewRecord(
                organizationId,
                actorKey,
                idempotencyKey,
                requestMethod,
                requestPath,
                requestHash,
                responseType,
                action
            ));
    }

    private <T> T replay(IdempotencyRecord existing, String requestHash, Class<T> responseType) {
        if (!existing.getRequestHash().equals(requestHash)) {
            throw new IllegalArgumentException("Idempotency key was already used for a different request payload");
        }
        try {
            return objectMapper.readValue(existing.getResponseBody(), responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to replay stored idempotent response", exception);
        }
    }

    private <T> T storeNewRecord(
        Long organizationId,
        String actorKey,
        String idempotencyKey,
        String requestMethod,
        String requestPath,
        String requestHash,
        Class<T> responseType,
        Supplier<T> action
    ) {
        T response = action.get();
        try {
            Organization organization = organizationRepository.getReferenceById(organizationId);
            IdempotencyRecord record = new IdempotencyRecord();
            record.setOrganization(organization);
            record.setActorKey(actorKey);
            record.setIdempotencyKey(idempotencyKey);
            record.setRequestMethod(requestMethod);
            record.setRequestPath(requestPath);
            record.setRequestHash(requestHash);
            record.setResponseStatusCode(200);
            record.setResponseType(responseType.getName());
            record.setResponseBody(objectMapper.writeValueAsString(response));
            idempotencyRecordRepository.save(record);
            return response;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to persist idempotent response", exception);
        }
    }

    private String requestHash(String requestMethod, String requestPath, Object requestBody) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String serializedPayload = objectMapper.writeValueAsString(requestBody);
            byte[] hash = digest.digest((requestMethod + "|" + requestPath + "|" + serializedPayload)
                .getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException | JsonProcessingException exception) {
            throw new IllegalStateException("Unable to hash idempotent request", exception);
        }
    }
}
