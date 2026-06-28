-- ============================================================
--  Smart Agriculture Advisor — Seed Data
--  Run each block against the corresponding database.
--  Safe to re-run: INSERT IGNORE skips rows that already exist.
-- ============================================================


-- ============================================================
--  1. farmer_db  →  farmers
-- ============================================================
USE farmer_db;

INSERT IGNORE INTO farmers (
    id, name, phone_number, email,
    country_code, region, district,
    latitude, longitude,
    farm_size_hectares, farm_type, soil_type, irrigation_method,
    preferred_language, timezone, device_type,
    ai_consent_given, status, deleted, version
) VALUES

-- John (Rajshahi) — ID already used in dev/testing
('f8a8b7d7-97ac-48cb-81bd-87054223e5da',
 'John Rahman', '+8801711111001', 'john.rahman@example.com',
 'BD', 'Rajshahi', 'Rajshahi Sadar',
 24.3745, 88.6042,
 3.5, 'RICE', 'LOAMY', 'FLOOD',
 'en', 'Asia/Dhaka', 'SMARTPHONE',
 true, 'ACTIVE', false, 0),

-- Farmer from Dhaka Division — mixed crops, drip irrigation
('a1b2c3d4-0001-0001-0001-000000000001',
 'Rahim Uddin', '+8801722222001', 'rahim.uddin@example.com',
 'BD', 'Dhaka', 'Manikganj',
 23.8634, 90.0082,
 2.0, 'VEGETABLE', 'SILT', 'DRIP',
 'bn', 'Asia/Dhaka', 'FEATURE_PHONE',
 true, 'ACTIVE', false, 0),

-- Farmer from Khulna — jute & rice, flood-prone area
('a1b2c3d4-0001-0001-0001-000000000002',
 'Fatema Begum', '+8801733333001', 'fatema.begum@example.com',
 'BD', 'Khulna', 'Satkhira',
 22.7010, 89.0682,
 4.2, 'MIXED', 'CLAY', 'RAIN_FED',
 'bn', 'Asia/Dhaka', 'SMARTPHONE',
 true, 'ACTIVE', false, 0),

-- Farmer from Sylhet — tea & fruit, hilly terrain
('a1b2c3d4-0001-0001-0001-000000000003',
 'Karim Ali', '+8801744444001', 'karim.ali@example.com',
 'BD', 'Sylhet', 'Moulvibazar',
 24.4822, 91.7755,
 1.8, 'FRUIT', 'LOAMY', 'SPRINKLER',
 'en', 'Asia/Dhaka', 'SMARTPHONE',
 true, 'ACTIVE', false, 0),

-- Farmer from Chittagong — wheat & vegetables
('a1b2c3d4-0001-0001-0001-000000000004',
 'Nasrin Akter', '+8801755555001', 'nasrin.akter@example.com',
 'BD', 'Chittagong', 'Comilla',
 23.4607, 91.1809,
 5.0, 'WHEAT', 'SANDY', 'FLOOD',
 'en', 'Asia/Dhaka', 'DESKTOP',
 true, 'ACTIVE', false, 0);


-- ============================================================
--  2. crop_db  →  crops
-- ============================================================
USE crop_db;

INSERT IGNORE INTO crops (
    id, name, variety, description,
    crop_type, season, ideal_soil_type,
    min_temperature_celsius, max_temperature_celsius,
    min_rainfall_mm, max_rainfall_mm,
    growing_duration_days, irrigation_required,
    typical_yield_per_hectare, country_code, region,
    status, deleted, version
) VALUES

-- Boro Rice (dry season, irrigated) — largest rice crop in BD
('c0000001-0000-0000-0000-000000000001',
 'Rice', 'Boro (BRRI-28)',
 'High-yield irrigated rice grown in winter/spring (Boro season). Most productive rice crop in Bangladesh.',
 'CEREAL', 'RABI', 'LOAMY',
 16.0, 33.0, 150.0, 300.0, 145,
 true, 5.5, 'BD', 'Rajshahi',
 'ACTIVE', false, 0),

