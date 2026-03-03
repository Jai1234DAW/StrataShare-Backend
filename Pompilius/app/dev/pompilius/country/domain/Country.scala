package dev.pompilius.country.domain

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait Country extends EnumEntry{
  def fullName: String
}

object Country extends Enum[Country] with PlayJsonEnum[Country] {

  val values: IndexedSeq[Country] = findValues

  @SuppressWarnings(Array("ObjectNames"))
  case object AF extends Country {
    val fullName = "Afghanistan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AX extends Country {
    val fullName = "Åland Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AL extends Country {
    val fullName = "Albania"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DZ extends Country {
    val fullName = "Algeria"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AS extends Country {
    val fullName = "American Samoa"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AD extends Country {
    val fullName = "Andorra"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AO extends Country {
    val fullName = "Angola"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AI extends Country {
    val fullName = "Anguilla"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AQ extends Country {
    val fullName = "Antarctica"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AG extends Country {
    val fullName = "Antigua and Barbuda"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AR extends Country {
    val fullName = "Argentina"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AM extends Country {
    val fullName = "Armenia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AW extends Country {
    val fullName = "Aruba"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AU extends Country {
    val fullName = "Australia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AT extends Country {
    val fullName = "Austria"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AZ extends Country {
    val fullName = "Azerbaijan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BS extends Country {
    val fullName = "Bahamas"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BH extends Country {
    val fullName = "Bahrain"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BD extends Country {
    val fullName = "Bangladesh"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BB extends Country {
    val fullName = "Barbados"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BY extends Country {
    val fullName = "Belarus"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BE extends Country {
    val fullName = "Belgium"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BZ extends Country {
    val fullName = "Belize"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BJ extends Country {
    val fullName = "Benin"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BM extends Country {
    val fullName = "Bermuda"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BT extends Country {
    val fullName = "Bhutan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BO extends Country {
    val fullName = "Bolivia, Plurinational State of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BQ extends Country {
    val fullName = "Bonaire, Sint Eustatius and Saba"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BA extends Country {
    val fullName = "Bosnia and Herzegovina"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BW extends Country {
    val fullName = "Botswana"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BV extends Country {
    val fullName = "Bouvet Island"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BR extends Country {
    val fullName = "Brazil"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IO extends Country {
    val fullName = "British Indian Ocean Territory"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BN extends Country {
    val fullName = "Brunei Darussalam"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BG extends Country {
    val fullName = "Bulgaria"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BF extends Country {
    val fullName = "Burkina Faso"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BI extends Country {
    val fullName = "Burundi"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KH extends Country {
    val fullName = "Cambodia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CM extends Country {
    val fullName = "Cameroon"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CA extends Country {
    val fullName = "Canada"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CV extends Country {
    val fullName = "Cape Verde"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KY extends Country {
    val fullName = "Cayman Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CF extends Country {
    val fullName = "Central African Republic"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TD extends Country {
    val fullName = "Chad"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CL extends Country {
    val fullName = "Chile"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CN extends Country {
    val fullName = "China"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CX extends Country {
    val fullName = "Christmas Island"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CC extends Country {
    val fullName = "Cocos (Keeling) Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CO extends Country {
    val fullName = "Colombia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KM extends Country {
    val fullName = "Comoros"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CG extends Country {
    val fullName = "Congo"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CD extends Country {
    val fullName = "Congo, the Democratic Republic of the"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CK extends Country {
    val fullName = "Cook Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CR extends Country {
    val fullName = "Costa Rica"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CI extends Country {
    val fullName = "Côte d'Ivoire"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HR extends Country {
    val fullName = "Croatia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CU extends Country {
    val fullName = "Cuba"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CW extends Country {
    val fullName = "Curaçao"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CY extends Country {
    val fullName = "Cyprus"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CZ extends Country {
    val fullName = "Czech Republic"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DK extends Country {
    val fullName = "Denmark"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DJ extends Country {
    val fullName = "Djibouti"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DM extends Country {
    val fullName = "Dominica"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DO extends Country {
    val fullName = "Dominican Republic"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object EC extends Country {
    val fullName = "Ecuador"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object EG extends Country {
    val fullName = "Egypt"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SV extends Country {
    val fullName = "El Salvador"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GQ extends Country {
    val fullName = "Equatorial Guinea"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ER extends Country {
    val fullName = "Eritrea"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object EE extends Country {
    val fullName = "Estonia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ET extends Country {
    val fullName = "Ethiopia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FK extends Country {
    val fullName = "Falkland Islands (Malvinas)"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FO extends Country {
    val fullName = "Faroe Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FJ extends Country {
    val fullName = "Fiji"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FI extends Country {
    val fullName = "Finland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FR extends Country {
    val fullName = "France"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GF extends Country {
    val fullName = "French Guiana"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PF extends Country {
    val fullName = "French Polynesia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TF extends Country {
    val fullName = "French Southern Territories"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GA extends Country {
    val fullName = "Gabon"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GM extends Country {
    val fullName = "Gambia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GE extends Country {
    val fullName = "Georgia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object DE extends Country {
    val fullName = "Germany"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GH extends Country {
    val fullName = "Ghana"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GI extends Country {
    val fullName = "Gibraltar"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GR extends Country {
    val fullName = "Greece"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GL extends Country {
    val fullName = "Greenland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GD extends Country {
    val fullName = "Grenada"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GP extends Country {
    val fullName = "Guadeloupe"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GU extends Country {
    val fullName = "Guam"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GT extends Country {
    val fullName = "Guatemala"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GG extends Country {
    val fullName = "Guernsey"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GN extends Country {
    val fullName = "Guinea"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GW extends Country {
    val fullName = "Guinea-Bissau"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GY extends Country {
    val fullName = "Guyana"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HT extends Country {
    val fullName = "Haiti"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HM extends Country {
    val fullName = "Heard Island and McDonald Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VA extends Country {
    val fullName = "Holy See (Vatican City State)"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HN extends Country {
    val fullName = "Honduras"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HK extends Country {
    val fullName = "Hong Kong"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object HU extends Country {
    val fullName = "Hungary"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IS extends Country {
    val fullName = "Iceland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IN extends Country {
    val fullName = "India"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ID extends Country {
    val fullName = "Indonesia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IR extends Country {
    val fullName = "Iran, Islamic Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IQ extends Country {
    val fullName = "Iraq"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IE extends Country {
    val fullName = "Ireland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IM extends Country {
    val fullName = "Isle of Man"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IL extends Country {
    val fullName = "Israel"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object IT extends Country {
    val fullName = "Italy"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object JM extends Country {
    val fullName = "Jamaica"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object JP extends Country {
    val fullName = "Japan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object JE extends Country {
    val fullName = "Jersey"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object JO extends Country {
    val fullName = "Jordan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KZ extends Country {
    val fullName = "Kazakhstan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KE extends Country {
    val fullName = "Kenya"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KI extends Country {
    val fullName = "Kiribati"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KP extends Country {
    val fullName = "Korea, Democratic People's Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KR extends Country {
    val fullName = "Korea, Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KW extends Country {
    val fullName = "Kuwait"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KG extends Country {
    val fullName = "Kyrgyzstan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LA extends Country {
    val fullName = "Lao People's Democratic Republic"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LV extends Country {
    val fullName = "Latvia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LB extends Country {
    val fullName = "Lebanon"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LS extends Country {
    val fullName = "Lesotho"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LR extends Country {
    val fullName = "Liberia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LY extends Country {
    val fullName = "Libya"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LI extends Country {
    val fullName = "Liechtenstein"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LT extends Country {
    val fullName = "Lithuania"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LU extends Country {
    val fullName = "Luxembourg"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MO extends Country {
    val fullName = "Macao"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MK extends Country {
    val fullName = "Macedonia, the former Yugoslav Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MG extends Country {
    val fullName = "Madagascar"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MW extends Country {
    val fullName = "Malawi"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MY extends Country {
    val fullName = "Malaysia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MV extends Country {
    val fullName = "Maldives"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ML extends Country {
    val fullName = "Mali"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MT extends Country {
    val fullName = "Malta"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MH extends Country {
    val fullName = "Marshall Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MQ extends Country {
    val fullName = "Martinique"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MR extends Country {
    val fullName = "Mauritania"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MU extends Country {
    val fullName = "Mauritius"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object YT extends Country {
    val fullName = "Mayotte"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MX extends Country {
    val fullName = "Mexico"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object FM extends Country {
    val fullName = "Micronesia, Federated States of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MD extends Country {
    val fullName = "Moldova, Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MC extends Country {
    val fullName = "Monaco"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MN extends Country {
    val fullName = "Mongolia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ME extends Country {
    val fullName = "Montenegro"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MS extends Country {
    val fullName = "Montserrat"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MA extends Country {
    val fullName = "Morocco"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MZ extends Country {
    val fullName = "Mozambique"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MM extends Country {
    val fullName = "Myanmar"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NA extends Country {
    val fullName = "Namibia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NR extends Country {
    val fullName = "Nauru"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NP extends Country {
    val fullName = "Nepal"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NL extends Country {
    val fullName = "Netherlands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NC extends Country {
    val fullName = "New Caledonia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NZ extends Country {
    val fullName = "New Zealand"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NI extends Country {
    val fullName = "Nicaragua"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NE extends Country {
    val fullName = "Niger"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NG extends Country {
    val fullName = "Nigeria"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NU extends Country {
    val fullName = "Niue"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NF extends Country {
    val fullName = "Norfolk Island"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MP extends Country {
    val fullName = "Northern Mariana Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object NO extends Country {
    val fullName = "Norway"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object OM extends Country {
    val fullName = "Oman"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PK extends Country {
    val fullName = "Pakistan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PW extends Country {
    val fullName = "Palau"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PS extends Country {
    val fullName = "Palestinian Territory, Occupied"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PA extends Country {
    val fullName = "Panama"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PG extends Country {
    val fullName = "Papua New Guinea"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PY extends Country {
    val fullName = "Paraguay"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PE extends Country {
    val fullName = "Peru"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PH extends Country {
    val fullName = "Philippines"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PN extends Country {
    val fullName = "Pitcairn"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PL extends Country {
    val fullName = "Poland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PT extends Country {
    val fullName = "Portugal"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PR extends Country {
    val fullName = "Puerto Rico"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object QA extends Country {
    val fullName = "Qatar"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object RE extends Country {
    val fullName = "Réunion"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object RO extends Country {
    val fullName = "Romania"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object RU extends Country {
    val fullName = "Russian Federation"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object RW extends Country {
    val fullName = "Rwanda"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object BL extends Country {
    val fullName = "Saint Barthélemy"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SH extends Country {
    val fullName = "Saint Helena, Ascension and Tristan da Cunha"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object KN extends Country {
    val fullName = "Saint Kitts and Nevis"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LC extends Country {
    val fullName = "Saint Lucia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object MF extends Country {
    val fullName = "Saint Martin (French part)"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object PM extends Country {
    val fullName = "Saint Pierre and Miquelon"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VC extends Country {
    val fullName = "Saint Vincent and the Grenadines"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object WS extends Country {
    val fullName = "Samoa"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SM extends Country {
    val fullName = "San Marino"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ST extends Country {
    val fullName = "Sao Tome and Principe"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SA extends Country {
    val fullName = "Saudi Arabia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SN extends Country {
    val fullName = "Senegal"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object RS extends Country {
    val fullName = "Serbia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SC extends Country {
    val fullName = "Seychelles"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SL extends Country {
    val fullName = "Sierra Leone"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SG extends Country {
    val fullName = "Singapore"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SX extends Country {
    val fullName = "Sint Maarten (Dutch part)"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SK extends Country {
    val fullName = "Slovakia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SI extends Country {
    val fullName = "Slovenia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SB extends Country {
    val fullName = "Solomon Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SO extends Country {
    val fullName = "Somalia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ZA extends Country {
    val fullName = "South Africa"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GS extends Country {
    val fullName = "South Georgia and the South Sandwich Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SS extends Country {
    val fullName = "South Sudan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ES extends Country {
    val fullName = "Spain"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object LK extends Country {
    val fullName = "Sri Lanka"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SD extends Country {
    val fullName = "Sudan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SR extends Country {
    val fullName = "SurifullName"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SJ extends Country {
    val fullName = "Svalbard and Jan Mayen"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SZ extends Country {
    val fullName = "Swaziland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SE extends Country {
    val fullName = "Sweden"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object CH extends Country {
    val fullName = "Switzerland"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object SY extends Country {
    val fullName = "Syrian Arab Republic"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TW extends Country {
    val fullName = "Taiwan, Province of China"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TJ extends Country {
    val fullName = "Tajikistan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TZ extends Country {
    val fullName = "Tanzania, United Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TH extends Country {
    val fullName = "Thailand"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TL extends Country {
    val fullName = "Timor-Leste"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TG extends Country {
    val fullName = "Togo"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TK extends Country {
    val fullName = "Tokelau"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TO extends Country {
    val fullName = "Tonga"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TT extends Country {
    val fullName = "Trinidad and Tobago"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TN extends Country {
    val fullName = "Tunisia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TR extends Country {
    val fullName = "Turkey"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TM extends Country {
    val fullName = "Turkmenistan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TC extends Country {
    val fullName = "Turks and Caicos Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object TV extends Country {
    val fullName = "Tuvalu"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object UG extends Country {
    val fullName = "Uganda"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object UA extends Country {
    val fullName = "Ukraine"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object AE extends Country {
    val fullName = "United Arab Emirates"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object GB extends Country {
    val fullName = "United Kingdom"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object US extends Country {
    val fullName = "United States"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object UM extends Country {
    val fullName = "United States Minor Outlying Islands"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object UY extends Country {
    val fullName = "Uruguay"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object UZ extends Country {
    val fullName = "Uzbekistan"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VU extends Country {
    val fullName = "Vanuatu"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VE extends Country {
    val fullName = "Venezuela, Bolivarian Republic of"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VN extends Country {
    val fullName = "Viet Nam"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VG extends Country {
    val fullName = "Virgin Islands, British"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object VI extends Country {
    val fullName = "Virgin Islands, U.S."
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object WF extends Country {
    val fullName = "Wallis and Futuna"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object EH extends Country {
    val fullName = "Western Sahara"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object YE extends Country {
    val fullName = "Yemen"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ZM extends Country {
    val fullName = "Zambia"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object ZW extends Country {
    val fullName = "Zimbabwe"
  }

  @SuppressWarnings(Array("ObjectNames"))
  case object XX extends Country {
    val fullName = "Other"
  }

  val default: Country = ES

}
