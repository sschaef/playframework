package sbt

import org.scalatools.testing.{ Event => TEvent, Result => TResult }

private[sbt] class PlayTestListener extends TestsListener {

  private var skipped, errors, passed, failures = 0

  private def count(event: TEvent) {
    event.result match {
      case TResult.Error => errors += 1
      case TResult.Success => passed += 1
      case TResult.Failure => failures += 1
      case TResult.Skipped => skipped += 1
    }
  }

  private def playReport(messageName: String, attributes: (String, String)*) {
    result.append("<li>" + messageName + " " + attributes.map {
      case (k, v) => k + ": " + tidy(v)
    }.mkString(" ") + "</li>")
  }

  val result = new collection.mutable.ListBuffer[String]

  def doComplete(finalResult: TestResult.Value) {
    val totalCount = failures + errors + skipped + passed
    val postfix = "Total " + totalCount + ", Failed " + failures + ", Errors " + errors + ", Passed " + passed + ", Skipped " + skipped
    result.append("</ul>")
    result.append("<p>" + postfix + "</p>")
  }

  def doInit {
    result.append("<p>Executing Test suit</p>")
    result.append("<ul>")
    failures = 0
    errors = 0
    passed = 0
    skipped = 0
  }

  /** called for each class or equivalent grouping */
  def startGroup(name: String) {
    playReport("test", "started" -> name)
  }

  /** called for each test method or equivalent */
  def testEvent(event: TestEvent) {

    event match {
      case e =>
        for (d <- e.detail) {
          event.detail.foreach(count)
          d match {
            case te: TEvent =>
              te.result match {
                case TResult.Success => playReport("test case", "finished, result" -> event.result.get.toString)
                case TResult.Error | TResult.Failure =>
                  playReport("test", "failed" -> te.testName, "details" -> (te.error.toString +
                    "\n" + te.error.getStackTrace.mkString("\n at ", "\n at ", "")))
                case TResult.Skipped =>
                  playReport("test", "ignored" -> te.testName)
              }
          }
        }
    }

  }

  /** called if there was an error during test */
  def endGroup(name: String, t: Throwable) {}
  /** called if test completed */
  def endGroup(name: String, result: TestResult.Value) {}

  def tidy(s: String) = s
    .replace("|", "||")
    .replace("'", "|'")
    .replace("\n", "|n")
    .replace("\r", "|r")
    .replace("\u0085", "|x")
    .replace("\u2028", "|l")
    .replace("\u2029", "|p")
    .replace("[", "|[")
    .replace("]", "|]")

}