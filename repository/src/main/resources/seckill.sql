/*
Navicat MySQL Data Transfer

Source Server         : ice
Source Server Version : 80021
Source Host           : localhost:3306
Source Database       : seckill

Target Server Type    : MYSQL
Target Server Version : 80021
File Encoding         : 65001

Date: 2021-06-01 20:36:28
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for sec_goods
-- ----------------------------
DROP TABLE IF EXISTS `sec_goods`;
CREATE TABLE `sec_goods` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT 'id，主键，因为可能一个商品有多个活动',
  `good_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动商品 id',
  `good_name` varchar(60) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动商品名称',
  `origin_price` decimal(8,2) DEFAULT NULL COMMENT '原始价格',
  `discount_price` decimal(8,2) DEFAULT NULL COMMENT '活动价格',
  `stock` int DEFAULT NULL COMMENT '参与活动商品的库存',
  `start_time` datetime DEFAULT NULL COMMENT '活动开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '活动结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1004 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sec_goods
-- ----------------------------
INSERT INTO `sec_goods` VALUES ('1000', '596935345022', 'iPhone', '8000.00', '6500.00', '5000', '2021-06-05 00:00:00', '2021-06-15 23:59:59');
INSERT INTO `sec_goods` VALUES ('1001', '281033007288', 'Huawei Mate 40', '6000.00', '4800.00', '3500', '2021-06-10 00:00:00', '2021-06-25 23:59:59');
INSERT INTO `sec_goods` VALUES ('1002', '277672092104', 'OPPO R37', '3800.00', '2999.00', '8000', '2021-06-18 00:00:00', '2021-07-27 23:59:59');
INSERT INTO `sec_goods` VALUES ('1003', '891864901641', 'Honor 9', '3000.00', '2499.00', '3000', '2021-06-01 00:00:00', '2021-06-30 23:59:59');

-- ----------------------------
-- Table structure for sec_orders
-- ----------------------------
DROP TABLE IF EXISTS `sec_orders`;
CREATE TABLE `sec_orders` (
  `order_id` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单id',
  `phone` char(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '手机号',
  `email` varchar(40) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '电子邮箱',
  `good_id` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '活动商品 id',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间',
  `status` char(1) COLLATE utf8mb4_general_ci NOT NULL COMMENT '订单状态：0表示完成，1表示待付款，2表示订单取消',
  PRIMARY KEY (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ----------------------------
-- Records of sec_orders
-- ----------------------------