-- Aman Rice (monsoon season) — traditional kharif crop
('c0000001-0000-0000-0000-000000000002',
 'Rice', 'Aman (BRRI-51)',
 'Transplanted monsoon rice. Accounts for ~40% of total rice production in Bangladesh.',
 'CEREAL', 'KHARIF', 'CLAY',
 22.0, 35.0, 1200.0, 2000.0, 120,
 false, 4.0, 'BD', NULL,
 'ACTIVE', false, 0),

-- Aus Rice (pre-kharif) — short-season variety
('c0000001-0000-0000-0000-000000000003',
 'Rice', 'Aus (BRRI-26)',
 'Short-duration pre-kharif rice. Grown from March to August, tolerates heat and drought.',
 'CEREAL', 'KHARIF', 'SANDY',
 25.0, 38.0, 800.0, 1400.0, 100,
 false, 2.8, 'BD', NULL,
 'ACTIVE', false, 0),

-- Jute — national fiber crop of Bangladesh
('c0000001-0000-0000-0000-000000000004',
 'Jute', 'Tossa (O-9897)',
 'Golden fiber of Bangladesh. Thrives in humid monsoon climate. Major export crop.',
 'FIBER', 'KHARIF', 'SILT',
 24.0, 37.0, 1500.0, 2500.0, 120,
 false, 2.5, 'BD', NULL,
 'ACTIVE', false, 0),

-- Mustard — winter oilseed crop
('c0000001-0000-0000-0000-000000000005',
 'Mustard', 'BARI Mustard-14',
 'Primary oilseed crop of Bangladesh. Grown in rabi season after aman rice harvest.',
 'OILSEED', 'RABI', 'LOAMY',
 10.0, 25.0, 200.0, 500.0, 85,
 false, 1.4, 'BD', NULL,
 'ACTIVE', false, 0),

-- Wheat — second major cereal
('c0000001-0000-0000-0000-000000000006',
 'Wheat', 'BARI Gom-26',
 'Cool-season cereal. Grown in the northwest (Rajshahi, Rangpur) during winter.',
 'CEREAL', 'RABI', 'LOAMY',
 8.0, 22.0, 200.0, 400.0, 110,
 true, 3.2, 'BD', 'Rajshahi',
 'ACTIVE', false, 0),

-- Potato — most important vegetable crop
('c0000001-0000-0000-0000-000000000007',
 'Potato', 'Diamant',
 'High-demand winter vegetable. Bangladesh is top 10 world producer. Susceptible to late blight in humid weather.',
 'VEGETABLE', 'RABI', 'SANDY',
 10.0, 20.0, 400.0, 600.0, 90,
 true, 20.0, 'BD', NULL,
 'ACTIVE', false, 0),

-- Tomato — high-value vegetable
('c0000001-0000-0000-0000-000000000008',
 'Tomato', 'BARI Tomato-14',
 'Popular cash crop for smallholders. Grown in rabi season; sensitive to waterlogging and excessive humidity.',
 'VEGETABLE', 'RABI', 'LOAMY',
 15.0, 28.0, 300.0, 600.0, 75,
 true, 25.0, 'BD', NULL,
 'ACTIVE', false, 0),

-- Lentil — winter legume
('c0000001-0000-0000-0000-000000000009',
 'Lentil', 'BARI Masur-7',
 'Important protein source. Improves soil nitrogen. Grown after aman rice harvest in north-west Bangladesh.',
 'LEGUME', 'RABI', 'CLAY',
 8.0, 25.0, 150.0, 400.0, 100,
 false, 1.2, 'BD', 'Rajshahi',
 'ACTIVE', false, 0),

-- Maize — fast-growing cereal for feed & food
('c0000001-0000-0000-0000-000000000010',
 'Maize', 'NK-40 Hybrid',
 'Fastest growing cereal in BD agriculture. Used for poultry feed, starch, and human food.',
 'CEREAL', 'RABI', 'LOAMY',
 15.0, 35.0, 500.0, 800.0, 95,
 true, 8.0, 'BD', NULL,
 'ACTIVE', false, 0),

