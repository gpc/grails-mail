package org.example

class TestService {
    def sendConfirmation() {
        def thread = Thread.start {
            sendMail {
                to "john@nowhere.net"
                subject "Confirmation"
                body view: "/mail/confirmation", model: [ name: "Peter" ]
            }
        }

        thread.join()
    }
}
