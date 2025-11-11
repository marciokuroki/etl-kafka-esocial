## Visão Geral

O motor de validações (`ValidationEngine`) é responsável por aplicar um conjunto de regras definidas para cada evento recebido, acumulando erros e avisos que serão tratados conforme severidade. Ele utiliza o padrão Strategy, onde cada regra implementa a interface `ValidationRule`.

## Componentes principais

- **ValidationRule (interface)**: Define os métodos `validate(EmployeeEventDTO, ValidationResult)` e `getRuleId()`.
- **ValidationSeverity (enum)**: Níveis ERROR, WARNING e INFO para classificar os impactos.
- **ValidationResult**: Armazena erros e avisos detectados, com getters para ambos e método para verificar validade do evento.
- **ValidationEngine**: Orquestra aplicação sequencial das regras no evento.
- **Regras concretas**: Classes específicas (`BusinessValidationRule`, `StructuralValidationRule`) que implementam regras de negócio e estruturais.


## Fluxo

1. Evento recebido pelo consumer.
2. Criação do `ValidationResult`.
3. Chamada `validationEngine.validate(event)`.
4. Para cada `ValidationRule`, executa `validate(event, result)`.
5. Resultado acumulado em `ValidationResult`.
6. Ação corretiva se houver erros (ex: enviar para DLQ).

## Extensibilidade

Novas regras podem ser criadas implementando `ValidationRule` e adicionadas ao contexto Spring para injeção automática no motor.

***

# Exemplos de Regras Extras para Implementação

## Regras Estruturais

- `CpfValidationRule`: valida formato e dígito verificador do CPF.
- `EmailFormatValidationRule`: valida email conforme regex.
- `PhoneNumberValidationRule`: valida formato numérico do telefone.
- `ZipCodeValidationRule`: valida formato CEP.


## Regras de Negócio

- `DateConsistencyValidationRule`: verifica ordem e lógica de datas (admissão < desligamento).
- `SalaryRangeValidationRule`: verifica se salário está dentro do intervalo legal.
- `StatusValidationRule`: valida consistência do status ativo vs data de desligamento.

***

# Como criar uma nova regra (template)

```java
@Component
public class NovaRegraDeValidacao implements ValidationRule {

    @Override
    public String getRuleId() {
        return "NOVA_REGRA_001";
    }

    @Override
    public void validate(EmployeeEventDTO event, ValidationResult result) {
        // Exemplo: Validar campo x com regra y
        if (condicaoInvalida) {
            result.addError(getRuleId(), "Mensagem de erro descrevendo problema",
                "campoX", event.getCampoX(), ValidationSeverity.ERROR);
        }
    }
}
```