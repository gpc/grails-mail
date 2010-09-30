package org.example

class TestController {
    def testService

    def index = {
        testService.sendConfirmation()
        render "OK"
    }
}
