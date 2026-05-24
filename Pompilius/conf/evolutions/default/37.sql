# Insertar recursos adicionales realistas para pruebas

# --- !Ups
INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
      -- Recursos de María (STUDENT - España)
      (835125411154232030, 'Gredos pegmatite tourmaline crystals GRE-PG-2024-07', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Gredos Mountains, Ávila, Spain',
       'Excellent black tourmaline crystals in quartz-feldspar pegmatite. Collected from abandoned quarry.',
       'Pegmatitic tourmaline specimen with museum quality', 45.00, 0),

      (835125411154232031, 'Almería gypsum desert rose AL-GY-2024-12', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Tabernas Desert, Almería, Spain',
       'Classic desert rose formation. Perfect example for educational purposes.',
       'Gypsum desert rose from arid environment', 0, 0),

      -- Recursos de Carlos (PROFESSIONAL - México)
      (835125411154232032, 'Zacatecas silver-bearing epithermal vein ZAC-AG-2024-05', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Zacatecas Mining District, Mexico',
       'High-grade silver ore with native silver visible. Sample from active mine level 8.',
       'Epithermal Ag-Pb-Zn mineralization with argentite', 0, 1),

      (835125411154232033, 'Chicxulub impact breccia analysis', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Chicxulub Crater, Yucatan Peninsula, Mexico',
       'Comprehensive study of impact breccia samples from ICDP drilling project. 45 thin sections analyzed.',
       'Petrographic analysis of K-Pg boundary impact breccia', 180.00, 0),

      (835125411154232034, 'Popocatépetl ash samples collection 2022-2024', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Popocatépetl Volcano monitoring station, Mexico',
       'Systematic collection of volcanic ash from 15 eruptive events. Grain size and chemical analysis included.',
       'Recent Popocatépetl volcanic ash monitoring', 0, 0),

      -- Recursos de John (STUDENT - USA)
      (835125411154232035, 'Yellowstone obsidian nodules YEL-OB-2023-11', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Yellowstone National Park, Wyoming, USA',
       'Black obsidian nodules with excellent conchoidal fracture. Natural surface samples.',
       'Rhyolitic volcanic glass from caldera system', 0, 1),

      (835125411154232036, 'Sierra Nevada batholith plutonic suite petrology', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Sierra Nevada, California, USA',
       'Multi-pluton study analyzing compositional variations. 85 samples with whole-rock geochemistry.',
       'Magmatic evolution of Mesozoic batholith complex', 220.00, 0),

      -- Recursos de Ana (PROFESSIONAL - Chile)
      (835125411154232037, 'Atacama halite salt crystals ATA-HAL-2024-03', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Salar de Atacama, Antofagasta, Chile',
       'Large halite crystals from hyperarid environment. Perfect cubic forms preserved.',
       'Evaporitic halite from driest desert on Earth', 0, 0),

      (835125411154232038, 'Coastal uplift terraces chronology central Chile', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'San Antonio to Valparaíso coast, Chile',
       'Marine terrace chronology using OSL dating.  Reconstructs 800ka of coastal tectonic uplift.',
       'Quaternary coastal uplift and paleoseismology study', 0, 1),

      (835125411154232039, 'Llaima volcano basaltic-andesite LLA-BA-2024-08', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Llaima Volcano, Araucanía Region, Chile',
       'Fresh 2008 eruption lava sample. Plagioclase phenocrysts clearly visible.',
       'Recent basaltic-andesite from active stratovolcano', 28.00, 0),

      -- Recursos de Argentina (asignados a Ana - PROFESSIONAL)
      (835125411154232040, 'Ischigualasto petrified wood ISC-PW-2023-09', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Ischigualasto Provincial Park (Valle de la Luna), San Juan, Argentina',
       'Triassic petrified wood with excellent cell structure preservation. Silicification complete.',
       'Fossilized wood from Triassic Ischigualasto Formation', 0, 1),

      (835125411154232041, 'Mendoza Andean copper porphyry exploration report', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'San Juan - Mendoza Andes, Argentina',
       'Exploration project with geochemical survey, 25 rock samples, alteration mapping.',
       'Porphyry copper exploration in Andean belt', 95.00, 0),

      (835125411154232042, 'Patagonian glacial erratic granite PAT-ER-2024-01', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'El Chaltén, Santa Cruz, Argentina',
       'Glacial erratic boulder fragment. Evidence of Last Glacial Maximum ice extent.',
       'Glacially transported granite block - Quaternary geology', 35.00, 0),

      -- Recursos de Laura (STUDENT - Colombia)
      (835125411154232043, 'Muzo emerald in calcite matrix MUZ-EM-2024-02', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Muzo Mining District, Boyacá, Colombia',
       'Small emerald crystal in host calcite. Characteristic hexagonal form. Educational specimen.',
       'Colombian emerald in sedimentary host rock', 150.00, 0),

      (835125411154232044, 'Eastern Cordillera fold-thrust belt structural analysis', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Eastern Cordillera, Colombian Andes',
       'Structural geology analysis with cross-sections and balanced restoration. 1:25,000 mapping.',
       'Andean orogeny structural evolution and petroleum systems', 0, 1),

      (835125411154232045, 'Guatavita Lake sediment cores paleoclimate', 'STUDY', 'PUBLIC', NOW(), NOW(),
       'Guatavita Lake, Cundinamarca, Colombia',
       'Holocene paleoclimate reconstruction from lake sediments. Pollen analysis and radiocarbon dating.',
       'Last 10,000 years climate change in tropical Andes', 0, 0),

      -- Recursos adicionales variados
      (835125411154232046, 'Death Valley borax minerals DV-BOR-2023-06', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Death Valley, California, USA',
       'Borax and other evaporite minerals from ancient lake bed. Historical mining area.',
       'Borate minerals from continental evaporite basin', 0, 0);

