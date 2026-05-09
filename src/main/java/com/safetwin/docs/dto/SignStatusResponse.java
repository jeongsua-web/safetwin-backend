package com.safetwin.docs.dto;

import com.safetwin.entity.Signature;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SignStatusResponse {

    private Long documentId;
    private String documentStatus;
    private int signatureCount;
    private List<SignatureItem> signatures;

    @Getter
    @Builder
    public static class SignatureItem {
        private Long id;
        private String signerName;
        private LocalDateTime signedAt;

        public static SignatureItem from(Signature sig) {
            return SignatureItem.builder()
                    .id(sig.getId())
                    .signerName(sig.getSignerName())
                    .signedAt(sig.getSignedAt())
                    .build();
        }
    }
}
