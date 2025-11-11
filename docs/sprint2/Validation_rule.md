## 📊 Regras de Validação Mapeadas

### Categoria 1: Validações Estruturais (Formato e Tipo)

Essas validações verificam se os dados estão no formato correto, independente de lógica de negócio.


| ID | Campo | Regra | Severidade | Mensagem de Erro |
| :-- | :-- | :-- | :-- | :-- |
| **VE-001** | CPF | Deve ter 11 dígitos numéricos e dígitos verificadores válidos | ERROR | "CPF inválido: deve conter 11 dígitos numéricos com dígitos verificadores corretos" |
| **VE-002** | PIS/PASEP | Deve ter 11 dígitos numéricos e dígito verificador válido | ERROR | "PIS/PASEP inválido: deve conter 11 dígitos numéricos com dígito verificador correto" |
| **VE-003** | Nome Completo | Obrigatório, mínimo 3 caracteres, máximo 200 caracteres | ERROR | "Nome completo deve ter entre 3 e 200 caracteres" |
| **VE-004** | Data de Nascimento | Formato YYYY-MM-DD, não pode ser futura | ERROR | "Data de nascimento inválida ou futura" |
| **VE-005** | Data de Admissão | Formato YYYY-MM-DD, não pode ser futura | ERROR | "Data de admissão inválida ou futura" |
| **VE-006** | Data de Desligamento | Formato YYYY-MM-DD, se informada | WARNING | "Data de desligamento inválida" |
| **VE-007** | Email | Formato válido (RFC 5322) | WARNING | "Email em formato inválido" |
| **VE-008** | Telefone | 10 ou 11 dígitos (DDD + número) | WARNING | "Telefone deve conter 10 ou 11 dígitos" |
| **VE-009** | CEP | 8 dígitos numéricos | WARNING | "CEP deve conter 8 dígitos numéricos" |
| **VE-010** | Salário | Valor numérico positivo (> 0) | ERROR | "Salário deve ser maior que zero" |


***

### Categoria 2: Validações de Negócio (Lógica e Dependências)

Essas validações envolvem lógica de negócio e relacionamentos entre campos.


| ID | Campo(s) | Regra | Severidade | Mensagem de Erro |
| :-- | :-- | :-- | :-- | :-- |
| **VN-001** | Data de Nascimento | Colaborador deve ter no mínimo 16 anos na data de admissão | ERROR | "Colaborador deve ter no mínimo 16 anos na data de admissão" |
| **VN-002** | Data de Nascimento | Colaborador não pode ter mais de 120 anos | ERROR | "Data de nascimento implica em idade superior a 120 anos" |
| **VN-003** | Data de Admissão | Não pode ser anterior a 01/01/1900 | ERROR | "Data de admissão não pode ser anterior a 01/01/1900" |
| **VN-004** | Data de Desligamento | Deve ser posterior à data de admissão | ERROR | "Data de desligamento deve ser posterior à data de admissão" |
| **VN-005** | Data de Desligamento | Se informada, status deve ser INACTIVE | ERROR | "Status deve ser INACTIVE quando há data de desligamento" |
| **VN-006** | Status | Se ACTIVE, data de desligamento deve estar vazia | ERROR | "Colaborador ativo não pode ter data de desligamento" |
| **VN-007** | Salário | Deve ser >= salário mínimo vigente (R\$ 1.320,00) | WARNING | "Salário inferior ao mínimo legal (R\$ 1.320,00)" |
| **VN-008** | Salário | Não pode exceder R\$ 1.000.000,00 | WARNING | "Salário excede limite razoável (R\$ 1.000.000,00)" |
| **VN-009** | Cargo | Se informado, deve ter entre 3 e 100 caracteres | WARNING | "Cargo deve ter entre 3 e 100 caracteres" |
| **VN-010** | Departamento | Se informado, deve ter entre 2 e 100 caracteres | WARNING | "Departamento deve ter entre 2 e 100 caracteres" |


***

### Categoria 3: Validações de Conformidade eSocial

Essas validações garantem conformidade específica com as tabelas e regras do eSocial.


