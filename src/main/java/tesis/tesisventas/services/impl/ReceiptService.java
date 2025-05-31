// ReceiptService.java - Versión con logs de debug
package tesis.tesisventas.services.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tesis.tesisventas.client.impl.ProductClientServiceImpl;
import tesis.tesisventas.client.impl.UserClientServiceImpl;
import tesis.tesisventas.dtos.ProductResponse;
import tesis.tesisventas.dtos.UserResponse;
import tesis.tesisventas.entities.FacturaEntity;
import tesis.tesisventas.entities.DetalleFacturaEntity;
import tesis.tesisventas.repositories.FacturaJpaRepository;
import tesis.tesisventas.models.Status;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ReceiptService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptService.class);

    @Autowired
    private FacturaJpaRepository facturaRepository;

    @Autowired
    private ProductClientServiceImpl productClient;

    @Autowired
    private UserClientServiceImpl userClient;

    /**
     * Genera comprobante PDF cuando el pago es aprobado
     */
    @Transactional(readOnly = true)
    public byte[] generateReceiptPDF(String orderCode) {
        logger.info("🔍 Iniciando generación de PDF para orden: {}", orderCode);

        try {
            // Buscar la factura por código
            logger.info("📋 Buscando factura con código: {}", orderCode);
            Optional<FacturaEntity> facturaOpt = facturaRepository.findByCodFacturaWithDetails(orderCode);

            if (facturaOpt.isEmpty()) {
                logger.error("❌ Factura no encontrada: {}", orderCode);
                throw new IllegalStateException("Factura no encontrada: " + orderCode);
            }

            FacturaEntity factura = facturaOpt.get();
            logger.info("✅ Factura encontrada - ID: {}, Estado: {}, Total: {}",
                    factura.getId(), factura.getStatus(), factura.getTotal());

            // Verificar que esté pagada
            if (!Status.PAGADA.toString().equals(factura.getStatus())) {
                logger.error("❌ Factura no está pagada. Estado actual: {}", factura.getStatus());
                throw new IllegalStateException("La factura no está pagada. Estado actual: " + factura.getStatus());
            }

            // Verificar detalles
            logger.info("📊 Verificando detalles de factura...");
            if (factura.getDetalles() != null) {
                logger.info("✅ Detalles encontrados: {} items", factura.getDetalles().size());
                factura.getDetalles().size(); // Forzar carga lazy
            } else {
                logger.warn("⚠️ No se encontraron detalles para la factura");
            }

            // Generar PDF
            logger.info("📄 Generando PDF...");
            byte[] pdfData = createPDF(factura);
            logger.info("✅ PDF generado exitosamente. Tamaño: {} bytes", pdfData.length);

            return pdfData;

        } catch (Exception e) {
            logger.error("❌ Error generando comprobante para {}: {}", orderCode, e.getMessage(), e);
            throw new RuntimeException("Error generando comprobante: " + e.getMessage(), e);
        }
    }

    private byte[] createPDF(FacturaEntity factura) throws DocumentException {
        logger.info("🏗️ Creando documento PDF...");

        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();
            logger.info("📖 Documento PDF abierto");

            // Header de la tienda
            logger.info("📝 Agregando header...");
            addStoreHeader(document);

            // Datos de la factura
            logger.info("📋 Agregando datos de factura...");
            addInvoiceDetails(document, factura);

            // Datos del cliente
            logger.info("👤 Agregando datos del cliente...");
            addCustomerDetails(document, factura);

            // Tabla de productos
            logger.info("🛍️ Agregando tabla de productos...");
            addProductsTable(document, factura);

            // Totales
            logger.info("💰 Agregando totales...");
            addTotals(document, factura);

            // Footer
            logger.info("📄 Agregando footer...");
            addFooter(document);

            document.close();
            logger.info("✅ PDF creado exitosamente. Tamaño final: {} bytes", baos.size());

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("❌ Error creando PDF: {}", e.getMessage(), e);
            if (document.isOpen()) {
                document.close();
            }
            throw e;
        }
    }

    private void addStoreHeader(Document document) throws DocumentException {
        try {
            // Título principal con manejo seguro de fuentes
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
            Paragraph title = new Paragraph("MI TIENDA ONLINE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            // Subtítulo
            Font subtitleFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL, BaseColor.GRAY);
            Paragraph subtitle = new Paragraph("COMPROBANTE DE VENTA", subtitleFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            document.add(subtitle);

            // Datos de la tienda
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);
            Paragraph storeInfo = new Paragraph(
                    "Dirección: Av. Córdoba 1234, Ciudad de Córdoba\n" +
                            "Teléfono: (351) 123-4567\n" +
                            "Email: ventas@mitienda.com\n" +
                            "CUIT: 20-12345678-9", normalFont);
            storeInfo.setAlignment(Element.ALIGN_CENTER);
            document.add(storeInfo);

            document.add(new Paragraph("\n"));

            // Línea separadora simple
            document.add(new Paragraph("_".repeat(80)));
            document.add(new Paragraph("\n"));

            logger.debug("✅ Header agregado correctamente");

        } catch (Exception e) {
            logger.error("❌ Error agregando header: {}", e.getMessage());
            throw e;
        }
    }

    private void addInvoiceDetails(Document document, FacturaEntity factura) throws DocumentException {
        try {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.getDefaultCell().setBorder(Rectangle.NO_BORDER);

            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 11);

            table.addCell(new Phrase("COMPROBANTE Nº:", boldFont));
            table.addCell(new Phrase("FECHA:", boldFont));

            table.addCell(new Phrase(factura.getCodFactura(), normalFont));
            String fechaFormateada = factura.getCreatedAt() != null ?
                    factura.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) :
                    "Fecha no disponible";
            table.addCell(new Phrase(fechaFormateada, normalFont));

            document.add(table);
            document.add(new Paragraph("\n"));

            logger.debug("✅ Detalles de factura agregados");

        } catch (Exception e) {
            logger.error("❌ Error agregando detalles de factura: {}", e.getMessage());
            throw e;
        }
    }

    private void addCustomerDetails(Document document, FacturaEntity factura) throws DocumentException {
        try {
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

            Paragraph customerTitle = new Paragraph("DATOS DEL CLIENTE:", boldFont);
            document.add(customerTitle);

            try {
                UserResponse user = userClient.getUserById(factura.getUserId());

                if (user != null) {
                    String customerInfo = String.format(
                            "Nombre: %s %s\nEmail: %s\nTeléfono: %s\nDocumento: %s",
                            user.getFirstName() != null ? user.getFirstName() : "N/A",
                            user.getLastName() != null ? user.getLastName() : "N/A",
                            user.getEmail() != null ? user.getEmail() : "N/A",
                            user.getTelephone() != null ? user.getTelephone().toString() : "N/A",
                            user.getNumberDoc() != null ? user.getNumberDoc().toString() : "N/A"
                    );
                    document.add(new Paragraph(customerInfo, normalFont));
                } else {
                    document.add(new Paragraph("Cliente: Información no disponible", normalFont));
                }
            } catch (Exception e) {
                logger.warn("⚠️ Error obteniendo datos del cliente: {}", e.getMessage());
                document.add(new Paragraph("Cliente: Error obteniendo información", normalFont));
            }

            document.add(new Paragraph("\n"));
            logger.debug("✅ Datos del cliente agregados");

        } catch (Exception e) {
            logger.error("❌ Error agregando datos del cliente: {}", e.getMessage());
            throw e;
        }
    }

    private void addProductsTable(Document document, FacturaEntity factura) throws DocumentException {
        try {
            PdfPTable table = new PdfPTable(4); // Reducido a 4 columnas
            table.setWidthPercentage(100);
            table.setWidths(new float[]{50, 20, 15, 15});

            Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
            Font dataFont = new Font(Font.FontFamily.HELVETICA, 9);

            // Headers con fondo gris
            BaseColor headerBg = BaseColor.DARK_GRAY;

            PdfPCell cell1 = new PdfPCell(new Phrase("PRODUCTO", headerFont));
            cell1.setBackgroundColor(headerBg);
            cell1.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell1);

            PdfPCell cell2 = new PdfPCell(new Phrase("CANTIDAD", headerFont));
            cell2.setBackgroundColor(headerBg);
            cell2.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell2);

            PdfPCell cell3 = new PdfPCell(new Phrase("PRECIO UNIT.", headerFont));
            cell3.setBackgroundColor(headerBg);
            cell3.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell3);

            PdfPCell cell4 = new PdfPCell(new Phrase("SUBTOTAL", headerFont));
            cell4.setBackgroundColor(headerBg);
            cell4.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell4);

            // Verificar que los detalles existan
            if (factura.getDetalles() == null || factura.getDetalles().isEmpty()) {
                logger.warn("⚠️ No hay detalles en la factura");
                PdfPCell noDataCell = new PdfPCell(new Phrase("No hay productos en esta factura", dataFont));
                noDataCell.setColspan(4);
                noDataCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(noDataCell);
            } else {
                logger.info("📦 Procesando {} detalles", factura.getDetalles().size());

                for (DetalleFacturaEntity detalle : factura.getDetalles()) {
                    try {
                        String nombreProducto = "Producto ID: " + detalle.getIdProducto();

                        // Intentar obtener nombre del producto
                        try {
                            ProductResponse producto = productClient.getProductById(detalle.getIdProducto());
                            if (producto != null && producto.getName() != null) {
                                nombreProducto = producto.getName();
                            }
                        } catch (Exception e) {
                            logger.warn("⚠️ Error obteniendo producto {}: {}", detalle.getIdProducto(), e.getMessage());
                        }

                        table.addCell(new PdfPCell(new Phrase(nombreProducto, dataFont)));

                        PdfPCell qtyCell = new PdfPCell(new Phrase(detalle.getCantidad().toString(), dataFont));
                        qtyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        table.addCell(qtyCell);

                        PdfPCell priceCell = new PdfPCell(new Phrase("$" + detalle.getPrecioUnitario(), dataFont));
                        priceCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(priceCell);

                        PdfPCell subtotalCell = new PdfPCell(new Phrase("$" + detalle.getSubtotal(), dataFont));
                        subtotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        table.addCell(subtotalCell);

                    } catch (Exception e) {
                        logger.error("❌ Error procesando detalle: {}", e.getMessage());
                        // Agregar fila con error
                        table.addCell(new PdfPCell(new Phrase("Error en producto", dataFont)));
                        table.addCell(new PdfPCell(new Phrase("0", dataFont)));
                        table.addCell(new PdfPCell(new Phrase("$0", dataFont)));
                        table.addCell(new PdfPCell(new Phrase("$0", dataFont)));
                    }
                }
            }

            document.add(table);
            document.add(new Paragraph("\n"));
            logger.debug("✅ Tabla de productos agregada");

        } catch (Exception e) {
            logger.error("❌ Error agregando tabla de productos: {}", e.getMessage());
            throw e;
        }
    }

    private void addTotals(Document document, FacturaEntity factura) throws DocumentException {
        try {
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(50);
            table.setHorizontalAlignment(Element.ALIGN_RIGHT);

            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font totalFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, BaseColor.DARK_GRAY);

            PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL A PAGAR:", boldFont));
            labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            labelCell.setBorder(Rectangle.NO_BORDER);

            PdfPCell totalCell = new PdfPCell(new Phrase("$" + factura.getTotal(), totalFont));
            totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalCell.setBorder(Rectangle.NO_BORDER);

            table.addCell(labelCell);
            table.addCell(totalCell);

            document.add(table);
            logger.debug("✅ Totales agregados");

        } catch (Exception e) {
            logger.error("❌ Error agregando totales: {}", e.getMessage());
            throw e;
        }
    }

    private void addFooter(Document document) throws DocumentException {
        try {
            document.add(new Paragraph("\n\n"));

            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.GRAY);

            String footerText = String.format(
                    "¡Gracias por su compra!\n\n" +
                            "Este comprobante es válido como factura de venta.\n" +
                            "Para cualquier consulta, comuníquese con nosotros.\n\n" +
                            "Generado el: %s",
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            );

            Paragraph footer = new Paragraph(footerText, footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            logger.debug("✅ Footer agregado");

        } catch (Exception e) {
            logger.error("❌ Error agregando footer: {}", e.getMessage());
            throw e;
        }
    }
}