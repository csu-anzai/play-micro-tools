package microtools.patch

object PatchOperation extends Enumeration {
  type Type = Value

  val ADD     = Value("add")
  val REMOVE  = Value("remove")
  val REPLACE = Value("replace")
  val MOVE    = Value("move")
  val COPY    = Value("copy")
  val TEST    = Value("test")
}
