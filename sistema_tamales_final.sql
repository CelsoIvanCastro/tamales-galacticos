-- MySQL Workbench Forward Engineering (Limpio y adaptado para MariaDB)

SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- -----------------------------------------------------
-- Schema sistema_tamales
-- -----------------------------------------------------
DROP SCHEMA IF EXISTS `sistema_tamales`;
CREATE SCHEMA `sistema_tamales` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `sistema_tamales`;

-- -----------------------------------------------------
-- Table `sistema_tamales`.`categoria`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`categoria` (
  `id_categoria` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id_categoria`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`usuario`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`usuario` (
  `id_usuario` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `apellido` VARCHAR(100) NULL DEFAULT NULL,
  `correo` VARCHAR(150) NOT NULL,
  `contrasena` VARCHAR(255) NOT NULL,
  `telefono` VARCHAR(20) NULL DEFAULT NULL,
  `tipo_usuario` ENUM('PERSONA', 'EMPRESA') NOT NULL DEFAULT 'PERSONA',
  `rol` ENUM('CLIENTE', 'EMPLEADO', 'OTRO') NOT NULL DEFAULT 'CLIENTE',
  `activo` TINYINT(1) NOT NULL DEFAULT '1',
  `fecha_registro` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_usuario`),
  UNIQUE INDEX `correo` (`correo` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`direccionusuario`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`direccionusuario` (
  `id_direccion` INT NOT NULL AUTO_INCREMENT,
  `id_usuario` INT NOT NULL,
  `calle` VARCHAR(150) NOT NULL,
  `numero` VARCHAR(20) NOT NULL,
  `colonia` VARCHAR(100) NOT NULL,
  `ciudad` VARCHAR(100) NOT NULL,
  `estado` VARCHAR(100) NOT NULL,
  `codigo_postal` VARCHAR(10) NOT NULL,
  `referencias` VARCHAR(255) NULL DEFAULT NULL,
  PRIMARY KEY (`id_direccion`),
  INDEX `id_usuario` (`id_usuario` ASC),
  CONSTRAINT `direccionusuario_ibfk_1`
    FOREIGN KEY (`id_usuario`)
    REFERENCES `sistema_tamales`.`usuario` (`id_usuario`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`pedido`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`pedido` (
  `id_pedido` INT NOT NULL AUTO_INCREMENT,
  `id_usuario` INT NOT NULL,
  `id_direccion` INT NOT NULL,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `estado` ENUM('PENDIENTE', 'ENTREGANDO', 'PREPARANDO', 'RECIBIDO', 'TERMINADO') NOT NULL DEFAULT 'PENDIENTE',
  `descuento` DECIMAL(10,2) NULL DEFAULT '0.00',
  `total` DECIMAL(10,2) NOT NULL,
  `metodo_pago` ENUM('TRANSFERENCIA', 'EFECTIVO', 'TARJETA') NOT NULL DEFAULT 'EFECTIVO',
  `estado_pago` ENUM('PAGADO', 'PENDIENTE') NOT NULL DEFAULT 'PENDIENTE',
  PRIMARY KEY (`id_pedido`),
  INDEX `id_usuario` (`id_usuario` ASC),
  INDEX `fk_pedido_direccion` (`id_direccion` ASC),
  CONSTRAINT `fk_pedido_direccion`
    FOREIGN KEY (`id_direccion`)
    REFERENCES `sistema_tamales`.`direccionusuario` (`id_direccion`),
  CONSTRAINT `pedido_ibfk_1`
    FOREIGN KEY (`id_usuario`)
    REFERENCES `sistema_tamales`.`usuario` (`id_usuario`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`producto`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`producto` (
  `id_producto` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `descripcion` VARCHAR(255) NULL DEFAULT NULL,
  `precio` DECIMAL(10,2) NOT NULL,
  `precio_mayoreo` DECIMAL(10,2) NULL DEFAULT NULL,
  `cantidad_minima_mayoreo` INT NULL DEFAULT '100',
  `existencia` INT NOT NULL DEFAULT '0',
  `id_categoria` INT NOT NULL,
  `activo` TINYINT(1) NOT NULL DEFAULT '1',
  PRIMARY KEY (`id_producto`),
  INDEX `id_categoria` (`id_categoria` ASC),
  CONSTRAINT `producto_ibfk_1`
    FOREIGN KEY (`id_categoria`)
    REFERENCES `sistema_tamales`.`categoria` (`id_categoria`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`detallepedido`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`detallepedido` (
  `id_detalle_pedido` INT NOT NULL AUTO_INCREMENT,
  `id_pedido` INT NOT NULL,
  `id_producto` INT NOT NULL,
  `cantidad` INT NOT NULL,
  `precio_unitario` DECIMAL(10,2) NOT NULL,
  `subtotal` DECIMAL(10,2) NOT NULL,
  PRIMARY KEY (`id_detalle_pedido`),
  INDEX `id_pedido` (`id_pedido` ASC),
  INDEX `id_producto` (`id_producto` ASC),
  CONSTRAINT `detallepedido_ibfk_1`
    FOREIGN KEY (`id_pedido`)
    REFERENCES `sistema_tamales`.`pedido` (`id_pedido`),
  CONSTRAINT `detallepedido_ibfk_2`
    FOREIGN KEY (`id_producto`)
    REFERENCES `sistema_tamales`.`producto` (`id_producto`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`ingrediente`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`ingrediente` (
  `id_ingrediente` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL,
  `unidad_medida` ENUM('kg', 'g', 'L', 'ml', 'pza') NOT NULL,
  `existencia` DECIMAL(10,3) NOT NULL DEFAULT '0.000',
  `stock_minimo` DECIMAL(10,3) NULL DEFAULT '0.000',
  `costo_unidad` DECIMAL(10,2) NULL DEFAULT '0.00',
  PRIMARY KEY (`id_ingrediente`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`detallereceta`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`detallereceta` (
  `id_detalle_receta` INT NOT NULL AUTO_INCREMENT,
  `id_producto` INT NOT NULL,
  `id_ingrediente` INT NOT NULL,
  `cantidad` DECIMAL(10,3) NOT NULL,
  PRIMARY KEY (`id_detalle_receta`),
  INDEX `id_producto` (`id_producto` ASC),
  INDEX `id_ingrediente` (`id_ingrediente` ASC),
  CONSTRAINT `detallereceta_ibfk_1`
    FOREIGN KEY (`id_producto`)
    REFERENCES `sistema_tamales`.`producto` (`id_producto`),
  CONSTRAINT `detallereceta_ibfk_2`
    FOREIGN KEY (`id_ingrediente`)
    REFERENCES `sistema_tamales`.`ingrediente` (`id_ingrediente`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`puesto`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`puesto` (
  `id_puesto` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL,
  PRIMARY KEY (`id_puesto`),
  UNIQUE INDEX `nombre` (`nombre` ASC))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`empleado`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`empleado` (
  `id_empleado` INT NOT NULL AUTO_INCREMENT,
  `id_usuario` INT NOT NULL,
  `id_puesto` INT NOT NULL,
  PRIMARY KEY (`id_empleado`),
  UNIQUE INDEX `id_usuario` (`id_usuario` ASC),
  INDEX `idx_empleado_puesto` (`id_puesto` ASC),
  CONSTRAINT `fk_empleado_puesto`
    FOREIGN KEY (`id_puesto`)
    REFERENCES `sistema_tamales`.`puesto` (`id_puesto`),
  CONSTRAINT `fk_empleado_usuario`
    FOREIGN KEY (`id_usuario`)
    REFERENCES `sistema_tamales`.`usuario` (`id_usuario`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`empresa`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`empresa` (
  `id_empresa` INT NOT NULL AUTO_INCREMENT,
  `id_usuario` INT NOT NULL,
  `razon_social` VARCHAR(150) NULL,
  `rfc` VARCHAR(20) NOT NULL,
  PRIMARY KEY (`id_empresa`),
  INDEX `id_usuario` (`id_usuario` ASC),
  CONSTRAINT `empresa_ibfk_1`
    FOREIGN KEY (`id_usuario`)
    REFERENCES `sistema_tamales`.`usuario` (`id_usuario`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`movimientomateriaprima`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`movimientomateriaprima` (
  `id_movimiento` INT NOT NULL AUTO_INCREMENT,
  `id_ingrediente` INT NOT NULL,
  `id_empleado` INT NOT NULL,
  `tipo_movimiento` ENUM('ENTRADA', 'SALIDA', 'AJUSTE', 'PRODUCCION', 'MERMA', 'DEVOLUCION') NOT NULL,
  `cantidad` DECIMAL(10,3) NOT NULL,
  `tipo_motivo` ENUM('COMPRA', 'PRODUCCION', 'PEDIDO', 'CANCELACION', 'INVENTARIO', 'MERMA', 'DEVOLUCION', 'OTRO') NOT NULL DEFAULT 'OTRO',
  `motivo` VARCHAR(100) NULL DEFAULT NULL,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_movimiento`),
  INDEX `id_ingrediente` (`id_ingrediente` ASC),
  INDEX `id_empleado` (`id_empleado` ASC),
  CONSTRAINT `movimientomateriaprima_ibfk_1`
    FOREIGN KEY (`id_ingrediente`)
    REFERENCES `sistema_tamales`.`ingrediente` (`id_ingrediente`),
  CONSTRAINT `movimientomateriaprima_ibfk_2`
    FOREIGN KEY (`id_empleado`)
    REFERENCES `sistema_tamales`.`empleado` (`id_empleado`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`movimientoproducto`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`movimientoproducto` (
  `id_movimiento` INT NOT NULL AUTO_INCREMENT,
  `id_producto` INT NOT NULL,
  `id_empleado` INT NOT NULL,
  `tipo_movimiento` ENUM('PRODUCCION', 'VENTA', 'AJUSTE', 'MERMA', 'DEVOLUCION', 'ENTRADA', 'SALIDA') NOT NULL,
  `cantidad` INT NOT NULL,
  `tipo_motivo` ENUM('COMPRA', 'PRODUCCION', 'PEDIDO', 'CANCELACION', 'INVENTARIO', 'MERMA', 'DEVOLUCION', 'OTRO') NOT NULL DEFAULT 'OTRO',
  `motivo` VARCHAR(100) NULL DEFAULT NULL,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id_movimiento`),
  INDEX `id_producto` (`id_producto` ASC),
  INDEX `id_empleado` (`id_empleado` ASC),
  CONSTRAINT `movimientoproducto_ibfk_1`
    FOREIGN KEY (`id_producto`)
    REFERENCES `sistema_tamales`.`producto` (`id_producto`),
  CONSTRAINT `movimientoproducto_ibfk_2`
    FOREIGN KEY (`id_empleado`)
    REFERENCES `sistema_tamales`.`empleado` (`id_empleado`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


-- -----------------------------------------------------
-- Table `sistema_tamales`.`produccion`
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `sistema_tamales`.`produccion` (
  `id_produccion` INT NOT NULL AUTO_INCREMENT,
  `id_producto` INT NOT NULL,
  `id_empleado` INT NOT NULL,
  `cantidad` INT NOT NULL,
  `fecha` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `costo_total` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  `costo_unitario` DECIMAL(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (`id_produccion`),
  INDEX `id_producto` (`id_producto` ASC),
  INDEX `id_empleado` (`id_empleado` ASC),
  CONSTRAINT `produccion_ibfk_1`
    FOREIGN KEY (`id_producto`)
    REFERENCES `sistema_tamales`.`producto` (`id_producto`),
  CONSTRAINT `produccion_ibfk_2`
    FOREIGN KEY (`id_empleado`)
    REFERENCES `sistema_tamales`.`empleado` (`id_empleado`))
ENGINE = InnoDB
DEFAULT CHARACTER SET = utf8mb4
COLLATE = utf8mb4_unicode_ci;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;

-- 1. Categorías 
INSERT INTO categoria (nombre) VALUES 
('Tamales Salados'), 
('Tamales Dulces'), 
('Tamales Oaxaqueños'), 
('Bebidas Calientes'), 
('Combos Familiares');

-- 2. Puestos 
INSERT INTO puesto (nombre) VALUES 
('Gerente General'), 
('Cocinero Principal'), 
('Auxiliar de Cocina'), 
('Cajero'), 
('Repartidor');

-- 3. Ingredientes (Usando unidades ENUM)
INSERT INTO ingrediente (nombre, unidad_medida, existencia, stock_minimo, costo_unidad) VALUES 
('Masa de maíz', 'kg', 50.000, 10.000, 18.50), 
('Carne de cerdo', 'kg', 20.000, 5.000, 95.00),
('Manteca de cerdo', 'kg', 15.000, 3.000, 45.00), 
('Hojas de maíz', 'pza', 30.000, 5.000, 25.00),
('Salsa verde', 'L', 10.000, 2.000, 35.00), 
('Azúcar', 'kg', 25.000, 5.000, 28.00);

-- 4. Usuarios (Con fechas de registro variadas)
INSERT INTO usuario (nombre, apellido, correo, contrasena, telefono, tipo_usuario, rol, fecha_registro) VALUES 
('Carlos', 'Valdez', 'carlos.valdez@email.com', 'hash123', '6671112233', 'PERSONA', 'CLIENTE', '2026-04-20 09:15:00'),
('Ana', 'López', 'ana.lopez@email.com', 'hash123', '6672223344', 'PERSONA', 'CLIENTE', '2026-04-22 11:30:00'),            
('Luis', 'García', 'luis.garcia@email.com', 'hash123', '6673334455', 'PERSONA', 'CLIENTE', '2026-04-25 16:00:00'),          
('Banquetes', 'Sinaloa', 'contacto@banquetessinaloa.com', 'hash123', '6674445566', 'EMPRESA', 'CLIENTE', '2026-04-28 10:00:00'), 
('Eventos', 'Premium', 'ventas@eventospremium.com', 'hash123', '6675556677', 'EMPRESA', 'CLIENTE', '2026-04-30 14:20:00'),    
('Desayunos', 'Empresariales', 'info@desayunosempresariales.com', 'hash123', '6676667788', 'EMPRESA', 'CLIENTE', '2026-05-02 08:45:00'), 
('Comedores', 'Industriales', 'compras@comedoresind.com', 'hash123', '6677778899', 'EMPRESA', 'CLIENTE', '2026-05-04 12:10:00'), 
('Fiestas', 'Mágicas', 'hola@fiestasmagicas.com', 'hash123', '6678889900', 'EMPRESA', 'CLIENTE', '2026-05-06 17:00:00'),      
('Roberto', 'Gómez', 'roberto.gomez@email.com', 'hash123', '6679990011', 'PERSONA', 'EMPLEADO', '2026-04-15 08:00:00'),      
('María', 'Pérez', 'maria.perez@email.com', 'hash123', '6670001122', 'PERSONA', 'EMPLEADO', '2026-04-15 08:05:00'),          
('Jorge', 'Soto', 'jorge.soto@email.com', 'hash123', '6671231234', 'PERSONA', 'EMPLEADO', '2026-04-15 08:10:00'),         
('Lucía', 'Beltrán', 'lucia.beltran@email.com', 'hash123', '6673214321', 'PERSONA', 'EMPLEADO', '2026-04-15 08:15:00'),    
('Pedro', 'Ríos', 'pedro.rios@email.com', 'hash123', '6674566543', 'PERSONA', 'EMPLEADO', '2026-04-15 08:20:00');        

-- 5. Direcciones de Usuarios 
INSERT INTO direccionusuario (id_usuario, calle, numero, colonia, ciudad, estado, codigo_postal, referencias) VALUES 
(1, 'Blvd. Emiliano Zapata', '1200', 'Lomas Culiacán', 'Culiacán', 'Sinaloa', '80200', 'Casa blanca con reja negra'),
(2, 'Av. Álvaro Obregón', '455', 'Centro', 'Culiacán', 'Sinaloa', '80000', 'Cerca de la catedral'),
(3, 'Calle Universitarios', '890', 'Tierra Blanca', 'Culiacán', 'Sinaloa', '80030', 'Frente al parque'),
(4, 'Blvd. Pedro Infante', '2300', 'Desarrollo Urbano Tres Ríos', 'Culiacán', 'Sinaloa', '80020', 'Edificio corporativo'),
(5, 'Calle Agricultores', '56', 'Las Quintas', 'Culiacán', 'Sinaloa', '80060', 'Local comercial en esquina');

-- 6. Empresas
INSERT INTO empresa (id_usuario, razon_social, rfc) VALUES 
(4, 'Banquetes Sinaloa S.A. de C.V.', 'BANSIN990101XX1'),
(5, 'Eventos Premium S. de R.L.', 'EVEPRE050505YY2'),
(6, 'Desayunos Empresariales del Noroeste', 'DESEMP101010ZZ3'),
(7, 'Comedores Industriales Culiacán', 'COMIND121212WW4'),
(8, 'Fiestas Mágicas S.A.', 'FIEMAG080808VV5');

-- 7. Empleados 
INSERT INTO empleado (id_usuario, id_puesto) VALUES 
(9, 1),   
(10, 2),  
(11, 3),  
(12, 4),  
(13, 5);  

-- 8. Productos 
INSERT INTO producto (nombre, descripcion, precio, precio_mayoreo, id_categoria, existencia) VALUES 
('Tamal de Puerco', 'Tradicional tamal sinaloense con carne de cerdo y salsa roja', 25.00, 22.00, 1, 50),
('Tamal de Elote', 'Tamal dulce de elote tierno con crema', 20.00, 18.00, 2, 40),
('Tamal Oaxaqueño de Pollo', 'Envuelto en hoja de plátano con mole', 35.00, 30.00, 3, 20),
('Champurrado', 'Bebida caliente a base de masa y chocolate (1 Litro)', 45.00, 40.00, 4, 15),
('Combo Familiar', '10 tamales surtidos y 2 litros de champurrado', 280.00, 250.00, 5, 5);

-- 9. Detalle de Recetas 
INSERT INTO detallereceta (id_producto, id_ingrediente, cantidad) VALUES 
(1, 1, 0.150), 
(1, 2, 0.050), 
(1, 3, 0.020), 
(1, 4, 1.000), 
(1, 5, 0.030), 
(4, 1, 0.200); 

-- 10. Pedidos 
INSERT INTO pedido (id_usuario, id_direccion, fecha, estado, total, metodo_pago, estado_pago) VALUES 
(1, 1, '2026-05-10 14:30:00', 'TERMINADO', 125.00, 'EFECTIVO', 'PAGADO'),
(4, 4, '2026-05-12 10:15:00', 'ENTREGANDO', 1100.00, 'TRANSFERENCIA', 'PAGADO'),
(2, 2, '2026-05-15 18:45:00', 'PREPARANDO', 280.00, 'TARJETA', 'PAGADO'),
(3, 3, '2026-05-20 09:00:00', 'PENDIENTE', 50.00, 'EFECTIVO', 'PENDIENTE'),
(5, 5, '2026-05-22 13:20:00', 'TERMINADO', 350.00, 'TRANSFERENCIA', 'PAGADO');

INSERT INTO pedido
(id_usuario, id_direccion, fecha, estado, total, metodo_pago, estado_pago)
VALUES
(3, 3, '2026-05-25 12:30:00', 'TERMINADO', 100.00, 'EFECTIVO', 'PAGADO'),
(1, 1, '2026-05-28 15:00:00', 'TERMINADO', 280.00, 'TARJETA', 'PAGADO'),
(2, 2, '2026-06-02 11:45:00', 'TERMINADO', 200.00, 'TRANSFERENCIA', 'PAGADO');


-- 11. Detalle de Pedidos 
INSERT INTO detallepedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal) VALUES 
(1, 1, 5, 25.00, 125.00), 
(2, 1, 50, 22.00, 1100.00), 
(3, 5, 1, 280.00, 280.00), 
(4, 1, 2, 25.00, 50.00),  
(5, 3, 10, 35.00, 350.00); 

INSERT INTO detallepedido
(id_pedido, id_producto, cantidad, precio_unitario, subtotal)
VALUES
(6, 1, 4, 25.00, 100.00),
(7, 5, 1, 280.00, 280.00),
(8, 2, 10, 20.00, 200.00);

-- 12. Producción 
INSERT INTO produccion (id_producto, id_empleado, fecha, cantidad, costo_total, costo_unitario) VALUES 
(1, 2, '2026-05-09 08:00:00', 100, 1200.00, 12.00),
(2, 2, '2026-05-11 08:30:00', 80, 800.00, 10.00),
(3, 3, '2026-05-14 09:00:00', 50, 900.00, 18.00),
(4, 3, '2026-05-18 07:30:00', 20, 400.00, 20.00),
(1, 2, '2026-05-21 08:00:00', 50, 600.00, 12.00);

-- 13. Movimientos de Materia Prima 
INSERT INTO movimientomateriaprima (id_ingrediente, id_empleado, fecha, tipo_movimiento, cantidad, tipo_motivo) VALUES 
(1, 1, '2026-05-08 10:00:00', 'ENTRADA', 100.000, 'COMPRA'),
(2, 1, '2026-05-08 10:05:00', 'ENTRADA', 50.000, 'COMPRA'),
(1, 2, '2026-05-09 08:10:00', 'SALIDA', 15.000, 'PRODUCCION'),
(2, 2, '2026-05-11 08:40:00', 'SALIDA', 5.000, 'PRODUCCION'),
(4, 3, '2026-05-15 12:00:00', 'MERMA', 2.000, 'MERMA');

-- 14. Movimientos de Productos 
INSERT INTO movimientoproducto (id_producto, id_empleado, fecha, tipo_movimiento, cantidad, tipo_motivo) VALUES 
(1, 2, '2026-05-09 11:00:00', 'PRODUCCION', 100, 'PRODUCCION'),
(2, 2, '2026-05-11 11:30:00', 'PRODUCCION', 80, 'PRODUCCION'),
(1, 4, '2026-05-10 14:35:00', 'VENTA', 5, 'PEDIDO'),
(1, 4, '2026-05-12 10:20:00', 'VENTA', 50, 'PEDIDO'),
(3, 3, '2026-05-19 15:00:00', 'MERMA', 2, 'MERMA');

/*SHOW TABLES;

SELECT * FROM categoria;
SELECT * FROM puesto;
SELECT * FROM ingrediente;
SELECT * FROM usuario;
SELECT * FROM direccionusuario;
SELECT * FROM empresa;
SELECT * FROM empleado;
SELECT * FROM producto;
SELECT * FROM detallereceta;
SELECT * FROM pedido;
SELECT * FROM detallepedido;
SELECT * FROM produccion;
SELECT * FROM movimientomateriaprima;
SELECT * FROM movimientoproducto;*/

-- SUBCONSULTAS
-- Productos más vendidos
SELECT p.nombre AS producto, SUM(dp.cantidad) AS total_unidades_vendidas
FROM producto p
JOIN detallepedido dp ON p.id_producto = dp.id_producto
GROUP BY p.id_producto, p.nombre
ORDER BY total_unidades_vendidas DESC;

-- Clientes con más compras
SELECT u.nombre, u.apellido, COUNT(p.id_pedido) AS total_pedidos, SUM(p.total) AS dinero_gastado
FROM usuario u
JOIN pedido p ON u.id_usuario = p.id_usuario
WHERE u.rol = 'CLIENTE'
GROUP BY u.id_usuario, u.nombre, u.apellido
ORDER BY total_pedidos DESC;

-- Empleados con más producciones
SELECT u.nombre, u.apellido, COUNT(pr.id_produccion) AS lotes_producidos, SUM(pr.cantidad) AS total_piezas_producidas
FROM empleado e
JOIN usuario u ON e.id_usuario = u.id_usuario
JOIN produccion pr ON e.id_empleado = pr.id_empleado
GROUP BY e.id_empleado, u.nombre, u.apellido
ORDER BY total_piezas_producidas DESC;

-- Primer cliente en hacer un pedido
SELECT u.nombre, u.apellido, p.fecha AS fecha_pedido, p.total
FROM pedido p
JOIN usuario u ON p.id_usuario = u.id_usuario
ORDER BY p.fecha ASC
LIMIT 1;

-- Mostrar el producto más caro comprado por cada cliente
SELECT u.nombre, u.apellido, pr.nombre AS producto_mas_caro, pr.precio
FROM usuario u
JOIN pedido p ON u.id_usuario = p.id_usuario
JOIN detallepedido dp ON p.id_pedido = dp.id_pedido
JOIN producto pr ON dp.id_producto = pr.id_producto
WHERE pr.precio = (
    SELECT MAX(pr2.precio)
    FROM pedido p2
    JOIN detallepedido dp2 ON p2.id_pedido = dp2.id_pedido
    JOIN producto pr2 ON dp2.id_producto = pr2.id_producto
    WHERE p2.id_usuario = u.id_usuario
)
GROUP BY u.id_usuario, u.nombre, u.apellido, pr.nombre, pr.precio;

-- Total vendido de cada producto
SELECT p.nombre AS producto, SUM(dp.cantidad) AS unidades_vendidas, SUM(dp.subtotal) AS ingreso_total
FROM producto p
JOIN detallepedido dp ON p.id_producto = dp.id_producto
GROUP BY p.id_producto, p.nombre
ORDER BY ingreso_total DESC;

-- PROYECTO FINAL MOMENT :P 

/*PARTE 1
	Operaciones avanzadas con SQL: JOIN, UNION, ORDER BY, GROUP BY y manipulación de FECHAS y TIEMPO*/
    
-- Decidí hacer consultas de interés que podrían ayudar a la dark kitchen a tomar decisiones estrategicas basadas en datos duros (ciencia de datos) :D 

/*1.1. PRODUCTOS QUE GENERAN MÁS GANANCIA A LOS QUE MENOS: ORDER BY, JOIN Y GROUP BY*/
SELECT p.nombre AS Producto, p.precio AS 'Precio de venta', ROUND(AVG(prd.costo_unitario),2) AS 'Costo promedio de producción', p.precio - ROUND(AVG(prd.costo_unitario),2) AS Ganancia
FROM producto AS p
INNER JOIN produccion AS prd ON p.id_producto = prd.id_producto
GROUP BY p.id_producto, p.nombre
ORDER BY Ganancia DESC;

/*1.2. GANANCIA REAL DE VENTAS: JOIN, ORDER BY, GROUP BY y SUBCONSULTA*/ 
SELECT
	pd.id_pedido AS 'Número de venta',
    pd.fecha AS Fecha,
    SUM(dp.cantidad) AS 'Total unidades vendidas',
    pd.total AS 'Total de la venta',
    prd.costo_unitario AS 'Costo unitario promedio de producción',
    SUM(dp.cantidad*prd.costo_unitario) AS 'Costo total promedio de producción',
    pd.total - SUM(dp.cantidad*prd.costo_unitario) AS Ganancia,
    ROUND(((pd.total-SUM(dp.cantidad * prd.costo_unitario))/pd.total)*100,2) AS 'Margen de ganancia %'
FROM pedido AS pd
INNER JOIN detallepedido AS dp ON pd.id_pedido=dp.id_pedido
INNER JOIN producto AS p ON dp.id_producto=p.id_producto
INNER JOIN (SELECT id_producto, ROUND(AVG(costo_unitario),2) AS costo_unitario
			FROM produccion
            GROUP BY id_producto) AS prd ON prd.id_producto=dp.id_producto
WHERE pd.estado='TERMINADO'
GROUP BY pd.id_pedido, pd.total, pd.fecha, prd.costo_unitario
ORDER BY Ganancia DESC;

/*1.3. IDENTIFICAR LAS FECHAS CON MAS VENTAS: ORDER BY, GROUP BY Y FECHAS.*/
SELECT fecha, COUNT(id_pedido) AS 'Número pedidos', SUM(total) AS Total
FROM pedido 
WHERE estado='TERMINADO'
GROUP BY fecha
ORDER BY Total DESC;

/*1.4. MATERIA PRIMA MÁS CONSUMIDA: JOIN, GROUP BY, ORDER BY*/
SELECT i.nombre AS Ingrediente, i.unidad_medida, SUM(mmp.cantidad) AS Cantidad
FROM movimientomateriaprima AS mmp
INNER JOIN ingrediente AS i ON mmp.id_ingrediente=i.id_ingrediente
WHERE mmp.tipo_movimiento='SALIDA' OR mmp.tipo_movimiento='PRODUCCION'
GROUP BY i.id_ingrediente, i.nombre
ORDER BY Cantidad DESC;

/*1.5. CLIENTES QUE HAN HECHO MÁS DE UNA VENTA: JOIN, GROUP BY, ORDER BY.*/
SELECT u.id_usuario AS 'ID cliente' ,u.nombre AS Cliente, COUNT(pd.id_pedido) AS Pedidos, SUM(pd.total) AS Total
FROM usuario AS u
INNER JOIN pedido AS pd ON u.id_usuario=pd.id_usuario
WHERE pd.estado='TERMINADO' AND u.rol='CLIENTE'
GROUP BY u.id_usuario, u.nombre
HAVING COUNT(pd.id_pedido) >= 2
ORDER BY Pedidos DESC;

/*1.6. HISTORIAL DE PRODUCCION Y PRODUCTOS VENDIDOS: JOIN, UNION, ORDER BY*/
SELECT 'PRODUCCIÓN' AS 'Tipo de movimiento', p.nombre AS Producto, mp.cantidad AS Cantidad, mp.fecha AS Fecha
FROM movimientoproducto AS mp
INNER JOIN producto AS p ON mp.id_producto=p.id_producto
WHERE mp.tipo_movimiento='PRODUCCION'

UNION

SELECT 'VENTA' AS 'Tipo de movimiento', p.nombre AS Producto, mp.cantidad AS Cantidad, mp.fecha AS Fecha
FROM movimientoproducto AS mp
INNER JOIN producto AS p ON mp.id_producto=p.id_producto
WHERE mp.tipo_movimiento='VENTA'

ORDER BY Fecha;

/*1.7. CUANTOS DÍAS, HORAS Y MINUTOS QUE HAN PASADO DESDE QUE SE PIDIERON LAS VENTAS CUYO ESTADO NO ES TERMINADO: FECHAS, TIEMPO, ORDER BY, JOIN*/
SELECT pd.id_pedido AS Pedido, u.nombre AS Cliente, pd.fecha AS Fecha, DATEDIFF(CURDATE(), pd.fecha) AS 'Días transcurridos',TIMESTAMPDIFF(HOUR,pd.fecha, NOW()) AS 'Horas transcurrids',TIMESTAMPDIFF(MINUTE,pd.fecha, NOW()) AS 'Minutos transcurridos', pd.estado AS Estado
FROM pedido AS pd
INNER JOIN usuario AS u ON pd.id_usuario=u.id_usuario
WHERE pd.estado != 'TERMINADO'
ORDER BY `Días transcurridos` DESC;

/*Parte Camila */
delimiter //
create procedure calculo_ventas_diarias(fecha_a_revisar date)
begin
	    select * from pedido where date(fecha) = fecha_a_revisar;
end //
delimiter ;

delimiter //
create procedure calculo_ventas_diarias_total(fecha_a_revisar date)
begin
	    select date(fecha) as "dia",
        sum(total) as "Total dia"
        from pedido where date(fecha) = fecha_a_revisar
        group by date(fecha);
end //

delimiter ;

/*Parte Azaan*/
-- 3.1 PROCEDIMIENTO: REPORTE DE CLIENTES DEL PRIMER TRIMESTRE
-- Selecciona clientes que compraron entre el 1 de enero y el 31 de marzo de un año específico
DELIMITER //

CREATE PROCEDURE reporte_clientes_primer_trimestre(IN anio_consulta INT)
BEGIN
    SELECT DISTINCT 
        u.id_usuario, 
        u.nombre, 
        u.apellido, 
        u.correo, 
        u.telefono
    FROM usuario AS u
    INNER JOIN pedido AS p ON u.id_usuario = p.id_usuario
    WHERE u.rol = 'CLIENTE'
      AND p.fecha >= CONCAT(anio_consulta, '-01-01 00:00:00')
      AND p.fecha <= CONCAT(anio_consulta, '-03-31 23:59:59')
    ORDER BY u.nombre ASC;
END //

DELIMITER ;

-- 3.2 PROCEDIMIENTO CON EXCEPCIÓN: TRY/CATCH PARA RESTRICCIÓN ÚNICA
-- Intenta insertar un usuario. Si el correo ya existe, atrapa el error y muestra un mensaje.
DELIMITER //

CREATE PROCEDURE insertar_cliente_excepcion(
    IN p_nombre VARCHAR(100),
    IN p_apellido VARCHAR(100),
    IN p_correo VARCHAR(150),
    IN p_contrasena VARCHAR(255),
    IN p_telefono VARCHAR(20)
)
BEGIN
    DECLARE EXIT HANDLER FOR 1062
    BEGIN
        SELECT 'ERROR: Restricción única activada. El correo electrónico ya está registrado.' AS Mensaje;
    END;
    INSERT INTO usuario (nombre, apellido, correo, contrasena, telefono, tipo_usuario, rol)
    VALUES (p_nombre, p_apellido, p_correo, p_contrasena, p_telefono, 'PERSONA', 'CLIENTE');
    SELECT 'EXITO: Cliente registrado correctamente.' AS Mensaje;
    
END //
DELIMITER ;

/*Parte Mafer*/
DELIMITER //
CREATE FUNCTION fn_validar_stock_producto(p_id_producto INT, p_cantidad INT)
RETURNS TINYINT(1)
READS SQL DATA
BEGIN
    DECLARE v_existencia INT;
    SELECT existencia INTO v_existencia FROM producto WHERE id_producto = p_id_producto;
    IF v_existencia >= p_cantidad THEN
        RETURN 1; 
    ELSE
        RETURN 0;
    END IF;
END //

CREATE FUNCTION fn_obtener_precio_vigente(p_id_producto INT, p_cantidad INT)
RETURNS DECIMAL(10,2)
READS SQL DATA
BEGIN
    DECLARE v_precio DECIMAL(10,2);
    DECLARE v_precio_mayoreo DECIMAL(10,2);
    DECLARE v_cant_mayoreo INT;
    SELECT precio, precio_mayoreo, cantidad_minima_mayoreo
    INTO v_precio, v_precio_mayoreo, v_cant_mayoreo
    FROM producto 
    WHERE id_producto = p_id_producto;
    -- Si lleva más de la cantidad mínima y existe un precio de mayoreo, se usa
    IF p_cantidad >= v_cant_mayoreo AND v_precio_mayoreo IS NOT NULL THEN
        RETURN v_precio_mayoreo;
    ELSE
        RETURN v_precio;
    END IF;
END //
DELIMITER ;

DELIMITER //
CREATE PROCEDURE sp_crear_pedido(
    IN p_id_usuario INT,
    IN p_id_direccion INT,
    IN p_metodo_pago VARCHAR(20),
    OUT p_id_pedido INT 
)
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 @sqlstate = RETURNED_SQLSTATE, @text = MESSAGE_TEXT;
        ROLLBACK;
        SELECT CONCAT('ERROR AL CREAR PEDIDO (', @sqlstate, '): ', @text) AS Mensaje_Error;
    END;
    START TRANSACTION;
    IF NOT EXISTS (SELECT 1 FROM direccionusuario WHERE id_direccion = p_id_direccion AND id_usuario = p_id_usuario) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'La dirección proporcionada no pertenece a este usuario.';
    END IF;
    INSERT INTO pedido (id_usuario, id_direccion, estado, total, metodo_pago)
    VALUES (p_id_usuario, p_id_direccion, 'PENDIENTE', 0.00, p_metodo_pago);
    SET p_id_pedido = LAST_INSERT_ID();
    COMMIT;
END //

CREATE PROCEDURE sp_agregar_detalle_pedido(
    IN p_id_pedido INT,
    IN p_id_producto INT,
    IN p_cantidad INT
)
BEGIN
    DECLARE v_precio_unitario DECIMAL(10,2);
    DECLARE v_subtotal DECIMAL(10,2);
    DECLARE v_estado_pedido VARCHAR(20);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        GET DIAGNOSTICS CONDITION 1 @sqlstate = RETURNED_SQLSTATE, @text = MESSAGE_TEXT;
        ROLLBACK;
        SELECT CONCAT('ERROR AL AGREGAR PRODUCTO (', @sqlstate, '): ', @text) AS Mensaje_Error;
    END;
    START TRANSACTION;
    SELECT estado INTO v_estado_pedido FROM pedido WHERE id_pedido = p_id_pedido;
    IF v_estado_pedido != 'PENDIENTE' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No se puede modificar. El pedido ya no está en estado PENDIENTE.';
    END IF;
    IF fn_validar_stock_producto(p_id_producto, p_cantidad) = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Stock insuficiente para cubrir la cantidad solicitada de este producto.';
    END IF;
    SET v_precio_unitario = fn_obtener_precio_vigente(p_id_producto, p_cantidad);
    SET v_subtotal = v_precio_unitario * p_cantidad;
    INSERT INTO detallepedido (id_pedido, id_producto, cantidad, precio_unitario, subtotal)
    VALUES (p_id_pedido, p_id_producto, p_cantidad, v_precio_unitario, v_subtotal);
    UPDATE producto SET existencia = existencia - p_cantidad WHERE id_producto = p_id_producto;
    UPDATE pedido SET total = total + v_subtotal WHERE id_pedido = p_id_pedido;
    COMMIT;
    SELECT 'EXITO: Producto agregado, stock descontado y total del pedido actualizado.' AS Resultado;
END //
DELIMITER ;
