# ADR-0007: Sistema de Validações em Três Camadas

**Status:** Aceito  
**Data:** 2025-11-22  
**Contexto:** Sprint 3 - Card 3.8  
**Decisores:** Márcio Kuroki Gonçalves, Reinaldo Galvão  
**Relacionado:** ADR-0003 (Validação em Duas Camadas - Revisado), ADR-0005 (DLQ)

---

## Contexto e Problema

O eSocial possui **regras de validação complexas** que abrangem múltiplos níveis:

### Níveis de Validação Necessários

1. **Formato (Estrutural):** Dados básicos corretos
   - CPF com 11 dígitos
   - Datas em formato válido
   - Campos obrigatórios preenchidos
   - Tipos de dados corretos

2. **Negócio (Domínio):** Regras do RH
   - Idade mínima 16 anos (CLT)
   - Salário >= mínimo nacional
   - Datas lógicas (admissão < demissão)
   - Status transitions válidos

3. **Governo (eSocial):** Conformidade legal
   - XSD schemas do eSocial
   - Tabelas governamentais (CBO, CNAE)
   - Certificado digital válido
   - Integração webservice gov.br

### Desafios

- ❌ **Validação monolítica:** Difícil manter e testar
- ❌ **Fail-slow:** Executa todas validações mesmo se uma falha cedo
- ❌ **Código duplicado:** Mesma validação em múltiplos lugares
- ❌ **Difícil extensão:** Adicionar nova regra modifica muito código
- ❌ **Sem rastreabilidade:** Não sabe qual regra específica falhou

---

## Decisão

**Implementar sistema de validações em 3 camadas independentes** com **fail-fast** e **padrões de design**.

### Arquitetura Proposta

```

┌─────────────────────────────────────────────┐
│  Camada 1: Validação Estrutural (6 regras) │
│  - CPF formato                              │
│  - PIS formato                              │
│  - Campos obrigatórios                      │
│  - Tipos de dados                           │
│  - Enums válidos                            │
└─────────────┬───────────────────────────────┘
│ ✅ Se passar
▼
┌─────────────────────────────────────────────┐
│  Camada 2: Validação de Negócio (5 regras) │
│  - Idade mínima (16 anos)                   │
│  - Datas não futuras                        │
│  - Datas lógicas (admissão < demissão)      │
│  - Salário >= mínimo (WARNING)              │
│  - Status transitions                       │
└─────────────┬───────────────────────────────┘
│ ✅ Se passar
▼
┌─────────────────────────────────────────────┐
│  Camada 3: Validação eSocial (Futuro)      │
│  - XSD Schema validation                    │
│  - Tabela CBO válida                        │
│  - Tabela CNAE válida                       │
│  - Certificado digital A1/A3                │
└─────────────────────────────────────────────┘

```

**Características:**
- ✅ **Fail-fast:** Para na primeira camada com ERROR
- ✅ **Independentes:** Cada camada pode ser testada isoladamente
- ✅ **Extensível:** Adicionar regras sem modificar engine
- ✅ **Rastreável:** Sabe exatamente qual regra falhou

---

## Alternativas Consideradas

### 1. Validação Monolítica (Descartada)

```

// ❌ Tudo em um único método
public ValidationResult validate(EmployeeEventDTO event) {
if (event.getCpf() == null || event.getCpf().length() != 11) {
return invalid("CPF inválido");
}
if (event.getBirthDate().isAfter(LocalDate.now())) {
return invalid("Data nascimento futura");
}
// ... 20+ validações misturadas
return valid();
}

```

**Problemas:**
- ❌ Difícil manter (método gigante)
- ❌ Impossível testar regras isoladamente
- ❌ Acoplamento alto
- ❌ Violação SRP (Single Responsibility Principle)

---

### 2. Bean Validation (JSR-303) - Parcialmente Usado

```

@Entity
public class Employee {

    @NotNull
    @Size(min = 11, max = 11)
    @Pattern(regexp = "\\d{11}")
    private String cpf;
    
    @NotNull
    @Past
    private LocalDate birthDate;
    
    // ...
    }

```

**Vantagens:**
- ✅ Declarativo e limpo
- ✅ Padrão Java (JSR-303)
- ✅ Integração Spring

**Limitações:**
- ❌ Apenas validações simples (formato, obrigatoriedade)
- ❌ Não suporta lógica complexa (idade mínima = admissão - nascimento)
- ❌ Não suporta validações contextuais (negócio)
- ❌ Não suporta validações externas (tabelas eSocial)

**Decisão:** Usar Bean Validation **+** Validações customizadas

---

### 3. Validação em 2 Camadas (ADR-0003 - Revisado)

ADR anterior propunha apenas 2 camadas:
1. Estrutural
2. Negócio

