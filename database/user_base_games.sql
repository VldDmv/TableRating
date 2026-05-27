
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `games`
--

DROP TABLE IF EXISTS `games`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `games` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `cover_url` varchar(500) DEFAULT NULL,
  `user_id` int NOT NULL,
  `score` int DEFAULT '0',
  `completed` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `games_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=276 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `games`
--

LOCK TABLES `games` WRITE;
/*!40000 ALTER TABLE `games` DISABLE KEYS */;
INSERT INTO `games` VALUES (28,'Delta force Xtreme 2','',5,14,1),(30,'Sherlock Holmes: The Mystery of the Persian Carpet',NULL,5,10,0),(31,'Tomato way (1, 2, 3)',NULL,5,4,1),(32,'Dynasty warriors 9',NULL,5,13,0),(33,'Ultima 9',NULL,5,15,1),(34,'System shock 2',NULL,5,97,1),(35,'Minecraft',NULL,5,96,1),(36,'Bioshock Infinite','https://upload.wikimedia.org/wikipedia/ru/3/35/BioShock_Infinite.jpeg',5,95,1),(37,'Crash Team Racing','https://e.snmc.io/lk/g/x/0fed5f0e78143c3d7913bdbf54bd2044/11550589',5,97,1),(38,'Thief 2',NULL,5,98,1),(40,'Half life',NULL,5,98,1),(41,'Left 4 dead 2',NULL,5,96,1),(42,'Worms Armageddon',NULL,5,95,1),(43,'Portal 2',NULL,5,99,1),(44,'Deus Ex: Human Revolution',NULL,5,95,1),(47,'Gothic',NULL,5,50,1),(50,'Half life 2',NULL,5,99,1),(53,'Hotline miami 2',NULL,5,58,1),(55,'NieR Replicant™ ver.1.22474487139...',NULL,5,33,1),(56,'Divinity Original Sin',NULL,5,73,1),(57,'Hearts of iron 4',NULL,5,61,1),(61,'Rake',NULL,5,15,1),(71,'Lego Worlds',NULL,5,36,1),(123,'Demon\'s Souls',NULL,5,37,1),(124,'Precursors',NULL,5,15,1),(125,'Gremlins, Inc.',NULL,5,74,1),(126,'Farlanders',NULL,5,41,1),(127,'Arx Fatalis','https://upload.wikimedia.org/wikipedia/en/3/3c/Arx_Fatalis_cover.png',5,36,1),(144,'F.E.A.R.',NULL,5,81,1),(145,'GTFO','',5,50,0),(146,'Delta force',NULL,5,65,1),(147,'Worms reloaded',NULL,5,71,1),(148,'Marble Age',NULL,5,65,1),(149,'Sherlock Holmes: Crimes and Punishments',NULL,5,69,1),(207,'Final Fantasy',NULL,5,31,1),(213,'Deus Ex',NULL,5,95,1),(236,'tret',NULL,39,5,0),(252,'66','https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR9-UP9bgvAqFOa8yYs9wS3taVgFpjOznSh3g&s',5,66,1),(253,'77',NULL,5,2,0),(263,'fasd00','https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcR3OD5J8mrFwpdUzJ0SUrDEN863WVBgun5pcQ&s',40,56,1),(264,'~`ed\'\'123@#','https://ch.puro.com/cdn/shop/files/NERO_16_1.jpg?v=1770692434&width=2048',5,11,1),(265,'7a','',5,77,1),(268,'55sfsd1','',46,86,1),(272,'55','',5,11,0),(273,'453','',52,11,0),(274,'4561',NULL,5,12,1);
/*!40000 ALTER TABLE `games` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
