# ✅ Estructura del Proyecto Actualizada

## 📁 MÓDULO RESOURCE

### Domain
```
resource/domain/
├── Resource.scala                    ← Entidad padre (STUDY | SAMPLE)
├── ResourceId.scala                  ← ID único
├── ResourceType.scala                ← Enum (STUDY | SAMPLE)
├── ResourceRepository.scala          ← Interfaz repositorio
├── ResourceAccess.scala              ← Relación N:M con User
└── ResourceAccessRepository.scala    ← Interfaz acceso
```

### Infrastructure
```
resource/infrastructure/repositories/
├── ResourceMySqlRepository.scala           ← Implementación BD
└── ResourceAccessMySqlRepository.scala     ← Implementación acceso N:M
```

---

## 📁 MÓDULO STUDY

### Domain
```
study/domain/
├── Study.scala                  ← Entidad (especializacion de Resource)
├── StudyId.scala
├── Area.scala
├── StudyRepository.scala
├── StudyFilter.scala
├── StudyAttachment.scala
├── StudyAttachmentRepository.scala
└── exceptions/
    ├── StudyNotFoundException.scala
    └── NotAllowedToUseException.scala
```

### Infrastructure
```
study/infrastructure/
├── controllers/
│   └── StudyController.scala
├── parsers/
│   ├── CreateStudyRequestParser.scala
│   ├── UpdateStudyRequestParser.scala
│   ├── AddStudySampleRequestParser.scala
│   ├── RemoveStudySampleRequestParser.scala
│   └── StudyAttachmentRequestParser.scala
├── writers/
│   ├── StudyWriter.scala
│   └── StudyWithAttachmentsWriter.scala
└── repositories/
    ├── StudyMySqlRepository.scala
    ├── StudyAttachmentMySqlRepository.scala
    └── StudySampleMySqlRepository.scala
```

---

## 📁 MÓDULO SAMPLE

### Domain
```
sample/domain/
├── Sample.scala                 ← Entidad (especialización de Resource)
├── SampleId.scala
├── SampleRepository.scala
├── SampleFilter.scala
├── SampleAttachment.scala       ← Relación N:M con Attachment
├── SampleAttachmentRepository.scala
└── exceptions/
    ├── SampleNotFoundException.scala
    └── SampleNotAllowedException.scala
```

### Infrastructure
```
sample/infrastructure/
├── controllers/
│   └── SampleController.scala
├── parsers/
│   ├── CreateSampleRequestParser.scala
│   └── UpdateSampleRequestParser.scala
├── writers/
│   └── SampleWriter.scala
└── repositories/
    ├── SampleMySqlRepository.scala
    └── SampleAttachmentMySqlRepository.scala  ← Nuevo
```

---

## 🔄 RELACIONES PRINCIPALES

### 1. User → Resource (1:N)
```
Un usuario POSEE muchos recursos
Consulta: SELECT * FROM resources WHERE owner_id = ?
```

### 2. User ↔ Resource (N:M vía ResourceAccess)
```
Un usuario puede ACCEDER a muchos recursos
Un recurso puede ser ACCESIBLE a muchos usuarios
Tabla: resource_access (resource_id, user_id)
```

### 3. Resource → Study (1:1)
```
Un recurso puede ser UN estudio
resourceId en tabla studies
```

### 4. Resource → Sample (1:1)
```
Un recurso puede ser UNA muestra
resourceId en tabla samples
```

### 5. Sample → Attachment (N:M)
```
Una muestra puede tener muchos archivos
Tabla: samples_attachments (sample_id, attachment_id)
```

---

## 💾 TABLAS PRINCIPALES

### resources
```
id BIGINT PRIMARY KEY
resource_type VARCHAR(50)      # STUDY | SAMPLE
owner_id BIGINT FK             # Único dueño
visibility VARCHAR(50)         # PUBLIC | PRIVATE | RESTRICTED
created DATETIME
updated DATETIME
```

### resource_access
```
resource_id BIGINT FK          # Recurso
user_id BIGINT FK              # Usuario con acceso (vendedor anterior)
PRIMARY KEY (resource_id, user_id)
```

### studies
```
id BIGINT PRIMARY KEY
resource_id BIGINT FK UNIQUE   # Enlace a Resource
name VARCHAR(255)
localization VARCHAR(255)
start_date DATETIME
end_date DATETIME
description TEXT
area VARCHAR(50)
methods TEXT
authors TEXT
coordinates VARCHAR(255)
observations TEXT
summary TEXT
antecedent BOOLEAN
section BOOLEAN
name_section VARCHAR(255)
created DATETIME
updated DATETIME
```

### samples
```
id BIGINT PRIMARY KEY
resource_id BIGINT FK UNIQUE   # Enlace a Resource
name VARCHAR(255)
description TEXT
minerals VARCHAR(255)
localization VARCHAR(255)
collection_methods TEXT
is_fresh BOOLEAN
sample_type VARCHAR(50)
materials_used TEXT
rock_type VARCHAR(255)
geological_processes TEXT
created DATETIME
updated DATETIME
```

### samples_attachments
```
sample_id BIGINT FK
attachment_id BIGINT FK
PRIMARY KEY (sample_id, attachment_id)
```

---

## 🔐 LÓGICA DE VALIDACIÓN

### ¿Quién POSEE un recurso?
```scala
resource.ownerId == currentUser.id
→ Puede revender, cambiar visibilidad, eliminar
```

### ¿Quién PUEDE VER un recurso?
```scala
resource.visibility match {
  case PUBLIC => true
  case PRIVATE => 
    resource.ownerId == user.id || 
    resourceAccess.contains(user.id)
  case RESTRICTED => 
    resourceAccess.contains(user.id)
}
```

### Al COMPLETAR una transacción:
```scala
// 1. Cambiar dueño
resource.copy(ownerId = buyer.id)

// 2. Dar acceso al vendedor anterior
resourceAccess.grantAccess(resource, seller)

// 3. Marcar transacción como COMPLETED
```

---

## 📊 FLUJO DE TRANSACCIÓN

```
1. User1 CREA Study
   Resource(owner=User1, visibility=PRIVATE)
   ResourceAccess: (vacío)

2. User1 VENDE a User2
   Transaction PENDING
   Payment PENDING

3. COMPLETAR transacción
   Resource(owner=User2)  ← CAMBIÓ
   ResourceAccess: (Study, User1)  ← AGREGADO
   Transaction COMPLETED
   Payment COMPLETED

4. User1 puede VER pero NO revender
   User2 puede REVENDER
```

---

## 🚀 PRÓXIMOS PASOS

1. ✅ Resource domain y repositorio creado
2. ✅ ResourceAccess N:M creado
3. ✅ Study actualizado (sin visibility)
4. ✅ Sample actualizado (con resourceId y attachments)
5. ⏳ Crear Transaction, Exchange, Payment
6. ⏳ Actualizar Controllers (crear recursos + transacciones)
7. ⏳ Actualizar Writers (serializar correctamente)
8. ⏳ Tests unitarios


