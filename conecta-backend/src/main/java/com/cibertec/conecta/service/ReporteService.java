package com.cibertec.conecta.service;

import com.cibertec.conecta.exception.RecursoNoEncontradoException;
import com.cibertec.conecta.model.Asistencia;
import com.cibertec.conecta.model.Inscripcion;
import com.cibertec.conecta.model.enums.EstadoInscripcion;
import com.cibertec.conecta.repository.AsistenciaRepository;
import com.cibertec.conecta.repository.EventoRepository;
import com.cibertec.conecta.repository.InscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private static final Charset WIN_ANSI = Charset.forName("windows-1252");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEN = 30;
    private static final float[] COLUMNAS = {25, 153, 134, 150, 82, 75, 48, 114};
    private static final String[] ENCABEZADOS = {"#", "Código", "Participante", "Correo", "Teléfono", "Estado", "Método", "Asistencia"};
    private static final int FILAS_POR_PAGINA = 24;

    private final EventoRepository eventos;
    private final InscripcionRepository inscripciones;
    private final AsistenciaRepository asistencias;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public byte[] generarReporteAsistencia(Integer idEvento) {
        var evento = eventos.findByIdConCategoria(idEvento)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado"));
        var inscritos = inscripciones.findByEventoIdEventoOrderByFechaInscripcionAsc(idEvento);
        var totales = inscritos.stream()
                .collect(Collectors.groupingBy(Inscripcion::getEstado, Collectors.counting()));
        var porInscripcion = asistencias.findByInscripcionEventoIdEventoOrderByFechaHoraAscIdAsistenciaAsc(idEvento)
                .stream().collect(Collectors.toMap(a -> a.getInscripcion().getIdInscripcion(), Function.identity()));
        int paginas = Math.max(1, (inscritos.size() + FILAS_POR_PAGINA - 1) / FILAS_POR_PAGINA);
        try (var documento = new PDDocument()) {
            var normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            var negrita = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            var papel = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            float ancho = papel.getWidth() - 2 * MARGEN;
            for (int p = 0; p < paginas; p++) {
                var pagina = new PDPage(papel);
                documento.addPage(pagina);
                try (var contenido = new PDPageContentStream(documento, pagina)) {
                    texto(contenido, negrita, 17, MARGEN, 551, ancho, "CONECTA - Reporte de asistencia");
                    texto(contenido, negrita, 13, MARGEN, 528, ancho, evento.getTitulo());
                    texto(contenido, normal, 10, MARGEN, 508, ancho,
                            evento.getFecha().format(FECHA) + "  |  "
                                    + evento.getHoraInicio().format(HORA) + " - " + evento.getHoraFin().format(HORA)
                                    + "  |  " + evento.getUbicacion());
                    texto(contenido, normal, 10, MARGEN, 490, ancho,
                            "Inscripciones: " + inscritos.size()
                                    + "  |  Asistieron: " + totales.getOrDefault(EstadoInscripcion.ASISTIO, 0L)
                                    + "  |  No asistieron: " + totales.getOrDefault(EstadoInscripcion.NO_ASISTIO, 0L)
                                    + "  |  Pendientes: " + totales.getOrDefault(EstadoInscripcion.CONFIRMADA, 0L)
                                    + "  |  Canceladas: " + totales.getOrDefault(EstadoInscripcion.CANCELADA, 0L));
                    if (inscritos.isEmpty()) {
                        texto(contenido, normal, 10, MARGEN, 454, ancho,
                                "El evento no tiene inscripciones registradas.");
                    } else {
                        contenido.setNonStrokingColor(0.91f, 0.94f, 0.97f);
                        contenido.addRect(MARGEN, 458, ancho, 18);
                        contenido.fill();
                        contenido.setNonStrokingColor(0f, 0f, 0f);
                        fila(contenido, negrita, 464, ENCABEZADOS);
                        int inicio = p * FILAS_POR_PAGINA;
                        int fin = Math.min(inicio + FILAS_POR_PAGINA, inscritos.size());
                        for (int i = inicio; i < fin; i++) {
                            Inscripcion inscripcion = inscritos.get(i);
                            Asistencia asistencia = porInscripcion.get(inscripcion.getIdInscripcion());
                            float y = 446 - (i - inicio) * 17;
                            fila(contenido, normal, y, valores(i + 1, inscripcion, asistencia));
                            contenido.setStrokingColor(0.82f, 0.86f, 0.90f);
                            contenido.setLineWidth(0.35f);
                            contenido.moveTo(MARGEN, y - 5);
                            contenido.lineTo(MARGEN + ancho, y - 5);
                            contenido.stroke();
                        }
                    }
                    texto(contenido, normal, 8, MARGEN, 24, ancho, "Página " + (p + 1) + " de " + paginas);
                }
            }
            var salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el reporte de asistencia", ex);
        }
    }

    private String[] valores(int numero, Inscripcion inscripcion, Asistencia asistencia) {
        var participante = inscripcion.getParticipante();
        String estado = switch (inscripcion.getEstado()) {
            case CONFIRMADA -> "Confirmada";
            case ASISTIO -> "Asistió";
            case NO_ASISTIO -> "No asistió";
            case CANCELADA -> "Cancelada";
        };
        return new String[]{String.valueOf(numero), inscripcion.getCodigo(), participante.nombreCompleto(),
                participante.getEmail(), participante.getTelefono() == null ? "-" : participante.getTelefono(), estado,
                asistencia == null ? "-" : asistencia.getMetodo().name(),
                asistencia == null ? "-" : asistencia.getFechaHora().format(FECHA_HORA)};
    }

    private void fila(PDPageContentStream contenido, PDType1Font fuente, float y, String[] valores) throws IOException {
        float x = MARGEN;
        for (int i = 0; i < COLUMNAS.length; i++) {
            texto(contenido, fuente, 8, x + 4, y, COLUMNAS[i] - 8, valores[i]);
            x += COLUMNAS[i];
        }
    }

    private void texto(PDPageContentStream contenido, PDType1Font fuente, float tamano, float x, float y,
                       float ancho, String valor) throws IOException {
        String limpio = new String(valor.replaceAll("[\\p{Cc}\\p{Zl}\\p{Zp}]", " ").getBytes(WIN_ANSI), WIN_ANSI);
        if (fuente.getStringWidth(limpio) * tamano / 1000 > ancho) {
            do {
                limpio = limpio.substring(0, limpio.length() - 1);
            } while (fuente.getStringWidth(limpio + "...") * tamano / 1000 > ancho);
            limpio += "...";
        }
        contenido.beginText();
        contenido.setFont(fuente, tamano);
        contenido.newLineAtOffset(x, y);
        contenido.showText(limpio);
        contenido.endText();
    }
}
