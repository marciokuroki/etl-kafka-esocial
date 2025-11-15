package com.esocial.consumer.model.entity.enums;

/**
 * Estados do evento durante todo seu ciclo de vida
 * Do recebimento até a conclusão no eSocial
 */
public enum EventStatus {
    
    RECEIVED("Evento recebido do Kafka", "📥"),
    VALIDATING("Validando evento contra regras", "🔍"),
    VALIDATION_FAILED("Validação falhou, evento será descartado", "❌"),
    VALIDATION_PASSED("Validação passou com sucesso", "✅"),
    PROCESSING("Processando evento (salvando dados)", "⚙️"),
    PROCESSING_FAILED("Falha ao processar evento", "🔥"),
    PROCESSED("Evento processado com sucesso", "✅"),
    SENDING_TO_ESOCIAL("Enviando para eSocial", "📤"),
    SENT_TO_ESOCIAL("Enviado para eSocial aguardando resposta", "⏳"),
    ESOCIAL_REJECTED("Rejeitado pelo eSocial", "⛔"),
    ESOCIAL_ACCEPTED("Aceito pelo eSocial", "🎉"),
    ESOCIAL_PROCESSED("Completamente processado pelo eSocial", "✔️"),
    ARCHIVED("Arquivado para histórico", "📦"),
    ERROR("Erro não recuperável", "🚨");
    
    private final String description;
    private final String emoji;
    
    EventStatus(String description, String emoji) {
        this.description = description;
        this.emoji = emoji;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getEmoji() {
        return emoji;
    }
    
    public String getDisplayName() {
        return emoji + " " + this.name().replace("_", " ");
    }
    
    /**
     * Verifica se é um estado terminal
     */
    public boolean isTerminal() {
        return this == VALIDATION_FAILED || 
               this == ESOCIAL_REJECTED || 
               this == ERROR || 
               this == ARCHIVED || 
               this == ESOCIAL_PROCESSED;
    }
    
    /**
     * Verifica se pode fazer retry
     */
    public boolean canRetry() {
        return this == ERROR || 
               this == PROCESSING_FAILED || 
               this == SENDING_TO_ESOCIAL;
    }
    
    /**
     * Verifica se está aguardando resposta eSocial
     */
    public boolean isPendingEsocial() {
        return this == SENT_TO_ESOCIAL || 
               this == SENDING_TO_ESOCIAL;
    }
    
    /**
     * Verifica se foi processado com sucesso
     */
    public boolean isSuccessful() {
        return this == ESOCIAL_ACCEPTED || 
               this == ESOCIAL_PROCESSED ||
               this == PROCESSED;
    }
    
    /**
     * Retorna o próximo estado esperado
     */
    public EventStatus getNextState() {
        switch (this) {
            case RECEIVED:
                return VALIDATING;
            case VALIDATING:
                return VALIDATION_PASSED;
            case VALIDATION_PASSED:
                return PROCESSING;
            case PROCESSING:
                return PROCESSED;
            case PROCESSED:
                return SENDING_TO_ESOCIAL;
            case SENDING_TO_ESOCIAL:
                return SENT_TO_ESOCIAL;
            case SENT_TO_ESOCIAL:
                return ESOCIAL_ACCEPTED;
            case ESOCIAL_ACCEPTED:
                return ESOCIAL_PROCESSED;
            case ESOCIAL_PROCESSED:
                return ARCHIVED;
            default:
                return null;
        }
    }
}