-- Muestras detalladas (SAMPLE)
INSERT INTO `sample` (
    `id`, `resource_id`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
      (835125411154233016, 835125411154232030,
       'Tourmaline (schorl), Quartz, K-feldspar (orthoclase), Albite, Muscovite',
       'Manual extraction from pegmatite pocket. Orientation and stratigraphic context documented.',
       0, 'Hand sample',
       'Hammer, chisel, protective goggles, field notebook, GPS',
       'Pegmatitic',
       'Late-stage granitic differentiation, hydrothermal crystallization, slow cooling at depth'),

      (835125411154233017, 835125411154232031,
       'Gypsum (CaSO4·2H2O)',
       'Surface collection from natural occurrence. No excavation required.',
       1, 'Desert rose',
       'Brush for cleaning, protective wrapping, photographic documentation',
       'Evaporitic',
       'Groundwater evaporation in arid climate, gypsum crystallization in sand matrix'),

      (835125411154233018, 835125411154232032,
       'Argentite (Ag2S), Native silver, Galena (PbS), Sphalerite (ZnS), Quartz, Calcite',
       'Underground sampling from active mine working. Safety protocols followed.',
       1, 'Ore sample',
       'Mine safety equipment, geological hammer, sample bags, labeling system',
       'Hydrothermal vein',
       'Epithermal fluid circulation, metal sulfide precipitation, vein formation in fracture system'),

      (835125411154233019, 835125411154232034,
       'Plagioclase, Pyroxene, Magnetite, Volcanic glass',
       'Systematic collection with ash traps during eruptive events. Real-time monitoring.',
       1, 'Volcanic ash',
       'Ash collection traps, sterile containers, GPS timestamps, photography',
       'Pyroclastic',
       'Magmatic degassing, explosive fragmentation, atmospheric dispersal, ash fall deposition'),

      (835125411154233020, 835125411154232035,
       'Volcanic glass (rhyolitic composition), minimal crystallization',
       'Natural surface collection from obsidian flow. No disturbance of archaeological sites.',
       1, 'Obsidian nodule',
       'Gloves, protective wrapping, GPS coordinates, photographic record',
       'Volcanic glass',
       'Rapid cooling of rhyolitic lava, suppression of crystal nucleation'),

      (835125411154233021, 835125411154232037,
       'Halite (NaCl), minor gypsum, traces of carnallite',
       'Surface sampling from salt flat crust. Dry season collection.',
       1, 'Evaporite crystal',
       'Non-metallic tools, sealed containers, humidity control packaging',
       'Evaporitic',
       'Hypersaline brine evaporation, NaCl saturation and precipitation'),

      (835125411154233022, 835125411154232039,
       'Plagioclase (labradorite-bytownite), Augite, Olivine, Magnetite, Volcanic glass',
       'Collection from 2008 lava flow margin. Fresh unweathered sample.',
       1, 'Hand sample',
       'Hammer, GPS, field camera, sample bag, field description notebook',
       'Volcanic',
       'Basaltic magma generation, fractional crystallization, effusive eruption, rapid surface cooling'),

      (835125411154233023, 835125411154232040,
       'Chalcedonic silica (SiO2), replacing original wood cellulose',
       'Surface collection from natural erosion. Fossil protected area permit obtained.',
       0, 'Fossil',
       'Soft brushes, protective wrapping, permits documentation, GPS location',
       'Sedimentary - fossilized',
       'Tree burial, groundwater silica infiltration, cell-by-cell replacement, diagenesis'),

      (835125411154233024, 835125411154232042,
       'Quartz, Plagioclase, K-feldspar, Biotite, Hornblende',
       'Fragment from large glacial erratic. Documented original boulder location.',
       0, 'Glacial erratic',
       'Hammer, chisel, GPS, photographic documentation of original boulder',
       'Igneous (glacially transported)',
       'Andean plutonism, glacial entrainment, ice transport, deposition after ice retreat'),

      (835125411154233025, 835125411154232043,
       'Emerald (beryl - Be3Al2Si6O18), Calcite (CaCO3), Pyrite traces',
       'Purchase from artisanal miner with legal documentation. Colombian geological heritage.',
       1, 'Crystal in matrix',
       'Legal purchase documentation, gemological loupe, photography, protective storage',
       'Hydrothermal in sedimentary',
       'Hydrothermal fluids in black shale, beryllium concentration, emerald crystallization in calcite veins'),

      (835125411154233026, 835125411154232046,
       'Borax (Na2B4O7·10H2O), Kernite, Halite, Gypsum',
       'Collection from historical mining area. Public land permit.',
       0, 'Evaporite sample',
       'Collection permit, non-sparking tools, sealed containers',
       'Evaporitic',
       'Miocene lake evaporation, borate concentration and precipitation');

-- Estudios detallados (STUDY)
INSERT INTO `study` (
    `id`, `resource_id`, `start_date`, `end_date`,
    `description`, `coordinates`, `area`, `methods`, `authors`,
    `section`, `antecedents`, `name_section`
) VALUES
      (835125411154234011, 835125411154232033,
       '2022-04-01 00:00:00', '2024-06-30 00:00:00',
       'Comprehensive petrographic analysis of Chicxulub impact breccia from ICDP Expedition 364 core samples. Identification of shocked minerals, melt particles, and target rock clasts. Determination of impact metamorphism grade and shock pressures.',
       '{"lat": 21.4, "lon": -89.5, "crater_diameter_km": 180, "impact_age_Ma": 66.0}',
       'PETROLOGY',
       'Optical microscopy, SEM-EDS, electron microprobe, Raman spectroscopy, shock metamorphism indicators',
       '["Dr. Carlos Fernández Ruiz", "Dr. Patricia Schultz", "MSc. Rodrigo Martínez"]',
       1, 1, 'Impact metamorphism petrology and shock effects'),

      (835125411154234012, 835125411154232036,
       '2022-01-10 00:00:00', '2024-08-20 00:00:00',
       'Petrological and geochemical study of Sierra Nevada batholith plutons. Analysis of magmatic differentiation trends, crustal contamination, and tectonic setting. Major and trace element whole-rock chemistry, Sr-Nd isotopes.',
       '{"lat": 37.8651, "lon": -119.5383, "area_km2": 65000, "age_range_Ma": "210-85"}',
       'PETROLOGY',
       'Field mapping, petrography, XRF whole-rock chemistry, ICP-MS trace elements, Sr-Nd isotopes MC-ICP-MS',
       '["John Smith", "Dr. Richard Anderson", "Dr. Emily Thompson"]',
       1, 1, 'Mesozoic magmatism in western North America'),

      (835125411154234013, 835125411154232038,
       '2020-09-01 00:00:00', '2023-12-31 00:00:00',
       'Chronological and geomorphological study of marine terraces along central Chilean coast. OSL dating of 12 terrace levels. Calculation of coastal uplift rates and correlation with megathrust earthquake cycle. Paleoseismic implications.',
       '{"coordinates": [{"lat": -33.5928, "lon": -71.6091}, {"lat": -33.0472, "lon": -71.6127}], "length_km": 80}',
       'GEOMORPHOLOGY',
       'Geomorphological mapping, topographic profiling with DGPS, OSL dating, grain size analysis, terrace elevation surveying',
       '["Dr. Ana Torres Moreno", "Dr. Marco Cisternas", "MSc. Felipe Ríos"]',
       1, 1, 'Quaternary tectonics and paleoseismology'),

      (835125411154234014, 835125411154232041,
       '2023-03-15 00:00:00', '2024-10-30 00:00:00',
       'Mineral exploration project targeting porphyry copper deposits in Andean belt. Regional stream sediment geochemistry, rock chip sampling, alteration mapping with ASTER satellite imagery. Identification of three anomalous areas.',
       '{"lat": -32.5, "lon": -69.8, "elevation_range": "2500-4200m", "area_km2": 580}',
       'GEOPHYSICS',
       'Stream sediment sampling, rock geochemistry ICP-MS, ASTER alteration mapping, structural mapping, porphyry indicator minerals',
       '["Pedro García", "Eng. Martín Sosa", "Geol. Carolina Vega"]',
       1, 1, 'Andean porphyry copper systems exploration'),

      (835125411154234015, 835125411154232044,
       '2023-05-10 00:00:00', '2024-11-15 00:00:00',
       'Structural geological study of Eastern Cordillera fold-thrust belt. Construction of balanced cross-sections. Analysis of relationship between surface structures and petroleum system. Timing of deformation using syn-tectonic sediments.',
       '{"coordinates": [{"lat": 6.2, "lon": -72.8}, {"lat": 5.8, "lon": -73.2}], "area_km2": 850}',
       'GEOTECHNICS',
       '1:25,000 structural mapping, cross-section construction and balancing, paleostress analysis, syn-tectonic sediment analysis',
       '["Laura Martínez", "Dr. Andrés Mora", "MSc. Juliana Gómez"]',
       1, 1, 'Andean deformation and petroleum systems'),

      (835125411154234016, 835125411154232045,
       '2021-06-20 00:00:00', '2023-09-30 00:00:00',
       'Paleoclimatic reconstruction for last 10,000 years using Guatavita Lake sediment cores. Pollen analysis identifying vegetation changes. AMS radiocarbon dating establishing chronology. Correlation with regional climate proxies.',
       '{"lat": 4.9800, "lon": -73.7681, "elevation": 3100, "lake_area_km2": 0.13}',
       'PALEONTOLOGY',
       'Lake sediment coring, pollen analysis and identification, AMS radiocarbon dating, magnetic susceptibility, LOI analysis',
       '["Laura Martínez", "Dr. Henry Hooghiemstra", "MSc. Carolina González"]',
       0, 1, NULL);

-- Asignar propietarios a los recursos
INSERT INTO `resource_user` (`resource_id`, `user_id`, `resource_user_type`, `created`, `deleted`) VALUES
    -- María (835125411154231297)
    (835125411154232030, 835125411154231297, 'OWNER', NOW(), 0),
    (835125411154232031, 835125411154231297, 'OWNER', NOW(), 0),

    -- Carlos (835125411154231298)
    (835125411154232032, 835125411154231298, 'OWNER', NOW(), 0),
    (835125411154232033, 835125411154231298, 'OWNER', NOW(), 0),
    (835125411154232034, 835125411154231298, 'OWNER', NOW(), 0),

    -- John (835125411154231299)
    (835125411154232035, 835125411154231299, 'OWNER', NOW(), 0),
    (835125411154232036, 835125411154231299, 'OWNER', NOW(), 0),
    (835125411154232046, 835125411154231299, 'OWNER', NOW(), 0),

    -- Ana (835125411154231300)
    (835125411154232037, 835125411154231300, 'OWNER', NOW(), 0),
    (835125411154232038, 835125411154231300, 'OWNER', NOW(), 0),
    (835125411154232039, 835125411154231300, 'OWNER', NOW(), 0),

    -- Ana (835125411154231300)
    (835125411154232040, 835125411154231300, 'OWNER', NOW(), 0),
    (835125411154232041, 835125411154231300, 'OWNER', NOW(), 0),
    (835125411154232042, 835125411154231300, 'OWNER', NOW(), 0),

    -- Laura (835125411154231302)
    (835125411154232043, 835125411154231302, 'OWNER', NOW(), 0),
    (835125411154232044, 835125411154231302, 'OWNER', NOW(), 0),
    (835125411154232045, 835125411154231302, 'OWNER', NOW(), 0),

    -- Algunas compras/bartering entre usuarios
    (835125411154232030, 835125411154231298, 'PURCHASED', NOW(), 0),  -- Carlos compró a María
    (835125411154232033, 835125411154231299, 'PURCHASED', NOW(), 0),  -- John compró a Carlos
    (835125411154232036, 835125411154231302, 'PURCHASED', NOW(), 0),  -- Laura compró a John
    (835125411154232039, 835125411154231297, 'PURCHASED', NOW(), 0),  -- María compró a Ana
    (835125411154232043, 835125411154231300, 'PURCHASED', NOW(), 0),  -- Ana compró a Laura
    (835125411154232032, 835125411154231300, 'ACCEPTED_AS_PAYMENT', NOW(), 0),  -- Ana recibió por barter de Carlos
    (835125411154232035, 835125411154231298, 'ACCEPTED_AS_PAYMENT', NOW(), 0),  -- Carlos recibió por barter de John
    (835125411154232038, 835125411154231302, 'ACCEPTED_AS_PAYMENT', NOW(), 0),  -- Laura recibió por barter de Ana
    (835125411154232040, 835125411154231299, 'ACCEPTED_AS_PAYMENT', NOW(), 0),  -- John recibió por barter de Pedro
    (835125411154232044, 835125411154231297, 'ACCEPTED_AS_PAYMENT', NOW(), 0);  -- María recibió por barter de Laura

-- ============================================================================
-- TRANSACCIONES DE PAGO + PAYMENT_INTENT + PAYMENT (datos visibles en módulo payments)
-- ============================================================================

INSERT INTO `transaction` (
    `id`, `transaction_type`, `transaction_status`, `seller_id`, `buyer_id`,
    `resource_id`, `fee`, `created`, `updated`, `completed_successfully_at`, `metadata`
) VALUES
      (835125411154235101, 'PAYMENT', 'COMPLETED', 835125411154231297, 835125411154231298,
       835125411154232030, 2.25, DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), '{"source":"seed37"}'),
      (835125411154235102, 'PAYMENT', 'COMPLETED', 835125411154231299, 835125411154231302,
       835125411154232036, 11.00, DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), '{"source":"seed37"}'),
      (835125411154235103, 'PAYMENT', 'COMPLETED', 835125411154231302, 835125411154231300,
       835125411154232043, 7.50, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), '{"source":"seed37"}'),
      (835125411154235104, 'PAYMENT', 'PENDING', 835125411154231298, 835125411154231301,
       835125411154232033, 9.00, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, '{"source":"seed37"}'),
      (835125411154235105, 'PAYMENT', 'CANCELLED', 835125411154231300, 835125411154231297,
       835125411154232039, 1.40, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, '{"source":"seed37"}');