| ID | Campo | Regra | Severidade | Mensagem de Erro |
| :-- | :-- | :-- | :-- | :-- |
| **VC-001** | CPF | Não pode ser CPF inválido conhecido (000.000.000-00, 111.111.111-11, etc) | ERROR | "CPF conhecido como inválido (sequência repetida)" |
| **VC-002** | Categoria Trabalhador | Deve ser código válido da Tabela 01 do eSocial | ERROR | "Categoria de trabalhador inválida conforme Tabela 01 do eSocial" |
| **VC-003** | Tipo de Contrato | Deve ser código válido da Tabela 03 do eSocial | ERROR | "Tipo de contrato inválido conforme Tabela 03 do eSocial" |
| **VC-004** | Natureza da Atividade | Deve ser código válido da Tabela 04 do eSocial | WARNING | "Natureza da atividade inválida conforme Tabela 04 do eSocial" |
| **VC-005** | CBO (Ocupação) | Deve ser código válido da CBO 2002 | WARNING | "Código CBO inválido" |
| **VC-006** | Grau de Instrução | Deve ser código válido da Tabela 05 do eSocial | WARNING | "Grau de instrução inválido conforme Tabela 05 do eSocial" |
| **VC-007** | Nacionalidade | Deve ser código de país válido (ISO 3166-1) | WARNING | "Código de nacionalidade inválido" |
| **VC-008** | UF | Deve ser sigla válida de estado brasileiro | WARNING | "UF inválida" |
| **VC-009** | Município | Deve ser código válido do IBGE | WARNING | "Código de município IBGE inválido" |
| **VC-010** | Raça/Cor | Deve ser código válido da Tabela 06 do eSocial | WARNING | "Código de raça/cor inválido conforme Tabela 06 do eSocial" |


***

### Categoria 4: Validações de Integridade (Cross-Field)

Validações que envolvem múltiplos campos e sua consistência.


| ID | Campos | Regra | Severidade | Mensagem de Erro |
| :-- | :-- | :-- | :-- | :-- |
| **VI-001** | CPF + Data Nascimento | Combinação CPF + Data deve ser única no sistema | ERROR | "Já existe colaborador com mesmo CPF e data de nascimento" |
| **VI-002** | CPF + Status | Não pode haver 2+ colaboradores ativos com mesmo CPF | ERROR | "CPF já cadastrado para outro colaborador ativo" |
| **VI-003** | Email | Se informado, deve ser único entre colaboradores ativos | WARNING | "Email já cadastrado para outro colaborador" |
| **VI-004** | PIS/PASEP | Deve ser único no sistema | ERROR | "PIS/PASEP já cadastrado" |
| **VI-005** | Admissão + Desligamento | Período de vínculo deve ter no mínimo 1 dia | ERROR | "Período de vínculo deve ter no mínimo 1 dia" |


***

## 📐 Regras de Validação por Tipo de Evento

### Evento S-2200 (Admissão)

**Campos Obrigatórios:**

- CPF
- Nome Completo
- Data de Nascimento
- Data de Admissão
- Categoria do Trabalhador
- Tipo de Contrato

**Campos Proibidos:**

- Data de Desligamento

**Validações Específicas:**

- Data de admissão não pode ser futura
- Idade mínima de 16 anos
- CPF válido

***

### Evento S-2205 (Alteração Cadastral)

**Campos Obrigatórios:**

- CPF
- Campo(s) alterado(s)

**Validações Específicas:**

- CPF deve existir na base
- Deve haver pelo menos uma alteração
- Não pode alterar CPF ou Data de Nascimento

***

### Evento S-2299 (Desligamento)

**Campos Obrigatórios:**

- CPF
- Data de Desligamento
- Motivo do Desligamento

**Validações Específicas:**

- Data de desligamento >= data de admissão
- Data de desligamento <= data atual
- Colaborador deve estar ativo

***

## 🎨 Matriz de Severidade

| Severidade | Comportamento | Quando Usar |
| :-- | :-- | :-- |
| **ERROR** | Bloqueia processamento, vai para DLQ | Dados incorretos que impedem envio ao eSocial |
| **WARNING** | Permite processamento, registra alerta | Dados suspeitos mas não bloqueantes |
| **INFO** | Apenas registra log | Informações úteis para auditoria |


***

## 📝 Exemplos de Dados

### ✅ Exemplo Válido

```json
{
  "employeeId": "EMP001",
  "cpf": "12345678909",
  "pis": "17033259504",
  "fullName": "João da Silva Santos",
  "birthDate": "1985-03-15",
  "admissionDate": "2020-01-10",
  "terminationDate": null,
  "jobTitle": "Analista de Sistemas",
  "department": "TI",
  "salary": 5500.00,
  "status": "ACTIVE"
}
```

