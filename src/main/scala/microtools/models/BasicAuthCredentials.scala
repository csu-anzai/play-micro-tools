package microtools.models

import java.nio.charset.StandardCharsets
import java.util.Base64

case class BasicAuthCredentials(user: String, pass: String) {
  def asBase64String: String =
    Base64.getEncoder.encodeToString(s"$user:$pass".getBytes(StandardCharsets.UTF_8))
}
