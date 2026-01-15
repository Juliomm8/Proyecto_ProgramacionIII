package com.jasgames.service;

import com.jasgames.model.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Carga datos de ejemplo y permite limpiar datos operativos.
 *
 * NO modifica:
 * - catálogo de juegos (juegos.json)
 * - cuentas de docentes / autenticación (docentes.json)
 * - auditoría (solo agrega registros)
 */
public final class DemoDataService {

    private final AppContext context;

    public DemoDataService(AppContext context) {
        this.context = context;
    }

    public void cargarDemo() {
        if (context == null) return;

        // 1) Aulas (catálogo)
        List<Aula> aulas = Arrays.asList(
                new Aula("Aula Azul", "#3498DB"),
                new Aula("Aula Verde", "#2ECC71"),
                new Aula("Aula Amarilla", "#F1C40F")
        );
        context.getAulaService().reemplazarAulas(aulas);

        // 2) Niños
        List<Nino> ninos = new ArrayList<>();

        ninos.add(nino(1, "Liz", 6, "TEA", "Aula Azul", "🦋", juegos(1,2,4)));
        ninos.add(nino(2, "Mateo", 7, "TEA", "Aula Azul", "🦁", juegos(1,2,3)));
        ninos.add(nino(3, "Sofía", 6, "TEA", "Aula Azul", "🐼", juegos(1,4,5)));

        ninos.add(nino(4, "Dylan", 8, "TEA", "Aula Verde", "🐸", juegos(2,3,4)));
        ninos.add(nino(5, "Valentina", 7, "TEA", "Aula Verde", "🦊", juegos(1,3,5)));

        ninos.add(nino(6, "Ian", 7, "TEA", "Aula Amarilla", "🐯", juegos(1,2,5)));
        ninos.add(nino(7, "Camila", 6, "TEA", "Aula Amarilla", "🐰", juegos(2,4,5)));
        ninos.add(nino(8, "Noah", 8, "TEA", "Aula Amarilla", "🐙", juegos(1,3,4,5)));

        // 3) PIA (para algunos niños)
        List<PIA> pias = new ArrayList<>();
        Map<Integer, PIA> piaPorNino = new HashMap<>();

        // Creamos PIA para 4 niños, con 2 objetivos cada uno
        crearPiaConObjetivos(pias, piaPorNino, ninos.get(0),
                "Mejorar identificación de colores y vocales",
                new ObjetivoPIA(1, "Reconocer colores básicos", 12, 3),
                new ObjetivoPIA(4, "Identificar vocales en palabras", 10, 3)
        );

        crearPiaConObjetivos(pias, piaPorNino, ninos.get(1),
                "Trabajar números y series",
                new ObjetivoPIA(2, "Contar y asociar cantidades", 12, 3),
                new ObjetivoPIA(3, "Completar series simples", 10, 3)
        );

        crearPiaConObjetivos(pias, piaPorNino, ninos.get(4),
                "Fortalecer series y fonemas",
                new ObjetivoPIA(3, "Series: patrones AB/ABB", 12, 3),
                new ObjetivoPIA(5, "Vocales: reconocimiento visual", 10, 3)
        );

        crearPiaConObjetivos(pias, piaPorNino, ninos.get(7),
                "Atención y comunicación",
                new ObjetivoPIA(1, "Colores: selección rápida", 12, 3),
                new ObjetivoPIA(5, "Vocales: sonidos básicos", 10, 3)
        );

        // 4) Sesiones (resultados)
        List<SesionJuego> sesiones = new ArrayList<>();
        List<Juego> catalogo = context.getJuegoService().obtenerTodos();

        // Generar sesiones por niño (algunos enlazados al PIA)
        LocalDateTime base = LocalDateTime.now().minusDays(7);

        Random r = new Random(12345);

        for (Nino n : ninos) {
            Set<Integer> asignados = n.getJuegosAsignados();
            if (asignados == null || asignados.isEmpty()) continue;

            // 4-7 sesiones por niño
            int cant = 4 + r.nextInt(4);
            List<Integer> ids = new ArrayList<>(asignados);
            for (int i = 0; i < cant; i++) {
                int idJuego = ids.get(r.nextInt(ids.size()));
                Juego j = juegoPorId(catalogo, idJuego);

                int dificultad = Math.max(1, Math.min(5, 1 + r.nextInt(3)));
                int rondasTotales = 5 + r.nextInt(4);
                int rondasCompletadas = Math.max(1, rondasTotales - r.nextInt(2));
                int puntaje = 20 + (rondasCompletadas * 10) + r.nextInt(15);

                LocalDateTime fh = base.plusDays(r.nextInt(7)).plusHours(8 + r.nextInt(6)).plusMinutes(r.nextInt(55));

                SesionJuego s = new SesionJuego(n.getId(), n.getNombre(), n.getAula(), j, dificultad, puntaje, fh);
                s.setRondasTotales(rondasTotales);
                s.setRondasCompletadas(rondasCompletadas);
                s.setIntentosTotales(rondasCompletadas + r.nextInt(6));
                s.setErroresTotales(r.nextInt(4));
                s.setAciertosTotales(rondasCompletadas);
                s.setAciertosPrimerIntento(Math.max(0, rondasCompletadas - r.nextInt(3)));
                s.setPistasUsadas(r.nextInt(3));
                s.setDuracionMs(30_000L + r.nextInt(120_000));
                s.setFechaFin(fh.plusSeconds(45 + r.nextInt(120)));

                // Vincular a PIA si corresponde
                PIA piaN = piaPorNino.get(n.getId());
                if (piaN != null) {
                    ObjetivoPIA obj = objetivoParaJuego(piaN, idJuego);
                    if (obj != null) {
                        s.setIdPia(piaN.getIdPia());
                        s.setIdObjetivoPia(obj.getIdObjetivo());
                    }
                }

                sesiones.add(s);
            }
        }

        // 5) Recalcular progreso de PIA basado en sesiones
        PiaService.recalcularProgresoLista(pias, sesiones);

        // 6) Ajustar puntos totales (sumatoria simple de puntajes)
        Map<Integer, Integer> puntosPorNino = new HashMap<>();
        for (SesionJuego s : sesiones) {
            if (s == null || s.getIdEstudiante() == null) continue;
            puntosPorNino.merge(s.getIdEstudiante(), Math.max(0, s.getPuntaje()), Integer::sum);
        }
        for (Nino n : ninos) {
            Integer pts = puntosPorNino.get(n.getId());
            if (pts != null) n.setPuntosTotales(pts);
        }

        // 7) Persistir (operación atómica por archivo)
        context.getPerfilService().reemplazarTodosNinos(ninos);
        context.getPiaService().reemplazarPias(pias);
        context.getSesionService().reemplazarResultados(sesiones);

        // Auditoría
        context.getAuditoriaService().registrar("DEMO_LOAD", "Cargó datos de ejemplo (" + ninos.size() + " niños, " + sesiones.size() + " sesiones).");
    }

