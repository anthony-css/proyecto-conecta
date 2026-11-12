package com.cibertec.conecta.controller;

import com.cibertec.conecta.service.ReporteService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping(value = "/eventos/{id}/asistencia.pdf",
                produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> reporteAsistencia(@PathVariable @Positive Integer id) {

        byte[] pdf = reporteService.generarReporteAsistencia(id);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("asistencia-evento-" + id + ".pdf")
                .build();

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(pdf);
    }
}
