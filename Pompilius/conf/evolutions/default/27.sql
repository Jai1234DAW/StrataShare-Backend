# Scripts de insert into para pruebas y modificacion de columna rock_type a sample_category

# --- !Ups
ALTER TABLE `Sample` RENAME COLUMN `rock_type` TO `sample_category`;

INSERT INTO `users` (
    `id`, `username`, `password_hash`, `enabled`, `email`, `interests`, `country`,
    `first_name`, `last_name`, `phone`, `avatar`, `cover_photo`, `language`,
    `created`, `updated`, `bio`
) VALUES
      (835125411154231297, 'maria_geology', 'yQs7N3b9Yh9spSVRgLU_cg:4abk:2Lj1wo1xRNstXKnxHfuFXgXNzMtPaWnUIPEJ8M6DGrE',
       1, 'test1@example.com', 'VOLCANOLOGY,PETROLOGY,MINERALOGY',
       'ES', 'María', 'López García', '+34612345678',
       NULL, NULL, 'es', NOW(), NOW(),
       'Geology student passionate about volcanology and petrology'),

      (835125411154231298, 'carlos_geo_pro', 'JBCYVMw5y_gpiRwDm3OokQ:4abk:IDpz4Jeb1LG7SQekTOKwAS9JRB1aAaHCa3V-BHUO4xY',
       1, 'test2@example.com', 'STRATIGRAPHY,SEDIMENTOLOGY,PETROLEUM_GEOLOGY,GEOPHYSICS',
       'MEX', 'Carlos', 'Fernández Ruiz', '+52155987654',
       NULL, NULL, 'es', NOW(), NOW(),
       'Professional geologist with 15 years of experience in mining exploration'),

      (835125411154231299, 'john_rocks', 'aP7kWtehfv6WptkirN1gdA:4abk:D3kXGNLnZHxTvM-oshv68ArOeM3FxhxkciLBNcMioMc',
       1, 'test3@example.com', 'SEISMOLOGY,GEODYNAMICS,GEOPHYSICS',
       'USA', 'John', 'Smith', '+14155551234',
       NULL, NULL, 'en', NOW(), NOW(),
       'Geology student specializing in seismology and tectonics'),

      (835125411154231300, 'ana_researcher', '9k29TeKSysmM5WaeW_-Apw:4abk:lbvkCJOlVIEvdt-YQDsBAHcWivlvIyAVE2YiIt_zMec',
       1, 'test4@example.com', 'STRATIGRAPHY,SEDIMENTOLOGY,PALEONTOLOGY,HISTORICAL_GEOLOGY',
       'CHL', 'Ana', 'Torres Moreno', '+56987654321',
       NULL, NULL, 'es', NOW(), NOW(),
       'Researcher specialized in stratigraphy and sedimentology'),

      (835125411154231301, 'pedro_rocas', 'QyYM4tRJIth9v0bGDO2pLA:4abk:BRByzznFX3heRSaieoUVdud8TNl3E0Sdo7u_UVpye4o',
       1, 'test5@example.com', 'MINERALOGY,PETROLOGY,GEOMORPHOLOGY',
       'ARG', 'Pedro', 'García', '+541134567890',
       NULL, NULL, 'es', NOW(), NOW(),
       'Geology enthusiast, mineral collector'),

      (835125411154231302, 'laura_minerals', 'dNV3gxSVwYTBQR_Pyg3AJg:4abk:NczM3M8dON97ZZGcgI39221aA56f7-Jy6wlBLy7n7QY',
       1, 'test6@example.com', 'MINERALOGY,PETROLOGY,GEOTECHNICS',
       'COL', 'Laura', 'Martínez', '+573001234567',
       NULL, NULL, 'es', NOW(), NOW(),
       'student in mineralogy and crystallography');

INSERT INTO `users_role` (`user_id`, `role`) VALUES
    (835125411154231297, 'STUDENT'),
    (835125411154231298, 'PROFESSIONAL'),
    (835125411154231299, 'STUDENT'),
    (835125411154231300, 'PROFESSIONAL'),
    (835125411154231301, 'AMATEUR'),
    (835125411154231302, 'STUDENT');

INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
      (835125411154232001,  'Villarrica olivine basalt VLL-2023-01', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Andes Mountains, Chile - Villarrica Volcano',
       'Sample collected during 2023 effusive eruption. Shows visible olivine crystals.',
       'Olivine basaltic volcanic rock from Villarrica', 0, 0),

      (835125411154232002, 'Guadarrama biotite granite GUA-GT-2024-15', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Guadarrama Mountains, Spain - Peñalara',
       'Hercynian granite sample with abundant biotite. Collected from natural outcrop.',
       'Carboniferous age biotite granite', 20, 0),

      (835125411154232003, 'Reynisfjara columnar basalt ISL-BC-2023-08', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Iceland - Reynisfjara basalt columns',
       'Hexagonal column fragment typical of slow cooling. Excellent educational example.',
       'Columnar basalt from Icelandic black beach', 0, 1),

      (835125411154232004, 'El Teniente hydrothermal breccia TEN-BX-2024-03', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'El Teniente Mine, Chile',
       'Mineralized breccia sample with visible chalcopyrite and molybdenite.',
       'Hydrothermal breccia with Cu-Mo mineralization', 0, 0),

      (835125411154232005, 'Neogene Stratigraphy Amazon Basin', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Amazon Basin, Brazil',
       'Complete study with 15 stratigraphic boreholes. Analysis of 250 meters sedimentary column.',
       'Stratigraphic analysis Amazon Basin - Solimões Formation', 250, 0),

      (835125411154232006,  'Teide volcano eruptive evolution last 200ka', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Teide National Park, Tenerife, Spain',
       'Multi-temporal investigation of eruptive activity. Includes K-Ar dating of 50 samples.',
       'Teide volcano eruptive evolution - last 200,000 years', 0, 1),

      (835125411154232007,  'San Andreas Fault seismotectonic monitoring Parkfield', 'STUDY', 'PUBLIC', NOW(), NOW(),
       'San Andreas Fault, California, USA',
       'Seismic microzonation with 30-station network. Data since 2020.',
       'San Andreas Fault seismotectonic study central segment', 0, 0),

      (835125411154232008,  'Precambrian basement geology Aconquija Massif', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Aconquija Massif, Argentina',
       '1:50,000 geological mapping with petrographic analysis of 80 samples.',
       'Precambrian metamorphic basement geology and tectonics', 10, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
      (835125411154233001, 835125411154232001,
       'Olivine, Pyroxene (Augite), Plagioclase, Magnetite',
       'Manual collection with geological hammer. Sample taken from recent flow on south flank.',
       1, 'Hand sample',
       'Geological hammer, sampling bag, GPS, camera',
       'Volcanic igneous basaltic',
       'Partial mantle melting, magmatic ascent, fractional crystallization, effusive eruption'),

      (835125411154233002, 835125411154232002,
       'Quartz (30%), K-feldspar (40%), Plagioclase (15%), Biotite (12%), Muscovite (3%)',
       'Extraction from natural outcrop using hammer and chisel. Orientation recorded.',
       0, 'Hand sample',
       'Hammer, chisel, Brunton compass, sealed bag to prevent alteration',
       'Intrusive igneous',
       'Hercynian plutonism, slow crystallization at depth, exhumation by erosion, weathering'),

      (835125411154233003, 835125411154232003,
       'Pyroxene, Calcic plagioclase, Magnetite, Ilmenite',
       'Naturally fallen hexagonal column fragment. No alteration of original sample.',
       1, 'Columnar basalt',
       'Extensive photographic documentation, column diameter measurement, high-precision GPS',
       'Igneous',
       'Fissure volcanism, slow uniform cooling, thermal contraction generating hexagonal prisms'),

      (835125411154233004, 835125411154232004,
       'Chalcopyrite (CuFeS2), Molybdenite (MoS2), Pyrite, Quartz, Biotite, Altered feldspar',
       'Diamond drill core DDH-450 depth 580m. Sample preserved in original conditions.',
       1, 'Hydrothermal breccia',
       'Diamond drilling HQ, core box, core photography',
       'Breccia with quartz-sulfide cement',
       'Porphyry intrusion, hydraulic fracturing, hydrothermal fluid circulation, sulfide precipitation');

INSERT INTO `study` (
    `id`, `resource_id`, `start_date`, `end_date`,
    `description`, `coordinates`, `area`, `methods`, `authors`,
    `section`, `antecedents`, `name_section`
) VALUES
      (835125411154234001, 835125411154232005,
       '2021-03-15 00:00:00', '2024-11-30 00:00:00',
       'Detailed stratigraphic analysis of Solimões Formation (Miocene-Pliocene). 15 stratigraphic boreholes reaching depths up to 250 meters. Identification of 8 main depositional sequences with sedimentological, paleontological and stratigraphic analysis.',
       '{"coordinates": [{"lat": -3.4653, "lon": -62.2159}, {"lat": -3.8734, "lon": -63.0289}], "area_km2": 1250}',
       'STRATIGRAPHY',
       'Stratigraphic drilling, grain size analysis, sedimentary facies analysis, palynology and vertebrate paleontology dating, stratigraphic correlation',
       '["Dr. Ana Torres Moreno", "Dr. Roberto Silva", "MSc. Patricia Gómez", "Eng. Luis Herrera"]',
       1, 1, 'Stratigraphic columns and depositional sequence analysis'),

      (835125411154234002, 835125411154232006,
       '2021-06-01 00:00:00', '2023-12-31 00:00:00',
       'Research on eruptive history, magmatic evolution and geodynamics of Teide-Pico Viejo volcanic complex. Multi-method study including detailed 1:10,000 geological mapping, petrographic and geochemical analysis of 120 samples, absolute K-Ar dating of 50 volcanic units.',
       '{"lat": 28.2723, "lon": -16.6415, "elevation": 3718, "crater_diameter": 80}',
       'VOLCANOLOGY',
       'Detailed geological mapping, petrography, ICP-MS geochemistry, K-Ar dating, Sr-Nd-Pb isotopic analysis, gravimetry, magnetometry',
       '["Prof. Dr. Juan Martínez", "Dr. Elena Ruiz", "Dr. Francisco Pérez", "MSc. Isabel Sánchez"]',
       1, 1, 'Theoretical framework of Teide eruptive evolution'),

      (835125411154234003, 835125411154232007,
       '2019-03-15 00:00:00', NULL,
       'Continuous 24/7 seismic activity monitoring project in Parkfield segment. Network of 30 broadband seismographic stations, 15 strong motion accelerometers, 12 permanent GPS stations. Database with over 50,000 recorded events.',
       '{"lat": 35.9940, "lon": -119.5466, "fault_segment": "Parkfield", "length_km": 35}',
       'SEISMOLOGY',
       'Broadband seismographic network, triaxial accelerographs, continuous geodetic GPS, InSAR, waveform processing, hypocenter determination',
       '["Dr. Robert Smith", "Dr. Jennifer Lee", "Dr. Michael Chen", "MSc. Sarah Johnson"]',
       0, 1, NULL),

      (835125411154234004, 835125411154232008,
       '2023-01-20 00:00:00', '2024-09-15 00:00:00',
       'Detailed 1:50,000 scale geological mapping of Precambrian metamorphic basement. Petrographic, structural and geochronological characterization. Identification of sedimentary and magmatic protoliths. P-T conditions determination of metamorphism.',
       '{"lat": -26.8333, "lon": -65.7833, "elevation_range": "1500-5000m", "area_km2": 450}',
       'GEODYNAMICS',
       '1:50,000 geological mapping, petrographic analysis, microstructural analysis, thermobarometry, U-Pb zircon dating LA-ICP-MS',
       '["Dr. Carlos Fernández", "Dr. María López", "MSc. Diego Romero", "Lic. Sofía Vargas"]',
       1, 1, 'Geological background of metamorphic basement');

INSERT INTO `resource_user` (`resource_id`, `user_id`, `resource_user_type`, `created`, `deleted`) VALUES
    (835125411154232001, 835125411154231297, 'OWNER', NOW(), 0),
    (835125411154232002, 835125411154231297, 'OWNER', NOW(), 0),
    (835125411154232003, 835125411154231299, 'OWNER', NOW(), 0),
    (835125411154232004, 835125411154231301, 'OWNER', NOW(), 0),
    (835125411154232005, 835125411154231298, 'OWNER', NOW(), 0),
    (835125411154232006, 835125411154231298, 'OWNER', NOW(), 0),
    (835125411154232007, 835125411154231300, 'OWNER', NOW(), 0),
    (835125411154232008, 835125411154231300, 'OWNER', NOW(), 0),
    (835125411154232001, 835125411154231302, 'PURCHASED', NOW(), 0),
    (835125411154232005, 835125411154231302, 'PURCHASED', NOW(), 0),
    (835125411154232007, 835125411154231298, 'PURCHASED', NOW(),0),
    (835125411154232008, 835125411154231297, 'PURCHASED', NOW(),0),
    (835125411154232003, 835125411154231300, 'PURCHASED', NOW(),0),
    (835125411154232004, 835125411154231299, 'ACCEPTED_AS_PAYMENT', NOW(),0);