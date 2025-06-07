INSERT INTO forma_pago (id, name, active)
VALUES ('11111111-1111-1111-1111-111111111111', 'Efectivo', true);

INSERT INTO forma_pago (id, name, active)
VALUES ('5ef1b016-64ff-4f2f-973c-cffd9a39c30e', 'Trasnfer', true);

INSERT INTO forma_pago (id, name, active)
VALUES ('22222222-2222-2222-2222-222222222222', 'mp', true);



INSERT INTO factura (
    id,
    cod_factura,
    user_id,
    total,
    id_forma_pago,
    status,
    created_at,
    payment_url
) VALUES (
             '12345678-1234-1234-1234-123456789012',        -- ID único de factura
             'COD-000001',                                   -- Código generado
             '550e8400-e29b-41d4-a716-446655440000',        -- UserID del POST
             150000.00,                                      -- Total (10 * 15000 = precio ficticio)
             '5ef1b016-64ff-4f2f-973c-cffd9a39c30e',        -- FormaPago del POST (Transfer)
             'PAGADA',                                       -- Estado completado
             '2025-01-31 10:30:00',                         -- Fecha actual
             NULL                                            -- Sin URL (ya pagado)
         );

-- ============================================
-- 2. DETALLE DE LA FACTURA
-- ============================================

INSERT INTO detalle_factura (
    id_detalle,
    id_factura,
    id_producto,
    cantidad,
    precio_unitario,
    subtotal
) VALUES (
             '11111111-2222-3333-4444-555555555555',        -- ID único del detalle
             '12345678-1234-1234-1234-123456789012',        -- ID de la factura creada arriba
             '11111111-1111-1111-1111-111111111111',        -- ProductoID del POST (EXACTO)
             10,                                             -- Cantidad del POST (EXACTA)
             15000.00,                                       -- Precio unitario ficticio
             150000.00                                       -- Subtotal (10 * 15000)
         );