package com.empresa.comissao.validation;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidadorDocumento {

    public static String normalizar(String documento) {
        if (documento == null)
            return null;
        return documento.replaceAll("[^0-9]", "");
    }

    public static boolean isCnpj(String cnpj) {
        String clean = normalizar(cnpj);
        if (clean == null || clean.length() != 14)
            return false;

        // Reject repeated sequences (00...00, 11...11, etc)
        if (clean.matches("(\\d)\\1{13}"))
            return false;

        try {
            int soma = 0, peso = 2;
            for (int i = 11; i >= 0; i--) {
                soma += (clean.charAt(i) - '0') * peso;
                peso = (peso == 9) ? 2 : peso + 1;
            }
            int r = soma % 11;
            char dig13 = (r < 2) ? '0' : (char) ((11 - r) + '0');

            soma = 0;
            peso = 2;
            for (int i = 12; i >= 0; i--) {
                soma += (clean.charAt(i) - '0') * peso;
                peso = (peso == 9) ? 2 : peso + 1;
            }
            r = soma % 11;
            char dig14 = (r < 2) ? '0' : (char) ((11 - r) + '0');

            return (dig13 == clean.charAt(12)) && (dig14 == clean.charAt(13));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCpf(String cpf) {
        String clean = normalizar(cpf);
        if (clean == null || clean.length() != 11)
            return false;

        if (clean.matches("(\\d)\\1{10}"))
            return false;

        try {
            int soma = 0, peso = 10;
            for (int i = 0; i < 9; i++) {
                soma += (clean.charAt(i) - '0') * peso--;
            }
            int r = 11 - (soma % 11);
            char dig10 = (r == 10 || r == 11) ? '0' : (char) (r + '0');

            soma = 0;
            peso = 11;
            for (int i = 0; i < 10; i++) {
                soma += (clean.charAt(i) - '0') * peso--;
            }
            r = 11 - (soma % 11);
            char dig11 = (r == 10 || r == 11) ? '0' : (char) (r + '0');

            return (dig10 == clean.charAt(9)) && (dig11 == clean.charAt(10));
        } catch (Exception e) {
            return false;
        }
    }

    public static void validarCnpj(String cnpj) {
        String normalizado = normalizar(cnpj);
        if (normalizado == null || normalizado.trim().isEmpty()) {
            throw new IllegalArgumentException("CNPJ não pode ser vazio.");
        }
        if (!isCnpj(normalizado)) {
            throw new IllegalArgumentException("CNPJ inválido: " + cnpj);
        }
    }

    public static void validarCpf(String cpf) {
        String normalizado = normalizar(cpf);
        if (normalizado == null || normalizado.trim().isEmpty()) {
            throw new IllegalArgumentException("CPF não pode ser vazio.");
        }
        if (!isCpf(normalizado)) {
            throw new IllegalArgumentException("CPF inválido: " + cpf);
        }
    }
}
