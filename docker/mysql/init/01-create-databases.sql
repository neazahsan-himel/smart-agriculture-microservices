-- Runs once, on the first start of an empty MySQL volume.
-- Tables are created by each service (spring.jpa.hibernate.ddl-auto=update).
CREATE DATABASE IF NOT EXISTS farmer_db;
CREATE DATABASE IF NOT EXISTS crop_db;
CREATE DATABASE IF NOT EXISTS weather_db;
CREATE DATABASE IF NOT EXISTS farm_asset_db;
CREATE DATABASE IF NOT EXISTS notification_db;
CREATE DATABASE IF NOT EXISTS ai_advisor_db;
CREATE DATABASE IF NOT EXISTS knowledge_db;
