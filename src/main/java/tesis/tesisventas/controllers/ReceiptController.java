// src/main/java/tesis/tesisventas/controllers/ReceiptController.java
package tesis.tesisventas.controllers;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tesis.tesisventas.services.impl.ReceiptService;


import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/receipts")
@CrossOrigin(origins = "http://localhost:4200")
public class ReceiptController {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptController.class);

    @Autowired
    private ReceiptService receiptService;

    /**
     * Genera y descarga el comprobante en PDF
     */
    @GetMapping("/download/{orderCode}")
    public ResponseEntity<?> downloadReceipt(@PathVariable String orderCode) {
        try {
            logger.info("🔽 Solicitando descarga de comprobante para orden: {}", orderCode);

            byte[] pdfData = receiptService.generateReceiptPDF(orderCode);

            if (pdfData == null || pdfData.length == 0) {
                logger.error("❌ PDF generado está vacío para orden: {}", orderCode);
                return ResponseEntity.badRequest().body(Map.of("error", "PDF generado está vacío"));
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "comprobante-" + orderCode + ".pdf");
            headers.setContentLength(pdfData.length);

            // Headers adicionales para evitar caché
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);

            logger.info("✅ Comprobante generado exitosamente para orden: {} - Tamaño: {} bytes", orderCode, pdfData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);

        } catch (IllegalStateException e) {
            logger.error("❌ Error de estado para orden {}: {}", orderCode, e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            errorResponse.put("orderCode", orderCode);

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ Error generando comprobante para orden {}: {}", orderCode, e.getMessage(), e);

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error interno generando comprobante");
            errorResponse.put("orderCode", orderCode);
            errorResponse.put("details", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Vista previa del comprobante (para mostrar en pantalla)
     */
    @GetMapping("/preview/{orderCode}")
    public ResponseEntity<?> previewReceipt(@PathVariable String orderCode) {
        try {
            logger.info("Generando vista previa para orden: {}", orderCode);

            byte[] pdfData = receiptService.generateReceiptPDF(orderCode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "comprobante-" + orderCode + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);

        } catch (Exception e) {
            logger.error("Error generando vista previa para orden {}: {}", orderCode, e.getMessage());

            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generando vista previa");
            errorResponse.put("orderCode", orderCode);

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Verificar si un comprobante está disponible
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, Object>> checkReceiptStatus(@PathVariable String orderCode) {
        try {
            // Intentar generar el comprobante para verificar el estado
            receiptService.generateReceiptPDF(orderCode);

            Map<String, Object> response = new HashMap<>();
            response.put("orderCode", orderCode);
            response.put("available", true);
            response.put("message", "Comprobante disponible para descarga");

            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("orderCode", orderCode);
            response.put("available", false);
            response.put("message", e.getMessage());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error verificando estado del comprobante {}: {}", orderCode, e.getMessage());

            Map<String, Object> response = new HashMap<>();
            response.put("orderCode", orderCode);
            response.put("available", false);
            response.put("message", "Error verificando comprobante");

            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint de prueba simple
     */
    @GetMapping("/test-pdf")
    public ResponseEntity<?> testPdf() {
        try {
            logger.info("🧪 Generando PDF de prueba...");

            // Crear PDF simple de prueba
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);

            document.open();
            document.add(new Paragraph("Prueba de PDF"));
            document.add(new Paragraph("Si puedes ver esto, iText funciona correctamente."));
            document.add(new Paragraph("Fecha: " + java.time.LocalDateTime.now()));
            document.close();

            byte[] pdfData = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "test.pdf");

            logger.info("✅ PDF de prueba generado: {} bytes", pdfData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfData);

        } catch (Exception e) {
            logger.error("❌ Error en PDF de prueba: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error generando PDF de prueba: " + e.getMessage()));
        }
    }
}