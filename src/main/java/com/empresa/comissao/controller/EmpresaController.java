package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.ModoComissao;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.service.StorageService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/empresa")
@RequiredArgsConstructor
@Slf4j
public class EmpresaController {

    private final EmpresaRepository empresaRepository;
    private final StorageService storageService;

    /**
     * Get current empresa configuration for the authenticated user's company.
     */
    @GetMapping("/config")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<EmpresaConfigResponse> getConfig(@AuthenticationPrincipal User user) {
        if (user.getEmpresa() == null) {
            return ResponseEntity.badRequest().build();
        }

        Empresa empresa = user.getEmpresa();
        return ResponseEntity.ok(new EmpresaConfigResponse(
                empresa.getId(),
                empresa.getNome(),
                empresa.getModoComissao(),
                empresa.getLogoPath() != null ? storageService.getFileUrl(empresa.getLogoPath()) : null));
    }

    /**
     * Update empresa configuration (modoComissao).
     * Only ADMIN_EMPRESA can change their company's settings.
     */
    @PatchMapping("/config")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<EmpresaConfigResponse> updateConfig(
            @AuthenticationPrincipal User user,
            @RequestBody UpdateEmpresaConfigRequest request) {

        if (user.getEmpresa() == null) {
            return ResponseEntity.badRequest().build();
        }

        Empresa empresa = empresaRepository.findById(user.getEmpresa().getId())
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        if (request.getModoComissao() != null) {
            empresa.setModoComissao(request.getModoComissao());
        }

        Empresa saved = empresaRepository.save(empresa);

        return ResponseEntity.ok(new EmpresaConfigResponse(
                saved.getId(),
                saved.getNome(),
                saved.getModoComissao(),
                saved.getLogoPath() != null ? storageService.getFileUrl(saved.getLogoPath()) : null));
    }

    /**
     * Upload company logo.
     * Stores in S3 (prod) or local filesystem (dev).
     */
    @PostMapping("/{id}/logo")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<?> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {

        // Security check: Ensure admin can only update their own company
        if (user.getEmpresa() == null || !user.getEmpresa().getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Você só pode atualizar o logo da sua própria empresa."));
        }

        try {
            Empresa empresa = empresaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

            // Generate unique filename
            String extension = getFileExtension(file);
            String key = "logos/empresa-" + id + "-" + System.currentTimeMillis() + extension;

            // Delete old logo if exists
            if (empresa.getLogoPath() != null) {
                storageService.deleteFile(empresa.getLogoPath());
            }

            // Upload new logo
            String fileUrl = storageService.uploadFile(file, key);

            // Update empresa
            empresa.setLogoPath(key);
            empresaRepository.save(empresa);

            log.info("Logo updated for empresa {}: {}", id, key);

            return ResponseEntity.ok(Map.of(
                    "message", "Logo atualizado com sucesso",
                    "logoUrl", fileUrl));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error uploading logo for empresa {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erro ao fazer upload: " + e.getMessage()));
        }
    }

    /**
     * Get company logo by ID.
     * Proxies the image from storage service.
     */
    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable Long id) {
        Empresa empresa = empresaRepository.findById(id).orElse(null);

        if (empresa == null || empresa.getLogoPath() == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] imageBytes = storageService.getFileBytes(empresa.getLogoPath());
        if (imageBytes == null) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = empresa.getLogoPath().endsWith(".png")
                ? MediaType.IMAGE_PNG
                : MediaType.IMAGE_JPEG;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(imageBytes);
    }

    private String getFileExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
            return originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            return switch (contentType) {
                case "image/png" -> ".png";
                case "image/webp" -> ".webp";
                default -> ".jpg";
            };
        }
        return ".jpg";
    }

    // DTOs
    @Data
    public static class EmpresaConfigResponse {
        private final Long id;
        private final String nome;
        private final ModoComissao modoComissao;
        private final String logoUrl;
    }

    @Data
    public static class UpdateEmpresaConfigRequest {
        private ModoComissao modoComissao;
    }
}
