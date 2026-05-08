# Cargado de datos completa

# ---!Ups
-- Recursos de María (835125411154231297)
INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
(835125411154232009, 'Pumice from Mount St. Helens eruption', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
 'Mount St. Helens, Washington, USA',
 'Highly vesicular pumice from 2000 eruption. Well-preserved pyroclastic texture.',
 'Dacitic pumice from plinian eruption', 40, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `collected_date`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
    (835125411154233005, 835125411154232009,
     '2023-10-15','Plagioclase, Hornblende, Biotite, Volcanic glass',
     'Surface collection from pyroclastic flow deposit. Sample documented with GPS.',
     0, 'Pumice fragment',
     'Sample bags, photographic documentation, GPS',
     'Pyroclastic',
     'Explosive volcanic eruption, magma fragmentation, rapid cooling');

INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
    (835125411154232010, 'Limestone with trilobite fossils', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
     'Burgess Shale, British Columbia, Canada',
     'Middle Cambrian limestone with exceptionally preserved trilobite fossils.',
     'Fossiliferous limestone with arthropod fauna', 0.00, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `collected_date`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
    (835125411154233006, 835125411154232010, '2025-2-25',
     'Calcite (90%), Fossil fragments (8%), Clay minerals (2%)',
     'Careful extraction from shale bed. Fossil orientation preserved.',
     0, 'Fossiliferous rock specimen',
     'Hammer, chisel, protective wrapping for fossils',
     'Sedimentary limestone',
     'Marine sedimentation, fossil burial, diagenesis, exceptional preservation');


-- Recursos de John (835125411154231299)
INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
      (835125411154232011, 'Eclogite from Alpine orogen', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Western Alps, Italy',
       'High-pressure metamorphic rock with garnet and omphacite.',
       'Eclogite from subducted oceanic crust', 28.00, 0),

      (835125411154232012, 'Quartzite with deformation bands', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Appalachian Mountains, Pennsylvania, USA',
       'Metamorphosed sandstone showing ductile deformation structures.',
       'Deformed quartzite from orogenic belt', 0, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `collected_date`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
      (835125411154233007, 835125411154232011,
       '2023-8-8','Garnet (50%), Omphacite (40%), Rutile (5%), Quartz (5%)',
       'Outcrop sampling from blueschist-eclogite transition zone.',
       0, 'Oriented hand sample',
       'Hammer, compass clinometer, orientation marker',
       'High-pressure metamorphic',
       'Subduction zone metamorphism, high-pressure recrystallization, eclogite facies'),

      (835125411154233008, 835125411154232012,
       '2024-3-3', 'Quartz (95%), Minor mica (3%), Iron oxides (2%)',
       'Block sample showing deformation bands. Structural measurements taken.',
       0, 'Oriented structural sample',
       'Rock hammer, compass, structural notebook',
       'Metamorphic quartzite',
       'Regional metamorphism, ductile deformation, grain boundary migration');


-- Recursos de Ana (835125411154231300) - Ya tiene 2 studies, agregar 1 sample
INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
    (835125411154232013, 'Shale with graptolite fossils', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
     'Lake District, England',
     'Ordovician black shale with well-preserved graptolite colonies.',
     'Fossiliferous shale with planktonic fauna', 0.0, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `collected_date`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
    (835125411154233009, 835125411154232013,
     '2024-9-9','Clay minerals (70%), Quartz (20%), Pyrite (5%), Organic matter (5%)',
     'Bedding-parallel sample from black shale sequence. Fossils visible on surface.',
     0, 'Fossiliferous shale',
     'Hammer, sample bags, photographic documentation',
     'Sedimentary shale',
     'Anoxic marine deposition, organic matter preservation, graptolite accumulation');


-- Recursos de Pedro (835125411154231301)
INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
      (835125411154232014, 'Amethyst geode cluster', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Rio Grande do Sul, Brazil',
       'Purple amethyst crystals in geode cavity. Crystals up to 5cm length.',
       'Amethyst quartz crystal geode', 45.00, 0),

      (835125411154232015, 'Calcite dog-tooth crystals', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Elmwood Mine, Tennessee, USA',
       'Clear calcite crystals showing scalenohedral habit.',
       'Hydrothermal calcite mineralization', 0, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `collected_date`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
      (835125411154233010, 835125411154232014,
       '2022-5-25', 'Quartz (amethyst variety) (98%), Minor iron compounds (2%)',
       'Extracted from basalt geode. Cleaned with soft brush and water.',
       1, 'Mineral specimen',
       'Hand tools, soft brushes, protective wrapping',
       'Quartz crystals',
       'Hydrothermal fluid circulation, silica precipitation, iron impurity incorporation'),

      (835125411154233011, 835125411154232015,
       '2020-5-8','Calcite (CaCO3) (95%), Minor sphalerite (3%), Trace fluorite (2%)',
       'Collected from mine dump. Crystal terminations well-preserved.',
       0, 'Mineral specimen',
       'Hand collection, protective padding',
       'Calcite crystals',
       'Hydrothermal mineralization, Mississippi Valley-type deposit, carbonate precipitation');


-- Recursos de Laura (835125411154231302) - Se agregarán 3
INSERT INTO `resource` (
    `id`, `name`, `resource_type`, `visibility`, `created`, `updated`,
    `location`, `observations`, `summary`, `price`, `is_barter`
) VALUES
      (835125411154232016, 'Tourmaline crystal in pegmatite', 'SAMPLE', 'PRIVATE', NOW(), NOW(),
       'Mount Mica, Maine, USA',
       'Black tourmaline (schorl) crystal with well-developed striations.',
       'Tourmaline in granitic pegmatite', 52.00, 0),

      (835125411154232017, 'Garnet crystal aggregate', 'SAMPLE', 'PUBLIC', NOW(), NOW(),
       'Wrangell, Alaska, USA',
       'Almandine garnet crystals in schist matrix.',
       'Metamorphic garnet porphyroblasts', 0, 0),

      (835125411154232018, 'Gemstone mineral characterization study', 'STUDY', 'PRIVATE', NOW(), NOW(),
       'Mogok Valley, Myanmar',
       'Comprehensive gemological study of ruby and sapphire deposits.',
       'Corundum gemstone geology and formation', 275.00, 0);

INSERT INTO `sample` (
    `id`, `resource_id`, `collected_date`, `minerals`, `collection_methods`,
    `is_fresh`, `sample_type`, `materials_used`, `sample_category`, `geological_processes`
) VALUES
      (835125411154233012, 835125411154232016,
       '2019-8-8','Tourmaline (schorl) (60%), Quartz (25%), Albite (10%), Muscovite (5%)',
       'Specimen from pegmatite pocket. Crystal measures 8cm in length.',
       1, 'Mineral in matrix',
       'Hammer, chisel, protective wrapping',
       'Granitic pegmatite',
       'Late-stage pegmatitic crystallization, boron enrichment, tourmaline formation'),

      (835125411154233013, 835125411154232017,
       '2013-9-6', 'Garnet (almandine) (70%), Muscovite (20%), Quartz (8%), Staurolite (2%)',
       'Hand sample with multiple garnet porphyroblasts up to 3cm diameter.',
       0, 'Metamorphic sample',
       'Geological hammer, sample bag',
       'Garnet-mica schist',
       'Regional metamorphism, garnet porphyroblast growth, Barrovian sequence');

INSERT INTO `study` (
    `id`, `resource_id`, `start_date`, `end_date`,
    `description`, `coordinates`, `area`, `methods`, `authors`,
    `section`, `antecedents`, `name_section`
) VALUES
    (835125411154234005, 835125411154232018,
     '2022-08-01 00:00:00', '2024-05-30 00:00:00',
     'Detailed gemological and geological study of ruby and sapphire deposits in Mogok Valley. Analysis of 200+ samples using gemological microscopy, FTIR, UV-Vis spectroscopy, and inclusion studies.',
     '{"lat": 22.9167, "lon": 96.5167, "elevation_m": 1200}',
     'MINERALOGY',
     'Gemological microscopy, FTIR spectroscopy, UV-Vis absorption, LA-ICP-MS trace elements, fluid inclusion analysis',
     '["Laura Martínez", "Dr. Robert Weldon", "Dr. Ken Scarratt"]',
     1, 1, 'Gemstone formation and treatment identification');


-- ============================================================================
-- RESOURCE_USER (Relaciones de propiedad adicionales)
-- ============================================================================
INSERT INTO `resource_user` (`resource_id`, `user_id`, `resource_user_type`, `created`, `deleted`) VALUES
-- María dueña de pumice
(835125411154232009, 835125411154231297, 'OWNER', NOW(), 0),

-- Carlos dueño de limestone con trilobites
(835125411154232010, 835125411154231298, 'OWNER', NOW(), 0),

-- John dueño de eclogite y quartzite
(835125411154232011, 835125411154231299, 'OWNER', NOW(), 0),
(835125411154232012, 835125411154231299, 'OWNER', NOW(), 0),

-- Ana dueña de shale con graptolites
(835125411154232013, 835125411154231300, 'OWNER', NOW(), 0),

-- Pedro dueño de amethyst y calcite
(835125411154232014, 835125411154231301, 'OWNER', NOW(), 0),
(835125411154232015, 835125411154231301, 'OWNER', NOW(), 0),

-- Laura dueña de tourmaline, garnet y study
(835125411154232016, 835125411154231302, 'OWNER', NOW(), 0),
(835125411154232017, 835125411154231302, 'OWNER', NOW(), 0),
(835125411154232018, 835125411154231302, 'OWNER', NOW(), 0);