    /**
     * Limpia datos operativos:
     * - niños (ninos.json)
     * - sesiones/resultados (resultados.json)
     * - PIA (pias.json)
     * - aulas se resetean a por defecto
     *
     * NO borra docentes ni juegos.
     */
    public void limpiarDatosOperativos() {
        if (context == null) return;

        context.getSesionService().limpiarResultados();
        context.getPiaService().limpiarPias();
        context.getPerfilService().limpiarTodosNinos();
        context.getAulaService().resetAulasPorDefecto();

        context.getAuditoriaService().registrar("DATA_CLEAR", "Limpió datos operativos (niños/sesiones/PIA) y reseteó aulas.");
    }

    // ---------------- Helpers ----------------

    private static Nino nino(int id, String nombre, int edad, String diagnostico, String aula, String avatar, Set<Integer> juegos) {
        Nino n = new Nino(id, nombre, edad, diagnostico);
        n.setAula(aula);
        n.setAvatar(avatar);
        n.setJuegosAsignados(juegos);
        // dificultad por juego (base)
        Map<Integer, Integer> dif = new HashMap<>();
        if (juegos != null) {
            for (Integer idJuego : juegos) {
                if (idJuego == null) continue;
                dif.put(idJuego, 1);
            }
        }
        n.setDificultadPorJuego(dif);
        return n;
    }

    private static Set<Integer> juegos(int... ids) {
        Set<Integer> s = new HashSet<>();
        for (int id : ids) s.add(id);
        return s;
    }

    private static void crearPiaConObjetivos(List<PIA> out, Map<Integer, PIA> map, Nino nino,
                                            String objetivoGeneral, ObjetivoPIA... objetivos) {
        if (nino == null) return;
        PIA pia = new PIA(nino.getId(), nino.getNombre(), nino.getAula(), objetivoGeneral);
        if (objetivos != null) {
            for (ObjetivoPIA o : objetivos) pia.agregarObjetivo(o);
        }
        out.add(pia);
        map.put(nino.getId(), pia);
    }

    private static Juego juegoPorId(List<Juego> catalogo, int idJuego) {
        if (catalogo != null) {
            for (Juego j : catalogo) {
                if (j != null && j.getId() == idJuego) return j;
            }
        }
        // fallback
        TipoJuego tipo;
        switch (idJuego) {
            case 1: tipo = TipoJuego.COLORES; break;
            case 2: tipo = TipoJuego.NUMEROS; break;
            case 3: tipo = TipoJuego.SERIES; break;
            case 4:
            case 5: tipo = TipoJuego.FONEMAS; break;
            default: tipo = TipoJuego.COLORES; break;
        }
        return new Juego(idJuego, "Juego " + idJuego, tipo, 1, "");
    }

    private static ObjetivoPIA objetivoParaJuego(PIA pia, int idJuego) {
        if (pia == null) return null;
        for (ObjetivoPIA o : pia.getObjetivos()) {
            if (o != null && o.getJuegoId() == idJuego) return o;
        }
        return null;
    }

    
}
