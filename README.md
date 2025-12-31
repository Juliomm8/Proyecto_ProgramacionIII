# JAS Games

> **Proyecto universitario desarrollado para las materias de Programación III e Ingeniería de Requerimientos.**
>
> **Comitente:** Escuela de Educación Básica Particular "Timoleón Povea Garzón"
>
> *Última actualización: 30 de diciembre de 2025*

---

## 🚧 Estado Actual: Pre-Alpha (30% Completado)

**⚠️ Atención:** Este software está en etapa de desarrollo.
La planificación contempla 5 módulos integrales, y la versión actual ya valida la **arquitectura base**, el **flujo de acceso híbrido**, la **gestión de perfiles**, la **persistencia** y una primera versión de **analítica (dashboard)**.

- **Interfaz:** Diseño provisional (en proceso de pulido).
- **Funcionalidad:** Módulos Docente/Estudiante operativos, con mejoras de UX, auditoría y filtros.

---

## 📖 Descripción del Proyecto

**JAS Games** es una plataforma educativa de escritorio diseñada bajo el enfoque **DUA (Diseño Universal para el Aprendizaje)** para apoyar a niños con Trastorno del Espectro Autista (TEA).

El proyecto nace de la necesidad de la *Escuela Timoleón Povea Garzón* de contar con herramientas tecnológicas que adapten la enseñanza tradicional a las necesidades neurodivergentes, enfocándose en áreas críticas como **atención, colores, números y fonemas**.

---

## 🎯 Alcance y Arquitectura del Sistema

El sistema final se compone de 5 módulos estratégicos:

### 1. Gestión de Juegos y Actividades (En Desarrollo)
Administración del catálogo de minijuegos, asignación de juegos y configuración de dificultad.

### 2. Perfiles y Planes Individuales - PIA (Implementado / En pulido)
CRUD de estudiantes (niños), gestión de datos básicos, asignación de juegos, aula y avatar.

### 3. Sesiones y Analítica (En Desarrollo / Funcional)
Registro de partidas y visualización de reportes:
- Persistencia de resultados (`resultados.json`)
- **Dashboard** con filtros avanzados (aula, dificultad, rango de fechas, búsqueda, orden)
- KPIs (indicadores) e interacción rápida desde el dashboard

### 4. Biblioteca de Recursos Multisensoriales (Planificado)
Repositorio de recursos (imágenes, audios, pictogramas) para personalización offline.

### 5. Comunicación y Recompensas (Planificado)
Gamificación (logros), recompensas y notificaciones para docentes y representantes.

---

## 🔐 Seguridad y Acceso (Híbrido)

El sistema maneja **dos flujos de entrada distintos**:

### 👩‍🏫 Docente (Administrativo)
- Login clásico: **usuario + contraseña**
- Persistencia en `data/docentes.json`
- Control por sesión (no se puede acceder a secciones docentes sin sesión iniciada)
- Registro de acciones en auditoría

### 🧒 Estudiante (Accesibilidad / UX)
- Login visual tipo cascada (sin teclado):
  1) Selección de **Aula**
  2) Selección de **estudiante** por ficha (nombre/avatar)
- Diseñado para niños (3–10 años) con mínima carga de lectura/escritura

---

## 🧾 Auditoría (Trazabilidad)
- Se registra actividad en `data/auditoria.log`
- El docente cuenta con un **panel de Auditoría** para:
  - Filtrar por tipo
  - Buscar por texto
  - Ver conteo de registros visibles/cargados

---

## 🏫 Aulas configurables (Gestión “pro”)
Las aulas se administran desde `data/aulas.json` (sin tocar código):
- Crear aulas
- Cambiar color
- Eliminar aulas (con migración segura de estudiantes a otra aula)

---

## 🛠 Tecnologías y Herramientas

- **Lenguaje:** Java (JDK 24)
- **UI:** Swing + IntelliJ UI Designer (.form)
- **Persistencia:** JSON (Gson)
- **Arquitectura:** Modelo - Vista - Servicio (separación UI / lógica / datos)

---

## 📦 Archivos de Datos (Persistencia)

En la carpeta `data/`:

- `ninos.json` → estudiantes
- `juegos.json` → catálogo/asignación
- `docentes.json` → credenciales docentes
- `aulas.json` → aulas y colores
- `resultados.json` → historial de partidas
- `auditoria.log` → bitácora de acciones (generado en ejecución)

> Recomendación: no versionar archivos generados en ejecución (ej. `auditoria.log`).

---

## 🚀 Instalación y Ejecución

1. **Prerrequisitos:** JDK instalado y un IDE compatible (recomendado: IntelliJ IDEA).
2. **Librerías:** Asegurar que `gson-2.10.1.jar` (en `/lib`) esté agregado al *Classpath*.
3. **Ejecutar:**
   - Abrir el proyecto en IntelliJ IDEA
   - Ejecutar `src/com/jasgames/ui/App.java`
   - Se mostrará la ventana inicial **AccesoWindow** (Docente / Estudiante)

