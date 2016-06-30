package microtools.hateoas

import play.api.libs.json._

object JsonTransformers {
  def addHAL(json: JsValue, actions: Seq[BusinessAction])(
      implicit linkBuilder: LinkBuilder): JsValue = {
    if (actions.isEmpty) json
    else
      json.transform(jsonHAL(actions)) match {
        case JsSuccess(transformed, _) => transformed
        case _                         => json
      }
  }

  def jsonHAL(actions: Seq[BusinessAction])(
      implicit linkBuilder: LinkBuilder): Reads[JsObject] = {
    val links = JsObject(actions.map { action =>
      action.rel -> Json.toJson(linkBuilder.actionLink(action))
    })
    __.read[JsObject].map( o => o ++ JsObject(Seq("_links" -> links)))
  }
}