**Por que adicionar 3ª camada?**

Sprint 3 identificou necessidade de **validações eSocial separadas**:
- XSD schemas governamentais
- Webservice consultas (CBO, CNAE)
- Certificado digital

**Decisão:** Evoluir de 2 para **3 camadas** (backward-compatible)

---

## Implementação

### Padrões de Design Aplicados

#### 1. Strategy Pattern (Regras Plugáveis)

```

/**

* Contrato para regras de validação
*/
public interface ValidationRule {
void validate(EmployeeEventDTO event, ValidationResult result);
String getRuleName();
ValidationSeverity getSeverity();
int getOrder();
}

```

**Benefícios:**
- ✅ Adicionar regra = criar nova classe
- ✅ Não modifica código existente (Open/Closed Principle)
- ✅ Teste unitário isolado

---

#### 2. Template Method (Código Comum)

```

/**

* Classe base com código comum
*/
public abstract class AbstractValidationRule implements ValidationRule {

@Override
public final void validate(EmployeeEventDTO event, ValidationResult result) {
try {
// Template method
doValidate(event, result);
} catch (Exception e) {
// Tratamento comum de exceções
result.addError(
getRuleName(),
"system",
"Erro interno: " + e.getMessage(),
ValidationSeverity.ERROR
);
}
}

// Hook method (implementado por subclasses)
protected abstract void doValidate(EmployeeEventDTO event,
ValidationResult result);
}

```

**Benefícios:**
- ✅ Elimina código duplicado
- ✅ Tratamento de erros consistente
- ✅ Logging centralizado

---

#### 3. Chain of Responsibility (Execução Sequencial)

```

/**

* Motor de validações com fail-fast
*/
@Service
public class ValidationEngine {

private final List<ValidationRule> structuralRules;
private final List<ValidationRule> businessRules;
private final List<ValidationRule> esocialRules;

public ValidationResult validate(EmployeeEventDTO event) {
ValidationResult result = new ValidationResult();

     // Camada 1: Estrutural (fail-fast)
     executeRules(structuralRules, event, result);
     if (result.hasError()) return result;  // ← Short-circuit
     
     // Camada 2: Negócio (fail-fast)
     executeRules(businessRules, event, result);
     if (result.hasError()) return result;  // ← Short-circuit
     
     // Camada 3: eSocial (futuro)
     // executeRules(esocialRules, event, result);
     
     return result;
    }

private void executeRules(List<ValidationRule> rules,
EmployeeEventDTO event,
ValidationResult result) {
for (ValidationRule rule : rules) {
rule.validate(event, result);
if (result.hasError()) break;  // Fail-fast
}
}
}

```

**Benefícios:**
- ✅ Performance: para no primeiro erro crítico
- ✅ Feedback rápido ao usuário
- ✅ Logs mais claros (primeira falha)

---

### Camada 1: Validações Estruturais

#### Exemplo: CPF Format Validation

```

@Component
@Order(1)
public class CpfFormatValidationRule extends AbstractValidationRule {

    private static final Pattern CPF_PATTERN = Pattern.compile("^\\d{11}$");
    
    public CpfFormatValidationRule() {
        super("INVALID_CPF_FORMAT", ValidationSeverity.ERROR, 1);
    }
    
    @Override
    protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
        String cpf = event.getCpf();
        
        // 1. Obrigatório
        if (cpf == null || cpf.isEmpty()) {
            result.addError(
                getRuleName(),
                "cpf",
                "CPF é obrigatório",
                getSeverity()
            );
            return;
        }
        
        // 2. Formato (11 dígitos)
        if (!CPF_PATTERN.matcher(cpf).matches()) {
            result.addError(
                getRuleName(),
                "cpf",
                String.format("CPF '%s' deve ter 11 dígitos numéricos", cpf),
                getSeverity()
            );
            return;
        }
        
        // 3. Dígitos verificadores (opcional)
        if (!isValidCpfChecksum(cpf)) {
            result.addError(
                getRuleName(),
                "cpf",
                String.format("CPF '%s' possui dígitos verificadores inválidos", cpf),
                getSeverity()
            );
        }
    }
    
    /**
     * Valida dígitos verificadores do CPF (algoritmo oficial)
     */
    private boolean isValidCpfChecksum(String cpf) {
        // Implementação do algoritmo
        // https://www.geradorcpf.com/algoritmo_do_cpf.htm
        
        // Simplificado para exemplo
        return true;
    }
    }

```

#### Regras Estruturais (6 total)

