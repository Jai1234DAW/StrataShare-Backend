# ✅ Reorganización Completada

## 📁 Nueva Estructura

```
resource/
├── domain/
│   ├── Resource.scala
│   ├── ResourceId.scala
│   ├── ResourceType.scala
│   ├── ResourceRepository.scala
│   ├── ResourceFilter.scala
│   ├── ResourceAccess.scala
│   ├── ResourceAccessRepository.scala
│   ├── exceptions/
│   │   └── ResourcesNotAllowedException.scala
│   │
│   ├── study/                    ← NUEVO
│   │   ├── Study.scala
│   │   ├── StudyId.scala
│   │   ├── StudyFilter.scala
│   │   ├── StudyRepository.scala
│   │   ├── Area.scala            ← (copiar de study/domain/)
│   │   └── exceptions/
│   │       └── StudyNotFoundException.scala
│   │       └── NotAllowedToUseException.scala
│   │
│   └── sample/                   ← NUEVO
│       ├── Sample.scala
│       ├── SampleId.scala
│       ├── SampleFilter.scala
│       ├── SampleRepository.scala
│       ├── SampleAttachment.scala
│       ├── SampleAttachmentRepository.scala
│       └── exceptions/
│           ├── SampleNotFoundException.scala
│           └── SampleNotAllowedException.scala
│
└── infrastructure/
    ├── repositories/
    │   ├── ResourceMySqlRepository.scala
    │   ├── ResourceAccessMySqlRepository.scala
    │   │
    │   ├── study/               ← NUEVO
    │   │   ├── StudyMySqlRepository.scala
    │   │   └── StudyAttachmentMySqlRepository.scala
    │   │
    │   └── sample/              ← NUEVO
    │       ├── SampleMySqlRepository.scala
    │       └── SampleAttachmentMySqlRepository.scala
    │
    ├── controllers/
    │   ├── study/               ← NUEVO
    │   │   └── StudyController.scala
    │   │
    │   └── sample/              ← NUEVO
    │       └── SampleController.scala
    │
    ├── parsers/
    │   ├── study/               ← NUEVO
    │   │   └── (todos los parsers de study)
    │   │
    │   └── sample/              ← NUEVO
    │       └── (todos los parsers de sample)
    │
    └── writers/
        ├── study/               ← NUEVO
        │   └── (todos los writers de study)
        │
        └── sample/              ← NUEVO
            └── (todos los writers de sample)
```

---

## 🔄 Imports que necesitan actualización

### En Study (nueva ubicación: resource/domain/study/)
```scala
// ANTES:
package dev.pompilius.study.domain
import dev.pompilius.resource.domain.ResourceId

// AHORA:
package dev.pompilius.resource.domain.study
import dev.pompilius.resource.domain.ResourceId
```

### En Sample (nueva ubicación: resource/domain/sample/)
```scala
// ANTES:
package dev.pompilius.sample.domain
import dev.pompilius.resource.domain.ResourceId

// AHORA:
package dev.pompilius.resource.domain.sample
import dev.pompilius.resource.domain.ResourceId
```

### En StudyMySqlRepository (nueva ubicación: resource/infrastructure/repositories/study/)
```scala
// ANTES:
package dev.pompilius.studies.infrastructure.repositories
import dev.pompilius.study.domain.Study

// AHORA:
package dev.pompilius.resource.infrastructure.repositories.study
import dev.pompilius.resource.domain.study.Study
```

### En SampleMySqlRepository (nueva ubicación: resource/infrastructure/repositories/sample/)
```scala
// ANTES:
package dev.pompilius.sample.infrastructure.repositories
import dev.pompilius.sample.domain.Sample

// AHORA:
package dev.pompilius.resource.infrastructure.repositories.sample
import dev.pompilius.resource.domain.sample.Sample
```

---

## 📝 Próximos Pasos

1. **Copiar los archivos de infrastructure** (repositories, controllers, parsers, writers) de:
   - `study/` → `resource/infrastructure/study/`
   - `sample/` → `resource/infrastructure/sample/`

2. **Actualizar todos los imports** en esos archivos

3. **Actualizar Module.scala** con los nuevos paths

4. **Los directorios antiguos** (study/ y sample/ en raíz) pueden eliminarse después

---

## ✅ Lo que ya está listo

- ✅ Study domain (en resource/domain/study/)
- ✅ Sample domain (en resource/domain/sample/)
- ✅ Todos los imports corregidos en domain

## ⏳ Lo que falta

- ⏳ Copiar infrastructure (repositories, controllers, etc)
- ⏳ Actualizar Module.scala
- ⏳ Eliminar directorios antiguos

¿Continúo moviendo los archivos de infrastructure?