---

## 👥 Autores - Equipo JAS Games
- **Julio Mera**
- **Jeremy Tomaselly**
- **Samuel Cobo**
- **Amelia Povea**
- **Alisson Armas**

---

## 📂 Estructura del Proyecto (Referencial)

```text
src/com/jasgames/
├── model/
│   ├── Nino.java
│   ├── Docente.java
│   ├── Aula.java
│   ├── Juego.java
│   ├── Actividad.java
│   ├── ResultadoJuego.java
│   └── TipoJuego.java
│
├── service/
│   ├── AppContext.java
│   ├── PerfilService.java
│   ├── JuegoService.java
│   ├── ResultadoService.java
│   ├── AuditoriaService.java
│   └── AulaService.java
│
├── ui/
│   ├── App.java
│   ├── DocenteWindow.java
│   ├── EstudianteWindow.java
│   ├── PerfilesPanel.java
│   ├── JuegosPanel.java
│   ├── DashboardPanel.java
│   ├── AuditoriaPanel.java
│   └── AulasPanel.java
│└── ui/login/
│   ├── AccesoWindow.java
│   ├── LoginDocenteWindow.java
│   └── AccesoEstudianteWindow.java
└── ui/juegos/
    ├── BaseJuegoPanel.java
    ├── JuegoListener.java
    └── JuegoColoresPanel.java
```
---

## ✅ Checklist del 70% restante (Pendiente)

> Objetivo: pasar de **Pre-Alpha (30%)** a una versión **estable** y presentable para entrega final.

### 🎮 Juegos y Contenido (Alta prioridad)
- [ ] **Implementar los 4 minijuegos faltantes** (actualmente solo hay 1 funcional).
- [ ] Definir para cada juego:
  - [ ] Objetivo pedagógico (colores, números, fonemas, atención, etc.)
  - [ ] Reglas / niveles / dificultad
  - [ ] Sistema de puntaje y condiciones de finalización
- [ ] Integrar resultados de todos los juegos al sistema de `resultados.json`.

### 🧩 UX/UI (Alta prioridad – Pulido visual general)
- [ ] Rediseñar visualmente las pantallas principales para que se vean más modernas y consistentes:
  - [ ] AccesoWindow / LoginDocenteWindow / AccesoEstudianteWindow
  - [ ] DocenteWindow (tabs) y EstudianteWindow
  - [ ] Panel Perfiles, Aulas, Dashboard y Auditoría
- [ ] Unificar estilos:
  - [ ] Tipografías, tamaños, márgenes/padding, colores y botones
  - [ ] Íconos/avatares, títulos, mensajes, y consistencia de layouts
- [ ] Mejorar accesibilidad para niños:
  - [ ] Botones más grandes, colores más claros, navegación simple
  - [ ] Minimizar lectura/teclado y reducir elementos distractores

### 🏫 Aulas (Escalable / Gestión completa)
- [ ] Terminar el pulido visual del sistema de aulas:
  - [ ] Colores y diseño final en botones/fichas/tablas
  - [ ] Confirmaciones más claras al eliminar aulas (migración)
- [ ] (Opcional pro) Asignar aulas a docentes:
  - [ ] Cada docente ve solo sus aulas/niños (control por rol/propiedad)

### 🔐 Seguridad y cuentas (Media prioridad)
- [ ] UI para **gestión de docentes** desde el sistema (crear/editar/eliminar) sin editar JSON manualmente.
- [ ] Mejorar mensajes de error en login (más claros y amigables).
- [ ] Validar reglas mínimas de contraseñas (si se requiere por el curso).

### 📊 Analítica y Reportes (Media prioridad)
- [ ] En Dashboard:
  - [ ] Mejorar diseño (tarjetas KPI, tablas más limpias)
  - [ ] Reportes adicionales: por aula, por estudiante, por juego, por rango
- [ ] Exportación (opcional):
  - [ ] Exportar reportes a PDF/CSV para docentes

### 🧾 Auditoría (Media prioridad)
- [ ] Mejoras visuales finales del panel de auditoría.
- [ ] Agregar filtros avanzados (por fecha / por usuario / por acción).
- [ ] Rotación o limpieza de log (evitar que crezca infinito).

### 🧱 Calidad y estabilidad (Alta prioridad antes de entrega)
- [ ] Validaciones de datos (no permitir campos vacíos / ids duplicados).
- [ ] Manejo de errores y fallback (si falta un JSON o está corrupto).
- [ ] Pruebas manuales con datos reales + datos de prueba:
  - [ ] Login docente / login visual estudiante
  - [ ] CRUD de perfiles + aulas
  - [ ] Juegos + guardado de resultados
  - [ ] Dashboard + Auditoría
- [ ] Mejorar README final: instalación, guía de uso, screenshots.

---