-- Onion — essential kitchen crop, high price volatility
('c0000001-0000-0000-0000-000000000011',
 'Onion', 'BARI Piaz-5',
 'High-demand cash crop with significant price fluctuations. BD imports heavily — local production is strategic.',
 'VEGETABLE', 'RABI', 'SANDY',
 12.0, 24.0, 200.0, 500.0, 120,
 true, 10.0, 'BD', NULL,
 'ACTIVE', false, 0),

-- Sugarcane — perennial cash crop
('c0000001-0000-0000-0000-000000000012',
 'Sugarcane', 'Isd-37',
 'Long-duration perennial crop grown near sugar mills in Rajshahi, Rangpur, and Khulna regions.',
 'OTHER', 'YEAR_ROUND', 'LOAMY',
 20.0, 38.0, 1000.0, 1500.0, 365,
 true, 55.0, 'BD', 'Rajshahi',
 'ACTIVE', false, 0);


-- ============================================================
--  3. weather_db  →  weather_records
-- ============================================================
USE weather_db;

INSERT IGNORE INTO weather_records (
    id, country_code, region,
    latitude, longitude, recorded_at, record_type,
    temperature_celsius, feels_like_celsius, humidity_percent,
    rainfall_mm, wind_speed_kmh, wind_direction, weather_condition,
    visibility_km, uv_index, data_source,
    status, deleted, version
) VALUES

-- Rajshahi — current observation (June, early monsoon)
('w0000001-0000-0000-0000-000000000001',
 'BD', 'Rajshahi', 24.3745, 88.6042,
 '2026-06-13 09:00:00', 'OBSERVATION',
 33.5, 40.0, 78, 12.0, 18.0, 'SW', 'PARTLY_CLOUDY',
 8.0, 7, 'MANUAL',
 'ACTIVE', false, 0),

-- Rajshahi — 24-hour forecast (rain expected)
('w0000001-0000-0000-0000-000000000002',
 'BD', 'Rajshahi', 24.3745, 88.6042,
 '2026-06-14 09:00:00', 'FORECAST',
 30.0, 36.0, 88, 45.0, 22.0, 'SW', 'RAINY',
 5.0, 3, 'MANUAL',
 'ACTIVE', false, 0),

-- Dhaka — current observation
('w0000001-0000-0000-0000-000000000003',
 'BD', 'Dhaka', 23.8103, 90.4125,
 '2026-06-13 09:00:00', 'OBSERVATION',
 34.0, 41.0, 82, 5.0, 15.0, 'S', 'CLOUDY',
 7.0, 6, 'MANUAL',
 'ACTIVE', false, 0),

-- Dhaka — forecast (thunderstorm tonight)
('w0000001-0000-0000-0000-000000000004',
 'BD', 'Dhaka', 23.8103, 90.4125,
 '2026-06-13 21:00:00', 'FORECAST',
 27.0, 31.0, 92, 80.0, 35.0, 'NW', 'THUNDERSTORM',
 3.0, 1, 'MANUAL',
 'ACTIVE', false, 0),

-- Khulna — observation (hot & humid, coastal)
('w0000001-0000-0000-0000-000000000005',
 'BD', 'Khulna', 22.8456, 89.5403,
 '2026-06-13 09:00:00', 'OBSERVATION',
 35.0, 43.0, 85, 0.0, 12.0, 'SE', 'SUNNY',
 10.0, 8, 'MANUAL',
 'ACTIVE', false, 0),

-- Sylhet — observation (higher rainfall, hilly)
('w0000001-0000-0000-0000-000000000006',
 'BD', 'Sylhet', 24.8949, 91.8687,
 '2026-06-13 09:00:00', 'OBSERVATION',
 28.0, 33.0, 91, 95.0, 20.0, 'SW', 'RAINY',
 4.0, 2, 'MANUAL',
 'ACTIVE', false, 0),

-- Chittagong — observation (drizzle, pre-monsoon)
('w0000001-0000-0000-0000-000000000007',
 'BD', 'Chittagong', 22.3569, 91.7832,
 '2026-06-13 09:00:00', 'OBSERVATION',
 31.0, 37.0, 86, 18.0, 25.0, 'SW', 'DRIZZLE',
 6.0, 4, 'MANUAL',
 'ACTIVE', false, 0);