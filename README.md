# JAS Games 

> **Proyecto universitario desarrollado para las materias de Programación III e Ingeniería de Requerimientos.**
>
> **Comitente:** Escuela de Educación Básica Particular "Timoleón Povea Garzón"
>
> *Última actualización: 11 de diciembre de 2025*

---

## 🚧 Estado Actual: Pre-Alpha (15% Completado)

**⚠️ Atención:** Este software se encuentra en etapa inicial de desarrollo.
Aunque la planificación abarca 5 módulos integrales, la versión actual es un prototipo funcional centrado en validar la **arquitectura base, la gestión de perfiles y la lógica de los juegos**.

* **Interfaz:** Diseño minimalista provisional (placeholders).
* **Funcionalidad:** Módulos de Docente y Estudiante parcialmente implementados.

---

## 📖 Descripción del Proyecto

**JAS Games** es una plataforma educativa de escritorio diseñada bajo el enfoque **DUA (Diseño Universal para el Aprendizaje)** para apoyar a niños con Trastorno del Espectro Autista (TEA).

El proyecto nace de la necesidad de la *Escuela Timoleón Povea Garzón* de contar con herramientas tecnológicas que adapten la enseñanza tradicional a las necesidades neurodivergentes, enfocándose en áreas críticas como **atención, colores, números y fonemas**.

---

## 🎯 Alcance y Arquitectura del Sistema

El sistema final está diseñado sobre 5 módulos estratégicos (definidos en la Ingeniería de Requerimientos):

### 1. Gestión de Juegos y Actividades (En Desarrollo)
Administración del catálogo de minijuegos. Permite configurar reglas, niveles de dificultad y estímulos multisensoriales para adaptarse al ritmo de cada niño.

### 2. Perfiles y Planes Individuales - PIA (Implementado)
Gestión de usuarios y creación de Planes Individuales de Aprendizaje (PIA).
* **Funcionalidad actual:** CRUD de estudiantes, diagnósticos y asignación de planes.

### 3. Sesiones y Analítica (En Desarrollo)
Registro automático de desempeño.
* **Dashboard:** Visualización de tablas con puntajes y filtrado por actividad para medir el progreso real.

### 4. Biblioteca de Recursos Multisensoriales (Planificado)
Repositorio centralizado de imágenes, audios y pictogramas para personalizar la experiencia sin depender de internet constante.

### 5. Comunicación y Recompensas (Planificado)
Sistema de gamificación (badges/logros) y notificaciones para mantener a los padres y docentes alineados con el avance del niño.

---

## 🔒 Requisitos Técnicos y de Diseño

El desarrollo se rige por altos estándares de calidad definidos en la fase de análisis:

* **Accesibilidad:** Diseño de interfaz siguiendo pautas **WCAG 2.1** (íconos grandes, bajo ruido visual, navegación simple) para usuarios con hipersensibilidad sensorial.
* **Seguridad:** Arquitectura preparada para encriptación de datos sensibles y control de acceso basado en roles (RBAC).
* **Persistencia:** Uso de **JSON (Gson)** para portabilidad y fácil respaldo de datos en entornos escolares con infraestructura limitada.

---

## 🛠 Tecnologías y Herramientas

- **Lenguaje:** Java (JDK 24)
- **Interfaz Gráfica:** Swing (JFrame, JPanel, LayoutManagers personalizados).
- **Diseño UI:** IntelliJ IDEA UI Designer (.form).
- **Persistencia de Datos:**
    - Archivos JSON para almacenar perfiles (`data/ninos.json`).
    - Librería **Google Gson (2.10.1)** para serialización/deserialización de objetos.
- **Arquitectura:** Modelo-Vista-Servicio (separación de lógica de negocio y UI).

---

## 🚀 Instalación y Ejecución

1. **Prerrequisitos:** Tener instalado el JDK y un IDE compatible (IntelliJ IDEA recomendado).
2. **Librerías:** Asegurarse de que la librería `gson-2.10.1.jar` (incluida en la carpeta `/lib`) esté agregada al *Classpath* del proyecto.
3. **Ejecución:**
    - Abrir el proyecto en IntelliJ IDEA.
    - Ejecutar la clase `src/com/jasgames/ui/App.java`.
    - Seleccionar el rol ("Docente" o "Estudiante") en la ventana inicial.

---

## 👥 Autores - Equipo JAS Games

- **Julio Mera** 
- **Jeremy Tomaselly** 
- **Samuel Cobo** 
- **Amelia Povea** 
- **Alisson Armas** 

---

## 📂 Estructura del Proyecto

```text
src/com/jasgames/
├── model/              # Clases de dominio (Entidades)
│   ├── Nino.java       # Datos del estudiante y lógica de puntajes
│   ├── Juego.java      # Definición de los juegos disponibles
│   ├── Actividad.java  # Instancia de un juego en ejecución
│   ├── PIA.java        # Plan Individual de Atención
│   ├── ResultadoJuego.java # Registro histórico de partidas
│   └── TipoJuego.java  # Enum (COLORES, NUMEROS, FONEMAS)
│
├── service/            # Lógica de Negocio y Persistencia
│   ├── AppContext.java # Inyección de dependencias (Singleton context)
│   ├── PerfilService.java # CRUD de niños y manejo de JSON (Gson)
│   ├── JuegoService.java  # Lógica de colas de actividades
│   └── ResultadoService.java # Gestión de estadísticas
│
├── ui/                 # Interfaz Gráfica (Swing Forms)
│   ├── App.java        # Main / Punto de entrada
│   ├── SeleccionUsuarioWindow.java # Selector de rol
│   ├── DocenteWindow.java    # Contenedor principal del docente
│   ├── EstudianteWindow.java # Contenedor principal del estudiante
│   ├── PerfilesPanel.java    # Panel de gestión de alumnos
│   ├── JuegosPanel.java      # Panel de asignación de juegos
│   └── DashboardPanel.java   # Panel de reportes