INSERT INTO `payment_intent` (
    `payment_id`, `transaction_id`, `gateway`, `gateway_intent_id`,
    `price`, `surcharge`, `amount`, `status`, `discount`, `url`,
    `created`, `buyer_reference`, `instrument`, `fingerprint`,
    `return_url_params`, `metadata`, `extra_info`, `updated`
) VALUES
      (835125411154237101, 835125411154235101, 'STRIPE', 'pi_seed_235101',
       45.00, 0.00, 45.00, 'succeeded', 0.00, NULL,
       DATE_SUB(NOW(), INTERVAL 8 DAY), 'seed-buyer-001', 'card', 'fp_seed_001',
       '{"sellerId":"835125411154231297","resourceId":"835125411154232030"}', '{"source":"seed37"}', NULL, DATE_SUB(NOW(), INTERVAL 8 DAY)),
      (835125411154237102, 835125411154235102, 'STRIPE', 'pi_seed_235102',
       220.00, 0.00, 220.00, 'succeeded', 0.00, NULL,
       DATE_SUB(NOW(), INTERVAL 6 DAY), 'seed-buyer-002', 'card', 'fp_seed_002',
       '{"sellerId":"835125411154231299","resourceId":"835125411154232036"}', '{"source":"seed37"}', NULL, DATE_SUB(NOW(), INTERVAL 6 DAY)),
      (835125411154237103, 835125411154235103, 'STRIPE', 'pi_seed_235103',
       150.00, 0.00, 150.00, 'succeeded', 0.00, NULL,
       DATE_SUB(NOW(), INTERVAL 4 DAY), 'seed-buyer-003', 'card', 'fp_seed_003',
       '{"sellerId":"835125411154231302","resourceId":"835125411154232043"}', '{"source":"seed37"}', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),
      (835125411154237104, 835125411154235104, 'STRIPE', 'pi_seed_235104',
       180.00, 0.00, 180.00, 'processing', 0.00, NULL,
       DATE_SUB(NOW(), INTERVAL 1 DAY), 'seed-buyer-004', 'card', 'fp_seed_004',
       '{"sellerId":"835125411154231298","resourceId":"835125411154232033"}', '{"source":"seed37"}', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),
      (835125411154237105, 835125411154235105, 'STRIPE', 'pi_seed_235105',
       28.00, 0.00, 28.00, 'canceled', 0.00, NULL,
       DATE_SUB(NOW(), INTERVAL 2 DAY), 'seed-buyer-005', 'card', 'fp_seed_005',
       '{"sellerId":"835125411154231300","resourceId":"835125411154232039"}', '{"source":"seed37"}', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO `payment` (
    `id`, `transaction_id`, `gateway`, `gateway_payment_id`,
    `amount`, `platform_fee`, `gateway_fee`, `net_amount`,
    `currency`, `receipt_url`, `instrument`, `refunded`, `refunded_amount`,
    `created`, `updated`, `metadata`
) VALUES
      (835125411154237101, 835125411154235101, 'STRIPE', 'ch_seed_235101',
       45.00, 2.25, 1.61, 41.14,
       'EUR', NULL, 'card', 0, 0.00,
       DATE_SUB(NOW(), INTERVAL 8 DAY), DATE_SUB(NOW(), INTERVAL 8 DAY), '{"source":"seed37"}'),
      (835125411154237102, 835125411154235102, 'STRIPE', 'ch_seed_235102',
       220.00, 11.00, 6.68, 202.32,
       'EUR', NULL, 'card', 0, 0.00,
       DATE_SUB(NOW(), INTERVAL 6 DAY), DATE_SUB(NOW(), INTERVAL 6 DAY), '{"source":"seed37"}'),
      (835125411154237103, 835125411154235103, 'STRIPE', 'ch_seed_235103',
       150.00, 7.50, 4.65, 137.85,
       'EUR', NULL, 'card', 0, 0.00,
       DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 4 DAY), '{"source":"seed37"}');


