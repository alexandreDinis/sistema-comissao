package com.empresa.comissao.validation;

import lombok.extern.slf4j.Slf4j;
import java.util.regex.Pattern;

@Slf4j
public class ValidadorPlaca {

    private static final Pattern PLACA_ANTIGA = Pattern.compile("^[A-Z]{3}[0-9]{4}$");
    private static final Pattern PLACA_MERCOSUL = Pattern.compile("^[A-Z]{3}[0-9][A-Z][0-9]{2}$");

    /**
     * Normaliza a placa: remove caracteres especiais, espaços e converte para
     * maiúsculas.
     * Ex: "abc-1234" -> "ABC1234"
     */
    public static String normalizar(String placa) {
        if (placa == null) {
            return null;
        }
        return placa.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Valida se a placa está no formato correto (Antiga ou Mercosul).
     * Lança exceção se inválida.
     */
    public static void validar(String placaRaw) {
        if (placaRaw == null || placaRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("Placa não pode ser vazia.");
        }

        String placa = normalizar(placaRaw);
        log.debug("🔍 Validando placa: '{}' -> normalizada: '{}'", placaRaw, placa);

        if (placa.length() != 7) {
            String mensagem = String.format(
                    "Placa deve ter 7 caracteres (após normalização). Recebido: '%s' (%d caracteres).",
                    placa, placa.length());
            log.warn("❌ Validação falhou: {}", mensagem);
            throw new IllegalArgumentException(mensagem);
        }

        boolean isAntiga = PLACA_ANTIGA.matcher(placa).matches();
        boolean isMercosul = PLACA_MERCOSUL.matcher(placa).matches();

        if (!isAntiga && !isMercosul) {
            String mensagem = String.format(
                    "Formato de placa inválido: '%s'. Formatos aceitos: Antiga (ABC1234) ou Mercosul (ABC1D23).",
                    placa);
            log.warn("❌ Validação falhou: {}", mensagem);
            throw new IllegalArgumentException(mensagem);
        }

        log.info("✅ Placa '{}' validada com sucesso", placa);
    }
}
