package com.project.ai.processing.voice;

import com.project.ai.config.WhisperProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 03/06/2026
 * @Time: 3:53 PM
 */
@Service
@Log4j2
@RequiredArgsConstructor
public class VoiceProcessor {


    private final WhisperProperties whisperProperties;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public String transcribe(String audioBase64, String mediaType) {
        log.info("[VoiceProcessor] START — mediaType={}", mediaType);

        try {
            byte[] audioBytes = Base64.getMimeDecoder().decode(audioBase64);
            String extension = resolveExtension(mediaType);

            RequestBody audioBody = RequestBody.create(audioBytes,
                    MediaType.parse(mediaType));

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "audio." + extension, audioBody)
                    .addFormDataPart("model", whisperProperties.getModel())
                    .addFormDataPart("response_format", "text")
                    .addFormDataPart("language", resolveLanguage())
                    .build();

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/audio/transcriptions")
                    .header("Authorization", "Bearer " + whisperProperties.getApiKey())
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "no body";
                    log.error("[VoiceProcessor] Whisper API error — status={} body={}", response.code(), errorBody);
                    throw new RuntimeException("Whisper API failed: " + response.code() + " — " + errorBody);
                }

                String transcribed = response.body().string().trim();
                log.info("[VoiceProcessor] END — transcribed='{}'", transcribed);
                return transcribed;
            }

        } catch (Exception e) {
            log.error("[VoiceProcessor] Failed to transcribe audio: {}", e.getMessage());
            throw new RuntimeException("Voice transcription failed: " + e.getMessage());
        }
    }

    private String resolveExtension(String mediaType) {
        if (mediaType == null) return "wav";
        return switch (mediaType.toLowerCase()) {
            case "audio/mpeg", "audio/mp3"  -> "mp3";
            case "audio/mp4", "audio/m4a"   -> "m4a";
            case "audio/webm"               -> "webm";
            case "audio/ogg"                -> "ogg";
            default                         -> "wav";
        };
    }

    private String resolveLanguage() {
        if ("auto".equalsIgnoreCase(whisperProperties.getLanguage())) {
            return "";  // empty = Whisper auto-detects
        }
        return whisperProperties.getLanguage();
    }
}