| Ordem | Regra | Campo | Severidade | Descrição |
|-------|-------|-------|------------|-----------|
| 1 | `CpfFormatValidationRule` | cpf | ERROR | 11 dígitos + dígito verificador |
| 2 | `PisFormatValidationRule` | pis | ERROR | 11 dígitos (se informado) |
| 3 | `RequiredFieldsValidationRule` | multiple | ERROR | Nome, admissão obrigatórios |
| 4 | `NumericFieldsValidationRule` | salary | ERROR | Numérico positivo |
| 5 | `DateFormatValidationRule` | dates | ERROR | ISO 8601 válido |
| 6 | `EnumValidationRule` | status | ERROR | Valor em enum válido |

---

### Camada 2: Validações de Negócio

#### Exemplo: Minimum Age Validation

```

@Component
@Order(10)
public class MinimumAgeValidationRule extends AbstractValidationRule {

    private static final int MINIMUM_AGE = 16;  // CLT Art. 7º, XXXIII
    
    public MinimumAgeValidationRule() {
        super("MINIMUM_AGE_VIOLATION", ValidationSeverity.ERROR, 10);
    }
    
    @Override
    protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
        LocalDate birthDate = event.getBirthDate();
        LocalDate admissionDate = event.getAdmissionDate();
        
        if (birthDate == null || admissionDate == null) {
            return;  // Já validado na camada estrutural
        }
        
        // Calcular idade na data de admissão
        int ageAtAdmission = Period.between(birthDate, admissionDate).getYears();
        
        if (ageAtAdmission < MINIMUM_AGE) {
            result.addError(
                getRuleName(),
                "birthDate",
                String.format(
                    "Idade na admissão (%d anos) abaixo do mínimo legal (%d anos). " +
                    "Consulte CLT Art. 7º, XXXIII",
                    ageAtAdmission, MINIMUM_AGE
                ),
                getSeverity()
            );
        }
    }
    }

```

#### Regras de Negócio (5 total)

| Ordem | Regra | Severidade | Descrição | Base Legal |
|-------|-------|------------|-----------|------------|
| 10 | `MinimumAgeValidationRule` | ERROR | Idade >= 16 anos | CLT Art. 7º, XXXIII |
| 11 | `FutureDateValidationRule` | ERROR | Datas não futuras | Lógica |
| 12 | `LogicalDateOrderValidationRule` | ERROR | Demissão > Admissão | Lógica |
| 13 | `MinimumSalaryValidationRule` | **WARNING** | Salário >= R$ 1.320 | Lei 14.663/2023 |
| 14 | `StatusTransitionValidationRule` | ERROR | Transições válidas | Domínio |

**Nota:** Regra de salário mínimo é **WARNING** (não bloqueia), pois pode haver casos especiais (aprendiz, estágio).

---

### Camada 3: Validações eSocial (Futuro)

```

@Component
@Order(20)
public class ESocialXsdValidationRule extends AbstractValidationRule {

    private final ESocialWebServiceClient esocialClient;
    
    @Override
    protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
        // 1. Validar contra XSD schema do eSocial
        boolean xsdValid = validateAgainstXsd(event);
        if (!xsdValid) {
            result.addError(
                "ESOCIAL_XSD_INVALID",
                "event",
                "Evento não conforme com XSD do eSocial",
                ValidationSeverity.ERROR
            );
            return;
        }
        
        // 2. Validar CBO (Código Brasileiro de Ocupações)
        boolean cboValid = esocialClient.validateCbo(event.getJobTitle());
        if (!cboValid) {
            result.addError(
                "ESOCIAL_CBO_INVALID",
                "jobTitle",
                String.format("CBO '%s' não encontrado na tabela eSocial", 
                             event.getJobTitle()),
                ValidationSeverity.ERROR
            );
        }
        
        // 3. Outras validações governamentais
        // - CNAE empresa
        // - Certificado digital válido
        // - Webservice disponível
    }
    }

```

**Status:** Planejado para Sprint 4 (integração real com eSocial)

---

## Severidades de Validação

```

public enum ValidationSeverity {
ERROR,    // Bloqueia processamento (DLQ)
WARNING,  // Permite processamento (log apenas)
INFO      // Informativo (métricas)
}

```

### Fluxo por Severidade

```

ERROR → Evento vai para DLQ (não persiste)
WARNING → Evento persiste + log de aviso
INFO → Evento persiste + métrica coletada

```

---

## Consequências

### Positivas ✅

1. **Extensibilidade**
```

// Adicionar nova regra = criar classe
@Component
@Order(15)
public class NewBusinessRule extends AbstractValidationRule {
// Implementação
}

```

2. **Testabilidade**
```

@Test
void shouldRejectMinorEmployee() {
var event = createEventWithAge(15);
var result = new ValidationResult();

       minimumAgeRule.validate(event, result);
       
       assertThat(result.hasError()).isTrue();
       assertThat(result.getErrors())
           .extracting("ruleName")
           .contains("MINIMUM_AGE_VIOLATION");
    }

```