**Validações Aplicadas:**

- ✅ VE-001: CPF válido (11 dígitos + verificadores)
- ✅ VE-002: PIS válido
- ✅ VE-003: Nome com 22 caracteres
- ✅ VN-001: Idade na admissão = 34 anos (≥ 16)
- ✅ VN-007: Salário (R\$ 5.500) > salário mínimo
- ✅ VN-006: Status ACTIVE sem data de desligamento

***

### ❌ Exemplo Inválido 1: CPF Inválido

```json
{
  "employeeId": "EMP002",
  "cpf": "123456789",  // ❌ Apenas 9 dígitos
  "fullName": "Maria Oliveira",
  "birthDate": "1990-07-22",
  "admissionDate": "2019-05-15",
  "salary": 4500.00,
  "status": "ACTIVE"
}
```

**Erros Detectados:**

- ❌ VE-001: CPF deve ter 11 dígitos
- ⚠️ VE-002: PIS não informado (WARNING se não obrigatório)

***

### ❌ Exemplo Inválido 2: Idade Menor que 16 Anos

```json
{
  "employeeId": "EMP003",
  "cpf": "98765432100",
  "fullName": "Pedro Henrique Junior",
  "birthDate": "2010-11-08",  // 14 anos em 2024
  "admissionDate": "2024-06-01",
  "salary": 1500.00,
  "status": "ACTIVE"
}
```

**Erros Detectados:**

- ❌ VN-001: Idade na admissão = 13 anos (< 16)

***

### ❌ Exemplo Inválido 3: Datas Inconsistentes

```json
{
  "employeeId": "EMP004",
  "cpf": "12345678909",
  "fullName": "Ana Paula Ferreira",
  "birthDate": "1992-02-14",
  "admissionDate": "2021-06-01",
  "terminationDate": "2020-12-31",  // ❌ Anterior à admissão
  "salary": 4800.00,
  "status": "INACTIVE"
}
```

**Erros Detectados:**

- ❌ VN-004: Data de desligamento (2020-12-31) < data de admissão (2021-06-01)

***

### ⚠️ Exemplo com Warnings

```json
{
  "employeeId": "EMP005",
  "cpf": "12345678909",
  "fullName": "Carlos Eduardo Souza",
  "birthDate": "1987-09-30",
  "admissionDate": "2017-11-10",
  "salary": 1200.00,  // ⚠️ Abaixo do salário mínimo
  "status": "ACTIVE"
}
```

**Warnings Detectados:**

- ⚠️ VN-007: Salário (R\$ 1.200) < salário mínimo (R\$ 1.320)

**Comportamento:** Evento é processado, mas warning é registrado.

***

## 📄 Artefatos Gerados

### 1. Planilha de Regras

Criar arquivo: `docs/sprint2/validation-rules.xlsx`

**Colunas:**

- ID da Regra
- Categoria
- Campo(s)
- Descrição
- Severidade
- Mensagem de Erro
- Exemplo Válido
- Exemplo Inválido
- Status Implementação


### 2. Documento de Mapeamento

Criar arquivo: `docs/sprint2/VALIDATION_RULES.md` (este documento)

***

## 🎯 Critérios de Aceite

- [x] Mínimo 30 regras documentadas
- [x] Distribuição: 10 estruturais + 10 negócio + 10 conformidade
- [x] Cada regra tem: ID, descrição, severidade, mensagem
- [x] Exemplos de dados válidos e inválidos documentados
- [x] Priorização por criticidade definida
- [x] Documento revisado e aprovado

***

## 📊 Estatísticas

**Total de Regras Mapeadas:** 34 regras

**Distribuição:**

- Validações Estruturais (VE): 10 regras (29%)
- Validações de Negócio (VN): 10 regras (29%)
- Validações de Conformidade (VC): 10 regras (29%)
- Validações de Integridade (VI): 5 regras (15%)

**Por Severidade:**

- ERROR: 22 regras (65%) - Bloqueantes
- WARNING: 12 regras (35%) - Não bloqueantes

**Cobertura de Campos:**

- CPF/PIS: 6 regras
- Datas: 8 regras
- Salário: 3 regras
- Campos textuais: 5 regras
- Códigos eSocial: 7 regras
- Cross-field: 5 regras