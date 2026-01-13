package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.ModoComissao;
import com.empresa.comissao.repository.EmpresaRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/empresa")
@RequiredArgsConstructor
public class EmpresaController {

    private final EmpresaRepository empresaRepository;

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
                empresa.getModoComissao()));
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
                saved.getModoComissao()));
    }

    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:uploads/logos}")
    private String uploadDir;

    @PostMapping("/{id}/logo")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<?> uploadLogo(
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal User user) {

        // Security check: Ensure admin can only update their own company
        if (user.getEmpresa() == null || !user.getEmpresa().getId().equals(id)) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body("Você só pode atualizar o logo da sua própria empresa.");
        }

        try {
            // Validate file content type
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("image/png") && !contentType.equals("image/jpeg"))) {
                return ResponseEntity.badRequest().body("Apenas PNG ou JPEG são permitidos.");
            }

            // Validate file size (max 2MB)
            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest().body("Arquivo muito grande. Máximo 2MB.");
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            } else {
                extension = contentType.equals("image/png") ? ".png" : ".jpg";
            }

            String fileName = "empresa-" + id + "-" + System.currentTimeMillis() + extension;

            // Ensure upload directory exists
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // Save file
            java.nio.file.Path filePath = uploadPath.resolve(fileName);
            java.nio.file.Files.copy(file.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            // Update empresa logic
            Empresa empresa = empresaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

            // Delete old logo if exists logic could be added here

            empresa.setLogoPath(fileName);
            empresaRepository.save(empresa);

            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Logo atualizado com sucesso",
                    "logoPath", fileName));

        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao fazer upload: " + e.getMessage());
        }
    }

    // DTOs
    @Data
    public static class EmpresaConfigResponse {
        private final Long id;
        private final String nome;
        private final ModoComissao modoComissao;
    }

    @Data
    public static class UpdateEmpresaConfigRequest {
        private ModoComissao modoComissao;
    }
}
