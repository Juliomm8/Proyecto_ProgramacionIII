# JAS Games

> **Proyecto universitario desarrollado para las materias de Programación III e Ingeniería de Requerimientos.**  
> **Comitente:** Escuela de Educación Básica Particular *“Timoleón Povea Garzón”*  
> *Última actualización: 16 de enero de 2026*

---

## ✅ Estado Actual: Beta / Entregable (funcional + pulido)
El sistema cuenta con el flujo completo **Docente / Estudiante**, persistencia robusta en JSON con backups, analítica (Dashboard + PIA), auditoría y 5 minijuegos integrados.

- **Docente:** login, gestión de perfiles, aulas, catálogo/asignación de juegos, PIA, dashboard, auditoría, backups/restauración, demo/limpieza y accesibilidad.
- **Estudiante:** acceso visual por aula/estudiante (sin teclado), ejecución de juegos y guardado automático de resultados.
- **Persistencia:** datos en `data/*.json` + backups automáticos en `data/backups/`.

---

## 🎮 Minijuegos implementados (5/5)
Catálogo en `data/juegos.json`:

- Discriminación de Colores
- Cuenta y Conecta
- Sigue la Serie
- Vocales Divertidas
- Explorando las Vocales

Incluyen niveles/dificultad, retroalimentación amigable (enfoque TEA) y registro de resultados para analítica.

---

## 🧩 Enfoque pedagógico
**JAS Games** es una plataforma educativa de escritorio diseñada bajo **DUA (Diseño Universal para el Aprendizaje)** para apoyar a niños con **TEA** mediante actividades lúdicas enfocadas en atención, colores, números, series y vocales.

---

## 🔐 Seguridad y Acceso

### 👩‍🏫 Docente
- Login con **usuario + contraseña** (`data/docentes.json`).
- **Creación de usuario docente desde la UI** (sin editar JSON manualmente).
- Acceso a paneles administrativos (Perfiles, Aulas, Dashboard, Auditoría, etc.).

### 🧒 Estudiante (accesible)
Acceso visual en 2 pasos:
1) Selección de **Aula**
2) Selección de **Estudiante** (ficha con nombre/avatar)

Incluye mejoras de UX:
- Diseño más infantil/visual en pantallas de acceso.
- Emojis/avatares renderizados de forma compatible (evita “cuadritos”).
- Confirmaciones no invasivas para operaciones normales (y confirmación para acciones sensibles).

---

## 📊 Analítica (Dashboard)
- Tabla de resultados (sesiones) con filtros por:
  - aula, estudiante, juego, dificultad, rango de fechas, búsqueda.
- **Debounce en búsqueda** (mejor rendimiento con listas grandes).
- **Eliminar sesión con “Deshacer”** (ventana de tiempo breve para revertir).
- **Exportar PIA a CSV** desde Dashboard.

---

## 🧠 PIA (Plan Individual de Apoyo)
- Gestión y seguimiento de objetivos por estudiante.
- Progreso se actualiza automáticamente según sesiones registradas.
- Recalculo de progreso cuando corresponde (por ejemplo, al eliminar/restaurar sesiones).

---

## 🧾 Auditoría
- Registro de acciones en `data/auditoria.log`.
- Panel de auditoría con búsqueda y filtros (con debounce).

---

## 💾 Backups y restauración (anti-pérdida de datos)
- Antes de sobrescribir archivos `data/*.json`, el sistema crea backups automáticos en:
  - `data/backups/YYYY-MM-DD_HH-mm-ss-SSS/`
- UI en modo docente para:
  - listar backups disponibles,
  - ver archivos contenidos,
  - **restaurar** un backup (con confirmación).

---

## 🧪 Demo y limpieza (para exposiciones)
En modo docente:
- **Demo:** carga datos de ejemplo (aulas, niños, PIA y sesiones).
- **Limpiar:** borra datos operativos (niños/sesiones/PIA) y resetea aulas.
- En ambos casos: crea **backup automático** antes de sobrescribir.

---

## ♿ Accesibilidad (persistente)
Configuraciones guardadas en `data/ui_settings.json`:
- Letra grande (Docente)
- Letra grande (Estudiante)
- Alto contraste (Estudiante)
- Pantalla completa (Estudiante)

Estas opciones se activan desde el botón **Accesibilidad** en Modo Docente.

---

## 🏫 Aulas configurables
Aulas administradas en `data/aulas.json`:
- Crear aulas
- Cambiar color
- Eliminar/migrar estudiantes de forma segura
- Acciones masivas y utilidades (copiar IDs, mover selección, etc.)

---

## 🛠 Tecnologías
- **Lenguaje:** Java (recomendado **JDK 21+**, probado con JDK 24)
- **UI:** Swing
- **Persistencia:** JSON (Gson)

---

## 📦 Archivos de datos (`data/`)
- `aulas.json` → aulas y colores  
- `ninos.json` → estudiantes  
- `docentes.json` → credenciales docentes  
- `juegos.json` → catálogo/configuración de juegos  
- `pias.json` → PIA por estudiante  
- `resultados.json` → historial de partidas (sesiones)  
- `auditoria.log` → bitácora de acciones  
- `ui_settings.json` → preferencias de accesibilidad  
- `backups/` → copias automáticas antes de sobrescrituras  

> Nota: algunos archivos se crean automáticamente la primera vez que se usan.

---

## 🚀 Ejecución (IntelliJ)
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
├── util/
└── ui/
    ├── login/
    ├── juegos/
    └── (paneles Docente/Estudiante)
```

---

## ✅ Checklist (actualizado)

### 🎮 Juegos y contenido
- [x] Implementar los 5 minijuegos del alcance.
- [x] Registro de partidas para analítica (`resultados.json`).
- [ ] Ajustes finos de métricas/puntajes (escalado TEA y más rondas).

### 🧩 UX/UI
- [x] Acceso con estilo más visual (Docente/Estudiante).
- [x] Login docente con creación de usuario desde la UI.
- [x] Selección visual (sin teclado) para estudiantes.
- [x] Corrección de avatares/emoji compatibles.
- [x] Mejoras en Dashboard: filtros + debounce + “Deshacer”.
- [x] Ventanas Ayuda y Acerca de.
- [ ] Unificación completa de tema global (opcional; se evitó forzar L&F por compatibilidad).

### 📊 Analítica y reportes
- [x] Dashboard funcional con filtros principales.
- [x] Exportar PIA a CSV.
- [ ] Mejoras visuales extra (KPIs/tarjetas, opcional).

### 🔐 Cuentas y administración
- [x] Login docente operativo.
- [x] Crear docente desde UI.
- [ ] CRUD completo de docentes (editar/eliminar desde la UI) (opcional).

### 🧾 Auditoría
- [x] Registro en `auditoria.log` y panel de visualización.
- [x] Búsqueda con debounce.
- [ ] Rotación/archivado automático del log (opcional).

### 💾 Calidad y estabilidad
- [x] Backups automáticos antes de sobrescribir JSON.
- [x] UI de restauración desde backups.
- [x] Demo/Limpiar con backups automáticos.
- [x] Configuración de accesibilidad persistente.
- [ ] Empaquetado (JAR ejecutable) + guía de distribución (pendiente final).