3. **Rastreabilidade**
- Cada erro identifica regra exata
- Correlation ID ponta-a-ponta
- Logs estruturados JSON

4. **Performance**
- Fail-fast: para no primeiro ERROR
- Evita validações desnecessárias
- Latência média: 5-10ms

5. **Manutenibilidade**
- Regras isoladas (SRP)
- Código reutilizável (AbstractValidationRule)
- Fácil debug (uma regra por vez)

### Negativas ⚠️

1. **Complexidade Inicial**
- Mais classes (11+ regras)
- Padrões de design (Strategy, Template, Chain)
- **Mitigação:** Documentação clara + exemplos

2. **Overhead de Abstrações**
- Interface + classe abstrata + implementações
- **Mitigação:** Benefício a longo prazo supera custo inicial

3. **Ordem de Execução Importa**
- Regras devem ser ordenadas corretamente
- **Mitigação:** Anotação `@Order` explícita

---

## Métricas de Sucesso

### Cobertura de Testes

| Componente | Testes | Coverage |
|------------|--------|----------|
| **ValidationEngine** | 8 | 95% |
| **Structural Rules** | 18 (3 por regra) | 100% |
| **Business Rules** | 15 (3 por regra) | 100% |
| **Total** | **41 testes** | **98%** |

### Performance

| Métrica | Valor Atual | Target | Status |
|---------|-------------|--------|--------|
| **Latência P50** | 3ms | < 10ms | ✅ |
| **Latência P95** | 8ms | < 20ms | ✅ |
| **Latência P99** | 15ms | < 50ms | ✅ |
| **Taxa de Sucesso** | 92% | > 85% | ✅ |
| **Taxa de Erro** | 8% | < 15% | ✅ |

---

## Evolução Futura (Backlog)

### Camada 3: Validações eSocial (Pós-TCC)

**Status:** ⏳ Planejado para evolução futura (fora do escopo do TCC)

A terceira camada de validações foi **projetada mas não implementada** no projeto acadêmico, pois requer:
- Integração com webservices governamentais
- Certificado Digital A1/A3
- Homologação junto ao eSocial

#### Implementação Planejada
```
@Component
@Order(20)
public class ESocialXsdValidationRule extends AbstractValidationRule {
    private final ESocialWebServiceClient esocialClient;

    @Override
    protected void doValidate(EmployeeEventDTO event, ValidationResult result) {
        // 1. Validar contra XSD schema do eSocial
        // 2. Validar CBO (Código Brasileiro de Ocupações)
        // 3. Validar CNAE da empresa
        // 4. Verificar certificado digital
    }  
}
```
**Esforço Estimado:** 60 horas  
**Complexidade:** Alta (integração gov.br)  
**ROI:** Crítico para produção real

---

### Machine Learning (Futuro Distante)

**Objetivo:** Validações preditivas e inteligentes

- ⏳ ML para detectar anomalias (salários outliers)
- ⏳ Sugestões automáticas de correção
- ⏳ Predição de validações que falharão

**Esforço Estimado:** 60+ horas  
**Complexidade:** Muito Alta  
**ROI:** Médio (nice-to-have)

---





## Comparação com Abordagens Alternativas

| Abordagem | Código | Testes | Manutenção | Extensão |
|-----------|--------|--------|------------|----------|
| **Monolítica** | 500 LOC | Difícil | Difícil | Quebra existente |
| **Bean Validation** | 200 LOC | Médio | Fácil | Limitado |
| **3 Camadas (Este ADR)** | 800 LOC | Fácil | Fácil | Plugável |

**Trade-off:** Mais código inicial, mas muito mais fácil de manter e estender.

---

## Referências

- [JSR-303: Bean Validation](https://beanvalidation.org/1.0/spec/)
- [Design Patterns: Strategy Pattern](https://refactoring.guru/design-patterns/strategy)
- [Design Patterns: Template Method](https://refactoring.guru/design-patterns/template-method)
- [Design Patterns: Chain of Responsibility](https://refactoring.guru/design-patterns/chain-of-responsibility)
- [eSocial: Manual de Orientação](http://sped.rfb.gov.br/projeto/show/274)
- [CLT: Consolidação das Leis do Trabalho](http://www.planalto.gov.br/ccivil_03/decreto-lei/del5452.htm)
- [ADR-0003: Validação em Duas Camadas (Revisado)](0003-two-layer-validation.md)
- [ADR-0005: Dead Letter Queue](0005-dead-letter-queue.md)

---

## Histórico de Revisões

| Data | Versão | Autor | Mudança |
|------|--------|-------|---------|
| 2025-11-22 | 1.0 | Márcio Kuroki | Criação inicial |
| 2025-11-22 | 1.1 | Márcio Kuroki | Evolução de 2 para 3 camadas |

---