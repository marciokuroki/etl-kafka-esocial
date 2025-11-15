package com.esocial.consumer.model.entity.enums;

public enum EventType {
    S_2200("S-2200", "Inscrição do Contribuinte Individual"),
    S_2300("S-2300", "Admissão de Trabalhador"),
    S_2306("S-2306", "Admissão de Aprendiz"),
    S_2400("S-2400", "Alterações Contratuais do Trabalhador"),
    S_2405("S-2405", "Alteração de Dados do Aprendiz"),
    S_2410("S-2410", "Remuneração de Trabalhador"),
    S_2420("S-2420", "Desligamento de Trabalhador"),
    S_3000("S-3000", "Exclusão de Evento"),
    S_5001("S-5001", "Informações do Contribuinte"),
    S_5002("S-5002", "Informações do Estabelecimento"),
    S_5003("S-5003", "Informações Sobre Obras de Construção Civil"),
    UNKNOWN("UNKNOWN", "Tipo desconhecido");
    
    private final String code;
    private final String description;
    
    EventType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static EventType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        for (EventType type : EventType.values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
