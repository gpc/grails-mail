import geb.spock.GebReportingSpec
import spock.lang.Issue


class MainSpec extends GebReportingSpec {

    @Issue("GRAILSPLUGINS-1885")
    def "Mail templates can be used on non-request threads"() {
        when: "I call a service method that generates an e-mail from a GSP template"
        go "test/index"
        report "index"

        then: "The index page is displayed"
        $().text() == "OK"

        when: "I look at the Greenmail page"
        go "greenmail/index"
        report "greenmail"

        then: "I see the sent mail"
        $("body h2").text() == "Email List"
        $("body tbody").find("tr").size() == 1
        $("body tbody").find("tr", 0).find("td", 2).text() == "Confirmation"
    }
}
