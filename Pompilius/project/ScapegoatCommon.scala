import com.sksamuel.scapegoat.sbt.ScapegoatSbtPlugin.autoImport.{
  Scapegoat,
  scapegoatDisabledInspections,
  scapegoatIgnoredFiles
}
import sbt.Def
import sbt.Keys.scalacOptions

object ScapegoatCommon {

  lazy val scapegoatSettings: Seq[Def.Setting[_]] = Seq(
    scapegoatIgnoredFiles := Seq(
      ".*/.*template.scala",
      ".*/conf/routes/.*.routes",
      ".*/controllers/ReverseRoutes.scala",
      ".*/controllers/javascript/JavaScriptReverseRoutes.scala",
      ".*/models/.*.scala",
      ".*/router/Routes.scala",
      ".*/target/.*"
    ),
    scapegoatDisabledInspections := Seq(
      "FinalModifierOnCaseClass",
      "ListSize",
      "BigDecimalDoubleConstructor",
      "MaxParameters",
      "ListAppend",
      "VariableShadowing",
      "AsInstanceOf",
      "PartialFunctionInsteadOfMatch",
      "BooleanParameter"
    ),
    Scapegoat / scalacOptions += "-P:scapegoat:overrideLevels:TraversableHead=Warning:OptionGet=Warning"
  )

}
