package dev.pompilius.resource.domain.study

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait Area extends EnumEntry {
  def description: String
}

object Area extends Enum[Area] with PlayJsonEnum[Area] {

  val values: IndexedSeq[Area] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object GEOMORPHOLOGY extends Area {
    val description = "Geomorphology - Study of landforms and erosive processes"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEOPHYSICS extends Area {
    val description = "Geophysics - Application of physical methods to investigate the Earth's structure"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PETROLEUM_GEOLOGY extends Area {
    val description = "Petroleum Geology - Search and exploitation of hydrocarbons"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PETROLOGY extends Area {
    val description = "Petrology - Study of formation, composition and alteration of rocks"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MINERALOGY extends Area {
    val description = "Mineralogy - Study of minerals and crystals"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object STRATIGRAPHY extends Area {
    val description = "Stratigraphy - Study of rock layers or strata"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PALEONTOLOGY extends Area {
    val description = "Paleontology - Study of fossils and ancient life"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VOLCANOLOGY extends Area {
    val description = "Volcanology - Study of volcanoes and volcanic phenomena"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SEISMOLOGY extends Area {
    val description = "Seismology - Study of earthquakes and seismic waves"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEODYNAMICS extends Area {
    val description = "Geodynamics - Study of crustal movements and deformations"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HYDROGEOLOGY extends Area {
    val description = "Hydrogeology - Study of groundwater and aquifers"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ENVIRONMENTAL_GEOLOGY extends Area {
    val description = "Environmental Geology - Environmental impact of geological processes"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SEDIMENTOLOGY extends Area {
    val description = "Sedimentology - Study of sediments and sedimentary rocks"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEOMETALLURGY extends Area {
    val description = "Geometallurgy - Optimization of mineral extraction processes"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GEOTECHNICS extends Area {
    val description = "Geotechnics - Application of geology in civil engineering"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MARINE_GEOLOGY extends Area {
    val description = "Marine Geology - Study of geological processes in marine environments"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HISTORICAL_GEOLOGY extends Area {
    val description = "Historical Geology - Study of the geological history of the Earth"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object OTHER extends Area {
    val description = "Other - Other area of geological research"
  }

}

