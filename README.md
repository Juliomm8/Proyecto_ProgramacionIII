# JAS Games

> **Proyecto universitario desarrollado para las materias de Programación III e Ingeniería de Requerimientos.**  
> **Comitente:** Escuela de Educación Básica Particular *“Timoleón Povea Garzón”*  
> *Última actualización: 7 de enero de 2026*

---

## ✅ Estado Actual: Alpha (70% Completado)

El sistema ya cuenta con el flujo completo **Docente / Estudiante**, gestión de datos (JSON), 5 minijuegos integrados y paneles administrativos con UX mejorada (filtros, orden, acciones rápidas).

- **Docente:** gestión de perfiles, aulas, catálogo/asignación de juegos, auditoría y dashboard.  
- **Estudiante:** acceso visual por aula/estudiante y ejecución de juegos.  
- **Persistencia:** datos en `data/*.json` + auditoría en `data/auditoria.log` (y `data/resultados.json` se crea automáticamente al registrar partidas).

---

## 🎮 Minijuegos implementados (5/5)

Actualmente están disponibles **5 minijuegos** (catálogo en `data/juegos.json`):

- Discriminación de Colores
- Cuenta y Conecta
- Sigue la Serie
- Vocales Divertidas
- Explorando las Vocales

Incluyen **niveles/dificultad**, retroalimentación amigable (enfoque TEA) y registro de resultados para analítica.

---

## ✨ Mejoras recientes (UX/Paneles)

### JuegosPanel (Docente)
- Lista “pro” con mejor legibilidad.
- Separación clara entre **dificultad GLOBAL** (catálogo) y **dificultad PERSONAL** (por estudiante).
- Acciones masivas (habilitar/deshabilitar y asignación a todos).
- Selector de estudiante optimizado (pensado para listas grandes).

### PerfilesPanel (Docente)
- Lista de estudiantes con filtros por aula + búsqueda + orden.
- Vista de detalle más clara (avatar, datos y acciones).
- **Aulas nuevas aparecen en el combo aunque estén vacías** (refresco y servicio compartido).

### AulasPanel (Docente)
- Contraste automático para texto según color de aula.
- Tooltips en tabla (textos largos).
- Ordenamiento por columnas (click en encabezados).
- Menú contextual + acciones masivas (mover selección y copiar IDs).

---

## 📖 Descripción del Proyecto

**JAS Games** es una plataforma educativa de escritorio diseñada bajo el enfoque **DUA (Diseño Universal para el Aprendizaje)** para apoyar a niños con **Trastorno del Espectro Autista (TEA)** mediante actividades lúdicas enfocadas en atención, colores, números, series y vocales.

---

## 🔐 Seguridad y Acceso (Híbrido)

### 👩‍🏫 Docente
- Login con **usuario + contraseña** (persistencia en `data/docentes.json`).
- Acceso a gestión y paneles administrativos.

### 🧒 Estudiante (accesible)
- Acceso visual por:
  1) Selección de **Aula**
  2) Selección de **estudiante** (ficha con nombre/avatar)

---

## 📊 Analítica (Dashboard)
- Visualización de resultados guardados.
- Filtros por **aula**, **dificultad**, **rango de fechas** y **orden**.

---

## 🧾 Auditoría
- Registro de acciones en `data/auditoria.log`.
- Panel para lectura rápida de registros.

---

## 🏫 Aulas configurables
Aulas administradas desde `data/aulas.json`:
- Crear aulas
- Cambiar color
- Eliminar (con migración segura de estudiantes a otra aula)

---

## 🛠 Tecnologías
- **Lenguaje:** Java (**JDK 24**)
- **UI:** Swing (paneles por código; pantallas de login aún con `.form`)
- **Persistencia:** JSON (Gson)

---

## 📦 Archivos de datos (`data/`)
- `aulas.json` → aulas y colores  
- `ninos.json` → estudiantes  
- `docentes.json` → credenciales docentes  
- `juegos.json` → catálogo/configuración de juegos  
- `resultados.json` → historial de partidas (**se crea al primer resultado**)  
- `auditoria.log` → bitácora de acciones  

---

## 🚀 Ejecución
1. Abrir el proyecto en IntelliJ IDEA.
2. Verificar que `lib/gson-2.10.1.jar` esté en el classpath.
3. Ejecutar: `src/com/jasgames/ui/App.java`

---

## 👥 Autores - Equipo JAS Games
- Julio Mera  
- Jeremy Tomaselly  
- Samuel Cobo  
- Amelia Povea  
- Alisson Armas  

---

## 📂 Estructura (referencial)

```text
src/com/jasgames/
├── model/
├── service/
└── ui/
    ├── login/
    ├── juegos/
    └── (paneles Docente/Estudiante)
```

---

## ✅ Checklist (actualizado)

### 🎮 Juegos y Contenido
- [x] Implementar los **5 minijuegos** definidos en el alcance.
- [x] Integrar registro de partidas para analítica (`resultados.json`).
- [ ] Afinar métricas/puntajes (más rondas, mejor escalado, cooldown y fallos) según TEA.

### 🧩 UX/UI
- [x] Rediseño **JuegosPanel** (lista, filtros, acciones masivas).
- [x] Rediseño **PerfilesPanel** (lista, detalle, avatar, acciones claras).
- [x] Mejoras **AulasPanel** (contraste, tooltips, orden, menú contextual, acciones masivas).
- [ ] Rediseñar pantallas de acceso (AccesoWindow / LoginDocenteWindow / AccesoEstudianteWindow).
- [ ] Unificar estilo global (tipografías, márgenes, componentes y tema).

### 📊 Analítica y reportes
- [x] Dashboard funcional con filtros principales.
- [ ] Mejorar visual del dashboard (tarjetas KPI, tablas más limpias).
- [ ] Exportar reportes (CSV/PDF) (opcional).

### 🔐 Cuentas y administración
- [x] Login docente operativo.
- [ ] UI para gestión de docentes (CRUD) dentro del sistema (sin editar JSON).

### 🧾 Auditoría
- [x] Registro en `auditoria.log` y panel de visualización.
- [ ] Filtros avanzados (fecha/usuario/acción) + rotación de log.

### 🧱 Calidad y estabilidad
- [ ] Validaciones completas (campos vacíos, ids duplicados, consistencia).
- [ ] Manejo de fallos (JSON corrupto/faltante) con mensajes amigables.
- [ ] Pruebas manuales con datos grandes (500+ estudiantes) y casos límite.

---
